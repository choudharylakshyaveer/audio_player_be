package org.example.repository;

import org.example.entity.AudioTrack;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AudioTrackRepo extends JpaRepository<AudioTrack, Long> {
}
