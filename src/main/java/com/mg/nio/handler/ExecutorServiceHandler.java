package com.mg.nio.handler;

import java.util.concurrent.ExecutorService;

public class ExecutorServiceHandler<T> implements Handler<T> {
    private final Handler<T> decorated;
    private final ExecutorService executorService;

    public ExecutorServiceHandler(Handler<T> decorated, ExecutorService executorService) {
        this.decorated = decorated;
        this.executorService = executorService;
    }

    @Override
    public void handle(T socket) {
        executorService.submit(() -> decorated.handle(socket));
    }
}
