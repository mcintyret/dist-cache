package com.mcintyret.cache.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetSocketAddress;

/**
 * User: tommcintyre
 * Date: 4/18/14
 */
public class TestMulticastClient {

    static final int GROUP_PORT = 4555;
//    private static final int GROUP_PORT = 5000;
    static final String GROUP_ADDRESS = "225.4.5.6";
//    private static final String GROUP_ADDRESS = "127.0.0.1";

    public static void main(String[] args) throws IOException {

        DatagramSocket socket = new DatagramSocket();

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = reader.readLine()) != null) {
            socket.send(new DatagramPacket(line.getBytes(), line.getBytes().length, new InetSocketAddress(GROUP_ADDRESS, GROUP_PORT)));
            System.out.println("wrote: " + line);
        }
    }
}
