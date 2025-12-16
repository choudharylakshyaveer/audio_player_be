package org.audio.player.config;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Path;

@Configuration
public class LuceneConfig {

  @Bean
  public Directory luceneDirectory() throws IOException {
    return FSDirectory.open(Path.of("lucene-index"));
  }

  @Bean
  public Analyzer analyzer() {
    return new StandardAnalyzer();
  }
}
