package eu.f.m;

import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.dom.util.DOMUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.*;
import java.util.Objects;
import java.util.StringJoiner;

import static org.apache.batik.util.SVGConstants.*;

/**
 * @author Marian-N. I.
 */
class BatikDrawChip extends AbstractDrawChipSVG {
    /**
     * Original Python code defines the text with {@literal style="font-size:50px; font-family:Verdana"}
     * but we may also use {@literal font-size="50" font-weight="bold" font-family="Verdana"}
     */
    private static final boolean USE_ORIG_STYLE = true;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // Get a DOMImplementation that knows SVG
    private static final DOMImplementation DOM_IMPL = SVGDOMImplementation.getDOMImplementation();
    private static final String GND = "GND";

    private final ICPackages allPackages;
    private final Document document;
    private Chip chip;

    BatikDrawChip (ICPackages icPackages) {
        allPackages = Objects.requireNonNull(icPackages, "no packages defined!");

        // Create an SVG document
        document = DOM_IMPL.createDocument(SVG_NAMESPACE_URI, SVG_SVG_TAG,null);
        document.setXmlStandalone(true);
    }

    @Override
    void setChipFile(File chipFile) {
        File cf = Objects.requireNonNull(chipFile, "no chip file!");
        if (!cf.canRead()) {
            logger.warn("cannot read chip file '{}'!", cf.getAbsolutePath());
            throw new IllegalArgumentException("cannot read chip file!");
        }
        chip = new Chip(cf, allPackages);
    }

