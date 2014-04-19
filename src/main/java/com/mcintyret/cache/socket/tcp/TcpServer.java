package com.mcintyret.cache.socket.tcp;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.UnsafeInput;
import com.esotericsoftware.kryo.io.UnsafeOutput;
import com.mcintyret.cache.message.AbstractMessageHandler;
import com.mcintyret.cache.message.Message;
import com.mcintyret.cache.socket.SocketUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * User: tommcintyre
 * Date: 9/30/13
 */
public class TcpServer extends AbstractMessageHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TcpServer.class);

    private final ServerSocketChannel serverSocketChannel;

    private final Map<SocketAddress, SocketChannel> socketMap = new HashMap<>();

    private final InetSocketAddress socketAddress;

    private final Selector selector = Selector.open();

    protected final Kryo kryo = new Kryo();

    protected final ByteBuffer buffer = ByteBuffer.allocate(10000);

    private final Queue<ChangeRequest> changeRequests = new ConcurrentLinkedQueue<>();

    private final ConcurrentMap<SocketAddress, Queue<Message>> messageQueues = new ConcurrentHashMap<>();

    private AtomicBoolean closed = new AtomicBoolean();

    public TcpServer() throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(SocketUtils.LOCALHOST, 0));
        ServerSocket ss = serverSocketChannel.socket();
        this.socketAddress = (InetSocketAddress) ss.getLocalSocketAddress();
        LOG.info("My TCP details: {}", socketAddress);
    }

    public final void start() throws IOException {
        LOG.info("Starting TCP Server");
        serverSocketChannel.configureBlocking(false);
        try {
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            new Thread(new SocketRunnable(), "Tcp Server Thread").start();
        } catch (ClosedChannelException e) {
            throw new IllegalStateException("Channel should not be closed!", e);
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        channel.read(buffer);
        Message message = (Message) kryo.readClassAndObject(new UnsafeInput(buffer.array()));

        LOG.info("Received {} from {}", message.getType(), channel.getRemoteAddress());
        handle(message, (InetSocketAddress) channel.getRemoteAddress());

        buffer.clear();
    }

    private void handleAccept(SelectionKey key) throws IOException {
        SocketChannel newChannel = ((ServerSocketChannel) key.channel()).accept();
        LOG.info("Socket connected from {}, local port: {}", newChannel.getRemoteAddress(), newChannel.socket().getLocalPort());
        newChannel.configureBlocking(false);

        SocketAddress socketAddress = newChannel.getRemoteAddress();
        newChannel.register(selector, SelectionKey.OP_READ, socketAddress);
        socketMap.put(socketAddress, newChannel);
    }

    private void handleWrite(SelectionKey key) throws IOException {
        SocketAddress SocketAddress = (SocketAddress) key.attachment();

        Queue<Message> messageQueue = messageQueues.get(SocketAddress);
        Message message;
        ByteBuffer buffer = ByteBuffer.allocate(1000);
        while ((message = messageQueue.poll()) != null) {
            LOG.info("Sending {} to {}", message.getType(), SocketAddress);
            SocketChannel channel = (SocketChannel) key.channel();
            Output output = new UnsafeOutput(buffer.array());
            kryo.writeClassAndObject(output, message);

            channel.write(buffer);
            if (buffer.remaining() > 0) {
                // ... or the socket's buffer fills up
                break;
            }
            buffer.clear();
        }

        if (messageQueue.isEmpty()) {
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    private void handleConnect(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();

        try {
            channel.finishConnect();
            LOG.info("Socket connected to {}, local port: {}", channel.getRemoteAddress(), channel.socket().getLocalPort());

        } catch (IOException e) {
            LOG.error("Error finishing connection", e);
            key.cancel();
            return;
        }

        // Register an interest in writing on this channel
        key.interestOps(SelectionKey.OP_WRITE);
    }


    public InetSocketAddress getSocketAddress() {
        return socketAddress;
    }

    public void sendMessage(Message message, SocketAddress socketAddress) {
        if (closed.get()) {
            throw new IllegalStateException("Can't send message - TCP server is closed");
        }
        try {
            getMessageQueue(socketAddress).add(message);
            SocketChannel channel = socketMap.get(socketAddress);
            if (channel == null) {
                channel = SocketChannel.open();
                channel.configureBlocking(false);
                channel.connect(socketAddress);
                socketMap.put(socketAddress, channel);
                changeRequests.add(new ChangeRequest(channel, ChangeRequest.ChangeType.REGISTER, socketAddress));
            } else {
                changeRequests.add(new ChangeRequest(channel, ChangeRequest.ChangeType.WRITE, socketAddress));
            }
            selector.wakeup();
        } catch (Exception e) {
            LOG.error("Error sending message " + message.getType() + " to " + socketAddress, e);
        }
    }

    private final class SocketRunnable implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    selector.select();
                    for (SelectionKey key : selector.selectedKeys()) {
                        if (!key.isValid()) {
                            LOG.warn("Invlaid key: {}", key);
                            key.cancel();
                        }
                        if (key.isAcceptable()) {
                            handleAccept(key);
                        } else if (key.isReadable()) {
                            handleRead(key);
                        } else if (key.isWritable()) {
                            handleWrite(key);
                        } else if (key.isConnectable()) {
                            handleConnect(key);
                        } else {
                            throw new IllegalStateException();
                        }
                    }
                    selector.selectedKeys().clear();

                    ChangeRequest changeRequest;
                    while ((changeRequest = changeRequests.poll()) != null) {
                        LOG.debug("Handling change request: {}", changeRequest);
                        switch (changeRequest.type) {
                            case WRITE:
                                changeRequest.channel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
                                break;
                            case REGISTER:
                                changeRequest.channel.register(selector, SelectionKey.OP_CONNECT, changeRequest.socketAddress);

                        }
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class ChangeRequest {

        private enum ChangeType {WRITE, REGISTER}

        private final SocketChannel channel;

        private final ChangeType type;

        private final SocketAddress socketAddress;

        private ChangeRequest(SocketChannel channel, ChangeType type, SocketAddress socketAddress) {
            this.channel = channel;
            this.type = type;
            this.socketAddress = socketAddress;
        }

        @Override
        public String toString() {
            return "type: " + type + ", channel: " + channel + ", address: " + socketAddress;
        }
    }

    private Queue<Message> getMessageQueue(SocketAddress details) {
        Queue<Message> queue = messageQueues.get(details);
        if (queue == null) {
            Queue<Message> newQ = new ConcurrentLinkedQueue<>();
            Queue<Message> existing = messageQueues.putIfAbsent(details, newQ);
            queue = existing == null ? newQ : existing;
        }
        return queue;
    }

    public void close() throws IOException {
        if (!closed.getAndSet(true)) {
            messageQueues.clear();
            changeRequests.clear();
            selector.close();
            for (SocketChannel channel : socketMap.values()) {
                channel.close();
            }
            socketMap.clear();
        }
    }
}
