package com.mcintyret.cache.socket.multicast;

import com.mcintyret.cache.message.Message;
import com.mcintyret.cache.message.MessageType;

/**
 * User: tommcintyre
 * Date: 4/18/14
 */
class MulticastMessage implements Message {

    private Message message;

    private long uniqueId;

    public MulticastMessage(Message message, long uniqueId) {
        this.message = message;
        this.uniqueId = uniqueId;
    }

    public MulticastMessage() {
    }

    @Override
    public MessageType getType() {
        return message.getType();
    }

    Message getMessage() {
        return message;
    }

    long getUniqueId() {
        return uniqueId;
    }
}
