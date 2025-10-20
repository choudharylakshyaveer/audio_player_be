package org.audio.player.annotations;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.audio.player.entity.AudioTrack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Component
public class TextReplaceListener {

    @Autowired
    private MetadataReplacementsConfig replacementsConfig;

    @PrePersist
    @PreUpdate
    public void beforeSave(Object entity) {
        if (entity != null
                && replacementsConfig != null
                && replacementsConfig.getFrom() != null
                && replacementsConfig.getTo() != null) {
            processEntity(entity);
        }
    }

    private void processEntity(Object entity) {
        if (entity == null) return;

        for (Field field : entity.getClass().getDeclaredFields()) {
            field.trySetAccessible();

            ReplaceText annotation = field.getAnnotation(ReplaceText.class);
            if (annotation == null) continue;

            try {
                Object value = field.get(entity);

                // --- String field ---
                if (value instanceof String strVal && strVal != null) {
                    String replaced = strVal;
                    for (String from : replacementsConfig.getFrom()) {
                        replaced = replaced.replace(from, replacementsConfig.getTo());
                    }
                    field.set(entity, replaced.trim());
                }

                // --- List<String> field ---
                else if (value instanceof List<?> listVal && !listVal.isEmpty() && listVal.get(0) instanceof String) {
                    List<String> mutableCopy = new ArrayList<>(listVal.size());
                    for (Object item : listVal) {
                        String replacedItem = (String) item;
                        for (String from : replacementsConfig.getFrom()) {
                            replacedItem = replacedItem.replace(from, replacementsConfig.getTo());
                        }
                        mutableCopy.add(replacedItem.trim());
                    }
                    field.set(entity, mutableCopy);
                }

                // --- Recursively process nested entities/embeddables ---
                else if (value != null && !isSimpleType(value.getClass())
                        && !value.getClass().getPackageName().startsWith("java.")) {
                    processEntity(value);
                }

            } catch (IllegalAccessException ignored) {
                // safe to ignore
            }
        }
    }

    private boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive()
                || clazz.equals(String.class)
                || Number.class.isAssignableFrom(clazz)
                || clazz.equals(Boolean.class)
                || clazz.equals(Character.class);
    }
}
