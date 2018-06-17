package bgu.spl181.net.api;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;

import bgu.spl181.net.api.bidi.BidiMessagingProtocol;
import bgu.spl181.net.srv.bidi.Locks;
import bgu.spl181.net.srv.bidi.Movie;
import bgu.spl181.net.srv.bidi.User;
import bgu.spl181.net.srv.bidi.Pair;
/**
 * This class represents the Movie rental protocol it supports bidirectional communication as described in BidiMessaging interface
 * and extends the middle UserServiceTexBasedProtocol.It provides  service for rental of movies.It supports of 4 main commands login register signout and different requests 
 * for movies.It keeps data structure of Movies and their details and priority queue of ids of movies.The priority queue ensures each time we add a movie it is assigned with id.max+1
 * Login and signout are treated completley in the USTBP as it holds the users database,this commands are generic for all possible service protocols.Request and Register is treated in both protocols as "layers model"
 * Each function in this class treats a command and update the databases and Json files in system.
 * We use ReadWritelocks in order to sync the system to work correctly with updates and functionality . 
 *
 */
public class MovieRentalServiceProtocol extends UserServiceTextBasedProtocol implements BidiMessagingProtocol<String> {

	private ConcurrentHashMap<String, Movie> movies = new ConcurrentHashMap<>();// map of all movies and their data
	private PriorityQueue<Integer> movieIdHeap;

    /**
     * The constructor initailizes the movies and movieheap fields it holds and USTBP protocol passing to it user databases and ReadWrite Locks that are used.
     * All parameters are references we get from the server.
     * @param locks-Readwrite locks that are used
     * @param movieIdHeap-movies ids database
     * @param userData-users database
     * @param userStates-logged in users
     * @param movieData-movies database
     */
	public MovieRentalServiceProtocol(Locks locks,PriorityQueue<Integer> movieIdHeap, ConcurrentHashMap<String, User> userData,
			ConcurrentHashMap<String, Integer> userStates, ConcurrentHashMap<String, Movie> movieData) {
		super(locks,userData, userStates);
		this.movies = movieData;
		this.movieIdHeap = movieIdHeap;
	}


	@Override
	/**
	 * This function treats the register command after it was partly treated in USTBP.
	 * It checks the optional datablock inserted and it should be country="name of movie" and updates the databases in the end
	 * USTBP register function calls this function if no errors found in command provided (username and password),else it sends error to client.
	 * This function return boolean indicator to the caller to indicate if command succeded ,else send an error message.
	 * Error messages are all sent from USTBP.
	 */
	protected boolean continueRegister(String username, String password, String data) {
		if (data.contains("country=")) {
			String country = data.substring(9, data.length() - 1);
			locks.getUsersDataLock().writeLock().lock();
			usersData.put(username, new User(username, "normal", password, country, new LinkedList<Pair>(), "0"));
			locks.getUsersDataLock().writeLock().unlock();
			updateUsers(usersData);
			return true;
		}
		locks.getUsersDataLock().writeLock().lock();
		usersData.remove(username);
		locks.getUsersDataLock().writeLock().unlock();
		locks.getUsersStatesLock().writeLock().lock();
		usersStates.remove(username);	
		locks.getUsersStatesLock().writeLock().unlock();
		return false;
	}

