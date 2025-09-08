package org.example.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.converter.StringListConverter;

import java.io.Serializable;
import java.util.List;

@Embeddable
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AudioTrackId implements Serializable {



    private String album;

    private String title;
}
