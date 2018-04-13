package dk.kb.tvsubtitleocr.lib.common;

import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;

public class Timer implements AutoCloseable {

    private final Logger log;
    private final String name;
    private final Instant starTime;
    private Instant stopTime;

    /**
     * Will get the current instant time when created, and use it to log the time difference when closed or stopped.
     * @param name The name of the action to log the time as.
     * @param log The logger to log to.
     */
    public Timer(String name, Logger log) {
        this.log = log;
        this.name = name;
        this.starTime = Instant.now();
    }

    protected void logResult(Logger log, String name, Instant starTime, Instant stopTime) {
        final long seconds = Duration.between(starTime, stopTime).getSeconds();
        log.info("{} took {} seconds to process.", name, seconds);
    }

    public void stop() {
        stopTime = Instant.now();
    }

    public void logResult() {
        logResult(log, name, starTime, stopTime);
    }

    @Override
    public void close() {
        stop();
        logResult();
    }

    public String getName() {
        return name;
    }

    public Instant getStarTime() {
        return starTime;
    }

    /**
     * The stop method must have been run first for the retrieved value to have been set.
     * @return
     */
    public Instant getStopTime() {
        return stopTime;
    }
}
