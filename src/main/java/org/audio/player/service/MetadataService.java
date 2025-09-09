package org.audio.player.service;

import org.audio.player.entity.AudioTrack;
import org.audio.player.repository.AudioTrackRepo;
import org.audio.player.utils.FlacMetadata;
import org.audio.player.utils.Mp3Metadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
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
