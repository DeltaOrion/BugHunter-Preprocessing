package me.jacob;

import java.util.concurrent.atomic.AtomicInteger;

public class IdGenerator {

    private static AtomicInteger NodeId = new AtomicInteger(0);

    private static AtomicInteger GraphId = new AtomicInteger(0);

    public static int getNodeId() {
        return NodeId.getAndIncrement();
    }

    public static int getGraphId() {
        return GraphId.getAndIncrement();
    }
}
