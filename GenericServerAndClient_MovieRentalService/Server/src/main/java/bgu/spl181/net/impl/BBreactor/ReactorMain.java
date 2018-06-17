package bgu.spl181.net.impl.BBreactor;

import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import bgu.spl181.net.api.MovieRentalEncoderDecoder;
import bgu.spl181.net.api.MovieRentalServiceProtocol;
import bgu.spl181.net.api.bidi.ConnectionImpl;
import bgu.spl181.net.srv.bidi.Locks;
import bgu.spl181.net.srv.bidi.MaxHeapComp;
import bgu.spl181.net.srv.bidi.Movie;
import bgu.spl181.net.srv.bidi.User;

/**
 * This class runs a reactor server parses the Json files to databases
 * and keeps all databases of system .userstates holds mapping to user name to its state of login,userdata holds mapping to user name to its user details
 * moviedata holds maping from movie name to its movie details movieheap holds max id of movie 
 * Locks to lock databases and Gson to parse Json files
 */
public class ReactorMain{
		
	public static void main(String[] args) {
    	ConcurrentHashMap<String,Integer> userStates=new ConcurrentHashMap<>(); //map of all registered users and their state
    	ConcurrentHashMap<String,User> userData=new ConcurrentHashMap<>();//map of all registered users and their password
    	ConcurrentHashMap<String,Movie> movieData=new ConcurrentHashMap<>();//map of all movies
    	PriorityQueue<Integer> movieIdHeap = new PriorityQueue<Integer>(11,new MaxHeapComp());
    	Locks locks=new Locks();
    	Gson gson = new Gson();
		try {
			LinkedList<Movie> movies = gson.fromJson(new JsonParser().parse(new FileReader("Database/Movies.json")).getAsJsonObject().get("movies").toString(), new TypeToken<LinkedList<Movie>>(){}.getType());
			LinkedList<User> users = gson.fromJson(new JsonParser().parse(new FileReader("Database/Users.json")).getAsJsonObject().get("users").toString(), new TypeToken<LinkedList<User>>(){}.getType());
			for(User u:users) {
				userStates.put(u.getUsername(),-1);
				userData.put(u.getUsername(), u);
			}
			for(Movie m:movies) {
				movieData.put(m.getName(), m);
				movieIdHeap.add(m.getId());
			}
			int numOfThreads = 5; 
			int port = Integer.decode(args[0]).intValue();
			Reactor<String> server=new Reactor<String>(numOfThreads,port, 
					() -> new MovieRentalServiceProtocol(locks,movieIdHeap,userData, userStates, movieData),
					() -> new MovieRentalEncoderDecoder(),
					new ConnectionImpl<String>());
			server.serve();
		}catch (IOException e) {
            e.printStackTrace();
        }
    }

}
