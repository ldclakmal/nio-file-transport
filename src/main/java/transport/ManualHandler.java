package transport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * This class is for manual handling the files since the watch service doesn't handle it in following situations
 * - overflow
 * - registering a directory
 *
 * @author Chanaka Lakmal
 * @since 1.0.0
 */
@SuppressWarnings("WeakerAccess")
public class ManualHandler extends AbstractPathMatcher implements Callable<Object> {

    private static final Logger logger =  LogManager.getLogger(ManualHandler.class);

    private HashMap<GRPattern, HashMap<String, Object>> patternMap;
    private long time;
    private final Path dir;
    private final int THRESHOLD = 1500;     // safe value for threshold after testing for many times

    /**
     * Register all the parameters sent by the FilePublisher in order to handle the missed files manually
     *
     * @param patternMap  pattern map which keeps the set of path patterns with the relevant dataMap
     * @param time        the time which the overflow has been occurred or the time which the directory has been registered
     * @param dir         the directory that should be scannned in
     */
    public ManualHandler(HashMap<GRPattern, HashMap<String, Object>> patternMap, long time, Path dir) {
        this.patternMap = patternMap;
        this.time = time;
        this.dir = dir;
    }

    /**
     * Calls just after the constructor called since the class is implemented from Callable interface
     *
     * @return null
     * @throws Exception if error occurs from processManually
     */
    @Override
    public Object call() throws Exception {
        processManually();
        return null;
    }

    /**
     * The method which process the files manually and check according to the registration or overflow event
     * Check for the last modified time of the file and compared it with the given time
     * Use a THRESHOLD here in order to enlarge the time gap since the lastModifiedTime round off the value into 1000
     *
     * @throws IOException if an error occurs while processing file or a directory in the tree
     */
    private void processManually() throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attrs) throws IOException {
                if (dir.equals(directory)) {
                    return FileVisitResult.CONTINUE;
                }
                return FileVisitResult.TERMINATE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                try {
                    if (Files.getLastModifiedTime(file).toMillis() < time + THRESHOLD) {
                        for (Map.Entry<GRPattern, HashMap<String, Object>> entry : patternMap.entrySet()) {
                            if (isMatchPattern(entry.getKey(), file)) {
                                System.err.println("---- " + file);
                                logger.debug("File {} submitted for scheduling", false);
                                break;
                            }
                        }
                    }
                } catch (IOException e) {
                    logger.error("Failed to get the modified time of file due to :", e);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}