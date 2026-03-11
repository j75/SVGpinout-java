package eu.f.m;

import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Optional;

/**
 * Get logo file content
 * @author Marian-N. I.
 */
class Logo {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final File logoDir;
    private final Tika tika;

    public Logo(File logoDir) {
        this.logoDir = logoDir;
        try {
            tika = new Tika(TikaConfig.getDefaultConfig());
        } catch (RuntimeException e) {
            logger.warn("Tika initialization error", e);
            throw e;
        }
    }

    String getBase64Data(LogoImage image) {
        if (image == null) {
            logger.warn("no image to process!");
            return null;
        }
        String mimeType = image.mimeType();
        byte[] buffer = image.content();
        if (mimeType == null || mimeType.isBlank()) {
            logger.warn("no MIME type!");
            return null;
        }
        logger.debug("MIME type = {}", mimeType);
        if ("image/jpeg".equals(mimeType) || "image/png".equals(mimeType)) {
            if (buffer != null && buffer.length > 8) {
                StringBuilder sb = new StringBuilder();
                byte[] content64 = Base64.getEncoder().encode(buffer);
                logger.debug("logo: {} bytes -> base64: {} bytes", buffer.length, content64.length);
                sb.append("data:").append(mimeType).append(";base64,")
                        .append(new String(content64));
                return sb.toString();
            }
        } else {
            logger.warn("bad MIME type '{}'", mimeType);
        }
        return null;
    }

    Optional<LogoImage> getFileContent (String logoFile) {
        if (logoFile == null || logoFile.isBlank()) {
            logger.info("no logo defined!");
            return Optional.empty();
        }
        logger.debug("searching logo '{}'", logoFile);
        byte[] fileContent;
         if (logoDir == null) {
            try (InputStream is = getClass ().getResourceAsStream("/logos/" + logoFile)) {
                if (is != null) {
                    int nRead;
                    byte[] data = new byte[16384];
                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        while ((nRead = is.read(data, 0, data.length)) != -1) {
                            baos.write(data, 0, nRead);
                        }
                        fileContent = baos.toByteArray();
                        String mimeType = tika.detect(fileContent);
                        logBufSize (logoFile, fileContent.length);
                        return fileContent.length > 0 ? Optional.of(new LogoImage(fileContent, mimeType)) : Optional.empty();
                    }
                }
            } catch (IOException e) {
                warn (logoFile, e);
            }
        }
        else {
            File logo = new File(logoDir, logoFile);
            logger.debug("searching '{}' logo file in folder '{}'", logoFile, logoDir.getAbsolutePath());
            try {
                fileContent = Files.readAllBytes(logo.toPath());
                String mimeType = tika.detect(fileContent);
                logBufSize (logo.getAbsolutePath(), fileContent.length);
                return fileContent.length > 0 ? Optional.of(new LogoImage(fileContent, mimeType)) : Optional.empty();
            } catch (IOException e) {
                warn (logoFile, e);
            }
        }
        return Optional.empty();
    }

    private void warn (String logoFile, Exception e) {
        logger.warn("logo '{}' => exception: {}", logoFile, e.getMessage());
    }

    private void logBufSize (String file, int length) {
        logger.debug("read {} bytes from file '{}'", length, file);
    }
}
