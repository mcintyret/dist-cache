package com.mcintyret.cache.message;

/**
 * User: tommcintyre
 * Date: 10/1/13
 */
public class GetMessage implements Message {

    private String key;

    public GetMessage(String key) {
        this.key = key;
    }

    public GetMessage() {
    }

    @Override
    public MessageType getType() {
        return MessageType.GET;
    }

    public String getKey() {
        return key;
    }

}