	@Override
	/**
	 * This method gets the params of Request command and calls the needed function to treat the request.
	 * Is called by USTBP.
	 */
	protected boolean continueRequest(String name,String param){		
		if(name.equals("balance"))
			return balance(param);
		else if(name.equals("info"))
			return info(param);
		else if(name.equals("rent"))
			return rent(param);
		else if(name.equals("return"))
			return returnc(param);
		else if(name.equals("addmovie"))
			return addmovie(param);
		else if(name.equals("remmovie"))
			return remmovie(param);
		else if(name.equals("changeprice"))
			return changeprice(param);
		else
			return false;
	}
    /**
     * This method returns the balance user has or adds balance to user
     * Is called by USTBP 
     * Updates the database
     * @param parameters
     * @return indicator if command succeeded.
     */
	private boolean balance(String parameters) {
		if (parameters.equals("info"))
			connections.send(connectionId, "ACK balance " + usersData.get(username).getBalance());
		else if (parameters.startsWith("add")) {
			int toAdd = Integer.parseInt(parameters.substring(4));
			User u = usersData.get(username);
			u.setBalance(u.getBalance() + toAdd);
			updateUsers(usersData);
			connections.send(connectionId, "ACK balance " + u.getBalance() + " added " + toAdd);
		}
		return true;
	}
    /**
     * This method returns list of movies or movie information.
     * Updates the databases
     * @param parameters name of movie
     * @return indicator if command succeeded
     */
	private boolean info(String parameters) {
		if (parameters.equals("")) {
			String movieList = "";
			for (ConcurrentHashMap.Entry<String, Movie> entry : movies.entrySet())
				movieList = movieList + " " + "\""+entry.getKey()+"\"";
			connections.send(connectionId, "ACK info" + movieList);
			return true;
		} else {
			String movieName = parameters.substring(1,parameters.length()-1);
			locks.getMoviesDataLock().readLock().lock();
			if (movies.containsKey(movieName)) {
				connections.send(connectionId, "ACK info " + movies.get(movieName).toString());
				locks.getMoviesDataLock().readLock().unlock();
				return true;
			}
			else {
				locks.getMoviesDataLock().readLock().unlock();
				return false;
			}
		}
	}
    /**
     * This method rents a movie from database,
     * updates the balance in user and the movie it rents and decreases the num of copies of movie in one.
     * Sends broadcast with movie details.
     * 
     * @param args movie name
     * @return boolean indicator if command succeeded
     */
	private boolean rent(String args) {
		String movieName = args.substring(1, args.length()-1);;
		locks.getMoviesDataLock().writeLock().lock();
		locks.getUsersDataLock().writeLock().lock();
		if (movies.containsKey(movieName))
			if (usersData.get(username).getBalance() >= movies.get(movieName).getPrice())
				if (movies.get(movieName).getAvailableAmount() > 0)
					if (movies.get(movieName).getBannedCountries().contains(usersData.get(username).getCountry()) == false)
						if (usersData.get(username).getMovies().contains(movieName) == false) {
							usersData.get(username).addMovie(movies.get(movieName));
							usersData.get(username).setBalance(
									usersData.get(username).getBalance() - movies.get(movieName).getPrice());
							movies.get(movieName).setAvailableAmount(movies.get(movieName).getAvailableAmount() - 1);
							connections.send(connectionId, "ACK rent "  + "\""+movieName+ "\"" + " success");
							this.broadcast(
									"BROADCAST movie " + "\""+movieName+ "\"" + " " + movies.get(movieName).getAvailableAmount()
											+ " " + movies.get(movieName).getPrice());
							locks.getMoviesDataLock().writeLock().unlock();
							locks.getUsersDataLock().writeLock().unlock();
							updateUsers(usersData);
							updateMovies(movies);
							return true;
						}
		locks.getMoviesDataLock().writeLock().unlock();
		locks.getUsersDataLock().writeLock().unlock();
		return false;
	}
    /**
     * 
     * This method returns movie from user updates the copies+1 of movie and updates users and movies databases
     * Broadcast is sent with details of movie.
     * @param args movie name
     * @return boolean indicator if command succeeded
     */
	private boolean returnc(String args) {
		String movieName = args.substring(1, args.length()-1);
		locks.getMoviesDataLock().writeLock().lock();
		locks.getUsersDataLock().writeLock().lock();
		if (movies.containsKey(movieName) == true)
			if (usersData.get(username).getMovies().contains(movieName)) {
				usersData.get(username).removeMovie(movies.get(movieName));
				movies.get(movieName).setAvailableAmount(movies.get(movieName).getAvailableAmount() + 1);
				connections.send(connectionId, "ACK return " + "\""+movieName+ "\"" +" success");
				this.broadcast("BROADCAST movie " + "\""+movieName+ "\"" + " " + movies.get(movieName).getAvailableAmount() + " "
						+ movies.get(movieName).getPrice());
				locks.getMoviesDataLock().writeLock().unlock();
				locks.getUsersDataLock().writeLock().unlock();
				updateUsers(usersData);
				updateMovies(movies);
				return true;
			}
		locks.getMoviesDataLock().writeLock().unlock();
		locks.getUsersDataLock().writeLock().unlock();
		return false;
	}
    /**
     * This method adds movie to database .Only admin user can do so,otherwise error sent.
     *Broadcast for movie is sent Databases are updated 
     * @param msg movie name
     * @return boolean indicator if command succeeded
     */
	private boolean addmovie(String msg) {
		if (usersData.get(username).getType().equals("admin")) {
			String movieName = msg.substring(1, msg.indexOf("\" "));
			String rest = msg.substring(msg.indexOf("\" ") + 2);
			int amount = Integer.parseInt(rest.substring(0, rest.indexOf(' ')));
			rest = rest.substring(rest.indexOf(' ') + 1);
			int price;
			LinkedList<String> bannedCountries = new LinkedList<>();
			if(rest.indexOf(' ')!=-1) {
				price = Integer.parseInt(rest.substring(0, rest.indexOf(' ')));
				rest = rest.substring(rest.indexOf(' ') + 1);	
				rest=rest+" ";
				while (rest.equals("")==false) {
					String country=rest.substring(1,rest.indexOf("\" "));
					rest=rest.substring(rest.indexOf("\" ")+2);
					bannedCountries.add(country);
				}
			}
			else {
				price = Integer.parseInt(rest);
			}
			locks.getMoviesDataLock().writeLock().lock();
			if (!movies.containsKey(movieName) && amount > 0 && price > 0) {
				movies.put(movieName, new Movie(String.valueOf(movieIdHeap.peek() + 1), movieName,
						String.valueOf(price), bannedCountries, String.valueOf(amount), String.valueOf(amount)));
				locks.getHeapLock().writeLock().lock();
				movieIdHeap.add(movieIdHeap.peek() + 1);
				locks.getHeapLock().writeLock().unlock();
				connections.send(connectionId, "ACK addmovie " + "\""+movieName + "\""+ " success");
				this.broadcast("BROADCAST movie " + "\""+ movieName+ "\""+ " " + movies.get(movieName).getAvailableAmount() + " "
						+ movies.get(movieName).getPrice());
				locks.getMoviesDataLock().writeLock().unlock();
				updateMovies(movies);
				return true;
			}
			locks.getMoviesDataLock().writeLock().unlock();
		}
		return false;
	}
    /**
     * This method removes a movie from database can be done only by admin.
     * Sends Broadcast of movie and update databases.
     * @param msg movie name
     * @return boolean indicator if command succeeded
     */
	private boolean remmovie(String msg) {
		if (usersData.get(username).getType().equals("admin")) {
			String movieName = msg.substring(1, msg.length()-1);
			locks.getMoviesDataLock().writeLock().lock();
			if (movies.containsKey(movieName) == true
					&& movies.get(movieName).getAvailableAmount() == movies.get(movieName).getTotalAmount()) {
				locks.getHeapLock().writeLock().lock();
				movieIdHeap.remove(movies.get(movieName).getId());
				locks.getHeapLock().writeLock().unlock();
				movies.remove(movieName);
				connections.send(connectionId, "ACK remmovie " + "\""+ movieName + "\""+ " success");
				this.broadcast("BROADCAST movie " + "\""+ movieName + "\""+ " removed");
				locks.getMoviesDataLock().writeLock().unlock();
				updateMovies(movies);
				return true;
			}
			locks.getMoviesDataLock().writeLock().unlock();
		}
		return false;
	}
     /**
      * This method changes price of movie can be done only by admin
      * Sends Broadcast of movie and updates databases.
      * @param msg price to change
      * @return boolean indicator if command succeeded
      */
	private boolean changeprice(String msg) {
		if (usersData.get(username).getType().equals("admin")) {
			String movieName = msg.substring(1, msg.lastIndexOf(' ')-1);
			int price = Integer.parseInt(msg.substring(msg.lastIndexOf(' ') + 1));
			locks.getMoviesDataLock().writeLock().lock();
			if (movies.containsKey(movieName) && price > 0) {
				movies.get(movieName).setPrice(price);
				connections.send(connectionId, "ACK changeprice " + "\""+ movieName + "\""+ " success");
				this.broadcast("BROADCAST movie " + "\""+ movieName + "\""+ " " + movies.get(movieName).getAvailableAmount() + " "
						+ movies.get(movieName).getPrice());
				locks.getMoviesDataLock().writeLock().unlock();
				updateMovies(movies);
				return true;
			}
			locks.getMoviesDataLock().writeLock().unlock();
		}
		return false;
	}
    /**
     * This method updates the Json database stringifying the usersdatabase we keep to Json
     * @param userData 
     */
	private void updateUsers(ConcurrentHashMap<String, User> userData) {
		locks.getUsersJsonLock().writeLock().lock();
		LinkedList<User> users = new LinkedList<User>(userData.values());
		Gson gson = new Gson();
		HashMap<String, LinkedList<User>> out = new HashMap<>();
		out.put("users", users);
		try (FileWriter writer = new FileWriter("Database/Users.json")) {
			gson.toJson(out, writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		locks.getUsersJsonLock().writeLock().unlock();
	}
	/**
     * This method updates the Json database stringifying the moviesdatabase we keep to Json
     * @param movieData 
     */
	private void updateMovies(ConcurrentHashMap<String, Movie> movieData) {
		locks.getMoviesJsonLock().writeLock().lock();
		LinkedList<Movie> movies = new LinkedList<Movie>(movieData.values());
		Gson gson = new Gson();
		HashMap<String, LinkedList<Movie>> out = new HashMap<>();
		out.put("movies", movies);
		try (FileWriter writer = new FileWriter("Database/Movies.json")) {
			gson.toJson(out, writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		locks.getMoviesJsonLock().writeLock().unlock();
	}
	
}
