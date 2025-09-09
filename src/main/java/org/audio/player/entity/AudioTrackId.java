package org.audio.player.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AudioTrackId implements Serializable {



    private String album;

    private String title;
}
