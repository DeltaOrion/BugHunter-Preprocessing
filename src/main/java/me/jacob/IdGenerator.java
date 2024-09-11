package me.jacob;

import java.util.concurrent.atomic.AtomicInteger;

public class IdGenerator {

    private static AtomicInteger Id = new AtomicInteger(0);

    public static int getId() {
        return Id.getAndIncrement();
    }
}
