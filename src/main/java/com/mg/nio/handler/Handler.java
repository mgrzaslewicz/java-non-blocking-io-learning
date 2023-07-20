package com.mg.nio.handler;

import java.net.Socket;

public interface Handler {
    void handle(Socket socket);
}
