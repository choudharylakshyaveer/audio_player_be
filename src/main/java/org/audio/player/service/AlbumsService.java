package org.audio.player.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.audio.player.dto.AlbumsDTO;
import org.audio.player.entity.AudioTrack;
import org.audio.player.repository.AudioTrackRepo;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@AllArgsConstructor
@Service
public class AlbumsService {

    AudioTrackRepo audioTrackRepo;

    public Set<AlbumsDTO> getAlbums(){
        return audioTrackRepo.getAlbums().stream().limit(20L).collect(Collectors.toSet());
    }

    public Set<AudioTrack> getAudioTrackByAlbum(String albumName){
        log.info("albumName: {1}", albumName);
        Set<AudioTrack> byAudioTrackAlbumIgnoreCase = audioTrackRepo.findByAlbumIgnoreCase(albumName);
        Stream<AudioTrack> audioTrackStream = byAudioTrackAlbumIgnoreCase.stream().map(audioTrack -> {
            audioTrack.setAlbum_movie_show_title("");
            return audioTrack;
        });
        return byAudioTrackAlbumIgnoreCase;
    }

    public String getAlbumImageById(Long id) {
        return audioTrackRepo.findById(id)
                .map(track -> {
                    String base64 = track.getAttached_picture();
                    if (base64 == null || base64.isEmpty()) return null;

                    try {
                        byte[] decoded = Base64.getDecoder().decode(base64);
                        BufferedImage original = ImageIO.read(new ByteArrayInputStream(decoded));
                        if (original == null) return base64; // fallback

                        // 🔹 Resize and compress
                        BufferedImage resized = resizeImage(original, 200, 200);
                        byte[] compressedBytes = compressJpeg(resized, 1f); // 100% quality

                        // 🔹 Encode back to Base64
                        return Base64.getEncoder().encodeToString(compressedBytes);

                    } catch (Exception e) {
                        e.printStackTrace();
                        return base64; // fallback to original image
                    }
                })
                .orElse(null);
    }

    private BufferedImage resizeImage(BufferedImage original, int width, int height) {
        int ow = original.getWidth();
        int oh = original.getHeight();
        double aspect = (double) ow / oh;

        if (width / (double) height > aspect) width = (int) (height * aspect);
        else height = (int) (width / aspect);

        Image scaled = original.getScaledInstance(width, height, Image.SCALE_FAST);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(scaled, 0, 0, null);
        g2d.dispose();
        return resized;
    }

    private byte[] compressJpeg(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        var writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) throw new IllegalStateException("No JPEG writers found");
        var writer = writers.next();

        try (var ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            var param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }
            writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }


}
