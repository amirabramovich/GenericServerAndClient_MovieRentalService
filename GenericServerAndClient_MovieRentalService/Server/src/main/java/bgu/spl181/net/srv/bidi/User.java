package bgu.spl181.net.srv.bidi;

import java.util.LinkedList;
/**
 * This class represents User object as described in UsersJson
 * We parse each user to this object
 * @author chen
 *
 */
public class User {
	private String username;
	private String type;
	private String password;
	private String country;
	private LinkedList<Pair> movies;
	private String balance;
	/**
	 * Constructor
	 * @param username
	 * @param type
	 * @param password
	 * @param country
	 * @param movies
	 * @param balance
	 */
	public User(	 
			String username,
			String type,
			String password,
			String country,
			LinkedList<Pair> movies,
			String balance) {
		this.username=username;
		this.type=type;
		this.password=password;
		this.country=country;
		this.movies=movies;
		this.balance=balance;	
	}
	
	public User(String username,String password) {
		this.username=username;
		this.password=password;
	}
    /**
     * This method adds movie to user 
     * @param movie to add 
     */
	public void addMovie(Movie movie) {
		movies.add(new Pair(movie.getId(),movie.getName()));
	}
	/**
	 * This method removes movie from user
	 * @param movie to remove
	 */
	public void removeMovie(Movie movie) {
		Pair tmp=null;
		for(Pair m:movies){
			if(m.getName().equals(movie.getName())){
				tmp=m;	
			}
		}
		movies.remove(tmp);
	}
	/**
	 * This method sets balance to user
	 * @param balance
	 */
	public void setBalance(Integer balance) {
		this.balance= String.valueOf(balance);
	}
	/**
	 * This method return username
	 * @return name
	 */
	public String getUsername() {
		return username;
	}
	/**
	 * This method returns country
	 * @return country
	 */
	public String getCountry() {
		return country;
	}
	/**
	 * This method returns balance of user
	 * @return
	 */
	public Integer getBalance() {
		return Integer.parseInt(balance);
	}
	/**
	 * This method returns type of user
	 * @return type
	 */
	public String getType() {
		return type;
	}
	/**
	 * This method returns password
	 * @return password
	 */
	public String getPassword() {
		return password;
	}
	/**
	 * This method returns list of movies
	 * @return list of movies
	 */
	public LinkedList<String> getMovies() {
		LinkedList<String> output=new LinkedList<>();
		for(Pair p:movies)
			output.add(p.getName());
		return output;
	}
}
