package com.mcintyret.cache.socket.multicast;

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
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * User: tommcintyre
 * Date: 4/16/14
 */
public class MulticastServer extends AbstractMessageHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MulticastServer.class);

    private static final long UNIQUE_ID = new Random().nextLong(); // Good enough for now

    private static final int GROUP_PORT = 4555;
    private static final String GROUP_ADDRESS = "225.4.5.6";

    private static final InetAddress GROUP = groupAddress();

    private final DatagramChannel datagramChannel;

    private final DatagramSocket sendSocket = new DatagramSocket();

    private final Selector selector;

    private final Kryo kryo = new Kryo();

    private final ByteBuffer buffer = ByteBuffer.allocate(1000);

    private final Queue<Message> messageQueue = new ConcurrentLinkedQueue<>();

    public MulticastServer() throws IOException {
        selector = Selector.open();

        this.datagramChannel = DatagramChannel.open(StandardProtocolFamily.INET)
                .setOption(StandardSocketOptions.SO_REUSEADDR, true)
                .bind(new InetSocketAddress(GROUP_PORT))
                .setOption(StandardSocketOptions.IP_MULTICAST_IF, SocketUtils.NETWORK_INTERFACE);
        datagramChannel.configureBlocking(false);
    }


    public final void start() throws IOException {
        LOG.info("Starting");
        datagramChannel.join(GROUP, SocketUtils.NETWORK_INTERFACE);
        try {
            datagramChannel.register(selector, SelectionKey.OP_READ);
            new Thread(new SocketRunnable(), "Multicast Server Thread").start();
        } catch (ClosedChannelException e) {
            throw new IllegalStateException("Channel should not be closed!", e);
        }
    }

    private void handleRead() throws IOException {
        SocketAddress remote = datagramChannel.receive(buffer);
        MulticastMessage message = (MulticastMessage) kryo.readClassAndObject(new UnsafeInput(buffer.array()));

        if (message.getUniqueId() != UNIQUE_ID) {
            LOG.info("Received {} from {}", message.getType(), remote);
            handle(message.getMessage(), (InetSocketAddress) remote);
        }

        buffer.clear();
    }

    private static InetAddress groupAddress() {
        try {
            return InetAddress.getByName(GROUP_ADDRESS);
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Unable to find Multicast address");
        }
    }


    private void doSendMessage(Message msg) throws IOException {
        byte[] buf = new byte[1000];
        Output output = new UnsafeOutput(buf);
        kryo.writeClassAndObject(output, new MulticastMessage(msg, UNIQUE_ID));
        DatagramPacket packet = new DatagramPacket(buf, buf.length, GROUP, GROUP_PORT);
        sendSocket.send(packet);
    }

    public void sendMessage(Message message) {
        messageQueue.add(message);
        selector.wakeup();
    }

    private final class SocketRunnable implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    selector.select();
                    for (SelectionKey key : selector.selectedKeys()) {
                        if (key.isReadable()) {
                            handleRead();
                        } else {
                            throw new IllegalStateException("Should only be read");
                        }
                    }
                    selector.selectedKeys().clear();
                    Message msg;
                    while ((msg = messageQueue.poll()) != null) {
                        LOG.info("Sending {} to everyone", msg.getType());
                        doSendMessage(msg);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void close() throws IOException {
        datagramChannel.close();
        sendSocket.close();
        selector.close();
    }
}
