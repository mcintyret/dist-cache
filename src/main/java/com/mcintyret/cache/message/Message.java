package com.mcintyret.cache.message;

import java.io.Serializable;

/**
 * User: tommcintyre
 * Date: 10/1/13
 */
public interface Message extends Serializable {

    MessageType getType();


}
