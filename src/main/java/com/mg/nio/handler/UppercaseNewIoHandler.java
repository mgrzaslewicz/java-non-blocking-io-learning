package com.mg.nio.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;

public class UppercaseNewIoHandler implements Handler<SocketChannel> {

    @Override
    public void handle(SocketChannel socket) {
        try (
                socket;
                var in = socket.socket().getInputStream();
                var out = socket.socket().getOutputStream();
        ) {
            uppercaseInToOut(in, out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void uppercaseInToOut(InputStream in, OutputStream out) throws IOException {
        var data = in.read();
        while (data != -1) {
            out.write(uppercase(data));
            data = in.read();
        }
        in.close();
        out.close();
    }

    private int uppercase(int data) {
        return Character.isLetter(data) ? data ^ ' ' : data;
    }

}
