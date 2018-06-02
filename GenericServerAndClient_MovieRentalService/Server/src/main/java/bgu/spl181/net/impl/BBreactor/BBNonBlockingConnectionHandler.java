package bgu.spl181.net.impl.BBreactor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import bgu.spl181.net.api.MessageEncoderDecoder;
import bgu.spl181.net.api.bidi.BidiMessagingProtocol;
import bgu.spl181.net.srv.bidi.ConnectionHandler;
/**
 * This class describes the movie non blocking connectionhandler for reactor
 * It implements the connectionhandler interface to support send(msg) function.
 * Connection handler holds bidiprotocol encdec socket and reactor class.
 * It holds ByteBuffer queue for memory efficiency  
 * @author chen
 *
 * @param <T> type of message
 */
public class BBNonBlockingConnectionHandler<T> implements ConnectionHandler<T>{

    private static final int BUFFER_ALLOCATION_SIZE = 1 << 13; //8k
    private static final ConcurrentLinkedQueue<ByteBuffer> BUFFER_POOL = new ConcurrentLinkedQueue<>();

    private final BidiMessagingProtocol<T> protocol;
    private final MessageEncoderDecoder<T> encdec;
    private final Queue<ByteBuffer> writeQueue = new ConcurrentLinkedQueue<>();
    private final SocketChannel chan;
    private final Reactor<T> reactor;

    public BBNonBlockingConnectionHandler(
            MessageEncoderDecoder<T> reader,
            BidiMessagingProtocol<T> protocol,
            SocketChannel chan,
            Reactor<T> reactor) {
        this.chan = chan;
        this.encdec = reader;
        this.protocol = protocol;
        this.reactor = reactor;
    }
/**
 * Reading from socket
 * @return runnable method to run by thread if message detected
 */
    public Runnable continueRead() {
        ByteBuffer buf = leaseBuffer();
        boolean success = false;
        try {
            success = chan.read(buf) != -1;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (success) {
            buf.flip();
            return () -> {
                try {
                    while (buf.hasRemaining()) {
                        T nextMessage = encdec.decodeNextByte(buf.get());
                        if (nextMessage != null) 
                            protocol.process(nextMessage);                       
                    }
                } finally {
                    releaseBuffer(buf);
                }
            };
        } else {
            releaseBuffer(buf);
            close();
            return null;
        }
    }
  /**
   * This method closes the channel
   */
    public void close() {
        try {
            chan.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
   /**
    * This method return indicator if there is connection 
    * @return return indicator if there is connection
    */
    public boolean isClosed() {
        return !chan.isOpen();
    }
   /**
    * Write to socket a response
    * 
    */
    public void continueWrite() {
        while (!writeQueue.isEmpty()) {
            try {
                ByteBuffer top = writeQueue.peek();
                chan.write(top);
                if (top.hasRemaining()) {
                    return;
                } else 
                    writeQueue.remove();              
            } catch (IOException ex) {
                ex.printStackTrace();
                close();
            }
        }

        if (writeQueue.isEmpty()) {
            if (protocol.shouldTerminate()) close();
            else reactor.updateInterestedOps(chan, SelectionKey.OP_READ);
        }
    }
    /**
     * allocate ByteBuffer
     * @return ByteBuffer
     */
    private static ByteBuffer leaseBuffer() {
        ByteBuffer buff = BUFFER_POOL.poll();
        if (buff == null) 
            return ByteBuffer.allocateDirect(BUFFER_ALLOCATION_SIZE);     
        buff.clear();
        return buff;
    }
    /**
     * Release buffer adding it back to pool
     * @param buff buff to release
     */
    private static void releaseBuffer(ByteBuffer buff) {
        BUFFER_POOL.add(buff);
    }

	@Override
	/**
	 * This method add to writeQueue the massage to send and update
	 *   the selector
	 */
	public void send(T msg) {
		if (msg != null) {
            writeQueue.add(ByteBuffer.wrap(encdec.encode(msg)));
            reactor.updateInterestedOps(chan, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        }		
	}

}
