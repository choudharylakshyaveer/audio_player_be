package org.audio.player.service;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.audio.player.entity.AudioTrack;
import org.audio.player.mapper.AudioTrackLuceneMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class AudioTrackLuceneIndexService {

  private final Directory directory;
  private final Analyzer analyzer;

  public void index(AudioTrack track) {
    try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {

      writer.updateDocument(
          new Term("id", track.getId().toString()), AudioTrackLuceneMapper.toDocument(track));

    } catch (IOException e) {
      throw new RuntimeException("Lucene indexing failed", e);
    }
  }

  public void deleteById(Long id) {
    try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {

      writer.deleteDocuments(new Term("id", id.toString()));

    } catch (IOException e) {
      throw new RuntimeException("Lucene delete failed", e);
    }
  }
}
