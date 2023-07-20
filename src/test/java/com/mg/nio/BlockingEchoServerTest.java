package com.mg.nio;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

public class BlockingEchoServerTest {

    private int getFreePort() throws IOException {
        var serverSocket = new ServerSocket(0);
        var port = serverSocket.getLocalPort();
        serverSocket.close();
        return port;
    }

    private static class Connection {
        private static final Logger logger = getLogger(Connection.class);
        private final Socket socket;

        private Connection(Socket socket) {
            this.socket = socket;
        }

        private static Connection startWith(int port) throws IOException {
            var socket = new Socket("localhost", port);
            logger.info("Started connection to {}", socket.getRemoteSocketAddress());
            return new Connection(socket);
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

        public Connection close() throws IOException {
            socket.getInputStream().close();
            return this;
        }

        public int receive() throws IOException {
            return socket.getInputStream().read();
        }

        public byte[] receiveNBytes(int length) throws IOException {
            return socket.getInputStream().readNBytes(length);
        }

    }

    @Test
    public void shouldAcceptOnlyOneConnection() throws IOException, InterruptedException {
        // given
        var port = getFreePort();
        var singleThreadExecutor = Executors.newSingleThreadExecutor();
        var latch = new CountDownLatch(1);
        var server = new BlockingEchoServer(singleThreadExecutor, port, latch::countDown);
        server.start();

        latch.await();
        // when
        Connection.startWith(port);
        Connection.startWith(port);
        // then
        assertThat(server.getAcceptedConnections()).isEqualTo(1);

        server.stop();
        singleThreadExecutor.shutdownNow();
    }

    @Test
    public void shouldEchoNonLetter() throws Exception {
        // given
        var port = getFreePort();
        var singleThreadExecutor = Executors.newSingleThreadExecutor();
        var latch = new CountDownLatch(1);
        var server = new BlockingEchoServer(singleThreadExecutor, port, latch::countDown);
        server.start();

        latch.await();
        var connection = Connection.startWith(port);
        // when
        connection.send(1);
        var data = connection.receive();
        // then
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
        var latch = new CountDownLatch(1);
        var server = new BlockingEchoServer(singleThreadExecutor, port, latch::countDown);
        server.start();

        latch.await();
        var connection = Connection.startWith(port);
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
