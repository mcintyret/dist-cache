package com.mcintyret.cache.message;

/**
 * User: tommcintyre
 * Date: 4/18/14
 */
public class PutMessage implements Message {

    private String key;

    private byte[] data;

    private boolean andGet;

    public PutMessage(String key, byte[] data, boolean andGet) {
        this.key = key;
        this.data = data;
        this.andGet = andGet;
    }

    public PutMessage() {
    }

    @Override
    public MessageType getType() {
        return MessageType.PUT;
    }

    public String getKey() {
        return key;
    }

    public byte[] getData() {
        return data;
    }

    public boolean isAndGet() {
        return andGet;
    }
}
