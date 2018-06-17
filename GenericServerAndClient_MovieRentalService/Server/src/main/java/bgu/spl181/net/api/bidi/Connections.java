package bgu.spl181.net.api.bidi;
/**
 * This interface should map a unique ID for each active client
   connected to the server. 
 *
 * @param <T>
 */
public interface Connections<T> {
    /**
     * This method sends msg to connection id
     * @param connectionId the connection to send to it
     * @param msg msg to send
     * @return indicator if send suceeded
     */
    boolean send(int connectionId, T msg);
    /**
     * This method send message to all connected client
     * @param msg msg send
     */
    void broadcast(T msg);
    /**
     * This method removes connections from connections database
     * @param connectionId connection to remove
     */
    void disconnect(int connectionId);
}
