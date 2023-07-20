package com.mg.nio;

import com.mg.nio.handler.Handler;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.slf4j.LoggerFactory.getLogger;

public class BlockingEchoServer {
    private static final Logger logger = getLogger(BlockingEchoServer.class);
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    private final int port;
    private final Handler<Socket> handler;
    private final Runnable onStartedListening;

    public BlockingEchoServer(int port, Handler<Socket> handler, Runnable onStartedListening) {
        this.port = port;
        this.handler = handler;
        this.onStartedListening = onStartedListening;
    }


    public void start() {
        new Thread(() -> {
            try (var serverSocket = new ServerSocket(port)) {
                logger.info("Started listening on {}", serverSocket.getLocalSocketAddress());
                onStartedListening.run();
                while (!stopped.get()) {
                    try {
                        var socket = serverSocket.accept();
                        handler.handle(socket);
                    } catch (IOException e) {
                        logger.error("Error while handling client connection", e);
                        throw new UncheckedIOException(e);
                    }
                }
            } catch (IOException e) {
                logger.error("Error while creating socket for accepting incoming connections", e);
                throw new UncheckedIOException(e);
            }
        }).start();
    }

    public void stop() {
        stopped.set(true);
    }

}
