package com.mg.nio;

import com.mg.nio.handler.Handler;

import java.util.concurrent.CountDownLatch;

record CountdownLatchHandler<T>(Handler<T> decorated, CountDownLatch latch) implements Handler<T> {
    @Override
    public void handle(T socket) {
        latch.countDown();
        decorated.handle(socket);
    }
}
