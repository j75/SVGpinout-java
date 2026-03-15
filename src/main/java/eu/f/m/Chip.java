package eu.f.m;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

/**
 * Description of an IC chip.
 *
 * @author Marian-N. I.
 */
class Chip {

    private final String name;
    private final String outputFile;
    private final String packageName;
    private final String logoFile;
    private final ICPackage pack;

    List<Pin> pins;

    /**
     * First chip CSV line: Chip name, output SVG file name, package, logo file name
     * All other lines: Pin name, Type, Direction (IN, OUT, BIDIR, or nothing).
     * <br/>
     * May throw an {@link IllegalArgumentException}.
     * Parameters cannot be {@code null}.
     *
     * @param f {@link File}
     * @param packages {@link ICPackages}
     * @author Marian-N. I.
     */
    Chip (File f, ICPackages packages) {
        Logger logger = LoggerFactory.getLogger(getClass());

        List<String[]> lines = new ArrayList<>();
        String[] firstLine = null;
        try (BufferedReader reader = new BufferedReader (new FileReader(f))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    logger.debug("blank line found");
                    continue;
                }
                logger.trace("line = '{}'", line);
                String[] fields = line.split(",");
                if (fields.length < 1) {
                    logger.warn("bad line: '{}'", line);
                    continue;
                }
                if (firstLine == null && fields.length >= 3) {
                    firstLine = fields;
                } else {
                    lines.add(fields);
                }
            }
            if (firstLine == null) {
                logger.warn("First line not detected in file '{}'!", f.getAbsolutePath());
                throw new IllegalArgumentException("First line not detected in file " + f.getAbsolutePath());
            }
        }
        catch (IOException e) {
            logger.warn("error reading file {}", f.getAbsolutePath(), e);
            throw new UncheckedIOException(e);
        }

        logger.debug("read {} lines from '{}' CSV file", lines.size(), f.getAbsolutePath());

        packageName = firstLine[2];
        pack = packages.getPackage(packageName)
                .orElseThrow(() -> new IllegalArgumentException ("chip has unknown package " + packageName));
        if (lines.size () < pack.pinCount()) {
            logger.warn("bad number of lines {}: chip '{}' has {} pins", lines.size(),
                    packageName, pack.pinCount());
        }
        name = firstLine[0];
        outputFile = firstLine[1];
        if (firstLine.length > 3) {
            logoFile = firstLine[3];
        } else {
            logoFile = null;
            logger.debug("no logo file declared!");
        }
        pins = new ArrayList<>();
        for (String[] line : lines) {
            pins.add(new Pin(line));
        }
    }

    String getName() {
        return name;
    }

    String getOutputFile() {
        return outputFile;
    }

    String getLogoFile() {
        return logoFile;
    }

    String getPackageName() {
        return packageName;
    }

    List<Pin> getPins() {
        return Collections.unmodifiableList(pins);
    }

    ICPackage getICPackage() {
        return pack;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getName() + "[", "]")
                .add("name = '" + name + "'")
                .add("outputFile = '" + outputFile + "'")
                .add("logoFile = '" + logoFile + "'")
                .add("package = " + pack)
                .add("pins = " + pins)
                .toString();
    }
}
