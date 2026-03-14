package com.dogparkhomes.infrastructure.image;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

@Service
public class ImageService {

    private static final String DEFAULT_IMAGE_FOLDER = "src/main/resources/static/images/";

    private final String imageFolder;

    public ImageService() {
        this(DEFAULT_IMAGE_FOLDER);
    }

    public ImageService(String imageFolder) {
        this.imageFolder = imageFolder.endsWith("/") ? imageFolder : imageFolder + "/";
    }

    public String getOrDownloadImage(String listingId, String imageUrl) {
        try {
            String fileName = listingId + ".jpg";
            File file = new File(imageFolder + fileName);

            // already exists
            if (file.exists()) {
                return "/images/" + fileName;
            }

            // download
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            URI uri = URI.create(imageUrl);
            URL url = uri.toURL();

            try (InputStream in = url.openStream();
                 FileOutputStream out = new FileOutputStream(file)) {
                in.transferTo(out);
            }
            
            return "/images/" + fileName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}