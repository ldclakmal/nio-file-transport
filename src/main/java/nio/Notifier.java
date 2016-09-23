package nio;

import org.adroitlogic.logging.api.Logger;
import org.adroitlogic.logging.api.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * This class is for handling the notifications of the watcher service
 *
 * @author Chanaka Lakmal
 */
public class Notifier implements Runnable {

    private WatchService watcher;
    private Map<WatchKey, Path> keys;
    private boolean trace;
    private FindPath.Finder finder;
    private Set<Path> pathList;
    private long processTime;
    private Stack<Pair<Long, Long>> timePairs;
    private Set<Path> fileList;
    private ExecutorService executorService;
    private PathMatcher fileNameMatcher;
    private static final Logger logger = LoggerFactory.getLogger(Notifier.class);

    /**
     * Creates a WatchService and registers the given directory
     * This method is used when the path is given as regex with or without a file name
     *
     * @param rootPath root path of the file structure
     * @param pattern  pattern object which should be checked files for with the pattern type and path pattern
     * @param fileList this should be a synchronized set since this is accessed by multi threads.
     *                 otherwise the size of the HashSet will be invalid
     *                 but the content is seemed that no duplicates up to now but not guaranteed
     * @throws IOException if error occurs when registering the watcher service and walk through file system
     */
    public Notifier(Path rootPath, Pattern pattern, Set<Path> fileList) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<>();
        this.timePairs = new Stack<>();
        this.fileList = fileList;
        this.executorService = Executors.newFixedThreadPool(Integer.MAX_VALUE);

        /*
         * Check for the pattern and keep relevant paths that should be notified the changes
         * in a HashSet called pathList
         * NOTE: the pathList will be updated if there are any relevant paths in the file system only
         */
        this.finder = new FindPath.Finder(pattern);
        Files.walkFileTree(rootPath, finder);
        this.pathList = finder.getPathList();

        /*
         * file name matcher for matching the file name with the given pattern
         */
        fileNameMatcher = FileSystems.getDefault().getPathMatcher(pattern.getPatternSyntax() + Paths.get(pattern.getPathPattern()).getFileName().toString());

        /*
         * Register watchers for all the directories
         * in order to detect newly created directories which matches the given pattern
         */
        registerAll(rootPath);

        /*
         * enable trace after initial registration
         */
        this.trace = true;
    }

    /**
     * Register the given directory with the WatchService
     *
     * @param dir the directory which is registering
     * @throws IOException if error occurs when registering the directory for watcher and return the key
     */
    private void register(Path dir) throws IOException {
        // this will set a WatchKey for the given directory if it is not registered earlier
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);

        /*
         * Keep the register time with the directory in order to detect the files which
         * have been created before the registration of watcher for the directory
         * Then manually check for files which have been created before the watcher set
         */
        long registerTime = System.currentTimeMillis();
        Pair<Path, Long> registerTimePair = new Pair<>(dir, registerTime);
        executorService.submit(new ManualNotifier(registerTimePair, fileNameMatcher, "register", pathList, fileList));
        logger.debug("Submitted {} for process manually due to initial registration", dir);

        if (trace) {
            Path prev = keys.get(key);
            if (prev == null) {
                // System.out.format("register: %s\n", dir);
                logger.debug("Registering a watcher for the newly created directory {} ", dir);
            } else {
                if (!dir.equals(prev)) {
//                    System.out.format("update: %s -> %s\n", prev, dir);
                    logger.debug("Updating a watcher for the created directory {} ", dir);
                }
            }
        }

        /*
         * put the keys in a HashMap which generates the event to check for changes
         */
        keys.put(key, dir);

        /*
         * update the pathList
         * since the pathList is a HashSet there will not be any duplicates
         */
        Files.walkFileTree(dir, finder);
        this.pathList = finder.getPathList();
        logger.debug("Updated the path list: Path List Size : {} ", pathList.size());
    }

    /**
     * Register the given directory, and all its sub-directories, with the WatchService...
     *
     * @param start starting directory
     * @throws IOException if error occurs while walk through file system
     */
    private void registerAll(final Path start) throws IOException {
        /*
         * register all the directory and sub-directories
         */
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Process all events for keys queued to the watcher
     * First the WatchService will add files to the queue and we process it by taking them as batches
     * key.pollEvents()
     * But if an OVERFLOW occurs it will set the kind of the batch as OVERFLOW
     * After we reset the queue it will start the process again
     */
    private void processEvents() {
        for (; ; ) {
            /*
             * even though this is an infinite loop this will wait for key to be signalled
             */
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            final Path dir = keys.get(key);
            if (dir == null) {
                System.err.println("WatchKey not recognized !");
                logger.error("WatchKey not recognized !");
                continue;
            }


            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();

                if (kind == OVERFLOW) {
                    /*
                     * Keep the stack of timePairs containing the following time pairs
                     * 1. the time we started the processing of last batch
                     * 2. the time we detected an overflow occurs
                     * so that we have to manually process the file system in order to detect the files which
                     * have been created within this time period
                     */
                    long overflowTime = System.currentTimeMillis();
                    timePairs.push(new Pair<>(Long.parseLong(String.valueOf(processTime)), Long.parseLong(String.valueOf(overflowTime))));
                    System.err.println("OVERFLOW !");
                    logger.warn("OVERFLOW ! | Directory : {} | Time : {}", dir, overflowTime);

                    /*
                     * Manually check for files which have created when an overflow occurs
                     * This will execute here because if an OVERFLOW event triggers it should be handled manually
                     */
                    executorService.submit(new ManualNotifier(dir, timePairs.pop(), fileNameMatcher, "overflow", pathList, fileList));
                    logger.debug("Submitted {} for process manually due to an overflow", dir);

                    continue;
                }

                /*
                 * Context for directory entry event is the file name of entry
                 */
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();               // eg. name = 10.xml
                Path child = dir.resolve(name);         // eg. path = /tmp/ram/test/1000Set/10.xml

                /*
                 * Keep the starting time of the processing time of current batch
                 * for the use of timePairs
                 */
                processTime = System.currentTimeMillis();
                logger.debug("PROCESS | Directory : {} | Time : {}", dir, processTime);

                /*
                 * Print out the event here
                 * child parent path should be in the pathList
                 * if fileExtension is null => no file type required
                 * else fileExtension should matched to the extension of the child
                 */
                if (pathList.contains(child.getParent())) {
                    if (fileNameMatcher.matches(name)) {
                        // System.out.format("%s: %s\n", event.kind().name(), child);
                        logger.debug("{}: {}", event.kind().name(), child);
                        fileList.add(child);
                    }
                }

                /*
                 * if directory is created, and watching recursively, then
                 * register it and its sub-directories
                 */
                if (kind == ENTRY_CREATE) {
                    try {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            registerAll(child);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            /*
             * reset key and remove from set if directory no longer accessible
             */
            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);

                /*
                 * this is because of all directories are inaccessible
                 */
                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    /*
     * Since the class is implemented by callable interface just after the constructor is called this method will executed
     */
    @Override
    public void run() {
        processEvents();
    }
}
