package org.audio.player.service;

import lombok.AllArgsConstructor;
import org.audio.player.dto.AlbumsDTO;
import org.audio.player.repository.AudioTrackRepo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@AllArgsConstructor
@Service
public class AlbumsService {

    AudioTrackRepo audioTrackRepo;

    public Set<AlbumsDTO> getAlbums(){
        return audioTrackRepo.getAlbums();
    }

}
