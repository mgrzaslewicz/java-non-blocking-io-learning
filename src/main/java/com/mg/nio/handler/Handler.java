package com.mg.nio.handler;

import java.net.Socket;

public interface Handler<T> {
    void handle(T socket);
}
