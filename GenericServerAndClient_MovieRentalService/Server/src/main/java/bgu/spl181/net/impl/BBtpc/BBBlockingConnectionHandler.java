package bgu.spl181.net.impl.BBtpc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

import bgu.spl181.net.api.MessageEncoderDecoder;
import bgu.spl181.net.api.bidi.BidiMessagingProtocol;
import bgu.spl181.net.srv.bidi.ConnectionHandler;
/**
 * This class represents blocking connectionhandler to support singlethread threadperclient
 * and implements connectionhandler interface send method.
 * It holds boolean connected to indicate if client connected
 * @param <T> type of message
 */
public class BBBlockingConnectionHandler<T> implements Runnable, ConnectionHandler<T>{

    private final BidiMessagingProtocol<T> protocol;
    private final MessageEncoderDecoder<T> encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;
   
    /**
     * Constructor 
     * @param sock socket for client
     * @param reader encdec
     * @param protocol 
     */
	
	public BBBlockingConnectionHandler(Socket sock, MessageEncoderDecoder<T> reader, BidiMessagingProtocol<T> protocol){
		this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;	
	}
	
	@Override
	/**
	 * This method closes the
	 * sock
	 */
	public void close() throws IOException {
		connected = false;
        sock.close();	
	}

	@Override
	/**
	* This method sends message to client
	 */
	public void send(T msg) {
		if (msg != null) {
			try { 
				Socket sock = this.sock;
				out = new BufferedOutputStream(sock.getOutputStream());
	            out.write(encdec.encode(msg));
	            out.flush();
			} catch (IOException ex) {
	            ex.printStackTrace();
	        }	
		}	
	}

	@Override
	/**
	 * This method reads from client messages and process them
	 */
	public void run() {
		try (Socket sock=this.sock){ 
			int read;
            in = new BufferedInputStream(sock.getInputStream());
            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
            T nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null) 
                	protocol.process(nextMessage);           
            }
        }catch (IOException ex) {
            ex.printStackTrace();
        }
	}
	/**
	 * This method returns the protocol
	 * @return protocol
	 */
	public BidiMessagingProtocol<T> getProtocol(){
		return protocol;
	}

}
