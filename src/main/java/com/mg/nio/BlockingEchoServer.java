package com.mg.nio;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.slf4j.LoggerFactory.getLogger;

public class BlockingEchoServer {
    private static final Logger logger = getLogger(BlockingEchoServer.class);
    private final ExecutorService executorService;
    private final int port;
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    public BlockingEchoServer(ExecutorService executorService, int port) {
        this.executorService = executorService;
        this.port = port;
    }


    public void start() {
        executorService.submit(this::startListening);
    }

    private void startListening() {
        try (var serverSocket = new ServerSocket(port)) {
            logger.info("Started listening on {}", serverSocket.getLocalSocketAddress());
            while (!stopped.get()) {
                var socket = serverSocket.accept();
                logger.info("Accepted connection from {}", socket.getRemoteSocketAddress());
                var in = socket.getInputStream();
                var out = socket.getOutputStream();
                uppercaseInToOut(in, out);
                socket.close();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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

}
