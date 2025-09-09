package org.audio.player.repository;

import org.audio.player.entity.AudioTrack;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AudioTrackRepo extends JpaRepository<AudioTrack, Long> {
}
