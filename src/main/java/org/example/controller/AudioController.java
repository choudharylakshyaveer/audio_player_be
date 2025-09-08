package org.example.controller;

import org.example.entity.AudioTrack;
import org.example.service.MetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
public class AudioController {

    @Autowired
    MetadataService metadataService;

    @GetMapping("/saveTrackMetadata")
    public ResponseEntity<Set<AudioTrack>> saveTrackMetadata(){
        Set<AudioTrack> audioTracks = metadataService.saveMetadata();
        return ResponseEntity.ok(audioTracks);
    }
}