    @Override
    void generate() {
        if (chip == null) {
            logger.warn("chip not defined!");
            return;
        }
        ICPackage chipPackage = chip.getICPackage();
        logger.debug("chip package = {}", chipPackage);

        int pincount = chipPackage.pinCount();
        int pincount_w = chipPackage.width();
        int pincount_h = chipPackage.height();
        int pin_number = chipPackage.getPinNumber();
        // original: ref_size
        int chipNameFontSize = chipPackage.refSize(); // int(package[5])
        int dot_size = chipPackage.dotPin1Size(); // int(package[6])
        // DIP height, overwritten for quad packages
        int chip_h =  chipPackage.dipHeight(); // int(package[7])
        float pin_space = pin_width * chipPackage.pinSpacingFactor(); // pin_width * float(package[8])

        if (dot_size == 0) {
            logger.warn("Unsupported package: {}", chipPackage );
            return;
        }

        int  maxPinNameLen = getMaxPinNameLength (chip.getPins());
        logger.trace("max. text length to be written: {}", maxPinNameLen);

        // Chip case width and height
        int chip_w = (int)(pincount_w * pin_width + (pincount_w - 1) * pin_space + corner_margin * 2);
        if (pincount_h != 0) {
            chip_h = (int)(pincount_h * pin_width + (pincount_h - 1) * pin_space + corner_margin * 2);
        }

        // SVG document width and height
        double doc_w, doc_h;
        int chip_x;
        //int maxPinSize = pin_length; // max (pin_length, maxPinNameLen * PNAME_FONT.getSize() / 2);
        int maxPinSize = maxPinNameLen > 8 ? (int)(pin_length + (maxPinNameLen - 8) / 0.1) : pin_length;
        if (pincount_h != 0) {
            doc_w = chip_w + maxPinSize * 2 + arrow_length * 2;
            chip_x = maxPinSize + arrow_length;
        } else {
            doc_w = chip_w;
            chip_x = 0;
        }
        doc_h = chip_h + maxPinSize * 2 + arrow_length * 2;
        logger.debug("SVG dimensions = {} x {}", doc_h, doc_w);
        // Root <svg> element
        Element svgDoc = document.getDocumentElement();
        svgDoc.setAttributeNS(null, "width", Double.toString(doc_w));
        svgDoc.setAttributeNS(null, "height", Double.toString(doc_h));
        svgDoc.setAttributeNS(null, "viewBox", "0 0 " + doc_w + " " + doc_h);

        // Chip package background
        // <rect>
        Element rect = document.createElementNS(SVG_NAMESPACE_URI, SVG_RECT_TAG);
        rect.setAttributeNS(null, "x", Integer.toString(chip_x));
        rect.setAttributeNS(null, "y",  Integer.toString(arrow_length + maxPinSize));
        rect.setAttributeNS(null, "width", Integer.toString(chip_w));
        rect.setAttributeNS(null, "height", Integer.toString(chip_h));
        rect.setAttributeNS(null, "fill", "white");
        rect.setAttributeNS(null, "stroke", "none");
        rect.setAttributeNS(null, "stroke-width", "0");
        svgDoc.appendChild(rect);

        float markings_x = (float)(chip_x + chip_w * 0.2);
        float markings_y = (float)(maxPinSize + arrow_length + chip_h * 0.1);
        // Logo, if present
        Logo logoImage = new Logo(logoDir);
        double sizeY = chip_h * 0.4;
        double sizeX = chip_w * 0.4;
        //double[] markingsY = new double[] { (maxPinSize + arrow_length + chip_h * 0.66)};
        logoImage.getFileContent(chip.getLogoFile())
                .ifPresent(i -> addLogoImage(svgDoc, logoImage, i, markings_x, markings_y + 5, sizeX, sizeY));

        double markingsY = markings_y + sizeY + chipNameFontSize;
        // Chip name
        if (chip.getName().length() * chipNameFontSize - markings_x * 2 > chip_w) {
            logger.debug("chip name length = {}, font size = {}, markings_x = {}, chip width = {}",
                    chip.getName().length(), chipNameFontSize, markings_x, chip_w);
            addText(svgDoc, chip.getName(), CHIP_FONT_NAME, chipNameFontSize, "bold", markings_x / 2, markingsY);
        } else {
            addText(svgDoc, chip.getName(), CHIP_FONT_NAME, chipNameFontSize, "bold", markings_x, markingsY);
        }

        // Pin 1 marker
        int xCenter = chip_x + (dot_size * 3);
        int yCenter = (int)(doc_h - (maxPinSize + (dot_size * 3)));
        // stroke_width = "1", stroke = "black",
        Element circle = document.createElementNS(SVG_NAMESPACE_URI, SVG_CIRCLE_TAG);
        circle.setAttributeNS(null, "cx", Integer.toString(xCenter));
        circle.setAttributeNS(null, "cy", Integer.toString(yCenter));
        circle.setAttributeNS(null, "r", Integer.toString(dot_size));
        circle.setAttributeNS(null, "fill", "none");
        circle.setAttributeNS(null, "stroke", "black");
        circle.setAttributeNS(null, "stroke-width", "1");
        svgDoc.appendChild(circle);

        /* Draw pins */
        Edge edge = Edge.BOTTOM; // Start with the bottom edge
        int pin_counter = 0;
        int pin_total = 0;

        for (Pin pin : chip.getPins()) {
            logger.debug("drawing {}", pin);

            String pin_name = pin.getName();
            double centering = maxPinSize / 2.0 - pin_name.length() * 4.8;
            double number_length = Integer.toString(pin_number).length() * 4.8;

            var corner_offset = chip_x + corner_margin;
            var bottom_offset = chip_x + chip_h - corner_margin - pin_width;
            var right_offset = chip_x + chip_w - corner_margin - pin_width;
            var pin_offset = pin_counter * (pin_width + pin_space);

            InsertCoordinates insertCoordinates = edge.computePosition(corner_offset, pin_offset, chip_h, maxPinSize,
                    number_length, chip_w, bottom_offset, centering, right_offset);
            double[] pin_insert = insertCoordinates.pinInsert();  // pin outline rectangle position
            double[] number_insert = insertCoordinates.numberInsert(); // position of pin number
            float[] text_insert = insertCoordinates.textInsert(); // position of pin name
            int[] pin_size = insertCoordinates.pinSize();
            short text_rotation = insertCoordinates.textRotationAngle();
            int[] arrow_insert = insertCoordinates.arrowInsert();
            short arrow_rotation = insertCoordinates.arrowRotationAngle();

            // Pin number
            addText(svgDoc, Integer.toString(pin_number), PIN_FONT_NAME, font_size, "regular",
                    (float)number_insert[0], (float)number_insert[1]);
            logger.debug("draw pin number {}, font size = {} @ {}:{}",
                    pin_number, font_size, number_insert[0], number_insert[1]);

            // Pin outline
            rect = document.createElementNS(SVG_NAMESPACE_URI, SVG_RECT_TAG);
            rect.setAttributeNS(null, "x", Double.toString(pin_insert[0]));
            rect.setAttributeNS(null, "y", Double.toString(pin_insert[1]));
            rect.setAttributeNS(null, "width", Integer.toString(pin_size[0]));
            rect.setAttributeNS(null, "height", Integer.toString(pin_size[1]));

            boolean isGND = GND.equalsIgnoreCase(pin_name);
            if (NOT_CONNECTED.equalsIgnoreCase(pin_name)) {
                rect.setAttributeNS(null, "fill", "white");
                rect.setAttributeNS(null, "stroke", "grey");
                rect.setAttributeNS(null, "stroke-dasharray", "5,5");
            }
            else if (isGND) {
                rect.setAttributeNS(null, "fill", "black");
                rect.setAttributeNS(null, "stroke", "grey");
            }
            else {
                rect.setAttributeNS(null, "fill", pin.getRGB());
                // stroke_width = "1", stroke = "black",
                rect.setAttributeNS(null, "stroke", "black");
            }
            rect.setAttributeNS(null, "stroke-width", "1");
            svgDoc.appendChild(rect);
            logger.debug("draw pin outline, size = {} x {} @ {}:{}", pin_size[0], pin_size[1], pin_insert[0], pin_insert[1]);

            // Pin name
            logger.debug("draw pin name '{}' @ {}:{}, rotation = {}°", pin_name,
                    text_insert[0], text_insert[1], text_rotation);
            drawPinName (pin_name, isGND, svgDoc, text_rotation, text_insert);

            // Direction arrow
            if (pin.hasDirection ()) {
                if (pin.isBiDirectional ()) {
                    arrow (svgDoc, EPinDirection.IN, arrow_rotation, arrow_insert);
                    arrow (svgDoc, EPinDirection.OUT, arrow_rotation, arrow_insert);
                } else {
                    arrow (svgDoc, pin.getDirection(), arrow_rotation, arrow_insert);
                }
            }

            if (pin_number == pincount) {
                pin_number = 1;
            }
            else {
                ++pin_number; // pin_number += 1
            }
            // Progress along chip edges
            ++pin_total; // pin_total += 1;
            ++pin_counter; // pin_counter += 1;
            if ((edge == Edge.BOTTOM) && (pin_counter == pincount_w)) {
                pin_counter = 0;
                if (pincount_h > 0) {
                    edge = Edge.RIGHT;    // Quad
                } else {
                    edge = Edge.UP;    // Dual
                }
            }
            else if ((edge == Edge.RIGHT) && (pin_counter == pincount_h)) {
                pin_counter = 0;
                edge = Edge.UP;
            }
            else if ((edge == Edge.UP) && (pin_counter == pincount_w)) {
                pin_counter = 0;
                edge = Edge.LEFT; // 3
            }

            logger.trace("pin_total = {}, pin_counter = {}, edge = {}", pin_total, pin_counter, edge);

        } // end of for (Pin pin : chip.getPins()) {

        if (pin_total != pincount) {
            logger.error("pin_total {} != pincount {} ", pin_total, pincount);
            System.exit(3);
        }

        // Chip package outline
        // stroke_width = "1", stroke = "black",
        rect = document.createElementNS(SVG_NAMESPACE_URI, SVG_RECT_TAG);
        rect.setAttributeNS(null, "x", Integer.toString(chip_x));
        rect.setAttributeNS(null, "y", Integer.toString(arrow_length + maxPinSize));
        rect.setAttributeNS(null, "width", Integer.toString(chip_w));
        rect.setAttributeNS(null, "height", Integer.toString(chip_h));
        rect.setAttributeNS(null, "fill", "none");
        rect.setAttributeNS(null, "stroke", "black");
        rect.setAttributeNS(null, "stroke-width", "1");
        svgDoc.appendChild(rect);

        File svgFile = getSvgFileName(chip.getOutputFile());
        logger.info("Drawing chip {} ('{}')", chip.getName(), svgFile.getAbsolutePath());
        // Serialize SVG
        try (Writer out = new OutputStreamWriter(new FileOutputStream(svgFile))) {
            DOMUtilities.writeDocument(document, out);
            logger.info("DONE!");
        }
        catch (IOException e) {
            logger.warn("cannot write SVG file '{}'", chip.getOutputFile(), e);
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String toString() {
        StringJoiner stringJoiner = new StringJoiner(", ", getClass().getName() + "[", "]")
                .add("chip = " + chip);

        if (hasLogo) {
            if (logoDir != null) {
                stringJoiner.add(", logos folder = '").add(logoDir.getAbsolutePath()).add("'");
            }
        } else {
            stringJoiner.add(", no logo in SVG");
        }
        if (outputDir != null) {
            stringJoiner.add(", output folder = '").add (outputDir.getAbsolutePath()).add ("'");
        }
        return stringJoiner.toString();
    }

    //
    private void addLogoImage(Element svgDoc, Logo logo, LogoImage image,
                              float posX, float posY, double sizeX, double sizeY) {
        logger.debug("draw logo @ {}:{} of size {}x{}", posX, posY, sizeX, sizeY);
        Element element = document.createElementNS(SVG_NAMESPACE_URI, SVG_IMAGE_TAG);
        element.setAttribute("preserveAspectRatio", "none");
        element.setAttribute("x", Float.toString(posX));
        element.setAttribute("y", Float.toString(posY));
        element.setAttribute("width", Double.toString(sizeX));
        element.setAttribute("height", Double.toString(sizeY));
        element.setAttribute("href", logo.getBase64Data(image));
        element.setAttribute("preserveAspectRatio", "xMinYMin");
        svgDoc.appendChild(element);
    }

    private void addText(Element parent, String text, String fontName, int fontSize, String fontWeight, float x, double y) {
        logger.debug("text '{}' will be written @ {} x {} with a size of {} pixels", text, x, y, fontSize);
        String svgNS = parent.getNamespaceURI();

        Element textEl = document.createElementNS(svgNS, "text");
        textEl.setAttribute("x", Float.toString(x));
        textEl.setAttribute("y", Double.toString(y));

        // Styling (SVG/CSS)
        if (USE_ORIG_STYLE) {
            textEl.setAttribute("style", "font-size:" + fontSize + "px; font-family:" + fontName);
        }
        else {
            textEl.setAttribute("font-family", fontName);
            textEl.setAttribute("font-weight", fontWeight);
            textEl.setAttribute("font-size", Integer.toString(fontSize));
        }
        textEl.setAttribute("fill", "black");

        // Text node (NOT an attribute)
        textEl.appendChild(document.createTextNode(text));

        parent.appendChild(textEl);
    }

    private void drawPinName(String pinName, boolean isGND, Element parent, short textRotation, float[] txtPosition) {
        String svgNS = parent.getNamespaceURI();

        Element textEl = document.createElementNS(svgNS, "text");

        textEl.setAttribute("x", Float.toString(txtPosition[0]));
        textEl.setAttribute("y", Float.toString(txtPosition[1]));

        // Styling (SVG/CSS)
        textEl.setAttribute("font-family", PIN_FONT_NAME);
        textEl.setAttribute("font-weight", "bold");
        textEl.setAttribute("font-size", Integer.toString(font_size));
        textEl.setAttribute("fill", isGND ? "white" : "black");

        textEl.setAttribute("transform", "rotate(" + textRotation + ")");

        // Text node (NOT an attribute)
        textEl.appendChild(document.createTextNode(pinName));

        parent.appendChild(textEl);
    }

    /**
     * Will draw something like {@literal <path d="M 380.0,-100.0 l -10.0,10.0 l 10.0,10.0 z" fill="black" stroke="black" stroke-width="1" transform="rotate(90)"/>}
     *
     * @param svgDoc {@link Element}
     * @param direction {@link EPinDirection}
     * @param rotation {@code short} the angle in degrees
     * @param position {@code int[2]} where the arrow should be inserted
     */
    private void arrow(Element svgDoc, EPinDirection direction, short rotation, int[] position) {
        if (!(direction == EPinDirection.IN || direction == EPinDirection.OUT)) {
            logger.warn("bad direction {}", direction);
            return;
        }
        logger.trace("drawing arrow in direction {} with a rotation of {}° @ {}:{}",
                direction, rotation, position[0], position[1]);
        StringBuilder d = new StringBuilder("M ");
        if (direction == EPinDirection.OUT) {
            d.append(position[0]).append(',').append(position[1])
                    .append(" l ").append(arrow_length).append(',').append(arrow_length)
                    .append(" l ").append(-arrow_length).append(',').append(arrow_length);
        } else {
            d.append(position[0] + arrow_length).append(',').append(position[1])
                    .append(" l ").append(-arrow_length).append(',').append(arrow_length)
                    .append(" l ").append(arrow_length).append(',').append(arrow_length);
        }
        d.append(" z");
        logger.trace("d attribute = '{}'", d);
        String svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI;
        Element arrow = document.createElementNS(svgNS, SVG_PATH_TAG);
        arrow.setAttribute("fill", "black");
        arrow.setAttribute("stroke", "black");
        //arrow.setAttribute("stroke-width=", "1");
        arrow.setAttribute("transform", "rotate(" + rotation + ')');
        arrow.setAttribute("d", d.toString());

        svgDoc.appendChild(arrow);
    }

}
