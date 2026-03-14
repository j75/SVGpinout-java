package eu.f.m;

import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Get logo file content
 * @author Marian-N. I.
 */
class Logo {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final Map<String,LogoImage> CACHE = new ConcurrentHashMap<>();

    private final File logoDir;
    private final Tika tika;
    private final boolean withCache;

    /**
     *
     * @param withCache {@code boolean} true if it will try to get the logo image from cache
     * @param logoDir {@link File} alternate logo files directory
     */
    public Logo(boolean withCache, File logoDir) {
        this.logoDir = logoDir;
        this.withCache = withCache;
        try {
            tika = new Tika(TikaConfig.getDefaultConfig());
        } catch (RuntimeException e) {
            logger.warn("Tika initialization error", e);
            throw e;
        }
    }

    /**
     *
     * @param logoDir {@link File} alternate logo files directory
     */
    public Logo(File logoDir) {
        this (false, logoDir);
    }

    String getBase64Data(LogoImage image) {
        if (image == null) {
            logger.warn("no image to process!");
            return null;
        }
        String mimeType = image.mimeType();
        if (mimeType == null || mimeType.isBlank()) {
            logger.warn("no MIME type!");
            return null;
        }
        logger.debug("MIME type = {}", mimeType);
        if ("image/jpeg".equals(mimeType) || "image/png".equals(mimeType)) {
            byte[] buffer = image.content();
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
        if (withCache && CACHE.containsKey(logoFile)) {
            return Optional.of(CACHE.get(logoFile));
        }
        LogoImage logoImage = getLogoFileContent(logoFile);
        if (logoImage == null) {
            return Optional.empty();
        }
        if (withCache) {
            CACHE.putIfAbsent(logoFile, logoImage);
            logger.debug("Cache now has {} entries", CACHE.size());
        }
        return Optional.of(logoImage);
    }

    private LogoImage getLogoFileContent (String logoFile) {

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
                        return fileContent.length > 0 ? new LogoImage(fileContent, mimeType) : null;
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
                return fileContent.length > 0 ? new LogoImage(fileContent, mimeType) : null;
            } catch (IOException e) {
                warn (logoFile, e);
            }
        }
        return null;
    }

    private void warn (String logoFile, Exception e) {
        logger.warn("logo '{}' => exception: {}", logoFile, e.getMessage());
    }

    private void logBufSize (String file, int length) {
        logger.debug("read {} bytes from file '{}'", length, file);
    }
}
