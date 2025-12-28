package org.audio.player.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.audio.player.dto.MetadataScanResult;
import org.audio.player.entity.AudioTrack;
import org.audio.player.service.*;
import org.audio.player.service.StreamTokenService;
import org.audio.player.utils.FileStreamingUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping
public class AudioController {

  private final MetadataService metadataService;
  private final AlbumsService albumsService;
  private final AudioService audioService;
  private final AudioSearchService audioSearchService;
    private final StreamTokenService streamTokenService;

  @GetMapping("/saveTrackMetadata")
  public ResponseEntity<MetadataScanResult> saveTrackMetadata() throws IOException {
    log.info("Executing /saveTrackMetadata");
    return ResponseEntity.ok(metadataService.saveMetadata());
  }

  @GetMapping("/albums")
  public ResponseEntity<?> getAlbums(@RequestParam(required = false) String albumName) {
    if (albumName == null || albumName.isBlank()) {
      log.info("Executing /albums (all albums)");
      return ResponseEntity.ok(albumsService.getAlbums());
    }

    log.info("Executing /albums?albumName={}", albumName);
    return ResponseEntity.ok(albumsService.getAudioTrackByAlbum(albumName));
  }

  @GetMapping(value = "/stream/flac/{trackId}", produces = "audio/flac")
  public void streamFlac(
      @PathVariable Long trackId,
      @RequestParam String token,
      HttpServletRequest request,
      HttpServletResponse response)
      throws IOException {

    if (streamTokenService.isValid(token, trackId)) {

      AudioTrack audioTrack = audioService.getAudioTrackById(trackId);
      Path flacPath = Paths.get(audioTrack.getFilePath());

      FileStreamingUtil.streamFile(flacPath, "audio/flac", request, response);
    } else {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setHeader(
          "WWW-Authenticate",
          "Bearer error=\"invalid_token\", error_description=\"Stream token expired\"");
      response
          .getWriter()
          .write(
              """
        {
          "error": "STREAM_TOKEN_EXPIRED",
          "message": "Stream token has expired"
        }
        """);
      return;
    }
  }

  @GetMapping("/image/{id}")
  public ResponseEntity<String> getAlbumImage(@PathVariable Long id) {
    log.info("Executing /image/{}", id);
    return ResponseEntity.ok(albumsService.getAlbumImageById(id));
  }

  @GetMapping("/column/{columnName}")
  public ResponseEntity<List<String>> getDistinctColumnValues(@PathVariable String columnName) {
    log.info("Executing /column/{}", columnName);
    return ResponseEntity.ok(audioService.getDistinctDataForOneCol(columnName));
  }

  @GetMapping("/column")
  public ResponseEntity<List<AudioTrack>> getTracksByColumnFilter(
      @RequestParam String columnName, @RequestParam String value) {
    log.info("Executing /column?columnName={}&value={}", columnName, value);
    return ResponseEntity.ok(audioService.getTracksByColumnFilter(columnName, value));
  }

  @GetMapping("/search")
  public ResponseEntity<?> getSearchResults(
      @RequestParam("q") String searchedValue,
      @RequestParam(value = "limit", required = false) Integer limit) {
    log.info("Executing /search?q={}", searchedValue);
    return ResponseEntity.ok(audioSearchService.getAudioSearchResults(searchedValue));
  }
}
