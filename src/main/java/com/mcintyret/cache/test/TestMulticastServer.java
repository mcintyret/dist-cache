package com.mcintyret.cache.test;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * User: tommcintyre
 * Date: 4/18/14
 */
public class TestMulticastServer {

    public static void main(String[] args) throws IOException {

        NetworkInterface ni = NetworkInterface.getByName(NETWORK_INTERFACE);

        DatagramChannel datagramChannel = DatagramChannel.open(StandardProtocolFamily.INET)
                .setOption(StandardSocketOptions.SO_REUSEADDR, true)
                .bind(new InetSocketAddress(GROUP_PORT))
                .setOption(StandardSocketOptions.IP_MULTICAST_IF, ni);
        datagramChannel.configureBlocking(false);
        //
        datagramChannel.join(InetAddress.getByName(GROUP_ADDRESS), ni);

        Selector selector = Selector.open();
        datagramChannel.register(selector, SelectionKey.OP_READ);
        ByteBuffer buf = ByteBuffer.allocate(1000);
        while (true) {
            selector.select();
            System.out.println("Selected something!");
            for (SelectionKey key : selector.selectedKeys()) {
                DatagramChannel channel = (DatagramChannel) key.channel();

                channel.receive(buf);
                System.out.println(new String(buf.array()));
                buf.clear();
            }
        }
//
//        MulticastSocket socket = new MulticastSocket(TestMulticastClient.GROUP_PORT);
//        socket.joinGroup(Inet4Address.getByName(TestMulticastClient.GROUP_ADDRESS));
//
//        while (true) {
//            byte[] buf = new byte[1000];
//            DatagramPacket packet = new DatagramPacket(buf, buf.length);
//            System.out.println("Receiving");
//            socket.receive(packet);
//
//            System.out.println(new String(buf));
//        }
    }


    private static final int GROUP_PORT = 4555;
    private static final String GROUP_ADDRESS = "225.4.5.6";

    private static final String NETWORK_INTERFACE = "en1";


}
