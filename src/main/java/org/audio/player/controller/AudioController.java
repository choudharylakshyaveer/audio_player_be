package org.audio.player.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.audio.player.dto.AlbumsDTO;
import org.audio.player.entity.AudioTrack;
import org.audio.player.service.AlbumsService;
import org.audio.player.service.AudioService;
import org.audio.player.service.MetadataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@RestController
public class AudioController {

    private final MetadataService metadataService;
    private final AlbumsService albumsService;
    private final AudioService audioService;

    @GetMapping("/saveTrackMetadata")
    public ResponseEntity<Set<AudioTrack>> saveTrackMetadata() {
        return ResponseEntity.ok(metadataService.saveMetadata());
    }

    @GetMapping("/albums")
    public ResponseEntity<Set<AlbumsDTO>> getAlbums() {
        return ResponseEntity.ok(albumsService.getAlbums());
    }

    @GetMapping("/albums/{albumName}")
    public ResponseEntity<Set<AudioTrack>> getAlbumTracks(@PathVariable String albumName) {
        return ResponseEntity.ok(albumsService.getAudioTrackByAlbum(albumName));
    }

    @GetMapping(value = "/stream/flac/{trackId}", produces = "audio/flac")
    public void streamFlac(
            @PathVariable Long trackId,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        // Locate the FLAC file
        AudioTrack audioTrack = audioService.getAudioTrackById(trackId);
        Path flacFile = Paths.get(audioTrack.getFilePath());

        if (!Files.exists(flacFile)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Set headers for progressive streaming
        response.setContentType("audio/flac");
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

        long fileLength = Files.size(flacFile);
        String range = request.getHeader("Range");
        long start = 0, end = fileLength - 1;

        if (range != null && range.startsWith("bytes=")) {
            String[] parts = range.replace("bytes=", "").split("-");
            start = Long.parseLong(parts[0]);
            if (parts.length > 1 && !parts[1].isEmpty()) {
                end = Long.parseLong(parts[1]);
            }
        }

        long contentLength = end - start + 1;
        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
        response.setHeader("Content-Length", String.valueOf(contentLength));

        try (var inputStream = Files.newInputStream(flacFile)) {
            inputStream.skip(start);
            inputStream.transferTo(response.getOutputStream());
        }
    }

    @GetMapping("/image/{id}")
    public ResponseEntity<String> getAlbumImage(@PathVariable Long id) {
        return ResponseEntity.ok(albumsService.getAlbumImageById(id));
    }

    @GetMapping("/column/{columnName}")
    public ResponseEntity<List<String>> getTracksByColumn(@PathVariable String columnName) {
        return ResponseEntity.ok(audioService.getDistinctDataForOneCol(columnName));
    }

    @GetMapping("/column/{columnName}/{filterValue}")
    public ResponseEntity<List<AudioTrack>> getTracksByColumnFilter(
            @PathVariable String columnName,
            @PathVariable String filterValue
    ) {
        return ResponseEntity.ok(audioService.getTracksByColumnFilter(columnName, filterValue));
    }


}
