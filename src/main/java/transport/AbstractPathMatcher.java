package transport;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

/**
 * This class will match the given path pattern with the file and returned a boolean variable
 * <p>
 * The pattern can be a "glob" or "regex" pattern
 * * More info: http://docs.oracle.com/javase/7/docs/api/java/nio/file/FileSystem.html#getPathMatcher(java.lang.String)
 *
 * @author Chanaka Lakmal
 * @since 1.0.0
 */
@SuppressWarnings("WeakerAccess")
public class AbstractPathMatcher {

    public boolean isMatchPattern(GRPattern GRPattern, Path file) {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher(GRPattern.getPatternSyntax() +
                Paths.get(GRPattern.getPathPattern()).toString());
        Path name = file.getFileName();
        return name != null && matcher.matches(file);
    }
}
