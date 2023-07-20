package com.mg.nio;

import com.mg.nio.handler.ExecutorServiceHandler;
import com.mg.nio.handler.Handler;
import com.mg.nio.handler.LoggingNewIoHandler;
import com.mg.nio.handler.UppercaseNewIoHandler;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

public class BlockingNewIoEchoServerTest {

    private int getFreePort() throws IOException {
        var serverSocket = new ServerSocket(0);
        var port = serverSocket.getLocalPort();
        serverSocket.close();
        return port;
    }

    private static class Connection {
        private static final Logger logger = getLogger(Connection.class);
        private final SocketChannel socket;

        private Connection(SocketChannel socket) {
            this.socket = socket;
        }

        private static Connection startWith(int port) throws IOException {
            logger.info("Starting connection at port {}...", port);
            var socket = SocketChannel.open(new InetSocketAddress("localhost", port));
            logger.info("Started connection to {}", socket.getRemoteAddress());
            return new Connection(socket);
        }

        public void send(byte[] data) throws IOException {
            var out = socket.socket().getOutputStream();
            out.write(data);
        }

        public Connection send(int data) throws IOException {
            var out = socket.socket().getOutputStream();
            out.write(data);
            return this;
        }

        public Connection close() throws IOException {
            socket.socket().getInputStream().close();
            return this;
        }

        public int receive() throws IOException {
            return socket.socket().getInputStream().read();
        }

        public byte[] receiveNBytes(int length) throws IOException {
            return socket.socket().getInputStream().readNBytes(length);
        }

    }

    private static class CountingAcceptedConnectionsHandler<T> implements Handler<T> {
        private final Handler<T> decorated;
        private final AtomicInteger acceptedConnections = new AtomicInteger(0);

        private CountingAcceptedConnectionsHandler(Handler<T> decorated) {
            this.decorated = decorated;
        }

        @Override
        public void handle(T socket) {
            acceptedConnections.incrementAndGet();
            decorated.handle(socket);
        }

        public int getAcceptedConnections() {
            return acceptedConnections.get();
        }
    }

    private record CountdownLatchHandler<T>(Handler<T> decorated, CountDownLatch latch) implements Handler<T> {
        @Override
        public void handle(T socket) {
            latch.countDown();
            decorated.handle(socket);
        }
    }

    @Test
    public void shouldAcceptOnlyOneConnection() throws IOException, InterruptedException {
        // given
        var port = getFreePort();
        var singleThreadExecutor = Executors.newSingleThreadExecutor();
        var latch = new CountDownLatch(1);

        var handler = new LoggingNewIoHandler(new UppercaseNewIoHandler());
        var acceptedConnectionsHandler = new CountingAcceptedConnectionsHandler<>(handler);
        var server = new BlockingNewIoEchoServer(port, acceptedConnectionsHandler, latch::countDown);
        server.start();

        latch.await();
        // when
        Connection.startWith(port);
        Connection.startWith(port);
        // then
        assertThat(acceptedConnectionsHandler.getAcceptedConnections()).isEqualTo(1);

        server.stop();
        singleThreadExecutor.shutdownNow();
    }

    @Test
    public void shouldAccept2Connections() throws IOException, InterruptedException {
        // given
        var port = getFreePort();
        var threadPoolExecutor = Executors.newFixedThreadPool(2);
        var serverReadyLatch = new CountDownLatch(1);
        var countingAcceptedConnectionsHandler = new CountingAcceptedConnectionsHandler<>(new LoggingNewIoHandler(new UppercaseNewIoHandler()));
        var allConnectionsLatch = new CountDownLatch(2);
        var countdownLatchHandler = new CountdownLatchHandler<>(countingAcceptedConnectionsHandler, allConnectionsLatch);
        var multithreadedHandler = new ExecutorServiceHandler<>(countdownLatchHandler, threadPoolExecutor);
        var server = new BlockingNewIoEchoServer(port, multithreadedHandler, serverReadyLatch::countDown);
        server.start();

        serverReadyLatch.await();
        // when
        Connection.startWith(port);
        Connection.startWith(port);
        // then
        allConnectionsLatch.await();
        assertThat(countingAcceptedConnectionsHandler.getAcceptedConnections()).isEqualTo(2);

        server.stop();
        threadPoolExecutor.shutdownNow();
    }

    @Test
    public void shouldEchoNonLetter() throws Exception {
        // given
        var port = getFreePort();
        var singleThreadExecutor = Executors.newSingleThreadExecutor();
        var serverReadyLatch = new CountDownLatch(1);
        var handler = new LoggingNewIoHandler(new UppercaseNewIoHandler());
        var server = new BlockingNewIoEchoServer(port, handler, serverReadyLatch::countDown);
        server.start();

        serverReadyLatch.await();
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
        var serverReadyLatch = new CountDownLatch(1);
        var handler = new LoggingNewIoHandler(new UppercaseNewIoHandler());
        var server = new BlockingNewIoEchoServer(port, handler, serverReadyLatch::countDown);
        server.start();

        serverReadyLatch.await();
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