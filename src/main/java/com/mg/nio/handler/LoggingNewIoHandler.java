package com.mg.nio.handler;

import org.slf4j.Logger;

import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

import static org.slf4j.LoggerFactory.getLogger;

public class LoggingNewIoHandler implements Handler<SocketChannel> {
    private static final Logger logger = getLogger(LoggingNewIoHandler.class);
    private final Handler<SocketChannel> decorated;

    public LoggingNewIoHandler(Handler<SocketChannel> decorated) {
        this.decorated = decorated;
    }

    @Override
    public void handle(SocketChannel socket) {
        SocketAddress socketAddress = null;
        try {
            socketAddress = socket.getRemoteAddress();
            logger.info("Accepted connection from {}", socketAddress);
            decorated.handle(socket);
        } catch (Exception e) {
            logger.error("Error while handling client connection", e);
            throw new RuntimeException(e);
        } finally {
            logger.info("Finished handling connection from {}", socketAddress);
        }
    }
}
