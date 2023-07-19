package com.mg.nio;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class BlockingEchoServerTest {

    @Test
    public void shouldBlockThread() {
        // given
        var singleThreadExecutor = Executors.newSingleThreadExecutor();
        var server = new BlockingEchoServer(singleThreadExecutor);
        // when
        server.start();
        // then
        var secondJobForTheThread = singleThreadExecutor.submit(() -> {
        });
        // then
        assertThatThrownBy(() -> secondJobForTheThread.get(100, TimeUnit.MILLISECONDS))
                .isInstanceOf(TimeoutException.class);
        server.stop();
        singleThreadExecutor.shutdownNow();
    }
}
