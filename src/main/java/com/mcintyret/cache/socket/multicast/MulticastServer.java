package com.mcintyret.cache.socket.multicast;

import com.esotericsoftware.kryo.io.Output;
import com.mcintyret.cache.message.AddressedMessage;
import com.mcintyret.cache.message.Message;
import com.mcintyret.cache.socket.AbstractServer;
import com.mcintyret.cache.socket.SocketDetails;

import java.io.IOException;
import java.net.*;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Random;

/**
 * User: tommcintyre
 * Date: 4/16/14
 */
public class MulticastServer extends AbstractServer {

    public static final long UNIQUE_ID = new Random().nextLong(); // Good enough for now

    private static final int GROUP_PORT = 4555;
    private static final String GROUP_ADDRESS = "225.4.5.6";

    private static final InetAddress GROUP = groupAddress();

    private final DatagramChannel datagramChannel;

    private final SocketDetails socketDetails;

    private final DatagramSocket sendSocket = new DatagramSocket();

    public MulticastServer() throws IOException {

        this.datagramChannel = DatagramChannel.open(StandardProtocolFamily.INET)
                .setOption(StandardSocketOptions.SO_REUSEADDR, true)
                .bind(new InetSocketAddress(GROUP_PORT))
                .setOption(StandardSocketOptions.IP_MULTICAST_IF, NETWORK_INTERFACE);
//
        MembershipKey key = datagramChannel.join(GROUP, NETWORK_INTERFACE);

        this.socketDetails = new SocketDetails((InetSocketAddress) datagramChannel.getLocalAddress());
//        this.socketDetails = null;
    }

    @Override
    protected void handleReadableKey(SelectionKey key) throws IOException {
        DatagramChannel channel = (DatagramChannel) key.channel();
        SocketAddress remote = channel.receive(buffer);
        handleSocketMessage(new SocketDetails((InetSocketAddress) remote));
    }

    private static InetAddress groupAddress() {
        try {
            return InetAddress.getByName(GROUP_ADDRESS);
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Unable to find Multicast address");
        }
    }

    @Override
    protected void handleAcceptableKey(SelectionKey key) {
        throw new IllegalStateException("Should never happen!");
    }

    @Override
    protected Message processMessage(Message message, SocketDetails socketDetails) {
        MulticastMessage multicastMessage = (MulticastMessage) message;
        if (multicastMessage.getUniqueId() != UNIQUE_ID) {
            handle(multicastMessage.getMessage(), socketDetails);
            return multicastMessage.getMessage();
        } else {
            return null;
        }
    }

    @Override
    public SocketDetails getSocketDetails() {
        return socketDetails;
    }

    @Override
    protected int getOperations() {
        return SelectionKey.OP_READ;
    }

    @Override
    protected SelectableChannel getChannel() {
        return datagramChannel;
    }

    @Override
    protected void doSendMessage(AddressedMessage msg) throws IOException {
        byte[] buf = new byte[1000];
        Output output = new Output(buf);
        kryo.writeClassAndObject(output, new MulticastMessage(msg.getMessage(), UNIQUE_ID));
        DatagramPacket packet = new DatagramPacket(buf, buf.length, GROUP, GROUP_PORT);
        sendSocket.send(packet);
    }
}
