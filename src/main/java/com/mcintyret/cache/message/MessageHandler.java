package com.mcintyret.cache.message;

import com.mcintyret.cache.socket.SocketDetails;

/**
 * User: tommcintyre
 * Date: 4/16/14
 */
public interface MessageHandler<M extends Message> {

    void handle(M message, SocketDetails source);

}
