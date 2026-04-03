package com.example.zk.simple;

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class Main {

    private static final String CONNECTION_STRING = "127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183";
    private static final int SESSION_TIMEOUT_MS = 3000;

    public static void main(String[] args) throws IOException, InterruptedException {
        CountDownLatch connectedLatch = new CountDownLatch(1);

        ZooKeeper zk = new ZooKeeper(CONNECTION_STRING, SESSION_TIMEOUT_MS, event -> {
            if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                System.out.println("ZooKeeper connected");
                connectedLatch.countDown();
            }
        });

        Thread stateWatcher = new Thread(() -> {
            while (true) {
                try {
                    System.out.println("ZK State: " + zk.getState());
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        stateWatcher.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("Shutdown signal received, closing ZooKeeper client...");
                if (zk != null) {
                    zk.close();
                }
                System.out.println("ZooKeeper client closed");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }));

        connectedLatch.await();
        System.out.println("Connected to ZooKeeper cluster: " + CONNECTION_STRING);
        Thread.currentThread().join();
    }
}
