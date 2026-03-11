package eu.f.m;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BatikDrawChipTest {
    @Mock
    private ICPackages packages;

    @TempDir
    Path outputFolder;

    private AbstractDrawChipSVG drawChip;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");
        drawChip = new BatikDrawChip(packages);
    }

    /**
     * 741 OP-AMP
     */
    @Test
    void generate() {
       assertNotNull(drawChip);
       RuntimeException thrown = assertThrows(NullPointerException.class, () -> drawChip.setChipFile(null));
       assertTrue(thrown.getMessage().contains("no chip file"));
       ICPackage pack = new ICPackage("DIP8", 8,4,0,1,50,8,200,1);
       when (packages.getPackage(eq ("DIP8"))).thenReturn(Optional.of(pack));
       ClassLoader classLoader = getClass().getClassLoader();
       File csvFile = new File(Objects.requireNonNull(classLoader.getResource("741.csv")).getFile());
       drawChip.setChipFile(csvFile);
       drawChip.setOutputDir(outputFolder.toFile().getAbsolutePath());
       drawChip.generate();
       IO.println("SVG in folder" + outputFolder);
    }

    @Test
    void generateDummy() {
        assertNotNull(drawChip);
        RuntimeException thrown = assertThrows(NullPointerException.class, () -> drawChip.setChipFile(null));
        assertTrue(thrown.getMessage().contains("no chip file"));
        ICPackage pack = new ICPackage("QFP12", 12, 3, 3, 1, 50, 5, 50, 1);
        when (packages.getPackage(eq ("QFP12"))).thenReturn(Optional.of(pack));
        ClassLoader classLoader = getClass().getClassLoader();
        File csvFile = new File(Objects.requireNonNull(classLoader.getResource("dummy.csv")).getFile());
        drawChip.setChipFile(csvFile);
        drawChip.setOutputDir(outputFolder.toFile().getAbsolutePath());
        drawChip.generate();
        IO.println("SVG in folder" + outputFolder);
    }

    @Test
    void generateNeo() {
        assertNotNull(drawChip);
        RuntimeException thrown = assertThrows(NullPointerException.class, () -> drawChip.setChipFile(null));
        assertTrue(thrown.getMessage().contains("no chip file"));
        ICPackage pack = new ICPackage("QFP48", 48, 12, 12, 1, 40, 30, 0, 0.5f);
        when (packages.getPackage(eq ("QFP48"))).thenReturn(Optional.of(pack));
        ClassLoader classLoader = getClass().getClassLoader();
        File csvFile = new File(Objects.requireNonNull(classLoader.getResource("neo-buf.csv")).getFile());
        drawChip.setChipFile(csvFile);
        drawChip.setOutputDir(outputFolder.toFile().getAbsolutePath());
        drawChip.generate();
        IO.println("SVG in folder" + outputFolder);
    }

    @Test
    void generateDIP40() {
        assertNotNull(drawChip);
        RuntimeException thrown = assertThrows(NullPointerException.class, () -> drawChip.setChipFile(null));
        assertTrue(thrown.getMessage().contains("no chip file"));
        ICPackage pack = new ICPackage("DIP40", 40, 20, 0, 1, 45, 20, 300, 1.0f);
        when (packages.getPackage(eq ("DIP40"))).thenReturn(Optional.of(pack));
        ClassLoader classLoader = getClass().getClassLoader();
        File csvFile = new File(Objects.requireNonNull(classLoader.getResource("MMP1206.csv")).getFile());
        drawChip.setChipFile(csvFile);
        drawChip.setOutputDir(outputFolder.toFile().getAbsolutePath());
        drawChip.generate();
        IO.println("SVG in folder" + outputFolder);
    }

    @Test
    void testColors() {
        assertNotNull(drawChip);
        ICPackage pack = new ICPackage("DIP14", 14,7,0,1,50,15,200,1);
        when (packages.getPackage(eq ("DIP14"))).thenReturn(Optional.of(pack));
        ClassLoader classLoader = getClass().getClassLoader();
        File csvFile = new File(Objects.requireNonNull(classLoader.getResource("test-colors.csv")).getFile());
        drawChip.setChipFile(csvFile);
        drawChip.setOutputDir(outputFolder.toFile().getAbsolutePath());
        drawChip.generate();
        IO.println("SVG in folder" + outputFolder);
    }

    @Test
    void getMaxPinNameLength() {
        assertNotNull(drawChip);
        assertEquals(0, drawChip.getMaxPinNameLength(null));

        List<Pin> pins = new ArrayList<>();
        int nbPins = 8;
        for (int i = 0; i < nbPins; i++) {
            char[] buf = new char[i + 1];
            Arrays.fill(buf, (char) ('a' + i));
            Pin pin = mock (Pin.class);
            when (pin.getName()).thenReturn(new String(buf));
            pins.add(pin);
        }
        assertEquals(nbPins, drawChip.getMaxPinNameLength(pins));
    }
}