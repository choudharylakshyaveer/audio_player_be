package org.media.server.controller;


import org.media.server.dto.Track;
import org.media.server.utils.AlbumArtExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/v1/api")
@CrossOrigin(origins= "*", allowedHeaders = "*", methods = {RequestMethod.GET})
public class AudioMediaController {

    @Autowired
    private AlbumArtExtractor albumArtExtractor;
    
    @Value("${audio.music.folderpath}")
    private String musicFolder;

    @Value("${audio.music.baseUrl}")
    private String baseUrl;
    private static final Logger LOGGER = LoggerFactory.getLogger(AudioMediaController.class);
    

    @GetMapping("/tracks")
    public List<Track> getTracks() {
        File folder = new File(musicFolder);
        File[] files = folder.listFiles((dir, name) -> name.matches(".*\\.(mp3|wav|ogg|m4a|mp4|flac)$"));
        List<Track> tracks = Arrays.stream(files)
                .map(file -> {
                    Track track = null;
                    Track.TrackBuilder builder=null;
                    try {
                        builder = Track.builder()
                                .trackName(file.getName())
                                //.albumImage( albumArtExtractor.extractAlbumArt(file))
                                .trackUrl(baseUrl + "/v1/api/stream/" + file.getName());

                        if(file.getName().matches(".*\\.(mp3|wav|ogg|m4a|flac)$")){
                            builder= builder.albumImage( albumArtExtractor.extractAlbumArt(file));
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return builder.build();
                })
                .collect(Collectors.toUnmodifiableList());
        if (tracks == null) return List.of();

        return tracks;

       /* return Arrays.stream(files)
                .map(file -> new Song(
                        file.getName(),
                        baseUrl+"/v1/api/stream/" + file.getName() // absolute URL for frontend
                ))
                .collect(Collectors.toList());*/
    }

    @GetMapping("/stream/{filename}")
    public ResponseEntity<FileSystemResource> getTrack(@PathVariable String filename) throws Exception {
        LOGGER.info("filename: {}", filename);
        File file = new File(musicFolder, filename);
        if (!file.exists()) {
            LOGGER.info("filename: {} not found", filename);
            return ResponseEntity.notFound().build();
        }

        String mimeType = Files.probeContentType(file.toPath());
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=" + file.getName())
                .contentType(MediaType.parseMediaType(mimeType))
                .body(new FileSystemResource(file));
    }

    public static class Song {
        private String title;
        private String url;

        public Song(String title, String url) {
            this.title = title;
            this.url = url;
        }

        public String getTitle() { return title; }
        public String getUrl() { return url; }
    }
}
