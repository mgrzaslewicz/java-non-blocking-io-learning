package com.mg.nio;

import com.mg.nio.handler.Handler;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.slf4j.LoggerFactory.getLogger;

public class BlockingNewIoEchoServer {
    private static final Logger logger = getLogger(BlockingNewIoEchoServer.class);
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    private final int port;
    private final Handler<SocketChannel> handler;
    private final Runnable onStartedListening;

    public BlockingNewIoEchoServer(int port, Handler<SocketChannel> handler, Runnable onStartedListening) {
        this.port = port;
        this.handler = handler;
        this.onStartedListening = onStartedListening;
    }


    public void start() {
        new Thread(() -> {
            try (var serverSocketChannel = ServerSocketChannel.open()) {
                serverSocketChannel.configureBlocking(true);
                serverSocketChannel.socket().bind(new InetSocketAddress(port));
                logger.info("Started listening on {}", serverSocketChannel.getLocalAddress());
                onStartedListening.run();
                while (!stopped.get()) {
                    try {
                        var socket = serverSocketChannel.accept();
                        handler.handle(socket);
                    } catch (IOException e) {
                        logger.error("Error while handling client connection", e);
                        throw new UncheckedIOException(e);
                    }
                }
            } catch (IOException e) {
                logger.error("Error while creating socket for accepting incoming connections", e);
            }
        }).start();
    }

    public void stop() {
        stopped.set(true);
    }

}
