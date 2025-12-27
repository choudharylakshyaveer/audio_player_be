package org.audio.player.controller;

import lombok.RequiredArgsConstructor;
import org.audio.player.dto.StreamTokenResponse;
import org.audio.player.entity.AudioTrack;
import org.audio.player.service.AudioService;
import org.audio.player.service.StreamTokenService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stream")
@RequiredArgsConstructor
public class StreamController {

    private final StreamTokenService streamTokenService;
    private final AudioService audioService;

    /**
     * Called AFTER user is authenticated via JWT
     */
    @PostMapping("/token/{trackId}")
    public StreamTokenResponse issue(@PathVariable Long trackId) {

        // optional ownership / entitlement check
        AudioTrack track = audioService.getAudioTrackById(trackId);

        String token = streamTokenService.create(trackId, track.getTrackLength());
        return new StreamTokenResponse(token);
    }
}

