package com.mg.nio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class BlockingEchoServer {
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
            while (!stopped.get()) {
                var socket = serverSocket.accept();
                var in = socket.getInputStream();
                var out = socket.getOutputStream();
                transferInToOut(in, out);
                socket.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void transferInToOut(InputStream in, OutputStream out) throws IOException {
        var data = in.read();
        while (data != -1) {
            out.write(data);
            data = in.read();
        }
        in.close();
        out.close();
    }

    public void stop() {
        stopped.set(true);
    }
}
