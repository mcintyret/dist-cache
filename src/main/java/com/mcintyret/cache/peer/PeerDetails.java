package com.mcintyret.cache.peer;

import com.mcintyret.cache.socket.SocketDetails;

/**
 * User: tommcintyre
 * Date: 4/16/14
 */
public class PeerDetails implements Comparable<PeerDetails> {

    private Integer id;

    private final SocketDetails tcpSocketDetails;

    public PeerDetails(SocketDetails tcpSocketDetails) {
        this.tcpSocketDetails = tcpSocketDetails;
    }

    public PeerDetails(int id, SocketDetails tcpSocketDetails) {
        this.id = id;
        this.tcpSocketDetails = tcpSocketDetails;
    }

    public Integer getId() {
        return id;
    }

    public SocketDetails getTcpSocketDetails() {
        return tcpSocketDetails;
    }

    public void setId(int id) {
        if (this.id != null) {
            throw new IllegalStateException("ID already set to " + this.id + " but trying to set it again to " + id);
        }
        this.id = id;
    }

    @Override
    public int compareTo(PeerDetails o) {
        return id.compareTo(o.id);
    }
}

