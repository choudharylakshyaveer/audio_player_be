package org.audio.player.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.audio.player.dto.AlbumsDTO;
import org.audio.player.entity.AudioTrack;
import org.audio.player.repository.AudioTrackRepo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@AllArgsConstructor
@Service
public class AlbumsService {

    AudioTrackRepo audioTrackRepo;

    public Set<AlbumsDTO> getAlbums(){
        return audioTrackRepo.getAlbums().stream().limit(20L).collect(Collectors.toSet());
    }

    public Set<AudioTrack> getAudioTrackByAlbum(String albumName){
        log.info("albumName: {1}", albumName);
        Set<AudioTrack> byAudioTrackAlbumIgnoreCase = audioTrackRepo.findByAlbumIgnoreCase(albumName);
        Stream<AudioTrack> audioTrackStream = byAudioTrackAlbumIgnoreCase.stream().map(audioTrack -> {
            audioTrack.setAlbum_movie_show_title("");
            return audioTrack;
        });
        return byAudioTrackAlbumIgnoreCase;
    }

}
