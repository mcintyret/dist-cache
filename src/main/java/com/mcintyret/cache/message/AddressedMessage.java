package com.mcintyret.cache.message;

import com.mcintyret.cache.socket.SocketDetails;

/**
 * User: tommcintyre
 * Date: 4/18/14
 */
public class AddressedMessage {

    private final SocketDetails recipient;

    private final Message message;

    public AddressedMessage(SocketDetails recipient, Message message) {
        this.recipient = recipient;
        this.message = message;
    }

    public SocketDetails getRecipient() {
        return recipient;
    }

    public Message getMessage() {
        return message;
    }
}
