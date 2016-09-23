package nio.sample;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Example to watch a directory (or tree) for changes to files
 * References: https://docs.oracle.com/javase/tutorial/essential/io/notification.html
 *
 * @author Chanaka Lakmal
 */
public class WatchDir implements Callable<Object> {

    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;
    private boolean trace;

    @SuppressWarnings("unchecked")
    private static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    // overridden method of Callable<V> interface which calls processEvents() method
    @Override
    public Object call() throws Exception {
        processEvents();
        return null;
    }

    /**
     * Creates a WatchService and registers the given directory
     * This method is used if and only if the direct url of the directory is given
     */
    public WatchDir(Path dir) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        keys = new HashMap<>();

        // register directory and process its events
        System.err.format("Scanning Path : %s\n", dir);
        registerAll(dir);

        // enable trace after initial registration
        this.trace = true;
    }

    /**
     * Register the given directory with the WatchService
     */
    private void register(Path dir) throws IOException {
        // this will set a WatchKey for the given directory if it is not registered earlier
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);
        if (trace) {
            Path prev = keys.get(key);
            if (prev == null) {
                System.out.format("register: %s\n", dir);
            } else {
                if (!dir.equals(prev)) {
                    System.out.format("update: %s -> %s\n", prev, dir);
                }
            }
        }
        // put the keys in a HashMap which generates the event to check for changes
        keys.put(key, dir);
    }

    /**
     * Register the given directory, and all its sub-directories, with the WatchService.
     */
    private void registerAll(final Path start) throws IOException {
        // register all the directory and sub-directories
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
     */
    private void processEvents() throws IOException, InterruptedException {
        for (; ; ) {
            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            final Path dir = keys.get(key);
            if (dir == null) {
                System.err.println("WatchKey not recognized !");
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();

                // TBD - provide example of how OVERFLOW event is handled
                if (kind == OVERFLOW) {
                    System.err.println("OVERFLOW !");
                    continue;
                }

                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                Path child = dir.resolve(name);

                // Print out the event here

                System.err.format("%s : %s: %s\n", key.toString().split("@")[1], event.kind().name(), child);

                /*
                 * if directory is created, and watching recursively, then
                 * register it and its sub-directories
                 */
                if (kind == ENTRY_CREATE) {
                    try {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            registerAll(child);
                        }
                    } catch (IOException x) {
                        // ignore to keep sample readable
                    }
                }
            }

            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);

                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }

    // main method for testing purposes
    public static void main(String[] args) throws IOException, InterruptedException {
        Path dir = FileSystems.getDefault().getPath(System.getProperty("java.io.tmpdir"), "/ram/nio");
        new WatchDir(dir).processEvents();
    }
}