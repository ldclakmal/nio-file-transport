package nio;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * This class is for manual handling the notification since the watch service doesn't handle it in following situations
 * - overflow
 * - registering a directory
 *
 * @author Chanaka Lakmal
 */
@SuppressWarnings("WeakerAccess")
public class ManualNotifier implements Callable<Object> {

    private Path dir;
    private String status;
    private long time;
    private Set<Path> pathList;
    private Set<Path> fileList;
    private Pair<Long, Long> timePair;
    private PathMatcher fileNameMatcher;
    private final int THRESHOLD = 1250;
    private static final Logger logger = LogManager.getLogger(ManualNotifier.class);

    /**
     * This constructor calls when an directory get registered
     *
     * @param registerPair    the Pair object which contains the dir with the registered time
     * @param fileNameMatcher the PathMatcher object in order to match the fileNamePattern
     * @param status          status says whether this is a registration of a directory of an overflow
     * @param pathList        the list which have the list of paths which should be checked for files
     * @param fileList        the return file list which collects the output
     */
    @SuppressWarnings("WeakerAccess")
    public ManualNotifier(Pair<Path, Long> registerPair, PathMatcher fileNameMatcher, String status, Set<Path> pathList, Set<Path> fileList) {
        this.dir = registerPair.getT();
        this.time = registerPair.getU();
        this.status = status;
        this.pathList = pathList;
        this.fileList = fileList;
        this.fileNameMatcher = fileNameMatcher;
    }

    /**
     * This constructor calls when an overflow get happens
     *
     * @param dir      the directory that should be searched in
     * @param timePair the Pair object which contains the start and end time that should be searched for files
     * @param status   status says whether this is a registration of a directory of an overflow
     * @param pathList the list which have the list of paths which should be checked for files
     * @param fileList the return file list which collects the output
     */
    @SuppressWarnings("WeakerAccess")
    public ManualNotifier(Path dir, Pair<Long, Long> timePair, PathMatcher fileNameMatcher, String status, Set<Path> pathList, Set<Path> fileList) {
        this.dir = dir;
        this.timePair = timePair;
        this.status = status;
        this.pathList = pathList;
        this.fileList = fileList;
        this.fileNameMatcher = fileNameMatcher;
    }

    /**
     * Calls just after the constructor called since the class is implemented from Callable interface
     *
     * @return null
     * @throws Exception if any error occurs due to processManually
     */
    @Override
    public Object call() throws Exception {
        processManually(dir, time, status);
        return null;
    }

    /**
     * The method which process the file structure manually and check according to the registration or overflow
     *
     * @param path   directory that should be checked - assigned by the constructor
     * @param time   registration time of the directory - assigned by the constructor
     * @param status status says whether this is a registration of a directory of an overflow
     * @throws IOException if an error occurs while processing file or a directory in the tree
     */
    private void processManually(final Path path, final long time, final String status) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (pathList.contains(dir)) {
                    return FileVisitResult.CONTINUE;
                }
                return FileVisitResult.TERMINATE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                switch (status.toLowerCase()) {
                    case "register":
                        if (Files.getLastModifiedTime(file).toMillis() < time + THRESHOLD) {
                            if (fileNameMatcher.matches(file.getFileName())) {
                                logger.debug("REGISTER || ENTRY_CREATE: {}", file);
                                fileList.add(file);
                            }
                        }
                        break;
                    case "overflow":
                        /*
                         * Check for the last modified time of the file and compared it with the start and end time in the given pair
                         * Use a THRESHOLD here in order to enlarge the time gap since the lastModifiedTime round off the value into 1000
                         */
                        if (fileNameMatcher.matches(file.getFileName())) {
                            if (Files.getLastModifiedTime(file).toMillis() >= timePair.getT() - THRESHOLD && Files.getLastModifiedTime(file).toMillis() <= timePair.getU() + THRESHOLD) {
                                logger.debug("OVERFLOW || ENTRY_CREATE: {}", file);
                                fileList.add(file);
                            }
                        }
                        break;
                    default:
                        break;
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

}
