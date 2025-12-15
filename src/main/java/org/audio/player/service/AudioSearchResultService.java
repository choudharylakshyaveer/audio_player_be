package org.audio.player.service;

import org.audio.player.dto.AlbumsDTO;
import org.audio.player.dto.SearchResultDTO;
import org.audio.player.entity.AudioTrack;
import org.audio.player.es.AudioTrackEsRepository;
import org.audio.player.repository.AudioTrackRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class AudioSearchResultService {

  @Autowired AudioTrackEsRepository audioTrackEsRepository;

  @Autowired AudioTrackRepo audioTrackRepo;

  public SearchResultDTO getSearchedResults(String searchedValue) {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Future<Set<AudioTrack>> searchedTracksByTitleFuture =
          executor.submit(() -> getSearchedTracksByTitle(searchedValue));
      Future<Set<AudioTrack>> searchedTracksByArtistFuture =
          executor.submit(() -> getSearchedTracksByArtist(searchedValue));
      Future<Set<AlbumsDTO>> searchedTracksByAlbumFuture =
          executor.submit(() -> getSearchedAlbums(searchedValue));
      Set<AudioTrack> searchedTracksByTitle = searchedTracksByTitleFuture.get();
      Set<AudioTrack> searchedTracksByArtist = searchedTracksByArtistFuture.get();
      Set<AlbumsDTO> searchedTracksByAlbum = searchedTracksByAlbumFuture.get();
      return SearchResultDTO.builder()
          .audioTracks(searchedTracksByTitle)
          .artists(searchedTracksByArtist)
          .albums(searchedTracksByAlbum)
          .build();
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  // albums -> only albums name
  private Set<AlbumsDTO> getSearchedAlbums(String searchedValue) {
    // should return album name and image
    return audioTrackEsRepository.searchAlbums(searchedValue).parallelStream()
        .map(
            audioTrackEs -> {
              AudioTrack audioTrack = audioTrackRepo.findById(audioTrackEs.getId()).get();
              return AlbumsDTO.builder()
                  .album(audioTrack.getAlbum())
                  .attachedPicture(audioTrack.getAttached_picture())
                  .build();
            })
        .collect(Collectors.toSet());
  }

  // Tracks from column album_movie_show_title
  private Set<AudioTrack> getSearchedTracksByTitle(String searchedValue) {
    return audioTrackEsRepository.searchByTitle(searchedValue).parallelStream()
        .map(
            audioTrackEs -> {
              return audioTrackRepo.findById(audioTrackEs.getId()).get();
            })
        .collect(Collectors.toSet());
  }

  // Artists
  private Set<AudioTrack> getSearchedTracksByArtist(String searchedValue) {
    return audioTrackEsRepository.searchByArtist(searchedValue).parallelStream()
        .map(
            audioTrackEs -> {
              return audioTrackRepo.findById(audioTrackEs.getId()).get();
            })
        .collect(Collectors.toSet());
  }
}
