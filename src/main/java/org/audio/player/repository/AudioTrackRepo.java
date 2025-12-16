package org.audio.player.repository;

import org.audio.player.dto.AlbumsDTO;
import org.audio.player.entity.AudioTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface AudioTrackRepo
    extends JpaRepository<AudioTrack, Long>, JpaSpecificationExecutor<AudioTrack> {

  @Query(
      "select distinct a.album as album, a.attached_picture as attachedPicture "
          + "from AudioTrack a   order by RAND()")
  Set<AlbumsDTO> getAlbums();

  Set<AudioTrack> findByAlbumIgnoreCase(String album);

  boolean existsByFileName(String fileName);

  boolean existsByAlbumAndTitle(String album, String title);

  @Query(
      """
            select (count(a) > 0) from AudioTrack a
            where a.album = :album and a.album_movie_show_title = :album_movie_show_title and a.title = :title""")
  boolean existsByAlbumAndAlbum_movie_show_titleAndTitle(
      @Param("album") String album,
      @Param("album_movie_show_title") String album_movie_show_title,
      @Param("title") String title);

  @Query(
"""
   select distinct a.album as album, a.attached_picture as attachedPicture
   from AudioTrack a
   where a.id in :ids
""")
  Set<AlbumsDTO> findDistinctAlbumsByTrackIds(@Param("ids") List<Long> ids);
}
