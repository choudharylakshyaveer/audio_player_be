package org.audio.player.dto;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.audio.player.entity.AudioTrack;

import java.util.Set;

@Builder
@Data
public class SearchResultDTO {

  Set<AlbumsDTO> albums;
  Set<AudioTrack> audioTracks;
  Set<String> artists;
}
