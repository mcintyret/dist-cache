package com.mcintyret.cache.socket;

import java.net.*;

/**
 * User: tommcintyre
 * Date: 4/16/14
 */
// TODO: just replace with InetSocketAddress?
public class SocketDetails {

    private final InetAddress inetAddress;

    private final int port;

    public SocketDetails(InetAddress inetAddress, int port) {
        this.inetAddress = inetAddress;
        this.port = port;
    }

    public SocketDetails(DatagramPacket packet) {
        this(packet.getAddress(), packet.getPort());
    }

    public SocketDetails(Socket socket) {
        this(socket.getInetAddress(), socket.getPort());
    }

    public SocketDetails(InetSocketAddress isa) {
        this(isa.getAddress(), isa.getPort());
    }

    @Override
    public String toString() {
        return inetAddress.toString() + ":" + port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SocketDetails that = (SocketDetails) o;

        if (port != that.port) return false;
        if (!inetAddress.equals(that.inetAddress)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = inetAddress.hashCode();
        result = 31 * result + port;
        return result;
    }

    public InetAddress getInetAddress() {
        return inetAddress;
    }

    public int getPort() {
        return port;
    }

    public SocketAddress asSocketAddress() {
        return new InetSocketAddress(inetAddress, port);
    }

}
