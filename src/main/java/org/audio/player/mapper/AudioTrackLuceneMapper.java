package org.audio.player.mapper;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.audio.player.entity.AudioTrack;

public final class AudioTrackLuceneMapper {

  private AudioTrackLuceneMapper() {}

  public static Document toDocument(AudioTrack track) {
    Document doc = new Document();

    doc.add(new StringField("id", track.getId().toString(), Field.Store.YES));

    addText(doc, "title", track.getTitle());
    addText(doc, "album", track.getAlbum());
    addText(doc, "album_movie_show_title", track.getAlbum_movie_show_title());
    addText(doc, "genre", track.getGenre());
    addText(doc, "year", track.getYear());

    if (track.getArtists() != null) {
      addText(doc, "artists", String.join(" ", track.getArtists()));
    }

    if (track.getAuthors() != null) {
      addText(doc, "authors", String.join(" ", track.getAuthors()));
    }

    return doc;
  }

  private static void addText(Document doc, String name, String value) {
    if (value != null && !value.isBlank()) {
      doc.add(new TextField(name, value, Field.Store.YES));
    }
  }
}
