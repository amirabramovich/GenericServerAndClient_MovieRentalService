/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bgu.spl181.net.srv.bidi;

import java.io.Closeable;
/**
 * Interface of connectionHandler
 *
 * @param <T> type of message
 */
public interface ConnectionHandler<T> extends Closeable{
/**
 * This method sends to client msg
 * @param msg
 */
    void send(T msg) ;

}
