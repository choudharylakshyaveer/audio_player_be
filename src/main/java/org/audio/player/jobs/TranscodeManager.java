package org.audio.player.jobs;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class TranscodeManager {

    private final Path baseTemp = Paths.get(System.getProperty("java.io.tmpdir"), "hls_cache");
    private final Map<Long, TranscodeJob> jobs = new ConcurrentHashMap<>();
    private final ScheduledExecutorService janitor = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService ioWaiter = Executors.newCachedThreadPool();

    public TranscodeManager() throws IOException {
        Files.createDirectories(baseTemp);
        // cleanup every minute
        janitor.scheduleAtFixedRate(this::cleanupIdleJobs, 60, 60, TimeUnit.SECONDS);
    }

    public TranscodeJob getOrCreateJob(Long trackId, Path sourceFile, boolean lossless, int hlsTimeSec) throws IOException {
        return jobs.compute(trackId, (k, existing) -> {
            if (existing != null && !existing.isStopped()) {
                existing.touch();
                return existing;
            }
            try {
                TranscodeJob job = new TranscodeJob(trackId, sourceFile, baseTemp, lossless, hlsTimeSec, ioWaiter);
                job.start();
                return job;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public TranscodeJob getJob(Long trackId) {
        TranscodeJob j = jobs.get(trackId);
        if (j != null) j.touch();
        return j;
    }

    private void cleanupIdleJobs() {
        Instant now = Instant.now();
        for (Map.Entry<Long, TranscodeJob> e : jobs.entrySet()) {
            TranscodeJob job = e.getValue();
            if (job.isIdleFor(Duration.ofMinutes(10))) { // configurable TTL
                job.stop();
                jobs.remove(e.getKey(), job);
            }
        }
    }
}
