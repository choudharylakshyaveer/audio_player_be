package org.audio.player.service;

import lombok.RequiredArgsConstructor;
import org.audio.player.entity.AudioTrack;
import org.audio.player.entity.Playlist;
import org.audio.player.repository.AudioTrackRepo;
import org.audio.player.repository.PlaylistRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.InputMismatchException;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PlaylistService {
    private final PlaylistRepo playlistRepo;
    private final AudioTrackRepo trackRepo;

    public Playlist createPlaylist(String name) {
        return playlistRepo.findByNameIgnoreCase(name)
                .orElseGet(() -> playlistRepo.save(Playlist.builder().name(name).build()));
    }

    public Playlist addTrackToPlaylist(Long playlistId, Long trackId) {
        Playlist playlist = playlistRepo.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("Playlist not found"));
        AudioTrack track = trackRepo.findById(trackId)
                .orElseThrow(() -> new IllegalArgumentException("Track not found"));
        playlist.getTracks().add(track);
        return playlistRepo.save(playlist);
    }

    public List<Playlist> getAllPlaylists() {
        return playlistRepo.findAll();
    }

    public Set<AudioTrack> getTracksByPlaylist(Long playlistId) {
        return playlistRepo.findById(playlistId)
                .map(Playlist::getTracks)
                .orElseThrow(() -> new IllegalArgumentException("Playlist not found"));
    }

    @Transactional
    public Long removeTrack(Long playlistId,
                            Long trackId) {
        if(playlistRepo.deletePlayListTrack(playlistId, trackId)>0){
            return trackId;
        }else {
            throw new InputMismatchException("Not found");
        }
    }
}
