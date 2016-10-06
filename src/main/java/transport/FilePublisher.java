package transport;

import org.adroitlogic.logging.api.Logger;
import org.adroitlogic.logging.api.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * This class is for handling the notifications of the watcher service. There is a watch service which keeps the eye on
 * the local file system and whenever a file or directory is created or modified it will trigger an event. Then if it is
 * a directory it will be registered in the keysMap and if it is a file it will be matched with the registered path patterns
 * and will be sent to the engine with the reference of matched path pattern.
 * <p>
 * The java.nio.file package provides a file change notification API, called the Watch Service API. This API enables to
 * register a directory (or directories) with the watch service. When registering, we tell the service which types of
 * events we are interested in: file creation, file deletion, or file modification. When the service detects an event of
 * interest, it is forwarded to the registered process. The registered process has a thread (or a pool of threads)
 * dedicated to watching for any events it has registered for. When an event comes in, it is handled as needed.
 *
 * @author Chanaka Lakmal
 * @since 1.0.0
 */
@SuppressWarnings("WeakerAccess")
public class FilePublisher extends AbstractPathMatcher implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(FilePublisher.class);

    private ExecutorService executorService;
    private WatchService watcher;
    private HashMap<GRPattern, HashMap<String, Object>> patternMap;
    private HashMap<WatchKey, Path> keysMap;

    /**
     * Register all the parameters sent by the NIOFileTransportListener and create a thread pool in order to handle the
     * files missed whenever an OVERFLOW event occurs and the initial directory registration.
     *
     * @param watcher     NIO watch service
     * @param patternMap  pattern map which keeps the set of path patterns with the relevant dataMap
     * @param keysMap     keys map which keeps the keys of directories with the directory path
     */
    public FilePublisher(WatchService watcher, HashMap<GRPattern, HashMap<String, Object>> patternMap, HashMap<WatchKey, Path> keysMap) {
        this.watcher = watcher;
        this.patternMap = patternMap;
        this.keysMap = keysMap;
        this.executorService = Executors.newFixedThreadPool(Integer.MAX_VALUE);
    }

    /**
     * Register the GRPattern and data map sent by the NIOFileTransportListener
     *
     * @param GRPattern GRPattern object which should be checked files for with the GRPattern type and path GRPattern
     * @param dataMap data map which contains all the parameters sent by the user
     * @throws IOException if error occurs when walk through file system
     */
    public void registerPattern(GRPattern GRPattern, HashMap<String, Object> dataMap) throws IOException {
        this.patternMap.put(GRPattern, dataMap);
        Path rootPath = Paths.get("/tmp");

        /*
         * register watchers for all the directories under the rootPath set by the user recursively
         * if the root path is not given it create a set of directories for the root path and register root directory
         */
        if(Files.notExists(rootPath)){
            Files.createDirectories(rootPath);
        }
        registerAll(rootPath);
    }

    /**
     * Register the given directory with the WatchService
     *
     * @param dir the directory which is registering
     * @throws IOException if error occurs when registering the directory for watcher and return the key
     */
    private void register(Path dir) throws IOException {
        /*
         * this will set a WatchKey for the given directory if it is not registered earlier
         */
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);
        logger.debug("A WatchKey {} registered for the directory {}", key.toString().split("@")[1], dir);

        /*
         * keep the register time of the directory in order to detect the files which have been created before the
         * registration of watcher for the directory. Then manually check for files which have been created before
         * we set the watcher
         */
        long registerTime = System.currentTimeMillis();

        /*
         * put the keys of directories in a HashMap which generates the event to check for changes
         */
        Path currentPath = keysMap.put(key, dir);
        if (currentPath != null) {
            logger.debug("Path {} is already in the map", dir);
        } else {
            logger.debug("Put the key {} to map | Directory : {} | Map : {}", key.toString().split("@")[1], dir, keysMap);
        }

        /*
         * create a new thread to catch the files which have been created before the registration of the newly created directory
         */
        executorService.submit(new ManualHandler(patternMap, registerTime, dir));
        logger.debug("Submitted {} for process manually due to initial registration", dir);
    }

    /**
     * Register the given directory, and all its sub-directories, with the WatchService...
     *
     * @param start starting directory
     * @throws IOException if error occurs while walk through file system
     */
    private void registerAll(final Path start) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Process all the events for keys queued to the watcher. Whenever a file or directory is created under the registered
     * directory of the keysMap that event will trigger here.
     * <p>
     * First the WatchService will add files to the queue and we process it by taking them as batches by key.pollEvents()
     * But if an OVERFLOW occurs it will set the kind of the batch as OVERFLOW -> handled using ManualHandler
     * After we reset the queue it will start the process again
     */
    private void dispatchEvents() {
        for (; ; ) {
            /*
             * even though this is an infinite loop this will wait for key to be signalled
             */
            WatchKey key;
            try {
                key = watcher.take();
                logger.debug("WatchKey {} was taken successfully", key.toString().split("@")[1]);
            } catch (InterruptedException e) {
                logger.error("Couldn't take the watcher due to :", e);
                return;
            }

            /*
             * check whether the directory which is returned under the key is registered in the keysMap
             * if it is not that is not a valid directory to scan for the files
             */
            final Path dir = keysMap.get(key);
            if (dir == null) {
                logger.error("WatchKey not recognized ! | Map : {}", keysMap);
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();

                if (kind == OVERFLOW) {
                    /*
                     * Keep the time we detected when an overflow occurs -> overflowTime
                     * so that we have to manually process the file system in order to detect the files which
                     * have been created before the overflowTime
                     */
                    long overflowTime = System.currentTimeMillis();
                    logger.warn("OVERFLOW ! | Directory : {} | Time : {}", dir, overflowTime);

                    /*
                     * Manually check for files which have created when an overflow occurs
                     * This will execute here because if an OVERFLOW event triggers it should be handled manually
                     */
                    executorService.submit(new ManualHandler(patternMap, overflowTime, dir));
                    logger.debug("Submitted {} for process manually due to an overflow", dir);

                    continue;
                }

                /*
                 * Context for directory entry event is the file name of entry
                 */
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();               // eg. name = 10.xml
                Path child = dir.resolve(name);         // eg. child = /tmp/ram/test/1000Set/10.xml

                logger.debug("PROCESS | Directory : {} | Path : {} | Kind : {}", dir, child, kind);

                /*
                 * if a file is created it will generate ENTRY_CREATE event initially and ENTRY_MODIFY event at the last
                 * so we capture the ENTRY_MODIFY event in order to call the sendMsg method at the end of the file creation
                 *
                 * the file should not be a directory also and the file path (child) should be matched with a pattern
                 * registered in the patternMap
                 */
                if (kind == ENTRY_MODIFY && !Files.isDirectory(child)) {
                    for (Map.Entry<GRPattern, HashMap<String, Object>> entry : patternMap.entrySet()) {
                        if (isMatchPattern(entry.getKey(), child)) {
                            System.err.println("---- "+child);
                            logger.debug("File {} submitted for scheduling", child);
                            break;
                        }
                    }
                }

                /*
                 * if directory is created, then register it and its sub-directories
                 */
                if (kind == ENTRY_CREATE) {
                    try {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            registerAll(child);
                        }
                    } catch (IOException e) {
                        logger.warn("Failed to register call registerAll() due to :", e);
                    }
                }
            }

            /*
             * reset key and remove from set if directory no longer accessible
             */
            boolean valid = key.reset();
            if (!valid) {
                keysMap.remove(key);
                logger.debug("Removed the key {} | Map : {}", key.toString().split("@")[1], keysMap);

                /*
                 * this is because of all directories are inaccessible
                 */
                if (keysMap.isEmpty()) {
                    logger.debug("Break the big loop due to no directories registered already");
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
     * Since the class is implemented by Runnable interface just after the constructor is called this method will executed
     */
    @Override
    public void run() {
        dispatchEvents();
    }
}
