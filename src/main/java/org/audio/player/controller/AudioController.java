package org.audio.player.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.audio.player.dto.AlbumsDTO;
import org.audio.player.entity.AudioTrack;
import org.audio.player.jobs.TranscodeJob;
import org.audio.player.jobs.TranscodeManager;
import org.audio.player.service.AlbumsService;
import org.audio.player.service.AudioService;
import org.audio.player.service.MetadataService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;


@AllArgsConstructor
@RestController
public class AudioController {

    MetadataService metadataService;
    private TranscodeManager manager;

    AlbumsService albumsService;
    AudioService audioService;

    @GetMapping("/saveTrackMetadata")
    public ResponseEntity<Set<AudioTrack>> saveTrackMetadata(){
        Set<AudioTrack> audioTracks = metadataService.saveMetadata();
        return ResponseEntity.ok(audioTracks);
    }

    @GetMapping("/albums")
    public ResponseEntity<Set<AlbumsDTO>> getAlbums(){
        return ResponseEntity.ok(albumsService.getAlbums());
    }

    @GetMapping("/albums/{albumName}")
    public ResponseEntity<Set<AudioTrack>> getAlbumTracks(@PathVariable String albumName){
        return ResponseEntity.ok(albumsService.getAudioTrackByAlbum(albumName));
    }

    @GetMapping(value = "/playlist/{trackId}", produces = "application/vnd.apple.mpegurl")
    public void playlist(
            @PathVariable Long trackId,
            @RequestParam(defaultValue = "false") boolean lossless,
            @RequestParam(defaultValue = "10") int hlsTime,
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws Exception {
        Path source = locateSource(trackId);
        TranscodeJob job = manager.getOrCreateJob(trackId, source, lossless, hlsTime);

        // wait up to 15s for playlist generation
        Path playlist = job.awaitPlaylist(15, TimeUnit.SECONDS);

        // Build absolute base URL dynamically
        String baseUrl = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort();

        // Read the playlist and rewrite segment paths
        List<String> lines = Files.readAllLines(playlist);
        StringBuilder modified = new StringBuilder();

        for (String line : lines) {
            if (line.trim().endsWith(".ts")) {
                // Replace relative segment path with absolute URL
                modified.append(baseUrl)
                        .append("/segment/")
                        .append(trackId)
                        .append("/")
                        .append(line.trim())
                        .append("\n");
            } else {
                modified.append(line).append("\n");
            }
        }

        resp.setContentType("application/vnd.apple.mpegurl");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(modified.toString());
        resp.getWriter().flush();
    }


    // Request: /audio/segment/{trackId}/{segmentName}
    @GetMapping(value = "/segment/{trackId}/{segmentName}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> segment(
            @PathVariable Long trackId,
            @PathVariable String segmentName
    ) throws Exception {
        TranscodeJob job = manager.getJob(trackId);
        if (job == null) return ResponseEntity.notFound().build();
        Path seg = job.getTempDir().resolve(segmentName);
        // wait for file to appear up to timeout (e.g., 10s)
        int waited = 0;
        while (!Files.exists(seg) && waited < 10000) {
            Thread.sleep(100);
            waited += 100;
        }
        if (!Files.exists(seg)) return ResponseEntity.notFound().build();
        byte[] data = Files.readAllBytes(seg);
        // set content-type for ts
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("video/MP2T"));
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }
    
    @GetMapping(value = "/albums/image/{id}")
    public ResponseEntity<String> playlist(
            @PathVariable Long id){
        return ResponseEntity.ok(albumsService.getAlbumImageById(id));
    }

    @GetMapping("/column/{columnName}")
    public ResponseEntity<List<String>> getTracksByColumn(@PathVariable String columnName){
        return ResponseEntity.ok(audioService.getDistinctDataForOneCol(columnName));
    }

    @GetMapping("/column/{columnName}/{filterValue}")
    public ResponseEntity<List<AudioTrack>> getTracksByColumnFilter(@PathVariable String columnName, @PathVariable String filterValue){
        return ResponseEntity.ok(audioService.getTracksByColumnFilter(columnName, filterValue));
    }

    // Example implementation — adapt to your storage
    private Path locateSource(Long trackId) {
        AudioTrack audioTrackById = audioService.getAudioTrackById(trackId);
        String base = "H:\\audio_songs"; // WINDOWS PATH in your tests
        String candidate = audioTrackById.getFilePath();
        Path p = Paths.get(candidate);
        if (!Files.exists(p)) throw new IllegalArgumentException("source not found: " + p);
        return p;
    }

    
}
