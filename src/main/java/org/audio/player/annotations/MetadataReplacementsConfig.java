package org.audio.player.annotations;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "metadata.replacements")
public class MetadataReplacementsConfig {
    private List<String> from;
    private String to;
}