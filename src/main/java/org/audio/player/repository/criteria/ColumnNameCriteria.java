package org.audio.player.repository.criteria;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.audio.player.entity.AudioTrack;
import org.audio.player.repository.AudioTrackRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class ColumnNameCriteria {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    AudioTrackRepo audioTrackRepo;

    public List<String> getSingleColumnData(String columnName){
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<List> cq = cb.createQuery(List.class);

        // Define FROM AudioTrack
        Root<AudioTrack> root = cq.from(AudioTrack.class);

        // SELECT DISTINCT columnName
        cq.select(root.get(columnName)).distinct(true);

        // Exclude NULLs
        cq.where(cb.isNotNull(root.get(columnName)));

        // Execute query
        List<String> resultList = entityManager.createQuery(cq).getResultStream()
                .flatMap(Collection::stream)
                .distinct()
                .toList();

        return resultList;
    }

    public List<AudioTrack> getTracksByColumnFilter(String column, String filterValue){
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<AudioTrack> query = cb.createQuery(AudioTrack.class);
        Root<AudioTrack> root = query.from(AudioTrack.class);

        // Build a LIKE predicate (case-insensitive, if applicable)
        query.select(root)
                .where(cb.like(cb.lower(root.get(column)), "%" + filterValue.toLowerCase() + "%"));

        return entityManager.createQuery(query).getResultList();
    }
}
