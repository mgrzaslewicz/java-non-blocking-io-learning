package com.mg.nio;

import java.io.IOException;
import java.net.ServerSocket;

public class FreePortFinder {
    public static int getFreePort() throws IOException {
        var serverSocket = new ServerSocket(0);
        var port = serverSocket.getLocalPort();
        serverSocket.close();
        return port;
    }
}
