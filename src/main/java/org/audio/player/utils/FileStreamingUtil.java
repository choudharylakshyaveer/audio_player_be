package org.audio.player.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class FileStreamingUtil {

    private FileStreamingUtil() {}

    public static void streamFile(Path filePath, String contentType,
                                  HttpServletRequest request,
                                  HttpServletResponse response) throws IOException {

        if (!Files.exists(filePath)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType(contentType);
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

        long fileLength = Files.size(filePath);
        String range = request.getHeader("Range");

        long start = 0;
        long end = fileLength - 1;

        if (range != null && range.startsWith("bytes=")) {
            String[] parts = range.substring(6).split("-"); // remove 'bytes='
            start = Long.parseLong(parts[0]);
            if (parts.length > 1 && !parts[1].isEmpty()) {
                end = Long.parseLong(parts[1]);
            }
        }

        long contentLength = end - start + 1;

        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
        response.setHeader("Content-Length", String.valueOf(contentLength));

        try (var inputStream = Files.newInputStream(filePath)) {
            inputStream.skip(start);
            inputStream.transferTo(response.getOutputStream());
        }
    }
}