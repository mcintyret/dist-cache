package com.mcintyret.cache.socket;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

/**
 * User: tommcintyre
 * Date: 4/18/14
 */
public class SocketUtils {

    public static final String NETWORK_INTERFACE_NAME = "en1";

    public static final NetworkInterface NETWORK_INTERFACE = getNetworkInterface(NETWORK_INTERFACE_NAME);

    public static final InetAddress LOCALHOST = NETWORK_INTERFACE.getInetAddresses().nextElement();

    private static NetworkInterface getNetworkInterface(String name) {
        try {
            return NetworkInterface.getByName(name);
        } catch (SocketException e) {
            throw new IllegalStateException("Unable to get network address: " + name);
        }
    }
}
