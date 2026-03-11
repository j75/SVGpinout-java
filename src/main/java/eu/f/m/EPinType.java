package eu.f.m;

/**
 * @author Marian-N. I.
 */
enum EPinType {
    ADDRESS('A'),
    CONTROL('C'),
    DATA('D'),
    GRAPHICS('G'),
    IN('I'),
    CLOCK('K'),
    MULTIPLEXED('M'),
    OUT('O'),
    POWER('P'),
    /**
     * new value added
     */
    ANALOG('B'),

    UNKNOWN('?');

    private final char symbol;
    EPinType(char c) {
        symbol = c;
    }

    char getSymbol () {
        return symbol;
    }

    static EPinType getPinType (String t)
    {
        if (t == null || t.isBlank()) {
            return UNKNOWN;
        }
        char pinSymbol = t.charAt(0);
        for (EPinType pinType : EPinType.values()) {
            if (pinSymbol == pinType.getSymbol()) {
                return pinType;
            }
        }
        return UNKNOWN;
    }
}
