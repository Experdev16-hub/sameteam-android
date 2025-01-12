package com.example.sameteam.widget.simplecropview.util;

import java.util.concurrent.ArrayBlockingQueue;

public final class ThreadPoolQueue extends ArrayBlockingQueue<Runnable> {

    public ThreadPoolQueue(int capacity) {
        super(capacity);
    }

    @Override
    public boolean offer(Runnable e) {
        try {
            put(e);
        } catch (InterruptedException e1) {
            return false;
        }
        return true;
    }

}