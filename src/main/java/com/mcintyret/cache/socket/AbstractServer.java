package com.mcintyret.cache.socket;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.mcintyret.cache.message.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * User: tommcintyre
 * Date: 4/16/14
 */
public abstract class AbstractServer extends AbstractMessageHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractServer.class);

    private static final String NETWORK_INTERFACE_NAME = "en1";

    protected static final NetworkInterface NETWORK_INTERFACE = getNetworkInterface(NETWORK_INTERFACE_NAME);

    private static NetworkInterface getNetworkInterface(String name) {
        try {
            return NetworkInterface.getByName(name);
        } catch (SocketException e) {
            throw new IllegalStateException("Unable to get network address: " + name);
        }
    }

    private final String name = getClass().getSimpleName();

    private final Queue<AddressedMessage> messageQueue = new ConcurrentLinkedQueue<>();

    protected final Selector selector;

    public AbstractServer() throws IOException {
        selector = Selector.open();
    }

    public final void start() throws IOException {
        LOG.info("{} starting", name);
        SelectableChannel channel = getChannel();
        channel.configureBlocking(false);
        try {
            channel.register(selector, getOperations());
            new Thread(new SocketRunnable(), name).start();
        } catch (ClosedChannelException e) {
            throw new IllegalStateException("Channel should not be closed!", e);
        }
    }

    public final void sendMessage(Message message) {
        sendMessage(new AddressedMessage(null, message));
    }

    public final void sendMessage(Message message, SocketDetails recipient) {
        sendMessage(new AddressedMessage(recipient, message));
    }

    public final void sendMessage(AddressedMessage message) {
        messageQueue.add(message);
        selector.wakeup();
    }

    private final class SocketRunnable implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    LOG.debug("{} selecting", name);
                    selector.select();
                    LOG.debug("{} selected something!", name);
                    for (SelectionKey key : selector.selectedKeys()) {
                        if (key.isAcceptable()) {
                            handleAcceptableKey(key);
                        } else if (key.isReadable()) {
                            handleReadableKey(key);
                        } else {
                            throw new IllegalStateException("Should only be read or accept");
                        }
                    }
                    selector.selectedKeys().clear();
                    AddressedMessage msg;
                    while ((msg = messageQueue.poll()) != null) {
                        Object recipient = msg.getRecipient() == null ? "everyone" : msg.getRecipient();
                        LOG.info("{} sending {} to {}", name, msg.getMessage().getType(), recipient);
                        doSendMessage(msg);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected abstract void handleReadableKey(SelectionKey key) throws IOException;

    protected abstract void handleAcceptableKey(SelectionKey key) throws IOException;

    protected final Kryo kryo = new Kryo();

    protected final ByteBuffer buffer = ByteBuffer.allocate(10000);

    protected void handleSocketMessage(SocketDetails socketDetails) throws IOException {
        Message message = (Message) kryo.readClassAndObject(new Input(buffer.array()));

        Message processedMessage = processMessage(message, socketDetails);

        if (processedMessage != null) {
            LOG.info("{} received {} from {}", name, processedMessage.getType(), socketDetails);
        }

        buffer.clear();
    }

    protected abstract Message processMessage(Message message, SocketDetails socketDetails);

    protected void sendMessageOnChannel(WritableByteChannel channel, Message message) throws IOException {
        Output output = new Output(buffer.array());
        kryo.writeClassAndObject(output, message);

        channel.write(buffer);

        buffer.clear();
    }

    public abstract SocketDetails getSocketDetails();

    protected abstract int getOperations();

    protected abstract SelectableChannel getChannel();

    protected abstract void doSendMessage(AddressedMessage msg) throws IOException;

}
