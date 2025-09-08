package org.example.utils;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

@Service
public class AlbumArtExtractor {

    @Autowired
    private ResourceLoader resourceLoader;

    public String extractAlbumArt(File audioFile) throws Exception {
        AudioFile f = AudioFileIO.read(audioFile);
        Tag tag = f.getTag();

        if (tag != null) {
            List<Artwork> artworkList = tag.getArtworkList();
            if (!artworkList.isEmpty()) {
                Artwork artwork = artworkList.get(0); // Get the first artwork
                byte[] imageData = artwork.getBinaryData();
                if(imageData == null){
                    Resource resource = resourceLoader.getResource("classpath:" + "images/no_album_art.png");

                    return Base64.getEncoder().encodeToString(resource.getContentAsByteArray());
                }else {
                    String base64Image = Base64.getEncoder().encodeToString(imageData);
                    return base64Image;
                }
                /*if (imageData != null) {
                    try (ByteArrayInputStream bis = new ByteArrayInputStream(imageData)) {
                        return ImageIO.read(bis); // Decode into BufferedImage
                    }
                }*/
            }
        }
        return null; // No album art found
    }
}