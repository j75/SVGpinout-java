package eu.f.m;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PinTest {

    @Test
    void getName() {
        String[] pins = new String[] {"name", "A", "IN"};
        Pin pin = new Pin(pins);
        assertEquals(pins[0], pin.getName());
    }

    @Test
    void getType() {
        Pin pin = new Pin(new String[]{"name", "A", "IN"});
        assertEquals(EPinType.ADDRESS, pin.getType());

        pin = new Pin(new String[] {"name", "Z", "IN"});
        assertEquals(EPinType.UNKNOWN, pin.getType());
    }

    @Test
    void getDirection() {
        Pin pin = new Pin(new String[]{"VCC", "P"});
        assertNull(pin.getDirection());

        pin = new Pin(new String[] {"name", "D", "IN"});
        assertEquals(EPinDirection.IN, pin.getDirection());
    }

    @Test
    void getRGBColor() {
        Pin pin = new Pin(new String[]{"name", "A", "IN"});
        assertEquals("rgb(255,255,127)", pin.getRGB());
        pin = new Pin(new String[]{"NC"});
        assertEquals("rgb(222,222,222)", pin.getRGB());
        pin = new Pin(new String[]{"VCC", "P"});
        assertEquals("rgb(255,0,0)", pin.getRGB());
    }
}