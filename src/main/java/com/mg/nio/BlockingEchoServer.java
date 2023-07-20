package com.mg.nio;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.slf4j.LoggerFactory.getLogger;

public class BlockingEchoServer {
    private static final Logger logger = getLogger(BlockingEchoServer.class);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final AtomicInteger acceptedConnections = new AtomicInteger(0);

    private final ExecutorService executorService;
    private final int port;
    private final Runnable onStartedListening;
    private final int numberOfParallelConnections;

    public BlockingEchoServer(ExecutorService executorService, int port, int numberOfParallelConnections) {
        this(executorService, port, () -> {
        }, numberOfParallelConnections);
    }

    public BlockingEchoServer(ExecutorService executorService, int port, Runnable onStartedListening, int numberOfParallelConnections) {
        this.executorService = executorService;
        this.port = port;
        this.onStartedListening = onStartedListening;
        this.numberOfParallelConnections = numberOfParallelConnections;
    }


    public void start() {
        new Thread(() -> {
            try (var serverSocket = new ServerSocket(port)) {
                logger.info("Started listening on {}", serverSocket.getLocalSocketAddress());
                onStartedListening.run();
                while (!stopped.get()) {
                    try {
                        var socket = serverSocket.accept();
                        handleConnection(socket);
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

    private void handleConnection(Socket socket) throws IOException {
        executorService.submit(() -> {
            acceptedConnections.incrementAndGet();
            logger.info("Accepted connection from {}", socket.getRemoteSocketAddress());
            try (socket; var in = socket.getInputStream(); var out = socket.getOutputStream();) {
                uppercaseInToOut(in, out);
            } catch (IOException e) {
                logger.error("Error while handling client connection", e);
                throw new UncheckedIOException(e);
            } finally {
                logger.info("Closed connection from {}", socket.getRemoteSocketAddress());
            }
        });
    }

    private void uppercaseInToOut(InputStream in, OutputStream out) throws IOException {
        var data = in.read();
        while (data != -1) {
            out.write(uppercase(data));
            data = in.read();
        }
        in.close();
        out.close();
    }

    private int uppercase(int data) {
        return Character.isLetter(data) ? data ^ ' ' : data;
    }

    public void stop() {
        stopped.set(true);
    }

    public int getAcceptedConnections() {
        return acceptedConnections.get();
    }

}
