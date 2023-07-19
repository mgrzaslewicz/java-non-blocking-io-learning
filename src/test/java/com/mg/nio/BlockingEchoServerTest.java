package com.mg.nio;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class BlockingEchoServerTest {

    private int getFreePort() throws IOException {
        var serverSocket = new ServerSocket(0);
        var port = serverSocket.getLocalPort();
        serverSocket.close();
        return port;
    }

    @Test
    public void shouldBlockThread() throws IOException {
        // given
        var port = getFreePort();
        var singleThreadExecutor = Executors.newSingleThreadExecutor();
        var server = new BlockingEchoServer(singleThreadExecutor, port);
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

    @Test
    public void shouldEchoNonLetter() throws Exception {
        // given
        var port = getFreePort();
        var singleThreadExecutor = Executors.newSingleThreadExecutor();
        var server = new BlockingEchoServer(singleThreadExecutor, port);
        // when
        server.start();
        // then
        var clientSocket = new Socket("localhost", port);
        var out = clientSocket.getOutputStream();
        out.write(1);
        out.flush();
        var in = clientSocket.getInputStream();
        var data = in.read();
        assertThat(data).isEqualTo(1);

        server.stop();
        singleThreadExecutor.shutdownNow();
    }
    @Test
    public void shouldMakeUppercase() throws Exception {
        // given
        var port = getFreePort();
        var singleThreadExecutor = Executors.newSingleThreadExecutor();
        var server = new BlockingEchoServer(singleThreadExecutor, port);
        server.start();

        var clientSocket = new Socket("localhost", port);
        var out = clientSocket.getOutputStream();
        var in = clientSocket.getInputStream();
        var messageOut = "hello".getBytes();
        // when
        out.write(messageOut);
        var messageIn = in.readNBytes(messageOut.length);
        // then
        assertThat(new String(messageIn)).isEqualTo("HELLO");

        out.close();
        in.close();
        server.stop();
        singleThreadExecutor.shutdownNow();
    }
}
