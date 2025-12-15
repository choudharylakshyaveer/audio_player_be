package org.audio.player.es;

import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.Set;

public interface AudioTrackEsRepository extends ElasticsearchRepository<AudioTrackEs, Long> {

  @Query(
      """
        {
          "match": {
            "album": {
                                   "query": "?0",
                                   "fuzziness": "AUTO"
                                 }
          }
        }
    """)
  Set<AudioTrackEs> searchAlbums(String searchedValue);

  @Query(
      """
        {
          "match": {
            "album_movie_show_title": {
                                   "query": "?0",
                                   "fuzziness": "AUTO"
                                 }
          }
        }
    """)
  Set<AudioTrackEs> searchByTitle(String title);

  @Query(
      """
        {
          "multi_match": {
            "query": "?0",
            "fields": ["artists"],
            "fuzziness": "AUTO"
          }
        }
        """)
  Set<AudioTrackEs> searchByArtist(String title);
}
