package eu.f.m;

/**
 * @author Marian-N. I.
 */
public enum EPinDirection {
    IN,
    OUT,
    BIDIR;

    static EPinDirection getDirection (String dir) {
        for (EPinDirection pinDir : EPinDirection.values()) {
            if (pinDir.name().equalsIgnoreCase(dir)) {
                return  pinDir;
            }
        }
        return null;
    }
}
