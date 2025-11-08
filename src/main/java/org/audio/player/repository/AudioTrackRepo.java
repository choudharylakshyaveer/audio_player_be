package org.audio.player.repository;

import org.audio.player.dto.AlbumsDTO;
import org.audio.player.entity.AudioTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface AudioTrackRepo extends JpaRepository<AudioTrack, Long>, JpaSpecificationExecutor<AudioTrack> {

    @Query("select distinct a.album as album, a.attached_picture as attachedPicture " +
            "from AudioTrack a " )
    Set<AlbumsDTO> getAlbums();

    Set<AudioTrack> findByAlbumIgnoreCase(String album);
}
