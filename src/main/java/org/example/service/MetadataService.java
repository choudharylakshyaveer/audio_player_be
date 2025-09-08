package org.example.service;

import org.example.entity.AudioTrack;
import org.example.repository.AudioTrackRepo;
import org.example.utils.FlacMetadata;
import org.example.utils.Mp3Metadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MetadataService {

    @Autowired
    Mp3Metadata mp3Metadata;

    @Autowired FlacMetadata flacMetadata;

    @Autowired
    AudioTrackRepo audioTrackRepo;

    @Value("${metadata.folders}")
    private List<String> folderPaths;

    public Set<AudioTrack> saveMetadata() {
        Set<AudioTrack> audioTracks = new HashSet<>();
//        File folder = new File("H:\\audio_songs");
//        File folder = new File("C:\\Users\\choud\\Downloads\\Top 50 Single Track");
//        List<File> folders = new ArrayList<>();
//        folders.add(new File("H:\\audio_songs"));
//        folders.add(new File("C:\\Users\\choud\\Downloads\\Top 50 Single Track"));


//        List<AudioTrack> flacFilesMetadata = null;
//        List<AudioTrack> mp3FilesMetadata = null;

        for (String folderPath : folderPaths) {
            File folder = new File(folderPath);
            File[] mp3Files = folder.listFiles((dir, name) -> name.matches(".*\\.(mp3)$"));
            File[] flacFiles = folder.listFiles((dir, name) -> name.matches(".*\\.(flac)$"));
            List<AudioTrack> flacFilesMetadata = flacMetadata.getFlacTracks(flacFiles);
            List<AudioTrack> mp3FilesMetadata = mp3Metadata.getMp3Tracks(mp3Files);
            audioTracks.addAll(flacFilesMetadata);
            audioTracks.addAll(mp3FilesMetadata);
        }

        audioTracks.addAll( audioTrackRepo.saveAll(audioTracks));



        return audioTracks;
    }

}
