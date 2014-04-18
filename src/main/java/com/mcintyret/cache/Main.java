package com.mcintyret.cache;

import com.mcintyret.cache.message.*;
import com.mcintyret.cache.peer.PeerDetails;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: tommcintyre
 * Date: 10/1/13
 */
public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private final TcpServer tcpServer = new TcpServer();

    private final MulticastServer multicastServer = new MulticastServer();

    private final PeerDetails me;

    private final Collection<PeerDetails> peers = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        new Main();
    }

    public Main() throws IOException {
        tcpServer.start();
        multicastServer.start();

        me = new PeerDetails(tcpServer.getSocketDetails());

        multicastServer.addMessageHandler(new GreetMessageHandler());
        tcpServer.addMessageHandler(new GreetResponseMessageHandler());

        multicastServer.sendMessage(new GreetMessage(tcpServer.getSocketDetails().getPort()));
    }

    private class GreetMessageHandler implements MessageHandler {
        @Override
        public void handle(Message message, SocketDetails source) {
            if (message.getType() == MessageType.GREET) {
                int tcpPort = ((GreetMessage) message).getPort();
                SocketDetails recipient = new SocketDetails(source.getInetAddress(), tcpPort);
                LOG.info("Sending greet response to {}", recipient);

                tcpServer.sendMessage(new GreetResponseMessage(me.getId(), peers.size()), recipient);
            }
        }
    }

    private class GreetResponseMessageHandler implements MessageHandler {

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
                    noIdSocketDetails.add(me.getTcpSocketDetails());
                    Collections.sort(noIdSocketDetails, SOCKET_DETAILS_COMPARATOR);

                    for (SocketDetails sd : noIdSocketDetails) {
                        maxId++;
                        if (sd == me.getTcpSocketDetails()) {
                            myId = maxId;
                            break;
                        }
                    }
                }
                if (myId == -1) {
                    throw new AssertionError("Couldn't come up with an ID for myself!");
                }
                me.setId(myId);
                LOG.info("Set my id to {}", myId);

                peers.addAll(respondedPeers.values());

                done = true;
            }
        }

        @Override
        public synchronized void handle(Message message, SocketDetails source) {
            if (message.getType() == MessageType.GREET_RESPONSE) {
                GreetResponseMessage grm = (GreetResponseMessage) message;
                if (done) {
                    // TODO: better handling of late responses?
                    LOG.warn("Shouldn't be receiving GreetResponseMessages now");
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
