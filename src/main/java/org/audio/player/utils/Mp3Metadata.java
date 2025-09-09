package org.audio.player.utils;

import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.CompositeParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.audio.player.entity.AudioTrack;
import org.audio.player.entity.AudioTrackId;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.*;
import org.jaudiotagger.tag.asf.AsfTag;
import org.jaudiotagger.tag.flac.FlacTag;
import org.jaudiotagger.tag.id3.*;
import org.jaudiotagger.tag.mp4.Mp4Tag;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag;
import org.jaudiotagger.tag.wav.WavTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class Mp3Metadata {

    @Autowired
    AlbumArtExtractor albumArtExtractor;

    private String fixEncoding(String text) {
        if (text == null) return null;
        try {
            byte[] bytes = text.getBytes("ISO-8859-1");
            return new String(bytes, "UTF-8");
        } catch (Exception e) {
            return text;
        }
    }

    private String normalizeText(String text) {
        if (text == null) return null;
        text = fixEncoding(text);
        return Normalizer.normalize(text, Normalizer.Form.NFC);
    }

    public String getFileExtension(String fileName) {
        if (fileName == null) return null;
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex == -1 || dotIndex == 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1);
    }

    public static Object getConcreteTagInstance(Tag tag) {
        if (tag == null) return null;

        if (tag instanceof ID3v24Tag) return (ID3v24Tag) tag;
        if (tag instanceof ID3v23Tag) return (ID3v23Tag) tag;
        if (tag instanceof ID3v22Tag) return (ID3v22Tag) tag;
        if (tag instanceof ID3v11Tag) return (ID3v11Tag) tag;
        if (tag instanceof ID3v1Tag) return (ID3v1Tag) tag;
        if (tag instanceof FlacTag) return (FlacTag) tag;
        if (tag instanceof Mp4Tag) return (Mp4Tag) tag;
        if (tag instanceof VorbisCommentTag) return (VorbisCommentTag) tag;
        if (tag instanceof AsfTag) return (AsfTag) tag;
        if (tag instanceof WavTag) return (WavTag) tag;

        return tag; // fallback generic
    }

    @Lazy
    @Bean
    public List<AudioTrack> getMp3Tracks(File[] files) {
        List<AudioTrack> audioTracks = new ArrayList<>();

        Arrays.stream(files).forEach(file -> {
            try {

                String extractAlbumArt = null;
                try {
                    extractAlbumArt = albumArtExtractor.extractAlbumArt(file);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                AudioFile audioFile = AudioFileIO.read(file);
                Tag tag = audioFile.getTag();

                AudioTrackId.AudioTrackIdBuilder audioTrackIdBuilder = AudioTrackId.builder();
                AudioTrack.AudioTrackBuilder trackBuilder = AudioTrack.builder();
                trackBuilder.attached_picture(extractAlbumArt);
                String filePath = file.getAbsolutePath();
                trackBuilder.filePath(filePath);

                try {
                    String title = normalizeText(tag.getFirst(FieldKey.TITLE));
                    audioTrackIdBuilder.title(title);
                    System.out.println("title: " + title);
                } catch (Exception ignored) {}

                try {
                    String album = normalizeText(tag.getFirst(FieldKey.ALBUM));
                    audioTrackIdBuilder.album(album);
                    trackBuilder.album_movie_show_title(album);
                    System.out.println("album: " + album);
                } catch (Exception ignored) {}

                try {
                    String artist = normalizeText(tag.getFirst(FieldKey.ARTIST));
                    if (artist != null) {
                        List<String> artists = Arrays.asList(artist.split(","));
                        trackBuilder.artists(artists);
                        System.out.println("artists: " + artists);
                    }
                } catch (Exception ignored) {}

                try {
                    String genre = normalizeText(tag.getFirst(FieldKey.GENRE));
                    trackBuilder.genre(genre);
                    System.out.println("genre: " + genre);
                } catch (Exception ignored) {}

                try {
                    String year = normalizeText(tag.getFirst(FieldKey.YEAR));
                    trackBuilder.year(year);
                    System.out.println("year: " + year);
                } catch (Exception ignored) {}

                try {
                    String comments = normalizeText(tag.getFirst(FieldKey.COMMENT));
                    trackBuilder.comments(comments);
                    System.out.println("comments: " + comments);
                } catch (Exception ignored) {}

                try {
                    String composer = normalizeText(tag.getFirst(FieldKey.COMPOSER));
                    trackBuilder.composer(composer);
                    System.out.println("composer: " + composer);
                } catch (Exception ignored) {}

                // 🔹 Run Apache Tika for extra metadata
                ContentHandler handler = new DefaultHandler();
                Metadata metadata = new Metadata();
                Parser parser = new CompositeParser();
                ParseContext parseContext = new ParseContext();

                try (FileInputStream fis = new FileInputStream(file);
                     TikaInputStream reader = TikaInputStream.get(file, metadata)) {

                    parser.parse(reader, handler, metadata, parseContext);

                    for (String name : metadata.names()) {
                        switch (name) {
                            case "xmpDM:audioChannelType" -> {
                                String audioChannelType = normalizeText(metadata.get(name));
                                trackBuilder.audioChannelType(audioChannelType);
                                System.out.println("audioChannelType: " + audioChannelType);
                            }
                            case "xmpDM:audioSampleRate" -> {
                                String audioSampleRate = normalizeText(metadata.get(name));
                                trackBuilder.audioSampleRate(audioSampleRate);
                                System.out.println("audioSampleRate: " + audioSampleRate);
                            }
                            case "channels" -> {
                                try {
                                    int channels = Integer.parseInt(metadata.get(name));
                                    trackBuilder.channels(channels);
                                    System.out.println("channels: " + channels);
                                } catch (NumberFormatException ignored) {}
                            }
                            case "Author" -> {
                                String author = metadata.get(name);
                                if (author != null) {
                                    List<String> authors = Arrays.asList(author.split(","));
                                    trackBuilder.authors(authors);
                                    System.out.println("authors: " + authors);
                                }
                            }
                            case "Content-Type" -> {
                                String contentType = normalizeText(metadata.get(name));
                                trackBuilder.content_type(contentType);
                                System.out.println("contentType: " + contentType);
                            }
                            case "samplerate" -> {
                                String samplerate = normalizeText(metadata.get(name));
                                trackBuilder.samplerate(samplerate);
                                System.out.println("samplerate: " + samplerate);
                            }
                        }
                    }
                }

                // 🔹 Build final AudioTrack
                AudioTrackId audioTrackId = audioTrackIdBuilder.build();
                trackBuilder.fileName(normalizeText(audioFile.getFile().getName()));
                trackBuilder.format("mp3");
                trackBuilder.fileExtension(normalizeText(getFileExtension(audioFile.getFile().getName())));
                trackBuilder.audioTrack(audioTrackId);

                AudioTrack audioTrack = trackBuilder.build();
                audioTracks.add(audioTrack);

                System.out.println("___________________________________");

            } catch (CannotReadException | IOException | TagException |
                     ReadOnlyFileException | InvalidAudioFrameException |
                     TikaException | SAXException e) {
                e.printStackTrace();
            }
        });

        return audioTracks;
    }
}
