package com.mcintyret.cache.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * User: tommcintyre
 * Date: 4/18/14
 */
public class Peers {

    private static final Logger LOG = LoggerFactory.getLogger(Peers.class);

    private final PeerDetails me;

    private final List<PeerDetails> peers;

    private final List<PeerChangeListener> peerChangeListeners = new CopyOnWriteArrayList<>();

    public Peers(PeerDetails me) {
        this.me = me;
        peers = new ArrayList<>();
        peers.add(me);
    }

    public void addPeer(PeerDetails peer) {
        synchronized (this) {
            peers.add(peer);
            LOG.info("Added remote peer at {}", peer.getTcpSocketDetails());
            Collections.sort(peers);
        }
        fireChange();
    }

    public void addPeers(Collection<PeerDetails> peers) {
        synchronized (this) {
            for (PeerDetails peer : peers) {
                this.peers.add(peer);
                LOG.info("Added remote peer at {}", peer.getTcpSocketDetails());
            }
            Collections.sort(this.peers);
        }
        fireChange();
    }

    public synchronized List<PeerDetails> getPeers() {
        return new ArrayList<>(peers);
    }

    public PeerDetails getMe() {
        return me;
    }

    public synchronized int getTotalPeersCount() {
        return peers.size();
    }

    public synchronized int getRemotePeersCount() {
        return peers.size() - 1;
    }

    public void addPeerChangeListener(PeerChangeListener peerChangeListener) {
        peerChangeListeners.add(peerChangeListener);
    }

    private void fireChange() {
        for (PeerChangeListener peerChangeListener : peerChangeListeners) {
            peerChangeListener.onChange(this);
        }
    }
}
