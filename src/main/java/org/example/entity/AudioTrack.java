package org.example.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.*;
import org.example.converter.StringListConverter;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.io.Serializable;
import java.util.List;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class AudioTrack implements Serializable {

//    @EmbeddedId
    private AudioTrackId audioTrack;

    private String audioChannelType;
    private String audioSampleRate;
    private Integer channels;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> authors;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> artists;

    @Convert(converter = StringListConverter.class)
    private List<String> additionalArtist;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    @Basic(fetch = FetchType.LAZY)
    @JsonIgnore
    private String attached_picture;
    private String album_movie_show_title;
    private String comments;
    private String year;
    private String genre;


    private String content_type;
    private String samplerate;
    private String composer;

    @Id
    private String fileName;
    private String fileExtension;

    private String vendor;
    private String albumArtist;
    private String encoder;



    private String encodingType;
    private String format;
    private String bitRate;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean lossless = false;

}