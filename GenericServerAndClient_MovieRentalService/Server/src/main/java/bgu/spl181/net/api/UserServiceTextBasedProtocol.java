package bgu.spl181.net.api;


import java.util.concurrent.ConcurrentHashMap;

import bgu.spl181.net.api.bidi.BidiMessagingProtocol;
import bgu.spl181.net.api.bidi.Connections;
import bgu.spl181.net.srv.bidi.Locks;
import bgu.spl181.net.srv.bidi.User;

/**
 * This class describes the USTBP protocol it supports Bidirectional communication as it implements the BidiProtocol
 * It holds the connections database indicating what users are connected to server,the users database with their names,and userstates database indicating which clients are loged in
 * It holds the current connectionId and boolean indicator if this connection is loggedin to sever or not.
 * In addition it holds the locks we use to sync the system
 * @author chen
 *
 */
public class UserServiceTextBasedProtocol implements BidiMessagingProtocol<String> {

	private boolean shouldTerminate = false;
	protected int connectionId; //current user's id
	protected Connections<String> connections; //list of user ids and handlers
	protected ConcurrentHashMap<String, Integer> usersStates; //map of all registered users and their state
	protected ConcurrentHashMap<String,User> usersData; //map of all registered users and their data 
	protected String username=null;//if current user is logged in
	protected Locks locks;
	/**
	 * Constructor
	 * @param locks the locks used to sync the system
	 * @param userData users database
	 * @param userStates userstates database
	 */
	public UserServiceTextBasedProtocol(Locks locks,ConcurrentHashMap<String, User> userData,ConcurrentHashMap<String, Integer> userStates) {
		this.usersStates=userStates;
		this.usersData=userData;
		this.locks=locks;
	}
	
	/**
	 * initiate the
		protocol with the active connections structure of the server and saves the owner client’s connection id.
	 */
	public void start(int connectionId, Connections<String> connections) {
		this.connectionId=connectionId;
		this.connections=connections;
	}
	
	/**
	 * As in MessagingProtocol, processes a given
	message. Unlike MessagingProtocol, responses are sent via the
	connections object send function.
	 */
	public void process(String message) {	
		message=message.replace("\r","");
		if(message.equals("SIGNOUT"))
			signout();
		else if(message.equals("REGISTER"))
			connections.send(connectionId, "ERROR registration failed");	
		else {
			String command = message.substring(0, message.indexOf(' '));
			String args = message.substring(command.length()+1);
			if(command.equals("LOGIN"))
				login(args);
			else if(command.equals("REGISTER"))
				register(args);
			else if(command.equals("REQUEST"))
				request(args);
		}
	}
	
	/**
	 * This method treats the login command and updates the databases
	 * If error found in formant username or password missing ,error sent to client
	 * @param msg username and password
	 */
	private void login(String msg) {
		if(msg.indexOf(' ')!=-1) {
			String username = msg.substring(0, msg.indexOf(' '));
			locks.getUsersStatesLock().writeLock().lock();
			if((isLogged()==false&&usersStates.get(username)!=null&&usersStates.get(username)==-1)) {
				String password=msg.substring(msg.indexOf(' ')+1);
				locks.getUsersDataLock().writeLock().lock();
				if(usersData.get(username).getPassword().equals(password)) {
					usersStates.put(username, connectionId);
					locks.getUsersStatesLock().writeLock().unlock();
					locks.getUsersDataLock().writeLock().unlock();
					this.username=username;
					connections.send(connectionId, "ACK login succeeded");
					return;
				}
				locks.getUsersDataLock().writeLock().unlock();
			}
			locks.getUsersStatesLock().writeLock().unlock();
		}
		connections.send(connectionId, "ERROR login failed");
	}
	/**
	 * This method treats the Signout command and updates the databases
	 * If error found ,error sent to client
	 */
	private void signout() {
		if(isLogged()==false)
			connections.send(connectionId, "ERROR signout failed");
		else {
			locks.getUsersStatesLock().writeLock().lock();
			usersStates.put(username, -1);
			locks.getUsersStatesLock().writeLock().unlock();
			username=null;
			connections.send(connectionId, "ACK signout succeeded");
			connections.disconnect(connectionId);
			shouldTerminate=true;
		}
	}
	/**
	 * This method treats the Register command and updates the databases
	 * If error found ,error sent to client
	 * We call continueregister method of service protocol to check datablcok param
	 * @param username password and optional datablock
	 */
	private void register(String msg) {
		if(msg.contains(" ")==true&&isLogged()==false){
			String username=msg.substring(0, msg.indexOf(" "));
			String rest=msg.substring(msg.indexOf(" ")+1);//msg without username
			int passEnd=rest.length();
			String data="";
			if(rest.contains(" ")) {
				passEnd=rest.indexOf(" ");
				data=rest.substring(rest.indexOf(" ")+1);
			}
			String password=rest.substring(0, passEnd);
			locks.getUsersDataLock().writeLock().lock();
			if(usersData.containsKey(username)==false) {
				usersData.put(username,new User(username, password));
				locks.getUsersDataLock().writeLock().unlock();
				locks.getUsersStatesLock().writeLock().lock();
				usersStates.put(username, -1);
				locks.getUsersStatesLock().writeLock().unlock();
				if(continueRegister(username,password,data)==true) {
					connections.send(connectionId, "ACK registration succeeded");
					return;
				}
			}else
				locks.getUsersDataLock().writeLock().unlock();	
		}
		connections.send(connectionId, "ERROR registration failed");	
	}
	/**
	 * This method treats request command and updates the databases
	 * if error found error sent to client,continuerequest method of service protocol is called for giving its service .
	 * Updates the databases
	 * @param params of request
	 */
	private void request(String args){
		String name="";
		String param="";
		int last=args.length();
		int space=args.indexOf(" ");
		if(space!=-1) {
			param=args.substring(space+1);
			last=space;	
		}
		name=args.substring(0,last);
		if((isLogged()==false)||(continueRequest(name,param)==false)) {
			connections.send(connectionId,"ERROR request "+name+" failed");	
		}
	}
	/**
	 * This method is meant to be overided by service protocol if no service protocol exists this method returns true as request succeded
	 * @param name name of request
	 * @param param additional param
	 * @return boolean indicator if command succeded
	 */
	protected boolean continueRequest(String name, String param) {
		return true;
	}
    /**This method sends broadcast to all logged in clients
     * 
     * @param msg msg to send
     */
	protected void broadcast(String msg){//helper function
		for(ConcurrentHashMap.Entry<String,Integer> entry : usersStates.entrySet()) 
		    if(entry.getValue()!=-1)
				    connections.send(entry.getValue(),msg);	
	}
    /**
     * This method is meant to be overided by service protocol if no service protocol exists this method return true as register succeded
     * (no relevance for the additional params)
     * @param username username
     * @param password password
     * @param data additional data
     * @return boolean indicator if command succeded
     */
	protected boolean continueRegister(String username, String password, String data) {
		return true;
	}	
	/**
	 * Returns true if user is logged in otherwise false
	 * @return boolean indicator if logged in
	 */
	protected boolean isLogged() {
		return username!=null;
	}
	/**
	 * Returns indicator if this connection should terminate
	 * @reutrn indicator if shouldTerminate
	 */
	public boolean shouldTerminate() {
		return shouldTerminate;
	}
}
