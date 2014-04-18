package com.mcintyret.cache.message;

import com.mcintyret.cache.socket.SocketDetails;

/**
 * User: tommcintyre
 * Date: 4/16/14
 */
public interface MessageHandler {

    void handle(Message message, SocketDetails source);

}
