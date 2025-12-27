package org.audio.player.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.audio.player.annotations.ReplaceText;
import org.audio.player.annotations.TextReplaceListener;
import org.audio.player.converter.StringListConverter;

import java.io.Serializable;
import java.util.List;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@EntityListeners(TextReplaceListener.class)
@Table(name = "audio_track", uniqueConstraints = @UniqueConstraint(columnNames = {"album", "title", "album_movie_show_title"}))
public class AudioTrack implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false)
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
    @ToString.Exclude
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
    @EqualsAndHashCode.Include
    private String album;
    @ReplaceText()
    @EqualsAndHashCode.Include
    private String title;


    private String fileExtension;
    private int trackLength;
}