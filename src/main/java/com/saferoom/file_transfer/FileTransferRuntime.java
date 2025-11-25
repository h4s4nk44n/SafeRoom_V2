package com.saferoom.file_transfer;

import java.util.concurrent.StructuredTaskScope;

/**
 * Manages the ancillary loops (NACK, retransmission, stats) using structured concurrency.
 */
public final class FileTransferRuntime implements AutoCloseable {
    private final StructuredTaskScope.ShutdownOnFailure scope =
        new StructuredTaskScope.ShutdownOnFailure();

    public void start(Runnable task) {
        scope.fork(() -> {
            task.run();
            return null;
        });
    }

    @Override
    public void close() {
        scope.close();
        scope.throwIfFailed(ex -> new RuntimeException("Transfer subtask failed", ex));
    }
}

