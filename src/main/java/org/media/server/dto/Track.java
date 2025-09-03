package org.media.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.springframework.core.io.FileSystemResource;

@Data
@Builder
@AllArgsConstructor
public class Track {
    String trackName;
    FileSystemResource fileSystemResource;
    String albumImage;
    String trackUrl;
}
