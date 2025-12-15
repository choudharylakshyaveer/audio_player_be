package org.audio.player.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.audio.player.dto.AlbumsDTO;
import org.audio.player.entity.AudioTrack;
import org.audio.player.service.AlbumsService;
import org.audio.player.service.AudioSearchService;
import org.audio.player.service.AudioService;
import org.audio.player.service.MetadataService;
import org.audio.player.utils.FileStreamingUtil;
import org.springframework.data.repository.query.Param;
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
@Slf4j
public class AudioController {

    private final MetadataService metadataService;
    private final AlbumsService albumsService;
    private final AudioService audioService;
    private final AudioSearchService audioSearchService;

    @GetMapping("/saveTrackMetadata")
    public ResponseEntity<Set<AudioTrack>> saveTrackMetadata() {
        log.info("Executing /saveTrackMetadata");
        return ResponseEntity.ok(metadataService.saveMetadata());
    }

    @GetMapping("/albums")
    public ResponseEntity<Set<AlbumsDTO>> getAlbums() {
        log.info("Executing /albums");
        return ResponseEntity.ok(albumsService.getAlbums());
    }

    @GetMapping("/albums/{albumName}")
    public ResponseEntity<Set<AudioTrack>> getAlbumTracks(@PathVariable String albumName) {
        log.info("Executing /albums/{}", albumName);
        return ResponseEntity.ok(albumsService.getAudioTrackByAlbum(albumName));
    }

    @GetMapping(value = "/stream/flac/{trackId}", produces = "audio/flac")
    public void streamFlac(
            @PathVariable Long trackId,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        log.info("Executing /stream/flac/{}", trackId);
        AudioTrack audioTrack = audioService.getAudioTrackById(trackId);
        Path flacPath = Paths.get(audioTrack.getFilePath());

        FileStreamingUtil.streamFile(flacPath, "audio/flac", request, response);
    }

    @GetMapping("/image/{id}")
    public ResponseEntity<String> getAlbumImage(@PathVariable Long id) {
        log.info("Executing /image/{}", id);
        return ResponseEntity.ok(albumsService.getAlbumImageById(id));
    }

    @GetMapping("/column/{columnName}")
    public ResponseEntity<List<String>> getTracksByColumn(@PathVariable String columnName) {
        log.info("Executing /column/{}", columnName);
        return ResponseEntity.ok(audioService.getDistinctDataForOneCol(columnName));
    }

    @GetMapping("/column/{columnName}/{filterValue}")
    public ResponseEntity<List<AudioTrack>> getTracksByColumnFilter(
            @PathVariable String columnName,
            @PathVariable String filterValue
    ) {
        log.info("Executing /column/{}/{}", columnName, filterValue);
        return ResponseEntity.ok(audioService.getTracksByColumnFilter(columnName, filterValue));
    }


    @GetMapping("/search")
    public ResponseEntity<?> getSearchResults( @RequestParam("q") String searchedValue, @Param("limit") Integer limit){
        log.info("Executing es/{}", searchedValue);

        return ResponseEntity.ok(audioSearchService.getAudioSearchResults(searchedValue));
    }

}
