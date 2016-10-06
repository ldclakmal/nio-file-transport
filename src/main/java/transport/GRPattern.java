package transport;

/**
 * This class is used for keep the GRPattern with the pattern syntax and the path pattern
 *
 * @author Chanaka Lakmal
 * @since 1.0.0
 */
@SuppressWarnings("WeakerAccess")
public class GRPattern {

    private String patternSyntax;
    private String pathPattern;

    /**
     * Create pattern object with the patternSyntax and the pathPattern
     *
     * @param patternSyntax whether the pattern is glob or regex
     * @param pathPattern   path pattern in the form of glob or regex
     */
    public GRPattern(String patternSyntax, String pathPattern) {
        this.patternSyntax = patternSyntax;
        this.pathPattern = pathPattern;
    }

    @SuppressWarnings("WeakerAccess")
    public String getPatternSyntax() {
        return patternSyntax;
    }

    @SuppressWarnings("WeakerAccess")
    public String getPathPattern() {
        return pathPattern;
    }
}