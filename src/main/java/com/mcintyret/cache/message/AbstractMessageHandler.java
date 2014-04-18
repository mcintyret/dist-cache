package com.mcintyret.cache.message;

import com.mcintyret.cache.socket.SocketDetails;

import java.util.EnumMap;
import java.util.Map;

/**
 * User: tommcintyre
 * Date: 4/18/14
 */
public class AbstractMessageHandler implements MessageHandler {

    private final Map<MessageType, MessageHandler> handlerMap = new EnumMap<>(MessageType.class);

    public void addMessageHandler(MessageType type, MessageHandler<?> handler) {
        MessageHandler existing = handlerMap.get(type);
        if (existing == null) {
            handlerMap.put(type, handler);
        } else if (existing instanceof AggregatingMessageHandler) {
            ((AggregatingMessageHandler) existing).addMessageHandler(handler);
        } else {
            handlerMap.put(type, new AggregatingMessageHandler(existing, handler));
        }
    }

    @Override
    public void handle(Message message, SocketDetails source) {
        MessageHandler handler = handlerMap.get(message.getType());
        if (handler != null) {
            handler.handle(message, source);
        }
    }
}
