package org.audio.player.es;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.audio.player.annotations.ReplaceText;
import org.audio.player.converter.StringListConverter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "audio-tracks")
public class AudioTrackEs {

    @Id
    private Long id;
    private String audioChannelType;
    private String audioSampleRate;
    private Integer channels;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> authors;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    @ReplaceText()
    private List<String> artists;

    @Convert(converter = StringListConverter.class)
    private List<String> additionalArtist;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    @Basic(fetch = FetchType.LAZY)
    @JsonIgnore
    private String attached_picture;

    @ReplaceText()
    @EqualsAndHashCode.Include
    private String album_movie_show_title;

    @ReplaceText()
    private String comments;
    private String year;
    private String genre;


    private String content_type;
    private String samplerate;
    private String composer;

    @Column(name = "file_name", unique = true, nullable = false)
    private String fileName;

    private String vendor;
    private String albumArtist;
    private String encoder;


    private String encodingType;
    private String format;
    private String bitRate;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean lossless = false;

    private String filePath;

    @ReplaceText()
    @Field(type = FieldType.Text)
    private String album;

    @ReplaceText()
    private String title;


    private String fileExtension;
}