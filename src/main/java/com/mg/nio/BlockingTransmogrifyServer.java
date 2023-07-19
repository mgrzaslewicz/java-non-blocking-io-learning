package com.mg.nio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class BlockingTransmogrifyServer {
    private final ExecutorService executorService;
    private final int port;
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    public BlockingTransmogrifyServer(ExecutorService executorService, int port) {
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
                transmogrifyInToOut(in, out);
                socket.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void transmogrifyInToOut(InputStream in, OutputStream out) throws IOException {
        var data = in.read();
        while (data != -1) {
            out.write(transmogrify(data));
            data = in.read();
        }
        in.close();
        out.close();
    }

    private int transmogrify(int data) {
        return Character.isLetter(data) ? data ^ ' ' : data;
    }

    public void stop() {
        stopped.set(true);
    }
}
