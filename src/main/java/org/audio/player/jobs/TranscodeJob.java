package org.audio.player.jobs;


import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Starts FFmpeg to write playlist.m3u8 and segment_%03d.ts into a temp dir.
 * Exposes methods to wait for playlist or for a segment file to appear.
 */
public final class TranscodeJob {

    private final Long trackId;
    private final Path sourceFile;
    private final Path tempDir;
    private final boolean lossless;
    private final int hlsTimeSec;
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final Instant createdAt;
    private volatile Instant lastAccess = Instant.now();
    private final CompletableFuture<Path> playlistReady = new CompletableFuture<>();
    private final ExecutorService logReader = Executors.newSingleThreadExecutor();
    private Process process;
    private final ExecutorService waiter;

    public TranscodeJob(Long trackId, Path sourceFile, Path baseTemp, boolean lossless, int hlsTimeSec, ExecutorService waiter) throws IOException {
        this.trackId = trackId;
        this.sourceFile = sourceFile;
        this.tempDir = Files.createTempDirectory(baseTemp, "hls_" + trackId + "_");
        this.lossless = lossless;
        this.hlsTimeSec = hlsTimeSec;
        this.createdAt = Instant.now();
        this.waiter = waiter;
    }

    public void start() throws IOException {
        List<String> cmd = buildCommand();
        ProcessBuilder pb;
        if (isWindows()) {
            // run through cmd /c to allow complex quoting
            List<String> full = new ArrayList<>();
            full.add("cmd");
            full.add("/c");
            full.add(String.join(" ", cmd)); // safe-ish because we quote paths
            pb = new ProcessBuilder(full);
        } else {
            pb = new ProcessBuilder(cmd);
        }
        pb.directory(tempDir.toFile());
        pb.redirectErrorStream(true);
        process = pb.start();

        // watch for playlist file
        watcherForFileAsync(tempDir.resolve("playlist.m3u8"));

        // read output (for debug) so ffmpeg won't block on buffers
        logReader.submit(() -> {
            try (var in = process.getInputStream()) {
                byte[] buf = new byte[4096];
                int r;
                while ((r = in.read(buf)) != -1) {
                    // optional: parse logs or print if debug
                }
            } catch (Exception ignored) {}
        });

        // don't block here; playlistReady will complete when file appears
    }

    private List<String> buildCommand() {
        /*
         Example FFmpeg command (writes files into tempDir):
         ffmpeg -ss 0 -i "C:\...\song.flac" -vn -c:a aac -b:a 192k -hls_time 10 -hls_list_size 0 -hls_segment_filename "segment_%03d.ts" "playlist.m3u8"
         */
        String in = quotePath(sourceFile.toAbsolutePath().toString());
        String playlist = quotePath(tempDir.resolve("playlist.m3u8").toAbsolutePath().toString());
        String segPattern = quotePath(tempDir.resolve("segment_%03d.ts").toAbsolutePath().toString());

        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-y");
        cmd.add("-i");
        cmd.add(in);
        cmd.add("-vn");
        if (lossless) {
            // write FLAC segments (may not be widely supported by clients)
            cmd.add("-c:a");
            cmd.add("flac");
        } else {
            cmd.add("-c:a");
            cmd.add("aac");
            cmd.add("-b:a");
            cmd.add("320k");
        }
        cmd.add("-threads");
        cmd.add("16"); // let ffmpeg choose
        cmd.add("-f");
        cmd.add("hls");
        cmd.add("-hls_time");
        cmd.add(String.valueOf(hlsTimeSec));
        cmd.add("-hls_list_size");
        cmd.add("0"); // full playlist
        cmd.add("-hls_flags");
        cmd.add("independent_segments");
        cmd.add("-hls_segment_filename");
        cmd.add(segPattern);
        cmd.add(playlist);
        return cmd;
    }

    private void watcherForFileAsync(Path playlistPath) {
        waiter.submit(() -> {
            try {
                // watch up to timeout for file to appear (adjust timeout as needed)
                int maxSeconds = 15;
                for (int i = 0; i < maxSeconds * 10; i++) {
                    if (Files.exists(playlistPath)) {
                        playlistReady.complete(playlistPath);
                        return;
                    }
                    Thread.sleep(100);
                }
                // timeout
                playlistReady.completeExceptionally(new TimeoutException("playlist not generated in time"));
            } catch (Exception e) {
                playlistReady.completeExceptionally(e);
            }
        });
    }

    public Path awaitPlaylist(long timeout, TimeUnit unit) throws Exception {
        touch();
        return playlistReady.get(timeout, unit);
    }

    public Path getTempDir() {
        touch();
        return tempDir;
    }

    public boolean isStopped() {
        return stopped.get();
    }

    public void stop() {
        if (!stopped.compareAndSet(false, true)) return;
        if (process != null && process.isAlive()) process.destroy();
        try { logReader.shutdownNow(); } catch (Exception ignored) {}
        // remove temp dir
        try {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (Exception ignored) {}
    }

    public boolean isIdleFor(Duration d) {
        return Instant.now().minus(d).isAfter(lastAccess);
    }

    public void touch() {
        lastAccess = Instant.now();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static String quotePath(String p) {
        // For ffmpeg it's safe to wrap paths with double quotes.
        // We also escape double quotes if present (unlikely in file paths).
        return "\"" + p.replace("\"", "\\\"") + "\"";
    }


}
