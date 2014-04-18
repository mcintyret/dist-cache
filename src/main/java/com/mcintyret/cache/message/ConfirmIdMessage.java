package com.mcintyret.cache.message;

/**
 * User: tommcintyre
 * Date: 4/18/14
 */
public class ConfirmIdMessage implements Message {

    private int id;

    public ConfirmIdMessage(int id) {
        this.id = id;
    }

    public ConfirmIdMessage() {
    }

    public int getId() {
        return id;
    }

    @Override
    public MessageType getType() {
        return MessageType.CONFIRM_ID;
    }
}
