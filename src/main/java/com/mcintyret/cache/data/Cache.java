package com.mcintyret.cache.data;

import com.google.common.util.concurrent.SettableFuture;
import com.mcintyret.cache.message.*;
import com.mcintyret.cache.peer.PeerChangeListener;
import com.mcintyret.cache.peer.PeerDetails;
import com.mcintyret.cache.peer.Peers;
import com.mcintyret.cache.socket.SocketDetails;
import com.mcintyret.cache.socket.tcp.TcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * User: tommcintyre
 * Date: 4/18/14
 */
public class Cache implements PeerChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(Cache.class);

    private final ConcurrentHashMap<String, byte[]> myCache = new ConcurrentHashMap<>();

    private final Map<String, SettableFuture<byte[]>> awaiting = new HashMap<>();

    private final List<PeerDetails> peers;

    private final PeerDetails me;

    private final TcpServer tcpServer;

    public Cache(Peers peers, final TcpServer tcpServer) {
        this(peers.getPeers(), peers.getMe(), tcpServer);
        peers.addPeerChangeListener(this);
    }

    public Cache(List<PeerDetails> peers, PeerDetails me, final TcpServer tcpServer) {
        this.peers = peers;
        this.me = me;
        this.tcpServer = tcpServer;

        tcpServer.addMessageHandler(MessageType.GET_RESPONSE, new MessageHandler<GetResponseMessage>() {
            @Override
            public void handle(GetResponseMessage grm, SocketDetails source) {

                SettableFuture<byte[]> future;
                synchronized (awaiting) {
                    future = awaiting.remove(grm.getKey());
                }
                if (future != null) {
                    future.set(grm.getData());
                }
            }
        });

        tcpServer.addMessageHandler(MessageType.GET, new MessageHandler<GetMessage>() {
            @Override
            public void handle(GetMessage get, SocketDetails source) {
                String key = get.getKey();
                LOG.info("Serving remote get request for '{}' from peer {}", key, source);
                byte[] result = myCache.get(key);
                tcpServer.sendMessage(new GetResponseMessage(key, result), source);
            }
        });

        tcpServer.addMessageHandler(MessageType.PUT, new MessageHandler<PutMessage>() {
            @Override
            public void handle(PutMessage put, SocketDetails source) {
                byte[] oldVal = myCache.put(put.getKey(), put.getData());

                if (put.isAndGet()) {
                    tcpServer.sendMessage(new GetResponseMessage(put.getKey(), oldVal), source);
                }
            }
        });
    }


    public byte[] get(String key) throws ExecutionException, InterruptedException {
        PeerDetails peer = getPeerDetails(key);
        if (peer == me) {
            LOG.info("Serving value for '{}' from local cache", key);
            return myCache.get(key);
        } else {
            Future<byte[]> future = getFuture(key);
            LOG.info("Requesting value for '{}' from peer at {}", key, peer.getTcpSocketDetails());
            tcpServer.sendMessage(new GetMessage(key), peer.getTcpSocketDetails());
            return future.get();
        }
    }

    private Future<byte[]> getFuture(String key) {
        SettableFuture<byte[]> future;
        synchronized (awaiting) {
            future = awaiting.get(key);
            if (future == null) {
                future = SettableFuture.create();
                awaiting.put(key, future);
            }
        }
        return future;
    }

    public void put(String key, byte[] val) {
        PeerDetails peerDetails = getPeerDetails(key);
        if (peerDetails == me) {
            LOG.info("Storing value for '{}' in local cache", key);
            myCache.put(key, val);
        } else {
            tcpServer.sendMessage(new PutMessage(key, val, false), peerDetails.getTcpSocketDetails());
        }
    }

    public byte[] putAndGet(String key, byte[] val) throws ExecutionException, InterruptedException {
        PeerDetails peerDetails = getPeerDetails(key);
        if (peerDetails == me) {
            return myCache.put(key, val);
        } else {
            Future<byte[]> future = getFuture(key);
            tcpServer.sendMessage(new PutMessage(key, val, true), peerDetails.getTcpSocketDetails());
            return future.get();
        }
    }

    private PeerDetails getPeerDetails(String key) {
        int hash = key.hashCode() % peers.size();
        return peers.get(hash);
    }

    @Override
    public void onChange(Peers peers) {
        if (!myCache.isEmpty()) {
            // Redistribute!
        }
    }
}
