package org.audio.player.service;

import org.audio.player.dto.SearchResultDTO;
import org.audio.player.es.AudioTrackEsRepository;
import org.audio.player.repository.AudioTrackRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AudioSearchService {

    @Autowired
    private AudioTrackEsRepository audioTrackEsRepository;

    @Autowired
    AudioSearchResultService audioSearchResultService;

    @Autowired
    private AudioTrackRepo audioTrackRepo;

    public SearchResultDTO getAudioSearchResults(String searchedValue){
        return audioSearchResultService.getSearchedResults(searchedValue);


    }

}
