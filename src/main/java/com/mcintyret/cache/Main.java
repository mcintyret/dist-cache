package com.mcintyret.cache;

import com.mcintyret.cache.data.Cache;
import com.mcintyret.cache.http.HttpServer;
import com.mcintyret.cache.message.*;
import com.mcintyret.cache.peer.PeerDetails;
import com.mcintyret.cache.peer.Peers;
import com.mcintyret.cache.socket.SocketDetails;
import com.mcintyret.cache.socket.multicast.MulticastServer;
import com.mcintyret.cache.socket.tcp.TcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * User: tommcintyre
 * Date: 10/1/13
 */
public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private final TcpServer tcpServer = new TcpServer();

    private final MulticastServer multicastServer = new MulticastServer();

    private final Peers peers;

    private final Cache cache;

    public static void main(String[] args) throws IOException {
        new Main();
    }

    public Main() throws IOException {
        tcpServer.start();
        multicastServer.start();

        peers = new Peers(new PeerDetails(tcpServer.getSocketDetails()));

        this.cache = new Cache(peers, tcpServer);

        multicastServer.addMessageHandler(MessageType.GREET, new GreetMessageHandler());
        tcpServer.addMessageHandler(MessageType.GREET_RESPONSE, new GreetResponseMessageHandler());
        tcpServer.addMessageHandler(MessageType.CONFIRM_ID, new ConfirmIdHandler());

        multicastServer.sendMessage(new GreetMessage(tcpServer.getSocketDetails().getPort()));
    }

    private class ConfirmIdHandler implements MessageHandler<ConfirmIdMessage> {

        @Override
        public void handle(ConfirmIdMessage message, SocketDetails source) {
            peers.addPeer(new PeerDetails(message.getId(), source));
        }
    }

    private class GreetMessageHandler implements MessageHandler<GreetMessage> {
        @Override
        public void handle(GreetMessage message, SocketDetails source) {
            SocketDetails recipient = new SocketDetails(source.getInetAddress(), message.getPort());

            tcpServer.sendMessage(new GreetResponseMessage(peers.getMe().getId(), peers.getRemotePeersCount()), recipient);
        }
    }

    private class GreetResponseMessageHandler implements MessageHandler<GreetResponseMessage> {

        private final ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);

        private final long maxTimeMillis = 10 * 1000;

        private Map<SocketDetails, PeerDetails> respondedPeers = new HashMap<>();

        private List<SocketDetails> noIdSocketDetails = new ArrayList<>();

        private boolean done = false;

        private int expectedPeers = 0;

        private int maxId = 0;

        public GreetResponseMessageHandler() {
            exec.schedule(new Runnable() {
                @Override
                public void run() {
                    completeGreeting();
                }
            }, maxTimeMillis, TimeUnit.MILLISECONDS);
        }

        private synchronized void completeGreeting() {
            if (!done) {
                int myId = -1;
                if (noIdSocketDetails.isEmpty()) {
                    myId = maxId + 1;
                } else {
                    noIdSocketDetails.add(peers.getMe().getTcpSocketDetails());
                    Collections.sort(noIdSocketDetails, SOCKET_DETAILS_COMPARATOR);

                    for (SocketDetails sd : noIdSocketDetails) {
                        maxId++;
                        if (sd == peers.getMe().getTcpSocketDetails()) {
                            myId = maxId;
                            break;
                        }
                    }
                }
                if (myId == -1) {
                    throw new AssertionError("Couldn't come up with an ID for myself!");
                }
                peers.getMe().setId(myId);
                LOG.info("Set my id to {}", myId);
                if (myId == 1) {
                    LOG.info("I am the magic number 1, therefore I am starting the HttpServer");
                    try {
                        new HttpServer(cache).start();
                    } catch (Exception e) {
                        throw new IllegalStateException("Unable to start HttpServer");
                    }
                }

                for (int i = 0; i < 10; i++) {
                    for (PeerDetails peer : respondedPeers.values()) {
                        tcpServer.sendMessage(new ConfirmIdMessage(myId), peer.getTcpSocketDetails());
                    }
                }

                peers.addPeers(respondedPeers.values());

                done = true;
            }
        }

        @Override
        public synchronized void handle(GreetResponseMessage grm, SocketDetails source) {
            if (done) {
                // TODO: better handling of late responses?
                LOG.warn("Shouldn't be receiving GreetResponseMessages now, from {}", source);
            } else {
                expectedPeers = Math.max(expectedPeers, grm.getKnownTotalPeers());
                Integer id = grm.getId();

                PeerDetails newPeerDetails = new PeerDetails(source);
                if (id == null) {
                    noIdSocketDetails.add(source);
                } else {
                    maxId = Math.max(maxId, id);
                    newPeerDetails.setId(id);
                }

                respondedPeers.put(source, newPeerDetails);

                if (respondedPeers.size() == expectedPeers) {
                    completeGreeting();
                }
            }
        }
    }

    private static final Comparator<SocketDetails> SOCKET_DETAILS_COMPARATOR = new Comparator<SocketDetails>() {
        @Override
        public int compare(SocketDetails o1, SocketDetails o2) {
            int comp = o1.getInetAddress().getHostAddress().compareTo(o2.getInetAddress().getHostAddress());
            if (comp != 0) {
                return comp;
            } else {
                return o2.getPort() - o1.getPort();
            }
        }
    };
}
