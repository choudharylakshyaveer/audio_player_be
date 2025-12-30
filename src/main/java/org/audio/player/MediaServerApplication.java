package org.audio.player;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "org.audio.player.repository")
public class MediaServerApplication
{
    public static void main( String[] args )
    {
        SpringApplication.run(MediaServerApplication.class, args);
    }
}
