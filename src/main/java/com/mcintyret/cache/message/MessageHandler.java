package com.mcintyret.cache.message;

import java.net.InetSocketAddress;

/**
 * User: tommcintyre
 * Date: 4/16/14
 */
public interface MessageHandler<M extends Message> {

    void handle(M message, InetSocketAddress source);

}
