package bgu.spl181.net.api.bidi;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl181.net.srv.bidi.ConnectionHandler;
/**
 * This class represents the connections connected to the server and methods to send them messages and broadcats.
 * Holding database to manage the connections makes it flexible to communicate with all clients or group of them.
 * The represantation of connections is with concurrent hash map to support therad safe operations
 *  and it maps id of connection to its connectionhandler.
 * 
 * 
 * @author chen
 *
 * @param <T> type of messages 
 */
public class ConnectionImpl<T> implements Connections<T>{

	private ConcurrentHashMap<Integer,ConnectionHandler<T>> connections=new ConcurrentHashMap<>() ;
	
	/**
	 * sends a message T to client represented
		by the given connId
	 */
	@Override
	public boolean send(int connectionId, T msg) {
		if(connections.containsKey(connectionId)) 
			connections.get(connectionId).send(msg);
		return (connections.containsKey(connectionId));	
	}
	/**
	 * sends a message T to all active clients. This
		includes clients that has not yet completed log-in by the User service text
		based protocol. Remember, Connections<T> belongs to the server pattern
		implementation, not the protocol!.
	 */
	@Override
	public void broadcast(T msg) {
		for(Entry<Integer, ConnectionHandler<T>> entry : connections.entrySet()) 
		    entry.getValue().send(msg);	
	}
	/**
	 * removes active client connId from map.
	 */
	@Override
	public void disconnect(int connectionId) {
		connections.remove(connectionId);
	}
	/**
	 * return the connections 
	 * @return hashmap 
	 */
	public ConcurrentHashMap<Integer,ConnectionHandler<T>> getConnections() {
		return connections;
	}
}
