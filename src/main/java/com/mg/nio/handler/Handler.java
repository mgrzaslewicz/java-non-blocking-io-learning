package com.mg.nio.handler;


public interface Handler<T> {
    void handle(T socket);
}
