package com.adamluckey;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import static com.adamluckey.Main.EOF;

/**
 * This is an abnormal use of locks for basic unlock/lock demonstration
 * and not the typical use of locking and unlocking using the reentrant lock
 */
public class Main {
    public static final String EOF = "EOF";

    public static void main(String[] args) {
        // Array list isn't thread safe, it needs to be synchronized below
        List<String> buffer = new ArrayList<>();
        ReentrantLock bufferLock = new ReentrantLock(); // prevent thread interference using a lock
        ExecutorService executorService = Executors.newFixedThreadPool(5); // set up a thread pool and fix it to 3
        MyProducer producer = new MyProducer(buffer, ThreadColor.ANSI_GREEN, bufferLock);
        MyConsumer consumer1 = new MyConsumer(buffer, ThreadColor.ANSI_PURPLE, bufferLock);
        MyConsumer consumer2 = new MyConsumer(buffer, ThreadColor.ANSI_CYAN, bufferLock);

        executorService.execute(producer); // add things to the buffer
        executorService.execute(consumer1); // remove things from the buffer
        executorService.execute(consumer2); // remove things from the buffer

        // this blocks until result is available
        Future<String> future = executorService.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                System.out.println(ThreadColor.ANSI_RED+"I'm being printed for the Callable class");
                return "This is the callable result";
            }
        });

        try{
            System.out.println(future.get());
        }catch (ExecutionException e){
            System.out.println("Something went wrong");
        }catch (InterruptedException e){
            System.out.println("Thread running the task was interrupted");
        }

        executorService.shutdown();
    }
}

class MyProducer implements Runnable {
    private List<String> buffer;
    private String color;
    private ReentrantLock bufferLock;

    public MyProducer(List<String> buffer, String color, ReentrantLock bufferLock) {
        this.buffer = buffer;
        this.color = color;
        this.bufferLock = bufferLock;
    }


    public void run() {
        String[] nums = {"1", "2", "3", "4", "5"};
        // add a number to the buffer array list
        for (String num : nums) {
            try {
                System.out.println(color + "Adding... " + num);
                bufferLock.lock();
                try {
                    buffer.add(num);
                } finally {
                    bufferLock.unlock();
                }
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println("Producer was interrupted");
            }
        }
        System.out.println(color + "Adding EOF and exiting...");
        bufferLock.lock();
        try {
            buffer.add("EOF");
        } finally {
            bufferLock.unlock();
        }
    }
}

class MyConsumer implements Runnable {
    private List<String> buffer;
    private String color;
    private ReentrantLock bufferLock;

    public MyConsumer(List<String> buffer, String color, ReentrantLock bufferLock) {
        this.buffer = buffer;
        this.color = color;
        this.bufferLock = bufferLock;
    }

    public void run() {
        int counter = 0;

        // remove entries from the buffer
        while (true) {
            // if lock is available, automatically lock
            if (bufferLock.tryLock()) {
                try {
                    if (buffer.isEmpty()) {
                        continue;
                    }
                    System.out.println("Lock attempts counter = " + counter);
                    counter = 0;
                    if (buffer.get(0).equals(EOF)) {
                        System.out.println(color + "Exiting");
                        break;
                    } else {
                        System.out.println(color + "Removed " + buffer.remove(0));
                    }
                } finally {
                    bufferLock.unlock();
                }
            } else {
                counter++;
            }
        }
    }
}