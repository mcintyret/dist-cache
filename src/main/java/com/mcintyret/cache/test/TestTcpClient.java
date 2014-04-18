package com.mcintyret.cache.test;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * User: tommcintyre
 * Date: 9/30/13
 */
public class TestTcpClient {

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("127.0.0.1", 63407);

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while ((line = reader.readLine()) != null) {
            writer.write(line + "\n");
            writer.flush();
            System.out.println("wrote: " + line);
        }
    }
}
