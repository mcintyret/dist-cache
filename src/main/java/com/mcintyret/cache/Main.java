package com.mcintyret.cache;

import com.mcintyret.cache.data.Cache;
import com.mcintyret.cache.http.HttpServer;
import com.mcintyret.cache.message.*;
import com.mcintyret.cache.peer.PeerDetails;
import com.mcintyret.cache.peer.Peers;
import com.mcintyret.cache.socket.multicast.MulticastServer;
import com.mcintyret.cache.socket.tcp.TcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
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

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    tcpServer.close();
                    multicastServer.close();
                } catch (IOException e) {
                    LOG.error("Error shutting down", e);
                }
            }
        });

        peers = new Peers(tcpServer.getSocketAddress());

        this.cache = new Cache(peers, tcpServer);

        multicastServer.addMessageHandler(MessageType.GREET, new GreetMessageHandler());
        tcpServer.addMessageHandler(MessageType.GREET_RESPONSE, new GreetResponseMessageHandler());
        tcpServer.addMessageHandler(MessageType.CONFIRM_ID, new ConfirmIdHandler());

        multicastServer.sendMessage(new GreetMessage(tcpServer.getSocketAddress().getPort()));


    }

    private class ConfirmIdHandler implements MessageHandler<ConfirmIdMessage> {

        @Override
        public void handle(ConfirmIdMessage message, InetSocketAddress source) {
            peers.confirmPeer(source, message.getId());
        }
    }

    private class GreetMessageHandler implements MessageHandler<GreetMessage> {
        @Override
        public void handle(GreetMessage message, InetSocketAddress source) {
            SocketAddress recipient = new InetSocketAddress(source.getAddress(), message.getPort());

            peers.addPendingPeer(recipient);

            tcpServer.sendMessage(new GreetResponseMessage(peers.getMyId(), peers.getTotalPeersCount() - 1), recipient);
        }
    }

    private final ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);

    private class GreetResponseMessageHandler implements MessageHandler<GreetResponseMessage> {

        private final long maxTimeMillis = 5 * 1000;

        private final Map<SocketAddress, PeerDetails> respondedPeers = new HashMap<>();

        private final List<SocketAddress> noIdSocketDetails = new ArrayList<>();

        private boolean done = false;

        private int expectedPeers = 0;

        private int maxId = 0;

        public GreetResponseMessageHandler() {
            exec.schedule(new Runnable() {
                @Override
                public void run() {
                    completeGreeting(true);
                }
            }, maxTimeMillis, TimeUnit.MILLISECONDS);
        }

        private synchronized void completeGreeting(boolean fromTimer) {
            if (!done) {
                if (fromTimer) {
                    LOG.info("Choosing id after timeout of {}ms", maxTimeMillis);
                }
                int myId = -1;
                if (noIdSocketDetails.isEmpty()) {
                    myId = maxId + 1;
                } else {
                    noIdSocketDetails.add(peers.getMyAddress());
                    Collections.sort(noIdSocketDetails, SOCKET_DETAILS_COMPARATOR);

                    for (SocketAddress address : noIdSocketDetails) {
                        maxId++;
                        if (address == peers.getMyAddress()) {
                            myId = maxId;
                            break;
                        }
                    }
                }
                if (myId == -1) {
                    throw new AssertionError("Couldn't come up with an ID for myself!");
                }
                peers.confirmPeer(peers.getMyAddress(), myId);
                LOG.info("Set my id to {}", myId);
                if (myId == 1) {
                    LOG.info("I am the magic number 1, therefore I am starting the HttpServer");
                    try {
                        new HttpServer(cache).start();
                    } catch (Exception e) {
                        throw new IllegalStateException("Unable to start HttpServer");
                    }
                }

                for (PeerDetails peer : respondedPeers.values()) {
                    tcpServer.sendMessage(new ConfirmIdMessage(myId), peer.getTcpSocketAddress());
                }

                peers.addConfirmedPeers(respondedPeers.values());

                done = true;
            }
        }

        @Override
        public synchronized void handle(GreetResponseMessage grm, InetSocketAddress source) {
            if (done) {
                // TODO: better handling of late responses?
                //- probably want to just start again?
                LOG.warn("Shouldn't be receiving GreetResponseMessages now, from {}", source);
            } else {
                expectedPeers = Math.max(expectedPeers, grm.getKnownTotalPeers());
                Integer id = grm.getId();

                if (id == null) {
                    noIdSocketDetails.add(source);
                } else {
                    maxId = Math.max(maxId, id);
                    respondedPeers.put(source, new PeerDetails(id, source));
                }

                int responded = respondedPeers.size() + noIdSocketDetails.size();

                LOG.info("Expecting {} peers, of which {} have responded", expectedPeers, responded);

                if (responded == expectedPeers) {
                    completeGreeting(false);
                }
            }
        }
    }

    private static final Comparator<SocketAddress> SOCKET_DETAILS_COMPARATOR = new Comparator<SocketAddress>() {
        @Override
        public int compare(SocketAddress o1, SocketAddress o2) {
            InetSocketAddress s1 = (InetSocketAddress) o1;
            InetSocketAddress s2 = (InetSocketAddress) o2;
            int comp = s1.getAddress().getHostAddress().compareTo(s2.getAddress().getHostAddress());
            if (comp != 0) {
                return comp;
            } else {
                return s2.getPort() - s1.getPort();
            }
        }
    };
}
