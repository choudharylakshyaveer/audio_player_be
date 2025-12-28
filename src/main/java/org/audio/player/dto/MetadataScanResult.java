package org.audio.player.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.File;
import java.util.Set;

@Data
@AllArgsConstructor
public class MetadataScanResult {

    private Set<File> uploadedFiles;
    private Set<File> notUploadedFiles;
    private Set<File> failedFiles;
}