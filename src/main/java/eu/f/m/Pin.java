package eu.f.m;

/**
 * @author Marian-N. I.
 */
class Pin {
    /**
     * Pin name
     */
    private final String name;

    private final EPinType type;

    private final EPinDirection direction;

    /**
     * Pin name, Type, Direction (IN, OUT, BIDIR, or nothing)
     * @param config {@link {@link String}{@code []}
     */
    Pin (String[] config) {
        name = config[0];
        type = config.length > 1 ? EPinType.getPinType(config[1]) : EPinType.UNKNOWN;
        if (type == EPinType.POWER) {
            direction = null;
        } else {
            direction = config.length > 2 ? EPinDirection.getDirection(config[2]) : null;
        }
    }

    String getName() {
        return name;
    }

    EPinType getType() {
        return type;
    }

    EPinDirection getDirection() {
        return direction;
    }

    boolean hasDirection () {
        return direction != null;
    }

    boolean isBiDirectional () {
        return EPinDirection.BIDIR == direction;
    }

    PinColor getColor () {
        PinColor c;
        switch (type) {
            case ADDRESS -> c = new PinColor(255, 255, 127);
            case CONTROL -> c = new PinColor(159,127,223);
            case DATA -> c = new PinColor(127,191,255);
            case GRAPHICS -> c = new PinColor(159,191,127);
            case IN ->  c = new PinColor(0,255,191);
            case CLOCK -> c = new PinColor (255,0,255);
            case MULTIPLEXED -> c = new PinColor (255,127,0);
            case OUT -> c = new PinColor (0,159,127);
            case POWER -> c = "GND".equalsIgnoreCase(name) ? PinColor.GREY : PinColor.RED;
            case ANALOG -> c = new PinColor(100,200,100);

            case UNKNOWN -> c = new PinColor(222, 222, 222);
            default -> c = PinColor.BLACK;
        }
        return c;
    }

    /**
     *
     * @return {@link String} RGB color to be used in the SVG attribute {@literal style="fill: rgb(r,g,b); ..."}
     */
    String getRGB () {
        PinColor c = getColor();
        return "rgb(" + c.r() + ',' + c.g() + ',' + c.b() + ')';
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getName())
                .append("[")
                .append("name = '").append(name).append("'")
                .append(", type = ").append(type);
        if (direction != null) {
            sb.append(", direction = ").append(direction);
        }
        return sb.append(']').toString();
    }
}
