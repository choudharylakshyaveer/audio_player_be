package org.audio.player.service;

import io.vavr.Tuple;
import lombok.extern.slf4j.Slf4j;
import org.audio.player.dto.MetadataScanResult;
import org.audio.player.entity.AudioTrack;
import org.audio.player.repository.AudioTrackRepo;
import org.audio.player.utils.FlacMetadata;
import org.audio.player.utils.Mp3Metadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MetadataService {

  @Autowired private Mp3Metadata mp3Metadata;

  @Autowired private FlacMetadata flacMetadata;

  @Autowired private AudioTrackRepo audioTrackRepo;

  @Autowired private AudioTrackLuceneIndexService luceneService;

  @Value("${metadata.folders}")
  private List<String> folderPaths;

  public MetadataScanResult saveMetadata() {

    Set<AudioTrack> audioTracks = new HashSet<>();
    Set<AudioTrack> duplicateTracks = new HashSet<>();

    Set<File> uploadedFiles = new HashSet<>();
    Set<File> notUploadedFiles = new HashSet<>();
    Set<File> failedFiles = new HashSet<>();

    for (String folderPath : folderPaths) {

      File root = normalizeRoot(folderPath);

      if (!root.exists() || !root.isDirectory()) {
        log.warn("Skipping invalid folder: {}", folderPath);
        continue;
      }

      Set<File> mp3Files = new HashSet<>();
      Set<File> flacFiles = new HashSet<>();

      try {
        collectAudioFiles(root, mp3Files, flacFiles);
      } catch (Exception e) {
        failedFiles.add(root);
        log.error("Failed scanning folder {}", root, e);
        continue;
      }

      // FLAC
      Tuple flacTracksTuple =
          flacMetadata.getFlacTracks(flacFiles.toArray(File[]::new), failedFiles);
      try {
        audioTracks.addAll((Collection<? extends AudioTrack>) flacTracksTuple.toSeq().get(0));
      } catch (Exception e) {
        failedFiles.addAll((Collection<? extends File>) flacTracksTuple.toSeq().get(1));
        log.error("FLAC metadata extraction failed", e);
      }

      // MP3
      try {
        audioTracks.addAll(mp3Metadata.getMp3Tracks(mp3Files.toArray(File[]::new)));
      } catch (Exception e) {
        failedFiles.addAll(mp3Files);
        log.error("MP3 metadata extraction failed", e);
      }
    }

    // ---------- SAVE + INDEX ----------
    ExecutorService executor = Executors.newFixedThreadPool(16);
    try {
      for (AudioTrack audioTrack : audioTracks) {
        executor.submit(
            () ->
                processTrack(
                    audioTrack, failedFiles, uploadedFiles, duplicateTracks, notUploadedFiles));
      }
    } finally {
      executor.shutdown(); // 1️⃣ stop accepting new tasks
    }

    try {
      boolean finished = executor.awaitTermination(1, TimeUnit.HOURS); // 2️⃣ wait

      if (!finished) {
        executor.shutdownNow(); // 3️⃣ force shutdown if timeout
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    return new MetadataScanResult(uploadedFiles, notUploadedFiles, failedFiles);
  }

    private void processTrack(AudioTrack audioTrack, Set<File> failedFiles, Set<File> uploadedFiles, Set<AudioTrack> duplicateTracks, Set<File> notUploadedFiles) {
        File file = new File(audioTrack.getFilePath());

        try {
          if (!audioTrackRepo.existsByAlbumAndAlbum_movie_show_titleAndTitle(
              audioTrack.getAlbum(),
              audioTrack.getAlbum_movie_show_title(),
              audioTrack.getTitle())) {

            AudioTrack saved = audioTrackRepo.save(audioTrack);

            try {
              luceneService.index(saved);
            } catch (Exception e) {
              failedFiles.add(file);
              log.error("Lucene indexing failed for {}", file, e);
                return;
            }

            uploadedFiles.add(file);

          } else {
            duplicateTracks.add(audioTrack);
            notUploadedFiles.add(file);
          }
        } catch (DataIntegrityViolationException e) {

          if (isDuplicateConstraint(e)) {
            log.debug("Duplicate track ignored: {}", file.getAbsolutePath());
              return;
          }

          failedFiles.add(file);
          log.error("DB constraint failure for {}", file, e);
        } catch (Exception e) {
          failedFiles.add(file);
          log.error("Unexpected error for {}", file, e);
        }

        log.warn(
            "Duplicate tracks ({}): {}",
            duplicateTracks.size(),
            duplicateTracks.stream()
                .map(AudioTrack::getFilePath)
                .collect(Collectors.toSet()));
    }

    private boolean isDuplicateConstraint(DataIntegrityViolationException e) {
    Throwable cause = e.getMostSpecificCause();
    if (cause == null) return false;
    String msg = cause.getMessage();
    if (msg == null) return false;
    return msg.contains("Duplicate entry")
        || msg.contains("UK")
        || msg.contains("unique constraint");
  }

  /** Recursive file collector using java.io.File */
  private void collectAudioFiles(File dir, Set<File> mp3Files, Set<File> flacFiles) {
    if (!dir.canRead()) return;

    String dirName = dir.getName();
    if ("System Volume Information".equalsIgnoreCase(dirName)
        || "$RECYCLE.BIN".equalsIgnoreCase(dirName)) {
      return;
    }

    File[] files = dir.listFiles();
    if (files == null) return;

    for (File file : files) {
      if (file.isDirectory()) {
        collectAudioFiles(file, mp3Files, flacFiles);
      } else {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".mp3")) {
          mp3Files.add(file);
        } else if (name.endsWith(".flac")) {
          flacFiles.add(file);
        }
      }
    }
  }

  /** Normalizes root paths for both Windows and Docker containers */
  private File normalizeRoot(String path) {
    // Docker container mapped folders
    if (path.startsWith("/audio-data/")) {
      return new File(path);
    }

    // Windows drive letters: H: → H:\
    if (path.matches("^[A-Za-z]:$")) {
      return new File(path + "\\");
    }

    return new File(path);
  }
}
