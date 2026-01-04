package org.audio.player.service;

import org.audio.player.dto.AlbumsDTO;
import org.audio.player.dto.PageSearchResultDTO;
import org.audio.player.dto.SearchResultDTO;
import org.audio.player.entity.AudioTrack;
import org.audio.player.repository.AudioTrackRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AudioSearchResultService {


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

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "title",
            "album",
            "artists",
            "authors",
            "album_movie_show_title",
            "genre"
    );

    public PageSearchResultDTO getTracks(
            String search,
            int page,
            int size,
            String sortBy,
            String direction
    ) {

        // 1️⃣ No search → DB pagination + sorting
        if (search == null || search.isBlank()) {

            Pageable pageable = PageRequest.of(page, size);

            Page<AudioTrack> trackPage = audioTrackRepo.findAll(pageable);

            List<AudioTrack> sorted =
                    trackPage.getContent().stream()
                            .sorted(buildComparator(sortBy, direction))
                            .toList();

            return PageSearchResultDTO.builder()
                    .audioTracks(new LinkedHashSet<>(sorted))
                    .totalTracks((int) trackPage.getTotalElements())
                    .page(page)
                    .size(size)
                    .build();
        }

        // 2️⃣ Search present → Lucene → IDs
        List<Long> allMatchedIds =
                audioTrackLuceneSearchService.search(search.trim(), Integer.MAX_VALUE);

        int totalTracks = allMatchedIds.size();
        int fromIndex = page * size;

        if (fromIndex >= totalTracks) {
            return PageSearchResultDTO.builder()
                    .audioTracks(Set.of())
                    .totalTracks(totalTracks)
                    .page(page)
                    .size(size)
                    .build();
        }

        int toIndex = Math.min(fromIndex + size, totalTracks);
        List<Long> pageIds = allMatchedIds.subList(fromIndex, toIndex);

        // 3️⃣ Fetch entities
        List<AudioTrack> tracks = audioTrackRepo.findAllById(pageIds);

        // 4️⃣ Preserve Lucene order OR apply user sorting
        Map<Long, AudioTrack> trackMap =
                tracks.stream().collect(Collectors.toMap(AudioTrack::getId, t -> t));

        Stream<AudioTrack> stream = pageIds.stream()
                .map(trackMap::get)
                .filter(Objects::nonNull);

        // If user provided sorting → override Lucene relevance
        if (sortBy != null && !sortBy.isBlank()) {
            stream = stream.sorted(buildComparator(sortBy, direction));
        }

        return PageSearchResultDTO.builder()
                .audioTracks(stream.collect(Collectors.toCollection(LinkedHashSet::new)))
                .totalTracks(totalTracks)
                .page(page)
                .size(size)
                .build();
    }




    private Comparator<AudioTrack> buildComparator(String sortBy, String direction) {

        Comparator<AudioTrack> comparator = switch (sortBy) {
            case "title" -> Comparator.comparing(AudioTrack::getTitle,
                    Comparator.nullsLast(String::compareToIgnoreCase));
            case "album" -> Comparator.comparing(AudioTrack::getAlbum,
                    Comparator.nullsLast(String::compareToIgnoreCase));
            case "year" -> Comparator.comparing(AudioTrack::getYear,
                    Comparator.nullsLast(String::compareTo));
            case "genre" -> Comparator.comparing(AudioTrack::getGenre,
                    Comparator.nullsLast(String::compareToIgnoreCase));
            case "trackLength" -> Comparator.comparingInt(AudioTrack::getTrackLength);
            default -> Comparator.comparing(AudioTrack::getId);
        };

        return "desc".equalsIgnoreCase(direction)
                ? comparator.reversed()
                : comparator;
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
