package com.mcintyret.cache.message;

/**
 * User: tommcintyre
 * Date: 10/1/13
 */
public class GreetMessage implements Message {

    private int port;

    public GreetMessage() {
    }

    public GreetMessage(int port) {
        this.port = port;
    }


    @Override
    public MessageType getType() {
        return MessageType.GREET;
    }

    public int getPort() {
        return port;
    }
}
