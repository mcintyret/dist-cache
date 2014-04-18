package com.mcintyret.cache.message;

/**
 * User: tommcintyre
 * Date: 4/18/14
 */
public class GetResponseMessage implements Message {

    private String key;

    private byte[] data;

    public GetResponseMessage(String key, byte[] data) {
        this.key = key;
        this.data = data;
    }

    public GetResponseMessage() {
    }

    @Override
    public MessageType getType() {
        return MessageType.GET_RESPONSE;
    }

    public String getKey() {
        return key;
    }

    public byte[] getData() {
        return data;
    }
}
