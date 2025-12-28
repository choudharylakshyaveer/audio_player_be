package org.audio.player.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class AlbumsDTO {

    String album;
    String attachedPicture;


}
