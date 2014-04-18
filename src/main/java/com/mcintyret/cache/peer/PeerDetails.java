package com.mcintyret.cache.peer;

import java.net.SocketAddress;

/**
 * User: tommcintyre
 * Date: 4/16/14
 */
public class PeerDetails implements Comparable<PeerDetails> {

    private final int id;

    private final SocketAddress tcpSocketAddress;

    public PeerDetails(int id, SocketAddress tcpSocketAddress) {
        this.id = id;
        this.tcpSocketAddress = tcpSocketAddress;
    }

    public Integer getId() {
        return id;
    }

    public SocketAddress getTcpSocketAddress() {
        return tcpSocketAddress;
    }

    @Override
    public int compareTo(PeerDetails o) {
        return o.id - id;
    }
}

