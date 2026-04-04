package com.example.nio.echo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;

public class NioServer {
    final static int PORT_NUMBER = 12345;

    public static void main(String[] args) {
        new NioServer().start(PORT_NUMBER);
    }

    public void start(final int portNumber) {
        HashSet<SocketChannel> clients = new HashSet<>();
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
             Selector selector = Selector.open()) {
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(portNumber));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            while (true) {
                if (selector.select() == 0) {
                    continue;
                }
                for (SelectionKey key : selector.selectedKeys()) {
                    if (key.isAcceptable()) {
                        if (key.channel() instanceof ServerSocketChannel) {
                            ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                            SocketChannel client = channel.accept();
                            Socket socket = client.socket();
                            String clientInfo = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
                            System.out.println("CONNECTED: " + clientInfo);
                            client.configureBlocking(false);
                            client.register(selector, SelectionKey.OP_READ);
                            clients.add(client);
                        } else {
                            throw new RuntimeException("Unknown channel");
                        }
                    } else if (key.isReadable()) {
                        if (key.channel() instanceof SocketChannel) {
                            SocketChannel client = (SocketChannel) key.channel();
                            int bytesRead = client.read(buffer);
                            if (bytesRead == -1) {
                                Socket socket = client.socket();
                                String clientInfo = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
                                System.out.println("DISCONNECTED: " + clientInfo);
                                client.close();
                                clients.remove(client);
                            }
                            buffer.flip();
                            String data = new String(buffer.array(),
                                    buffer.position(), bytesRead);
                            System.out.print(data);
                            for (SocketChannel entry : clients) {
                                while (buffer.hasRemaining()) {
                                    entry.write(buffer);
                                }
                                buffer.rewind();
                            }
                            buffer.clear();
                        } else {
                            throw new RuntimeException("Unknown channel");
                        }
                    }
                }
                selector.selectedKeys().clear();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            for (SocketChannel client : clients) {
                try {
                    client.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}