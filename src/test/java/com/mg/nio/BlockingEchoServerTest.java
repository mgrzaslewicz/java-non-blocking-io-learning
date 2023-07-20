package com.mg.nio;

import com.mg.nio.handler.ExecutorServiceHandler;
import com.mg.nio.handler.LoggingHandler;
import com.mg.nio.handler.UppercaseHandler;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

public class BlockingEchoServerTest {

    private static class Connection {
        private static final Logger logger = getLogger(Connection.class);
        private final Socket socket;

        private Connection(Socket socket) {
            this.socket = socket;
        }

        private static Connection open(int port) throws IOException {
            logger.info("Starting connection at port {}...", port);
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
        var port = FreePortFinder.getFreePort();
        var singleThreadExecutor = Executors.newSingleThreadExecutor();
        var latch = new CountDownLatch(1);

        var handler = new LoggingHandler(new UppercaseHandler());
        var acceptedConnectionsHandler = new CountingAcceptedConnectionsHandler<>(handler);
        var server = new BlockingEchoServer(port, acceptedConnectionsHandler, latch::countDown);
        server.start();

        latch.await();
        // when
        Connection.open(port);
        Connection.open(port);
        // then
        assertThat(acceptedConnectionsHandler.getAcceptedConnections()).isEqualTo(1);

        server.stop();
        singleThreadExecutor.shutdownNow();
    }

    @Test
    public void shouldAccept2Connections() throws IOException, InterruptedException {
        // given
        var port = FreePortFinder.getFreePort();
        var threadPoolExecutor = Executors.newFixedThreadPool(2);
        var serverReadyLatch = new CountDownLatch(1);
        var countingAcceptedConnectionsHandler = new CountingAcceptedConnectionsHandler<>(new LoggingHandler(new UppercaseHandler()));
        var allConnectionsLatch = new CountDownLatch(2);
        var countdownLatchHandler = new CountdownLatchHandler<>(countingAcceptedConnectionsHandler, allConnectionsLatch);
        var multithreadedHandler = new ExecutorServiceHandler<>(countdownLatchHandler, threadPoolExecutor);
        var server = new BlockingEchoServer(port, multithreadedHandler, serverReadyLatch::countDown);
        server.start();

        serverReadyLatch.await();
        // when
        Connection.open(port);
        Connection.open(port);
        // then
        allConnectionsLatch.await();
        assertThat(countingAcceptedConnectionsHandler.getAcceptedConnections()).isEqualTo(2);

        server.stop();
        threadPoolExecutor.shutdownNow();
    }

    @Test
    public void shouldEchoNonLetter() throws Exception {
        // given
        var port = FreePortFinder.getFreePort();
        var singleThreadExecutor = Executors.newSingleThreadExecutor();
        var serverReadyLatch = new CountDownLatch(1);
        var handler = new LoggingHandler(new UppercaseHandler());
        var server = new BlockingEchoServer(port, handler, serverReadyLatch::countDown);
        server.start();

        serverReadyLatch.await();
        var connection = Connection.open(port);
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
        var port = FreePortFinder.getFreePort();
        var singleThreadExecutor = Executors.newSingleThreadExecutor();
        var serverReadyLatch = new CountDownLatch(1);
        var handler = new LoggingHandler(new UppercaseHandler());
        var server = new BlockingEchoServer(port, handler, serverReadyLatch::countDown);
        server.start();

        serverReadyLatch.await();
        var connection = Connection.open(port);
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
