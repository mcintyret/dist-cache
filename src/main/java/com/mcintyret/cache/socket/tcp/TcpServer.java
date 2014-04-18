package com.mcintyret.cache.socket.tcp;

import com.mcintyret.cache.message.AddressedMessage;
import com.mcintyret.cache.message.Message;
import com.mcintyret.cache.socket.AbstractServer;
import com.mcintyret.cache.socket.SocketDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Map;

/**
 * User: tommcintyre
 * Date: 9/30/13
 */
public class TcpServer extends AbstractServer {

    private static final Logger LOG = LoggerFactory.getLogger(TcpServer.class);

    private static final InetAddress LOCALHOST = NETWORK_INTERFACE.getInetAddresses().nextElement();

    private final ServerSocketChannel serverSocketChannel;

    private final Map<SocketDetails, SocketChannel> socketMap = new HashMap<>();

    private final SocketDetails socketDetails;

    public TcpServer() throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(LOCALHOST, 0));
        ServerSocket ss = serverSocketChannel.socket();
        this.socketDetails = new SocketDetails(ss.getInetAddress(), ss.getLocalPort());
        LOG.info("My TCP details: {}", socketDetails);
    }

    @Override
    protected void handleReadableKey(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        channel.read(buffer);
        handleSocketMessage(new SocketDetails(channel.socket()));
    }

    @Override
    protected void handleAcceptableKey(SelectionKey key) throws IOException {
        SocketChannel newChannel = ((ServerSocketChannel) key.channel()).accept();
        LOG.info("Socket connected from {}", newChannel.getRemoteAddress());
        newChannel.configureBlocking(false);
        newChannel.register(selector, SelectionKey.OP_READ);
        SocketDetails socketDetails = new SocketDetails((InetSocketAddress) newChannel.getRemoteAddress());
        socketMap.put(socketDetails, newChannel);
    }

    @Override
    protected void processMessage(Message message, SocketDetails socketDetails) {
        informMessageHandlers(socketDetails, message);
    }

    @Override
    public SocketDetails getSocketDetails() {
        return socketDetails;
    }

    @Override
    protected int getOperations() {
        return SelectionKey.OP_ACCEPT;
    }

    @Override
    protected SelectableChannel getChannel() {
        return serverSocketChannel;
    }

    @Override
    protected void doSendMessage(AddressedMessage msg) throws IOException {
        SocketDetails socketDetails = msg.getRecipient();
        SocketChannel channel = socketMap.get(socketDetails);
        if (channel == null) {
            channel = SocketChannel.open(socketDetails.asSocketAddress());
            socketMap.put(socketDetails, channel);
        }
        sendMessageOnChannel(channel, msg.getMessage());
    }

}
