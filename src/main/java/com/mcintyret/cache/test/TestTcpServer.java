package com.mcintyret.cache.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * User: tommcintyre
 * Date: 9/30/13
 */
public class TestTcpServer {

    public static void main(String[] args) throws IOException, InterruptedException {
        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        serverSocket.configureBlocking(false);
        serverSocket.bind(new InetSocketAddress("127.0.0.1", 1234));

        final Selector selector = Selector.open();

        serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        ByteBuffer buf = ByteBuffer.allocate(1000);

        while (true) {
            System.out.println("Selecting");
            selector.select();
            System.out.println("Got something!");

            for (SelectionKey key : selector.selectedKeys()) {
                if (key.isAcceptable()) {
                    SocketChannel socket = serverSocket.accept();
                    socket.configureBlocking(false);
                    System.out.println("Socket connected from " + socket.getRemoteAddress());
                    socket.register(selector, SelectionKey.OP_READ);
                } else if (key.isReadable()) {
                    SocketChannel socketChannel = (SocketChannel) key.channel();
                    socketChannel.read(buf);
                    System.out.println(new String(buf.array(), 0, buf.position()));
                    buf.clear();
                }
            }
            selector.selectedKeys().clear();
        }
    }
}
