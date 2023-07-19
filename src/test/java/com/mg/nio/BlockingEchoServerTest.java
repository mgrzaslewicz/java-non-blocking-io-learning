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

    private static class Connection {
        private final Socket socket;

        private Connection(Socket socket) {
            this.socket = socket;
        }

        private static Connection withPort(int port) throws IOException {
            return new Connection(new Socket("localhost", port));
        }

        public void send(byte[] data) throws IOException {
            var out = socket.getOutputStream();
            out.write(data);
        }

        public Connection send(int data) throws IOException {
            var out = socket.getOutputStream();
            out.write(data);
            return this;
        }

        public void close() throws IOException {
            socket.getInputStream().close();
        }

        public int receive() throws IOException {
            return socket.getInputStream().read();
        }

        public byte[] receiveNBytes(int length) throws IOException {
            return socket.getInputStream().readNBytes(length);
        }
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
        var connection = Connection.withPort(port);
        connection.send(1);
        var data = connection.receive();
        assertThat(data).isEqualTo(1);

        server.stop();
        connection.close();
        singleThreadExecutor.shutdownNow();
    }

    @Test
    public void shouldMakeUppercase() throws Exception {
        // given
        var port = getFreePort();
        var singleThreadExecutor = Executors.newSingleThreadExecutor();
        var server = new BlockingEchoServer(singleThreadExecutor, port);
        server.start();

        var connection = Connection.withPort(port);
        var messageOut = "hello".getBytes();
        // when
        connection.send(messageOut);
        var messageIn = connection.receiveNBytes(messageOut.length);
        // then
        assertThat(new String(messageIn)).isEqualTo("HELLO");

        connection.close();
        server.stop();
        singleThreadExecutor.shutdownNow();
    }
}
