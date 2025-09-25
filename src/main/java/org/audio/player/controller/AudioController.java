package org.audio.player.controller;

import lombok.AllArgsConstructor;
import org.audio.player.dto.AlbumsDTO;
import org.audio.player.entity.AudioTrack;
import org.audio.player.service.AlbumsService;
import org.audio.player.service.MetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;


@AllArgsConstructor
@RestController
public class AudioController {

    MetadataService metadataService;

    AlbumsService albumsService;

    @GetMapping("/saveTrackMetadata")
    public ResponseEntity<Set<AudioTrack>> saveTrackMetadata(){
        Set<AudioTrack> audioTracks = metadataService.saveMetadata();
        return ResponseEntity.ok(audioTracks);
    }

    @GetMapping("/albums")
    public ResponseEntity<Set<AlbumsDTO>> getAlbums(){
        return ResponseEntity.ok(albumsService.getAlbums());
    }
}
