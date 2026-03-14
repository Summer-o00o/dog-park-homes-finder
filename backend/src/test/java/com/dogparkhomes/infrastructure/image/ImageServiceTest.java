package com.dogparkhomes.infrastructure.image;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ImageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void getOrDownloadImage_returnsPath_whenFileAlreadyExists() throws Exception {
        Path imageDir = tempDir.resolve("images");
        Files.createDirectories(imageDir);
        Path existingFile = imageDir.resolve("listing-123.jpg");
        Files.writeString(existingFile, "existing image content");

        ImageService service = new ImageService(imageDir.toString());
        String result = service.getOrDownloadImage("listing-123", "http://example.com/any-url");

        assertEquals("/images/listing-123.jpg", result);
    }

    @Test
    void getOrDownloadImage_downloadsAndReturnsPath_whenFileDoesNotExist() throws Exception {
        Path imageDir = tempDir.resolve("images");
        Files.createDirectories(imageDir);
        Path sourceFile = imageDir.resolve("source.jpg");
        byte[] imageBytes = new byte[]{0x00, (byte) 0xFF, 0x0D};
        Files.write(sourceFile, imageBytes);

        ImageService service = new ImageService(imageDir.toString());
        String fileUrl = sourceFile.toUri().toURL().toString();
        String result = service.getOrDownloadImage("new-listing", fileUrl);

        assertEquals("/images/new-listing.jpg", result);
        Path downloaded = imageDir.resolve("new-listing.jpg");
        assertTrue(Files.exists(downloaded));
        assertArrayEquals(imageBytes, Files.readAllBytes(downloaded));
    }

    @Test
    void getOrDownloadImage_returnsNull_whenUrlIsInvalid() {
        ImageService service = new ImageService(tempDir.toString());
        String result = service.getOrDownloadImage("listing-fail", "not-a-valid-url");
        assertNull(result);
    }

    @Test
    void getOrDownloadImage_returnsNull_whenDownloadFails() {
        ImageService service = new ImageService(tempDir.toString());
        String result = service.getOrDownloadImage("listing-fail",
                "file:///nonexistent/path/to/image.jpg");
        assertNull(result);
    }
}
