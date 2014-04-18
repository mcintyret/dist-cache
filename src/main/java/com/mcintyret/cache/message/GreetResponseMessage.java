package com.mcintyret.cache.message;

import com.mcintyret.cache.socket.SocketDetails;

/**
 * User: tommcintyre
 * Date: 4/16/14
 */
public class GreetResponseMessage implements Message {

    private Integer id;

    private int knownTotalPeers;

    public GreetResponseMessage() {
    }

    public GreetResponseMessage(Integer id, int knownTotalPeers) {
        this.id = id;
        this.knownTotalPeers = knownTotalPeers;
    }

    @Override
    public MessageType getType() {
        return MessageType.GREET_RESPONSE;
    }

    public Integer getId() {
        return id;
    }

    public int getKnownTotalPeers() {
        return knownTotalPeers;
    }
}
