package org.audio.player.service;

import org.audio.player.dto.AlbumsDTO;
import org.audio.player.dto.SearchResultDTO;
import org.audio.player.entity.AudioTrack;
import org.audio.player.repository.AudioTrackRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class AudioSearchResultService {

  //  @Autowired AudioTrackEsRepository audioTrackEsRepository;

  @Autowired AudioTrackLuceneSearchService audioTrackLuceneSearchService;

  @Autowired AudioTrackRepo audioTrackRepo;

  public SearchResultDTO getSearchedResults(String searchedValue) {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Future<Set<AudioTrack>> searchedTracksByTitleFuture =
          executor.submit(() -> getSearchedTracksByTitle(searchedValue));
      Future<Set<String>> searchedTracksByArtistFuture =
          executor.submit(() -> getSearchedArtists(searchedValue));
      Future<Set<AlbumsDTO>> searchedTracksByAlbumFuture =
          executor.submit(() -> getSearchedAlbums(searchedValue));
      Set<AudioTrack> searchedTracksByTitle = searchedTracksByTitleFuture.get();
      Set<String> searchedTracksByArtist = searchedTracksByArtistFuture.get();
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

    List<Long> ids = audioTrackLuceneSearchService.searchInField("album", searchedValue, 100);
    ;

    return audioTrackRepo.findDistinctAlbumsByTrackIds(ids);
  }

  // Tracks from column album_movie_show_title
  private Set<AudioTrack> getSearchedTracksByTitle(String searchedValue) {
    return audioTrackRepo
        .findAllById(
            audioTrackLuceneSearchService.searchInField(
                "album_movie_show_title", searchedValue, 100))
        .stream()
        .collect(Collectors.toSet());
  }

  // Artists
  private Set<String> getSearchedArtists(String searchedValue) {

    return audioTrackRepo
        .findAllById(audioTrackLuceneSearchService.searchInField("artists", searchedValue, 100))
        .stream()
        .map(AudioTrack::getArtists) // List<String>
        .filter(Objects::nonNull)
        .flatMap(List::stream) // Stream<String>
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }
}
