package com.example.nio.echo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class NioClient {
    final static int PORT_NUMBER = 12345;

    public static void main(String[] args) {
        new NioClient().start(PORT_NUMBER, new Scanner(System.in));
    }

    public void start(final int portNumber, final Scanner scanner) {
        try (SocketChannel serverChannel = SocketChannel.open()) {
            serverChannel.connect(new InetSocketAddress(portNumber));
            serverChannel.configureBlocking(true);
            System.out.println("Connection established!");
            Runnable r = () -> {
                ByteBuffer buffer2 = ByteBuffer.allocate(1024);
                while (true) {
                    try {
                        int bytesRead = serverChannel.read(buffer2);
                        if (bytesRead > 0) {
                            buffer2.flip();
                            byte[] readData = new byte[bytesRead];
                            buffer2.get(readData);
                            System.out.print("SERVER: " + new String(readData));
                            buffer2.clear();
                        }
                    } catch (IOException e) {}
                }
            };
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.start();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            while (true) {
                String line = scanner.nextLine();
                if (line.equalsIgnoreCase("quit")) {
                    break;
                }
                line += System.lineSeparator();
                ((ByteBuffer)buffer.clear()).put(line.getBytes()).flip();
                while (buffer.hasRemaining()) {
                    serverChannel.write(buffer);
                }
                buffer.clear();
            }
            System.out.println("Client is closing...");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}