package org.wikipedia.kanzatbot;

@FunctionalInterface
public interface ThrowableRunnable {
    void run() throws Exception;
}
