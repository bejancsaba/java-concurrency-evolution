package com.concurrency.evolution;

import jdk.jfr.Configuration;
import jdk.jfr.Recording;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
class ConcurrencySupport {

    static final int USERS = 10;
    static final int PERSISTENCE_FORK_FACTOR = 3;

    static final long SERVICE_A_LATENCY = 1000;
    static final long SERVICE_B_LATENCY = 500;
    static final long PERSISTENCE_LATENCY = 300;

    static Recording recording;
    static long start;

    @SneakyThrows
    static String serviceA(int i) {
        Thread.sleep(SERVICE_A_LATENCY);
        return "A" + i;
    }

    @SneakyThrows
    static String serviceB(int i) {
        Thread.sleep(SERVICE_B_LATENCY);
        return "B" + i;
    }

    @SneakyThrows
    static String service(String name, long latency, int iteration) {
        Thread.sleep(latency);
        return name + iteration;
    }

    @SneakyThrows
    static String persistence(int persistence, String serviceA, String serviceB) {
        Thread.sleep(PERSISTENCE_LATENCY);
        log.info("[{}] [{}] Persistence-{} - Executed", serviceA, serviceB, persistence);
        return "";
    }

    static void start() {
        try {
            recording = new Recording(Configuration.getConfiguration("profile"));
            Path destination = Paths.get("java-concurrency.jfr");
            recording.setDestination(destination);
            recording.start();
        } catch (Exception e) {
            log.error("Error during starting of recording: {}", e.getMessage());
        }
        log.info("Starting...");
        start = System.currentTimeMillis();
    }

    static void stop(int threadCount, long expectedTime) {
        long finished = System.currentTimeMillis() - start;
        log.info("---------------------------");
        log.info("{} ms - Total Time", finished);
        log.info("{} ms - Expected Time", expectedTime);
        log.info("{} ms - Administrative time", finished - expectedTime);
        log.info("{} - Legacy Thread Count", threadCount);
        recording.stop();
        recording.close();
        log.info("Recording at: {}", recording.getDestination().toAbsolutePath());
    }

    static void stop() {
        stop(fullConcurrencyThreadCount(), fullConcurrencyExpectedTime());
    }

    static int fullConcurrencyThreadCount() {
        return USERS + USERS * (2 + PERSISTENCE_FORK_FACTOR);
    }

    static int fullConcurrencyExpectedTime() {
        return (int) SERVICE_A_LATENCY + (int) PERSISTENCE_LATENCY;
    }
}
