package org.audio.player.service;

import org.audio.player.entity.AudioTrack;
import org.audio.player.repository.AudioTrackRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AudioService {

    @Autowired
    AudioTrackRepo audioTrackRepo;

    public AudioTrack getAudioTrackById(Long id){
        return audioTrackRepo.findById(id).get();
    }


}
