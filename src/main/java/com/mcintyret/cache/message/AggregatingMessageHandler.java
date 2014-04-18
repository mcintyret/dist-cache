package com.mcintyret.cache.message;

import com.mcintyret.cache.socket.SocketDetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: tommcintyre
 * Date: 4/18/14
 */
public class AggregatingMessageHandler<M extends Message> implements MessageHandler<M> {

    private final List<MessageHandler<M>> messageHandlers;

    @SafeVarargs
    public AggregatingMessageHandler(MessageHandler<M>... messageHandlers) {
        this.messageHandlers = new ArrayList<>(messageHandlers.length);
        Collections.addAll(this.messageHandlers, messageHandlers);
    }

    @Override
    public void handle(M message, SocketDetails source) {
        for (MessageHandler<M> messageHandler : messageHandlers) {
            messageHandler.handle(message, source);
        }
    }

    public void addMessageHandler(MessageHandler<M> handler) {
        messageHandlers.add(handler);
    }
}
