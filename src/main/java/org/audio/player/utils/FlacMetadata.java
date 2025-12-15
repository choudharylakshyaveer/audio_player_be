package org.audio.player.utils;

import org.audio.player.entity.AudioTrack;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.flac.FlacFileReader;
import org.jaudiotagger.audio.mp3.MP3FileReader;
import org.jaudiotagger.tag.TagException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class FlacMetadata {
    public static Logger logger = Logger.getLogger("org.example.utils.FlacMetadata");

    @Autowired AlbumArtExtractor albumArtExtractor;

    @Lazy
    @Bean
    public Set<AudioTrack> getFlacTracks(File[] files) {
        Set<AudioTrack> audioTracks = new HashSet<>();
        Arrays.stream(files).forEach(
                file -> {
                    AudioTrack.AudioTrackBuilder audioTrackBuilder = AudioTrack.builder();
                    audioTrackBuilder.fileName(file.getName());
//                    AudioTrackId.AudioTrackIdBuilder audioTrackIdBuilder = AudioTrackId.builder();
                    AudioFile audioFile = getAudioFile(file);
                    String vendor = audioFile.getTag().getFields("VENDOR").getFirst().toString();
                    audioTrackBuilder.vendor(vendor);
                    String title = audioFile.getTag().getFields("TITLE").getFirst().toString();
                    audioTrackBuilder.album_movie_show_title(title);
                    String artist = audioFile.getTag().getFields("ARTIST").getFirst().toString();
                    audioTrackBuilder.artists(List.of(artist));

                    String filePath = file.getAbsolutePath();
                    audioTrackBuilder.filePath(filePath);

                    String albumArtist = getFieldValue(audioFile, "ALBUMARTIST");
                    audioTrackBuilder.albumArtist(albumArtist);

                    String album = getFieldValue(audioFile, "ALBUM");
                    audioTrackBuilder.album(album);

                    String genre = getFieldValue(audioFile, "GENRE");
                    audioTrackBuilder.genre(genre);

                    String encoder = getFieldValue(audioFile, "ENCODER");
                    audioTrackBuilder.encoder(encoder);


                    String date = getFieldValue(audioFile, "DATE");
                    if(date!=null){
                        String year = String.valueOf(LocalDate.parse(audioFile.getTag().getFields("DATE").getFirst().toString()).getYear());
                        audioTrackBuilder.year(year);
                    }
                    try {
                        audioTrackBuilder.attached_picture(albumArtExtractor.extractAlbumArt(file));
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, e.getMessage());
                    }
                    if(audioFile.getAudioHeader().getChannels() != null){
                        int channels = Integer.parseInt(audioFile.getAudioHeader().getChannels());
                        audioTrackBuilder.channels(channels);
                    }

                    if(audioFile.getAudioHeader().getEncodingType() != null) {
                        String encodingType = audioFile.getAudioHeader().getEncodingType();
                        audioTrackBuilder.encodingType(encodingType);
                    }
                    if(audioFile.getAudioHeader().getFormat() != null) {
                        String format = audioFile.getAudioHeader().getFormat();
                        audioTrackBuilder.format(format);
                    }
                    if(audioFile.getAudioHeader().getBitRate() != null) {
                        String bitRateKbps = audioFile.getAudioHeader().getBitRate();
                        audioTrackBuilder.bitRate(bitRateKbps);
                    }
                    Boolean lossless = audioFile.getAudioHeader().isLossless();
                    audioTrackBuilder.lossless(lossless);
//                    audioTrackBuilder.audioTrack(audioTrackIdBuilder.build());

                    audioTracks.add(audioTrackBuilder.build());

                });
        return audioTracks;
        
    }



    private String getFieldValue(AudioFile audioFile, String fieldName){
        String fieldValue = null;
        if (audioFile.getTag()
                .getFields(fieldName) != null) {
            if (audioFile.getTag()
                    .getFields(fieldName).size() > 0) {
                fieldValue = audioFile.getTag()
                        .getFields(fieldName).getFirst().toString();
            }
        }
        return fieldValue;
        }

    private static AudioFile getAudioFile(File file) {
        FlacFileReader flacFileReader = new FlacFileReader();
        MP3FileReader mp3FileReader = new MP3FileReader();
        AudioFile audioFile = null;
        try {
            audioFile = flacFileReader.read(file);
        } catch (CannotReadException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TagException e) {
            throw new RuntimeException(e);
        } catch (ReadOnlyFileException e) {
            throw new RuntimeException(e);
        } catch (InvalidAudioFrameException e) {
            throw new RuntimeException(e);
        }
        return audioFile;
    }



}
