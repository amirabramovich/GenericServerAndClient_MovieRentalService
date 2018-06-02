package bgu.spl181.net.srv.bidi;

import java.util.concurrent.locks.ReentrantReadWriteLock;
/**
 * This class represents the locks we use for each database we hold in order 
 * to sync the functionality and system for thread saftey
 * We hold 6 locks.Two for movies and users database we hold,2 for movies and users Json update files,
 * userstates database managing logedin clients  and Heapqueue managing movies id lock
 * We use fair policy
 * @author chen
 *
 */
public class Locks {
	private ReentrantReadWriteLock usersJsonLock=new ReentrantReadWriteLock(true);
	private ReentrantReadWriteLock usersDataLock=new ReentrantReadWriteLock(true);
	private ReentrantReadWriteLock usersStatesLock=new ReentrantReadWriteLock(true);
	private ReentrantReadWriteLock moviesJsonLock=new ReentrantReadWriteLock(true);
	private ReentrantReadWriteLock moviesDataLock=new ReentrantReadWriteLock(true);
	private ReentrantReadWriteLock heapLock=new ReentrantReadWriteLock(true);
	
	public ReentrantReadWriteLock getUsersJsonLock(){return usersJsonLock;}
	public ReentrantReadWriteLock getUsersDataLock(){return usersDataLock;}
	public ReentrantReadWriteLock getUsersStatesLock(){return usersStatesLock;}
	public ReentrantReadWriteLock getMoviesJsonLock(){return moviesJsonLock;}
	public ReentrantReadWriteLock getMoviesDataLock(){return moviesDataLock;}
	public ReentrantReadWriteLock getHeapLock(){return heapLock;}
}
