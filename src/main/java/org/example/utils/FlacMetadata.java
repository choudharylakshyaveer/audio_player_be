package org.example.utils;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.AudioTrack;
import org.example.entity.AudioTrackId;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.flac.FlacFileReader;
import org.jaudiotagger.audio.mp3.MP3FileReader;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.flac.FlacTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class FlacMetadata {
    public static Logger logger = Logger.getLogger("org.example.utils.FlacMetadata");

    @Autowired AlbumArtExtractor albumArtExtractor;

    @Lazy
    @Bean
    public List<AudioTrack> getFlacTracks(File[] files) {
        List<AudioTrack> audioTracks = new ArrayList<>();
//        File folder = new File("C:\\Users\\choud\\Downloads\\Top 50 Single Track");
//        File folder = new File("H:\\audio_songs");
//        File folder = new File("C:\\Users\\choud\\Downloads\\Telegram Desktop");
//        File[] files = folder.listFiles((dir, name) -> name.matches(".*\\.(flac)$"));
        Arrays.stream(files).forEach(
                file -> {
                    AudioTrack.AudioTrackBuilder audioTrackBuilder = AudioTrack.builder();
                    audioTrackBuilder.fileName(file.getName());
                    AudioTrackId.AudioTrackIdBuilder audioTrackIdBuilder = AudioTrackId.builder();
                    AudioFile audioFile = getAudioFile(file);
                    String vendor = audioFile.getTag().getFields("VENDOR").getFirst().toString();
                    audioTrackBuilder.vendor(vendor);
                    String title = audioFile.getTag().getFields("TITLE").getFirst().toString();
                    audioTrackBuilder.album_movie_show_title(title);
                    String artist = audioFile.getTag().getFields("ARTIST").getFirst().toString();
                    audioTrackBuilder.artists(List.of(artist));

                    String albumArtist = getFieldValue(audioFile, "ALBUMARTIST");
                    audioTrackBuilder.albumArtist(albumArtist);

                    String album = getFieldValue(audioFile, "ALBUM");
                    audioTrackIdBuilder.album(album);

                    String genre = getFieldValue(audioFile, "GENRE");
                    audioTrackBuilder.genre(genre);

                    String encoder = getFieldValue(audioFile, "ENCODER");
                    audioTrackBuilder.encoder(encoder);


                    String date = getFieldValue(audioFile, "DATE");
                    if(date!=null){
                        String year = String.valueOf(LocalDate.parse(audioFile.getTag().getFields("DATE").getFirst().toString()).getYear());
                        audioTrackBuilder.year(year);
                    }


                    /*if (((FlacTag) audioFile.getTag()).getImages().size()>0){
                        String imageData = ((FlacTag) audioFile.getTag()).getImages().getFirst()
                                .getImageData().toString();
                        audioTrackBuilder.image(imageData);
                    }*/
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
                    if(audioFile.getAudioHeader().getBitRate() != null){
                    String bitRateKbps = audioFile.getAudioHeader().getBitRate();
                    audioTrackBuilder.bitRate(bitRateKbps);
}
                    Boolean lossless = audioFile.getAudioHeader().isLossless();
                    audioTrackBuilder.lossless(lossless);
                    audioTrackBuilder.audioTrack(audioTrackIdBuilder.build());

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
