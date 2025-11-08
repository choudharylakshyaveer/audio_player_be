package org.audio.player.service;

import org.audio.player.entity.AudioTrack;
import org.audio.player.repository.AudioTrackRepo;
import org.audio.player.repository.criteria.ColumnNameCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AudioService {

    @Autowired
    AudioTrackRepo audioTrackRepo;

    @Autowired
    ColumnNameCriteria columnNameCriteria;

    public AudioTrack getAudioTrackById(Long id){
        return audioTrackRepo.findById(id).get();
    }

    public List<String> getDistinctDataForOneCol(String columnName){
        return columnNameCriteria.getSingleColumnData(columnName);
    }

    public List<AudioTrack> getTracksByColumnFilter(String column, String filterValue){
        return columnNameCriteria.getTracksByColumnFilter(column, filterValue);
    }
}
