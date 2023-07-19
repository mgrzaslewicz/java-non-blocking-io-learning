package com.mg.nio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class BlockingEchoServer {
    private final ExecutorService executorService;
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    public BlockingEchoServer(ExecutorService executorService) {
        this.executorService = executorService;
    }


    public void start() {
        executorService.submit(this::startListening);
    }

    private void startListening() {
        try (var serverSocket = new ServerSocket(0)) {
            while (!stopped.get()) {
                var socket = serverSocket.accept();
                var in = socket.getInputStream();
                var out = socket.getOutputStream();
                transferInToOut(in, out);
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
    }

    public void stop() {
        stopped.set(true);
    }
}
