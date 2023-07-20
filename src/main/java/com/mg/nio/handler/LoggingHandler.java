package com.mg.nio.handler;

import org.slf4j.Logger;

import java.net.Socket;

import static org.slf4j.LoggerFactory.getLogger;

public class LoggingHandler implements Handler<Socket> {
    private static final Logger logger = getLogger(LoggingHandler.class);
    private final Handler<Socket> decorated;

    public LoggingHandler(Handler<Socket> decorated) {
        this.decorated = decorated;
    }

    @Override
    public void handle(Socket socket) {
        logger.info("Accepted connection from {}", socket.getRemoteSocketAddress());
        try {
            decorated.handle(socket);
        } catch (Exception e) {
            logger.error("Error while handling client connection", e);
            throw new RuntimeException(e);
        } finally {
            logger.info("Finished handling connection from {}", socket.getRemoteSocketAddress());

        }
    }
}
