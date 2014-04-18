package com.mcintyret.cache;

import java.io.IOException;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * User: tommcintyre
 * Date: 4/16/14
 */
public class QueueChannel extends AbstractSelectableChannel {


    /**
     * Initializes a new instance of this class.
     */
    protected QueueChannel(SelectorProvider provider) {
        super(provider);
    }

    @Override
    protected void implCloseSelectableChannel() throws IOException {

    }

    @Override
    protected void implConfigureBlocking(boolean block) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int validOps() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
