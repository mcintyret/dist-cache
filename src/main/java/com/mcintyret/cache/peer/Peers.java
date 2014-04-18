package com.mcintyret.cache.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * User: tommcintyre
 * Date: 4/18/14
 */
public class Peers {

    private static final Logger LOG = LoggerFactory.getLogger(Peers.class);

    private PeerDetails me;

    private final SocketAddress myAddress;

    private final Set<PeerDetails> confirmedPeers = new TreeSet<>();

    private final Set<SocketAddress> pendingPeers = new HashSet<>();

    private final List<PeerChangeListener> peerChangeListeners = new CopyOnWriteArrayList<>();

    public Peers(SocketAddress myAddress) {
        this.myAddress = myAddress;
        pendingPeers.add(myAddress);
    }

    public void addConfirmedPeer(PeerDetails peer) {
        synchronized (this) {
            doAddConfirmedPeer(peer);
        }
        fireChange();
    }

    public void addConfirmedPeers(Collection<PeerDetails> peers) {
        synchronized (this) {
            for (PeerDetails peer : peers) {
                doAddConfirmedPeer(peer);
            }
        }
        fireChange();
    }

    private void doAddConfirmedPeer(PeerDetails peer) {
        if (confirmedPeers.add(peer)) {
            LOG.info("Added remote peer at {}", peer.getTcpSocketAddress());
        } else {
            LOG.warn("Attempted to add remote peer at {}, but this peer was already known", peer.getTcpSocketAddress());
        }
    }

    public synchronized void addPendingPeer(SocketAddress pendingPeer) {
        pendingPeers.add(pendingPeer);
    }

    public synchronized void confirmPeer(SocketAddress socketAddress, int id) {
        if (!pendingPeers.remove(socketAddress)) {
            throw new IllegalStateException("Trying to confirm peer that was never pending: " + socketAddress);
        }
        PeerDetails peerDetails = new PeerDetails(id, socketAddress);
        if (socketAddress.equals(myAddress)) {
            me = peerDetails;
        }
        confirmedPeers.add(peerDetails);
    }

    public synchronized List<PeerDetails> getConfirmedPeers() {
        return new ArrayList<>(confirmedPeers);
    }

    public synchronized PeerDetails getMe() {
        return me;
    }

    public synchronized Integer getMyId() {
        return me == null ? null : me.getId();
    }

    public synchronized int getConfirmedPeersCount() {
        return confirmedPeers.size();
    }

    public synchronized int getTotalPeersCount() {
        return pendingPeers.size() + confirmedPeers.size();
    }

    public void addPeerChangeListener(PeerChangeListener peerChangeListener) {
        peerChangeListeners.add(peerChangeListener);
    }

    private void fireChange() {
        for (PeerChangeListener peerChangeListener : peerChangeListeners) {
            peerChangeListener.onChange(this);
        }
    }

    public SocketAddress getMyAddress() {
        return myAddress;
    }
}
