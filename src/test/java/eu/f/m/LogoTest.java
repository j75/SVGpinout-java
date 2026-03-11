package eu.f.m;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class LogoTest {
    //@TempDir
    //Path outputFolder;

    @BeforeEach
    void setUp() {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");
    }

    @Test
    void getBase64Data() {
        Logo logoImage = new Logo(null);
        String image = logoImage.getFileContent("logo_ti.png")
                .map(logoImage::getBase64Data)
                .orElse(null);
        assertNotNull (image);
        assertTrue (image.startsWith("data:image/png;base64,iVBOR"));
    }

    @Test
    void getFileContent() {
        Logo logoImage = new Logo(null);
        assertFalse(logoImage.getFileContent(null).isPresent());
        assertTrue(logoImage.getFileContent("logo_ti.png").isPresent());
        assertTrue(logoImage.getFileContent("sample-jpg-files-sample_320x213.jpg").isPresent());
    }

    @Test
    void getMimeType() {
        Logo logoImage = new Logo(new File ("src/test/resources"));
        LogoImage image = logoImage.getFileContent("741.csv").orElse(null);
        assertNotNull (image);
        assertFalse(image.mimeType().startsWith("image/"));
        image = logoImage.getFileContent("logos/sample-jpg-files-sample_320x213.jpg").orElse(null);
        assertNotNull (image);
        assertTrue(image.mimeType().startsWith("image/"));
    }
}