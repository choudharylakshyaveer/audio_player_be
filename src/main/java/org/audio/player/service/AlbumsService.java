package org.audio.player.service;

import lombok.AllArgsConstructor;
import org.audio.player.dto.AlbumsDTO;
import org.audio.player.repository.AudioTrackRepo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class AlbumsService {

    AudioTrackRepo audioTrackRepo;

    public Set<AlbumsDTO> getAlbums(){
        return audioTrackRepo.getAlbums().stream().limit(10L).collect(Collectors.toSet());
    }

}
