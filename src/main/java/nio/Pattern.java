package nio;

/**
 * This class is used for keep the Pattern with the pattern syntax and the path pattern
 *
 * @author Chanaka Lakmal
 */
public class Pattern {
    private String patternSyntax;
    private String pathPattern;

    public Pattern(String patternSyntax, String pathPattern) {
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