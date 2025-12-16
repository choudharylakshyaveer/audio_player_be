package org.audio.player.service;

import lombok.extern.slf4j.Slf4j;
import org.audio.player.entity.AudioTrack;
import org.audio.player.repository.AudioTrackRepo;
import org.audio.player.utils.FlacMetadata;
import org.audio.player.utils.Mp3Metadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MetadataService {

    @Autowired
    Mp3Metadata mp3Metadata;

    @Autowired
    FlacMetadata flacMetadata;

    @Autowired
    AudioTrackRepo audioTrackRepo;

    @Value("${metadata.folders}")
    private List<String> folderPaths;

    @Autowired
    private AudioTrackLuceneIndexService luceneService;

//    @Transactional
    public Set<AudioTrack> saveMetadata() {

        Set<AudioTrack> audioTracks = new HashSet<>();
        Set<AudioTrack> savedTracks = new HashSet<>();

        for (String folderPath : folderPaths) {
            File folder = new File(folderPath);
            File[] mp3Files = folder.listFiles((dir, name) -> name.matches(".*\\.(mp3)$"));
            File[] flacFiles = folder.listFiles((dir, name) -> name.matches(".*\\.(flac)$"));

            Set<AudioTrack> flacFilesMetadata = flacMetadata.getFlacTracks(flacFiles);
            Set<AudioTrack> mp3FilesMetadata = mp3Metadata.getMp3Tracks(mp3Files);

            audioTracks.addAll(
                    flacFilesMetadata.stream()
                            .filter(audioTrack -> audioTrack.getAlbum() != null && !audioTrack.getAlbum().isEmpty())
                            .collect(Collectors.toSet())
            );

            audioTracks.addAll(
                    mp3FilesMetadata.stream()
                            .filter(audioTrack -> audioTrack.getTitle() != null && !audioTrack.getTitle().isEmpty()
                                    && audioTrack.getAlbum() != null && !audioTrack.getAlbum().isEmpty())
                            .collect(Collectors.toSet())
            );
        }

        Set<AudioTrack> duplicateTracks = new HashSet<>();

        for (AudioTrack audioTrack : audioTracks) {
            try {
                if (/*!audioTrackRepo.existsByFileName(audioTrack.getFileName()) &&*/ !audioTrackRepo.existsByAlbumAndAlbum_movie_show_titleAndTitle(audioTrack.getAlbum(), audioTrack.getAlbum_movie_show_title(), audioTrack.getTitle())) {

                    AudioTrack saved = audioTrackRepo.save(audioTrack);
                    luceneService.index(saved);
                    savedTracks.add(saved);
                } else {
                    duplicateTracks.add(audioTrack);
                }
            }
            catch (DataIntegrityViolationException e){
                log.error("DataIntegrityViolationException saving track {}", audioTrack.getFileName(), e);
            }
            catch (Exception e) {
                log.error("Error saving track {}", audioTrack.getFileName(), e);
            }
        }


        log.warn("Duplicate tracks ({}):\n{}",
                duplicateTracks.size(),
                duplicateTracks.stream()
                        .map(AudioTrack::getFilePath)
                        .collect(Collectors.toSet())
        );

        return savedTracks;
    }


}
