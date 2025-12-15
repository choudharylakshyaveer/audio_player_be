package org.audio.player;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "org.audio.player.repository")
@EnableElasticsearchRepositories(basePackages = "org.audio.player.es")
public class MediaServerApplication
{
    public static void main( String[] args )
    {
        SpringApplication.run(MediaServerApplication.class, args);
    }
}
