package org.audio.player.service;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.audio.player.dto.AlbumsDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AudioTrackLuceneSearchService {

    private final Directory directory;
    private final Analyzer analyzer;

    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "title",
            "album",
            "artists",
            "authors",
            "album_movie_show_title",
            "genre"
    );


    public List<Long> search(String text, int limit) {

        try (DirectoryReader reader = DirectoryReader.open(directory)) {

            IndexSearcher searcher = new IndexSearcher(reader);

            String[] fields = {
                    "title",
                    "album",
                    "artists",
                    "authors",
                    "album_movie_show_title",
                    "genre"
            };

            MultiFieldQueryParser parser =
                    new MultiFieldQueryParser(fields, analyzer);

            Query query = parser.parse(text);

            TopDocs topDocs = searcher.search(query, limit);

            List<Long> ids = new ArrayList<>();
            for (ScoreDoc sd : topDocs.scoreDocs) {
                Document doc = reader.storedFields().document(sd.doc); // ✅
                ids.add(Long.valueOf(doc.get("id")));
            }
            return ids;

        } catch (Exception e) {
            throw new RuntimeException("Lucene search failed", e);
        }
    }

    public List<Long> searchAlbumOnly(String text, int limit) {

        try (DirectoryReader reader = DirectoryReader.open(directory)) {

            IndexSearcher searcher = new IndexSearcher(reader);

            QueryParser parser = new QueryParser("album", analyzer);
            Query query = parser.parse(text);

            TopDocs topDocs = searcher.search(query, limit);

            List<Long> ids = new ArrayList<>();
            for (ScoreDoc sd : topDocs.scoreDocs) {
                Document doc = reader.storedFields().document(sd.doc);
                ids.add(Long.valueOf(doc.get("id")));
            }
            return ids;

        } catch (Exception e) {
            throw new RuntimeException("Album search failed", e);
        }
    }

    public List<Long> searchInField(
            String field,
            String text,
            int limit) {

        validateField(field);

        try (DirectoryReader reader = DirectoryReader.open(directory)) {

            IndexSearcher searcher = new IndexSearcher(reader);

            String normalized = text.toLowerCase();

            // 1️⃣ Exact query (highest priority)
            Query exactQuery =
                    new QueryParser(field, analyzer).parse(text);

            // 2️⃣ Fuzzy query (fallback)
            Query fuzzyQuery =
                    new FuzzyQuery(new Term(field, normalized), 2);

            // 3️⃣ Combine with strong boosting
            BooleanQuery combinedQuery =
                    new BooleanQuery.Builder()
                            .add(
                                    new BoostQuery(exactQuery, 5.0f),
                                    BooleanClause.Occur.SHOULD
                            )
                            .add(
                                    new BoostQuery(fuzzyQuery, 0.5f),
                                    BooleanClause.Occur.SHOULD
                            )
                            .build();

            TopDocs topDocs = searcher.search(combinedQuery, limit);

            List<Long> ids = new ArrayList<>();
            for (ScoreDoc sd : topDocs.scoreDocs) {
                Document doc = reader.storedFields().document(sd.doc);
                ids.add(Long.valueOf(doc.get("id")));
            }
            return ids;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Lucene search failed for field: " + field, e);
        }
    }



    private void validateField(String field) {
        if (!ALLOWED_FIELDS.contains(field)) {
            throw new IllegalArgumentException(
                    "Invalid search field: " + field);
        }
    }

}
