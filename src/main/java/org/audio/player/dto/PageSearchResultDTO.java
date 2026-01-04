package org.audio.player.dto;

import java.util.Set;
import lombok.Builder;
import lombok.Data;
import org.audio.player.entity.AudioTrack;

@Builder
@Data
public class PageSearchResultDTO {
  Set<AudioTrack> audioTracks;
  int totalTracks;
  int page;
  int size;
}
