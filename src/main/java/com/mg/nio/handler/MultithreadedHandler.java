package com.mg.nio.handler;

import java.net.Socket;
import java.util.concurrent.ExecutorService;

public class MultithreadedHandler implements Handler {
    private final Handler decorated;
    private final ExecutorService executorService;

    public MultithreadedHandler(Handler decorated, ExecutorService executorService) {
        this.decorated = decorated;
        this.executorService = executorService;
    }

    @Override
    public void handle(Socket socket) {
        executorService.submit(() -> decorated.handle(socket));
    }
}
