package com.mg.nio;

import com.mg.nio.handler.Handler;

import java.util.concurrent.atomic.AtomicInteger;

public class CountingAcceptedConnectionsHandler<T> implements Handler<T> {
        private final Handler<T> decorated;
        private final AtomicInteger acceptedConnections = new AtomicInteger(0);

        CountingAcceptedConnectionsHandler(Handler<T> decorated) {
            this.decorated = decorated;
        }

        @Override
        public void handle(T socket) {
            acceptedConnections.incrementAndGet();
            decorated.handle(socket);
        }

        public int getAcceptedConnections() {
            return acceptedConnections.get();
        }
    }
