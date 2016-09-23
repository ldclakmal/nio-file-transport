package nio;

/**
 * Reference : https://docs.oracle.com/javase/tutorial/essential/io/find.html
 * <p>
 * sample code that finds files that match the specified glob pattern.
 * For more information on what constitutes a glob pattern, see
 * https://docs.oracle.com/javase/tutorial/essential/io/fileOps.html#glob
 * <p>
 * The file or directories that match the pattern are printed to
 * standard out.  The number of matches is also printed.
 * <p>
 * When executing this application, you must put the glob pattern
 * in quotes, so the shell will not expand any wild cards:
 * java Find . -name "*.java"
 *
 * @author Chanaka Lakmal
 */

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.nio.file.FileVisitResult.CONTINUE;

public class FindPath {

    public static class Finder extends SimpleFileVisitor<Path> {

        private final PathMatcher matcher;
        private Set<Path> pathList;

        public Finder(Pattern pattern) {
            matcher = FileSystems.getDefault().getPathMatcher(pattern.getPatternSyntax() + Paths.get(pattern.getPathPattern()).getParent().toString());
            pathList = Collections.synchronizedSet(new HashSet<Path>());
        }

        // Compares the glob pattern against the file or directory name.
        void find(Path dir) {
            Path name = dir.getFileName();
            if (name != null && matcher.matches(dir)) {
                // System.out.format("Matched Path Pattern : %s \n", dir);
                pathList.add(dir);
            }
        }

        // Invoke the pattern matching method on each file.
        //        @Override
        //        public FileVisitResult visitFile(Path file,
        //                                         BasicFileAttributes attrs) {
        //            find(file);
        //            return CONTINUE;
        //        }


        // Invoke the pattern matching method on each directory.
        @Override
        public FileVisitResult preVisitDirectory(Path dir,
                                                 BasicFileAttributes attrs) {
            find(dir);
            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file,
                                               IOException exc) {
            System.err.println(exc);
            return CONTINUE;
        }

        public Set<Path> getPathList() {
            return pathList;
        }
    }
}