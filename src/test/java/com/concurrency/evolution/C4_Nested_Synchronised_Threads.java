package com.concurrency.evolution;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.concurrency.evolution.ConcurrencySupport.PERSISTENCE_FORK_FACTOR;
import static com.concurrency.evolution.ConcurrencySupport.ITERATION;
import static com.concurrency.evolution.ConcurrencySupport.SERVICE_A_LATENCY;
import static com.concurrency.evolution.ConcurrencySupport.SERVICE_B_LATENCY;
import static com.concurrency.evolution.ConcurrencySupport.persistence;
import static com.concurrency.evolution.ConcurrencySupport.service;
import static com.concurrency.evolution.ConcurrencySupport.start;
import static com.concurrency.evolution.ConcurrencySupport.stop;

@Slf4j
public class C4_Nested_Synchronised_Threads {

    @Test
    public void shouldExecuteIterationsConcurrently() throws InterruptedException {
        start();

        List<Thread> threads = new ArrayList<>();
        for (int iteration = 1; iteration <= ITERATION; iteration++) {
            Thread thread = new Thread(new Iteration(iteration));
            thread.start();
            threads.add(thread);
        }

        // Stop Condition - Not the most optimal but gets the work done
        for (Thread thread : threads) {
            thread.join();
        }

        stop();
    }

    static class Iteration implements Runnable {

        private final int iteration;
        private final List<String> serviceResult = new ArrayList<>();

        Iteration(int iteration) {
            this.iteration = iteration;
        }

        @SneakyThrows
        @Override
        public void run() {
            Thread threadA = new Thread(new Service(this, "A", SERVICE_A_LATENCY, iteration));
            Thread threadB = new Thread(new Service(this, "B", SERVICE_B_LATENCY, iteration));
            threadA.start();
            threadB.start();
            threadA.join();
            threadB.join();

            List<Thread> threads = new ArrayList<>();
            for (int i = 1; i <= PERSISTENCE_FORK_FACTOR; i++) {
                Thread thread = new Thread(new Persistence(i, serviceResult.get(0), serviceResult.get(1)));
                thread.start();
                threads.add(thread);
            }

            // Not the most optimal but gets the work done
            for (Thread thread : threads) {
                thread.join();
            }
        }

        public synchronized void addToResult(String result) {
            serviceResult.add(result);
        }
    }

    static class Service implements Runnable {

        private final Iteration callback;
        private final String name;
        private final long latency;
        private final int iteration;

        Service(Iteration callback, String name, long latency, int iteration) {
            this.callback = callback;
            this.name = name;
            this.latency = latency;
            this.iteration = iteration;
        }

        @Override
        public void run() {
            callback.addToResult(service(name, latency, iteration));
        }
    }

    static class Persistence implements Runnable {

        private final int fork;
        private final String serviceA;
        private final String serviceB;

        Persistence(int fork, String serviceA, String serviceB) {
            this.fork = fork;
            this.serviceA = serviceA;
            this.serviceB = serviceB;
        }

        @Override
        public void run() {
            persistence(fork, serviceA, serviceB);
        }
    }
}
