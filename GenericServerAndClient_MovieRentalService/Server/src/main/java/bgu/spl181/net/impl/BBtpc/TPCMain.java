package bgu.spl181.net.impl.BBtpc;

import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import bgu.spl181.net.api.MessageEncoderDecoder;
import bgu.spl181.net.api.MovieRentalEncoderDecoder;
import bgu.spl181.net.api.MovieRentalServiceProtocol;
import bgu.spl181.net.api.bidi.BidiMessagingProtocol;
import bgu.spl181.net.api.bidi.ConnectionImpl;
import bgu.spl181.net.srv.bidi.Locks;
import bgu.spl181.net.srv.bidi.MaxHeapComp;
import bgu.spl181.net.srv.bidi.Movie;
import bgu.spl181.net.srv.bidi.User;
/**
 * This class runs threadperclient server parsing Json files to databases
 * establishing sockets with clients
 * @author chen
 *
 */
public class TPCMain{
	
	private final int port;
    private final Supplier<BidiMessagingProtocol<String>> protocolFactory;
    private final Supplier<MessageEncoderDecoder<String>> encdecFactory;
    private ServerSocket sock;
    private ConnectionImpl<String> connections;
    /**
     * Constructor gets supplier for protocol encdec
     * ,port,initializes sock and connections
     * @param port to listen to
     * @param protocolFactory  protocol
     * @param encdecFactory  encdec
     */
    public TPCMain(
            int port,
            Supplier<BidiMessagingProtocol<String>> protocolFactory,
            Supplier<MessageEncoderDecoder<String>> encdecFactory) {
        this.port = port;
        this.protocolFactory = protocolFactory;
        this.encdecFactory = encdecFactory;
		this.sock = null;
		this.connections=new ConnectionImpl<>();
    }
/**
 * This method closes the socket
 * 
 */
	public void close(){
		if (sock != null)
			try {
				sock.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
/**
 * Main method of server it establishes the server socket listening to clients and
 * establishing clientsockets and connectionhandlers to sockets linking thread per client
 * supply id for each connection
 */
	public void serve() {
		try (ServerSocket serverSock = new ServerSocket(port)){	
			int counter=0;
			System.out.println("Server started");
			this.sock = serverSock;//just to be able to close
            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSock = serverSock.accept();       
                BidiMessagingProtocol<String> protocol =protocolFactory.get();
                protocol.start(counter, connections);  
                BBBlockingConnectionHandler<String> handler = new BBBlockingConnectionHandler<>(clientSock, encdecFactory.get(), protocol);
                connections.getConnections().put(counter, handler);
                new Thread(handler).start();
                counter++;
            }
        } catch (IOException ex) {  ex.printStackTrace(); }
        System.out.println("server closed!!!");		
	}
	
	/**
	 * parse Json files and run server on given port
	 * @param args port to listen
	 */
    public static void main(String[] args) {
    	ConcurrentHashMap<String,Integer> userStates=new ConcurrentHashMap<>(); //map of all registered users and their state
    	ConcurrentHashMap<String,User> userData=new ConcurrentHashMap<>();//map of all registered users and their data
    	ConcurrentHashMap<String,Movie> movieData=new ConcurrentHashMap<>();//map of all movies and their data    	
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
			int port = Integer.decode(args[0]).intValue();
			TPCMain server=new TPCMain(port, () -> new MovieRentalServiceProtocol(locks,movieIdHeap,userData, userStates, movieData), () -> new MovieRentalEncoderDecoder());
			server.serve();
		}catch (IOException e) {
            e.printStackTrace();
        }
    }
}
