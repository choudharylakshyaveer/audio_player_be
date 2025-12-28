package org.audio.player.repository;


import org.audio.player.entity.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PlaylistRepo extends JpaRepository<Playlist, Long> {
    Optional<Playlist> findByNameIgnoreCase(String name);


    @Modifying
    @Query(value = "DELETE FROM playlist_tracks WHERE playlist_id = ?1 AND track_id = ?2", nativeQuery = true)
    int deletePlayListTrack(Long playlistId, Long trackId);

}
