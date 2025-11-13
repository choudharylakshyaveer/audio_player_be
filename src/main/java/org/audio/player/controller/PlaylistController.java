package org.audio.player.controller;

import lombok.RequiredArgsConstructor;
import org.audio.player.entity.AudioTrack;
import org.audio.player.entity.Playlist;
import org.audio.player.service.PlaylistService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/playlist")
public class PlaylistController {
    private final PlaylistService playlistService;


    @PostMapping
    public ResponseEntity<Playlist> createPlaylist(@RequestParam String name) {
        return ResponseEntity.ok(playlistService.createPlaylist(name));
    }

    @PostMapping("/{playlistId}/addTrack/{trackId}")
    public ResponseEntity<Playlist> addTrack(
            @PathVariable Long playlistId,
            @PathVariable Long trackId) {
        return ResponseEntity.ok(playlistService.addTrackToPlaylist(playlistId, trackId));
    }

    @GetMapping
    public ResponseEntity<List<Playlist>> getAll() {
        return ResponseEntity.ok(playlistService.getAllPlaylists());
    }

    @GetMapping("/{playlistId}")
    public ResponseEntity<Set<AudioTrack>> getTracks(@PathVariable Long playlistId) {
        return ResponseEntity.ok(playlistService.getTracksByPlaylist(playlistId));
    }

    @DeleteMapping("/{playlistId}/removeTrack/{trackId}")
    public ResponseEntity<Long> removeTrack( @PathVariable Long playlistId,
                                               @PathVariable Long trackId){
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(playlistService.removeTrack(playlistId, trackId));
    }


}
