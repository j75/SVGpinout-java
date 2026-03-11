package eu.f.m;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * @author Marian-N. I.
 */
abstract class AbstractDrawChipSVG
{
    protected static final int pin_width = 20;
    protected static final int arrow_length = pin_width / 2;

    protected static final int corner_margin = 40;
    protected static final int pin_length = 80;
    protected static final int font_size = 16;

    protected static final String NOT_CONNECTED = "NC";

    protected static final String PIN_FONT_NAME = "Bitstream Vera Sans Mono";
    protected static final String CHIP_FONT_NAME = "Verdana Sans Mono";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected File outputDir;
    protected File logoDir;

    /**
     * used in benchmark to compare with the Python version, where logo is not embedded
     */
    protected boolean hasLogo = true;

    /**
     *
     * @param outputDirName {@link String}
     */
    void setOutputDir(String outputDirName) {
        String dir = Objects.requireNonNull(outputDirName, "folder should not be null!");
        outputDir = new File(dir);
        if (!outputDir.canWrite()) {
            logger.warn("cannot write to '{}' folder", outputDir.getAbsolutePath());
            throw new IllegalArgumentException("cannot write to output folder");
        }
    }

    /**
     *
     * @param newLogosDir {@link String}
     */
    void setLogoDir(String newLogosDir) {
        File dir = new File (newLogosDir);
        if (dir.isDirectory() && dir.canRead()) {
            logger.debug("will search logos in the '{}' folder", dir.getAbsolutePath());
            logoDir = dir;
        }
        else {
            logger.warn("bad logos folder '{}'", dir.getAbsolutePath());
            throw new IllegalArgumentException("bad logos folder!");
        }
    }

    /**
     * @param chipFile CSV input {@link File}
     */
    abstract void setChipFile(File chipFile);

    /**
     * Generate SVG file
     */
    abstract void generate();

    void setNoLogo() {
        hasLogo = false;
    }

    /**
     *
     * @param pins {@link List} of {@link Pin}
     * @return maximum text length to be written as pin name
     */
    int getMaxPinNameLength(List<Pin> pins) {
        if (pins == null || pins.isEmpty()) {
            return 0;
        }
        int lenMax = 0;
        for (int i = 0; i < pins.size(); i++) {
            Pin pin = pins.get(i);
            if (pin.getName().length() > lenMax) {
                logger.trace("index = {} '{}'.length() = {}", i, pin.getName(), pin.getName().length());
                lenMax = pin.getName().length();
            }
        }
        return lenMax;
    }

    protected File getSvgFileName(String outputFile) {
        File svgFile;
        String pathname = outputFile.endsWith(".svg") ? outputFile : outputFile + ".svg";
        if (outputDir != null && outputDir.isDirectory() && outputDir.canWrite()) {
            logger.debug("SVG file will be generated in folder '{}'", outputDir.getAbsolutePath());
            svgFile = new File(outputDir.getAbsolutePath(), pathname);
        }
        else {
            svgFile = new File(pathname);
        }
        logger.debug("write SVG to '{}' file", svgFile.getAbsolutePath());
        return svgFile;
    }

}
