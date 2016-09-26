import nio.Notifier;
import nio.Pattern;
import nio.sample.Sample;
import nio.sample.WatchDir;
import nio.util.FileOperations;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This is a test class for testing the local files
 * From the beginning of test cases the file system was tested for several cases
 *
 * @author Chanaka Lakmal
 */
public class LocalFileTest {

    private static final int DELAY = 1000;

    private static final String ROOT_PATH = System.getProperty("java.io.tmpdir") + File.separator + "ram" + File.separator + "nio";
    private static final String TEST_PATH = System.getProperty("java.io.tmpdir") + File.separator + "ram" + File.separator + "test";
    private static final String INNER_PATH = ROOT_PATH + File.separator + "A";

    private final SampleFilesDirs sampleFilesDirs = new SampleFilesDirs();
    private final AssureFile assureFile = new AssureFile();

    private static Set<Path> fileList1;
    private static Set<Path> fileList2;
    private static Set<Path> fileList3;

    @Rule
    public TestName testName = new TestName();

    /**
     * Test basic sample code given by Oracle
     * <p>
     * Issues:
     * - Can't identify the files which creates before registering the watch service
     * - Overflow occurs for large number of files
     *
     * @throws IOException          if an error occurs while creating and deleting the files and directories
     * @throws InterruptedException if an error occurs while executing the ExecutorService thread pool
     */
    @Test
    public void testSample() throws IOException, InterruptedException {
        final Path testPath = FileSystems.getDefault().getPath(TEST_PATH);

        ExecutorService service = Executors.newFixedThreadPool(Integer.MAX_VALUE);
        service.submit(new Sample(testPath, true));

        sampleFilesDirs.createNFiles(100, TEST_PATH, TEST_PATH + "/sample");

        service.shutdown();
        service.awaitTermination(10, TimeUnit.SECONDS);
    }

    /**
     * Test for the direct path using the ExecutorService thread pool
     * 2 sample paths tested here
     * - /tmp/nio/
     * - /tmp/test/
     * <p>
     * Issues:
     * - Can't identify the files which creates before registering the watch service
     * - Overflow occurs for large number of files
     *
     * @throws IOException          if an error occurs while creating and deleting the files and directories
     * @throws InterruptedException if an error occurs while executing the ExecutorService thread pool
     */
    @Test
    public void testPath() throws IOException, InterruptedException {
        final Path rootPath = FileSystems.getDefault().getPath(ROOT_PATH);
        final Path testPath = FileSystems.getDefault().getPath(TEST_PATH);
        final Path smbPath = FileSystems.getDefault().getPath("/mnt/smb/chanaka");

        ExecutorService service = Executors.newFixedThreadPool(Integer.MAX_VALUE);
        service.submit(new WatchDir(rootPath));
        service.submit(new WatchDir(testPath));
        service.submit(new WatchDir(smbPath));

//        sampleFilesDirs.createTestFiles(ROOT_PATH);

        service.shutdown();
        service.awaitTermination(1, TimeUnit.HOURS);
    }

    /**
     * Test for recursive paths using the ExecutorService thread pool
     * 3 sample paths tested here
     * - /tmp/nio/
     * - /tmp/test/
     * - /tmp/nio/A
     * Since there are 3 watchers for these paths if a file created in /tmp/nio/A
     * 2 watchers should be get activated
     * <p>
     * Issues:
     * - Can't identify the files which creates before registering the watch service
     * - Overflow occurs for large number of files
     *
     * @throws IOException          if an error occurs while creating and deleting the files and directories
     * @throws InterruptedException if an error occurs while executing the ExecutorService thread pool
     */
    @Test
    public void testInnerPaths() throws IOException, InterruptedException {
        final Path rootPath = FileSystems.getDefault().getPath(ROOT_PATH);
        final Path testPath = FileSystems.getDefault().getPath(TEST_PATH);
        final Path innerPath = FileSystems.getDefault().getPath(INNER_PATH);

        ExecutorService service = Executors.newFixedThreadPool(Integer.MAX_VALUE);
        service.submit(new WatchDir(rootPath));
        service.submit(new WatchDir(testPath));
        service.submit(new WatchDir(innerPath));

        sampleFilesDirs.createTestFiles(ROOT_PATH);

        service.shutdown();
        service.awaitTermination(10, TimeUnit.SECONDS);
    }

    /**
     * Test for path patterns using the ExecutorService thread pool
     * this checks inside of the directories recursively also
     * eg:
     * - ROOT_PATH            => inside any directory in the root directory
     * - ROOT_PATH/**         => inside any directory in the sub directory of the root directory
     * - ROOT_PATH/*          => inside the first level directories seen in the root directory
     * - ROOT_PATH\\**\\*A    => inside any level of directories there should be a folder ends with letter A
     * <p>
     * Issues:
     * - Can't identify the files which creates before registering the watch service
     * - Overflow occurs for large number of files
     *
     * @throws IOException          if an error occurs while creating and deleting the files and directories
     * @throws InterruptedException if an error occurs while executing the ExecutorService thread pool
     */
    @Test
    public void testPathPatterns() throws IOException, InterruptedException {
        Path rootPath = FileSystems.getDefault().getPath(ROOT_PATH);

        ExecutorService service = Executors.newFixedThreadPool(Integer.MAX_VALUE);
        Pattern[] patterns = new Pattern[]{
                new Pattern("glob:", ROOT_PATH + "/**/*A/*"),
                new Pattern("glob:", ROOT_PATH + "/*")
        };
        service.submit(new Notifier(rootPath, patterns[0], fileList1));
        service.submit(new Notifier(rootPath, patterns[1], fileList2));

        sampleFilesDirs.createTestFiles(ROOT_PATH);

        service.shutdown();
        service.awaitTermination(10, TimeUnit.SECONDS);
    }

    /**
     * Test for path patterns with file types using the ExecutorService thread pool
     * eg:
     * - ROOT_PATH/*.xml            => check for any xml file inside any directory in the root directory
     * - ROOT_PATH\\**\\*.xml       => check for any xml file inside any directory in the sub directory of the root directory
     * - ROOT_PATH\\**\\*A\\*.xml   => check for any xml file inside any level of directories; there should be a folder ends with letter A
     * <p>
     * this checks inside of the directories recursively also
     *
     * @throws IOException          if an error occurs while creating and deleting the files and directories
     * @throws InterruptedException if an error occurs while executing the ExecutorService thread pool
     */
    @Test
    public void testPathPatterns_FileTypes() throws IOException, InterruptedException {
        Path rootPath = FileSystems.getDefault().getPath(ROOT_PATH);

        ExecutorService service = Executors.newFixedThreadPool(Integer.MAX_VALUE);
        Pattern[] patterns = new Pattern[]{
                new Pattern("glob:", ROOT_PATH + "/*.xml"),
                new Pattern("glob:", ROOT_PATH + "/**/*.xml")
        };
        service.submit(new Notifier(rootPath, patterns[0], fileList1));
        service.submit(new Notifier(rootPath, patterns[1], fileList2));

        sampleFilesDirs.createNFileTree(100, ROOT_PATH, ROOT_PATH + File.separator + 100 + "Set");

        service.shutdown();
        service.awaitTermination(10, TimeUnit.SECONDS);
    }

    /**
     * Test for large number of files set such as 10000
     * Executor service is used for generating thread pool of watchers
     *
     * @throws IOException          if an error occurs while creating and deleting the files and directories
     * @throws InterruptedException if an error occurs while executing the ExecutorService thread pool
     */
    @Test
    public void testOverflow() throws IOException, InterruptedException {
        Path testPath = FileSystems.getDefault().getPath(TEST_PATH);

        ExecutorService service = Executors.newFixedThreadPool(Integer.MAX_VALUE);
        Pattern[] patterns = new Pattern[]{
                new Pattern("glob:", TEST_PATH + "/**/*.xml"),
                new Pattern("glob:", TEST_PATH + "/*/*.xml")
        };
        service.submit(new Notifier(testPath, patterns[0], fileList1));
        service.submit(new Notifier(testPath, patterns[1], fileList2));

        int N = 10000;
        sampleFilesDirs.createNFiles(N, TEST_PATH, TEST_PATH + File.separator + N + "Set");
        N = 100;
        sampleFilesDirs.createNFiles(N, TEST_PATH, TEST_PATH + File.separator + N + "Set");

        service.shutdown();
        service.awaitTermination(10, TimeUnit.SECONDS);
    }

    /**
     * Test for large number of files set such as 10000
     * Test for file tree structure
     * Executor service is used for generating thread pool of watchers
     *
     * @throws IOException          if an error occurs while creating and deleting the files and directories
     * @throws InterruptedException if an error occurs while executing the ExecutorService thread pool
     */
    @Test
    public void testOverflow_FileTree() throws IOException, InterruptedException {
        Path testPath = FileSystems.getDefault().getPath(TEST_PATH);

        ExecutorService service = Executors.newFixedThreadPool(Integer.MAX_VALUE);
        Pattern[] patterns = new Pattern[]{
                new Pattern("glob:", TEST_PATH + "/**/*.xml"),
                new Pattern("glob:", TEST_PATH + "/**/*A/*.txt")
        };
        service.submit(new Notifier(testPath, patterns[0], fileList1));
        service.submit(new Notifier(testPath, patterns[1], fileList2));

        int N = 10000;
        sampleFilesDirs.createNFiles(N, TEST_PATH, TEST_PATH + File.separator + N + "Set");
        N = 100;
        sampleFilesDirs.createNFiles(N, TEST_PATH, TEST_PATH + File.separator + N + "Set");
        N = 1000;
        sampleFilesDirs.createNFileTree(N, TEST_PATH, TEST_PATH + File.separator + N + "Set");

        service.shutdown();
        service.awaitTermination(10, TimeUnit.SECONDS);
    }

    /**
     * Test for large number of files set such as 10000
     * Test for file tree structure with file type
     * Executor service is used for generating thread pool of watchers
     *
     * @throws IOException          if an error occurs while creating and deleting the files and directories
     * @throws InterruptedException if an error occurs while executing the ExecutorService thread pool
     */
    @Test
    public void testOverflow_FileTree_FileType() throws IOException, InterruptedException {
        Path testPath = FileSystems.getDefault().getPath(TEST_PATH);

        ExecutorService service = Executors.newFixedThreadPool(Integer.MAX_VALUE);
        Pattern[] patterns = new Pattern[]{
                new Pattern("glob:", TEST_PATH + "/**/*.xml"),
                new Pattern("glob:", TEST_PATH + "/**/*.txt"),
                new Pattern("glob:", TEST_PATH + "/**/*A/*.txt")
        };
        service.submit(new Notifier(testPath, patterns[0], fileList1));     // Ans: 32200
        service.submit(new Notifier(testPath, patterns[1], fileList2));     // Ans: 3800
        service.submit(new Notifier(testPath, patterns[2], fileList3));     // Ans: 300

        int N = 25000;
        sampleFilesDirs.createNFiles_MultipleFileTypes(N, TEST_PATH, TEST_PATH + File.separator + N + "Set");
        N = 10000;
        sampleFilesDirs.createNFiles_MultipleFileTypes(N, TEST_PATH, TEST_PATH + File.separator + N + "Set");
        N = 1000;
        sampleFilesDirs.createNFileTree_MultipleFileTypes(N, TEST_PATH, TEST_PATH + File.separator + N + "Set");

        service.shutdown();
        service.awaitTermination(10, TimeUnit.SECONDS);
    }

    /**
     * Test for path pattern type (regex & glob)
     * Executor service is used for generating thread pool of watchers
     *
     * @throws IOException          if an error occurs while creating and deleting the files and directories
     * @throws InterruptedException if an error occurs while executing the ExecutorService thread pool
     */
    @Test
    public void testOverflow_FileTree_FileType_PatternType() throws IOException, InterruptedException {
        Path testPath = FileSystems.getDefault().getPath(TEST_PATH);

        ExecutorService service = Executors.newFixedThreadPool(Integer.MAX_VALUE);

        Pattern[] patterns = new Pattern[]{
                new Pattern("regex:", TEST_PATH + "/[A-Za-z0-9]*/[0-9]*.xml"),
                new Pattern("glob:", TEST_PATH + "/**/*.xml"),
                new Pattern("regex:", TEST_PATH + "/[A-Za-z0-9]*/[0-9]*/[0-9]*A/[0-9]*.txt")
        };
        service.submit(new Notifier(testPath, patterns[0], fileList1));     // Ans: 31500
        service.submit(new Notifier(testPath, patterns[1], fileList2));     // Ans: 32200
        service.submit(new Notifier(testPath, patterns[2], fileList3));     // Ans: 300

        int N = 25000;
        sampleFilesDirs.createNFiles_MultipleFileTypes(N, TEST_PATH, TEST_PATH + File.separator + N + "Set");
        N = 10000;
        sampleFilesDirs.createNFiles_MultipleFileTypes(N, TEST_PATH, TEST_PATH + File.separator + N + "Set");
        N = 1000;
        sampleFilesDirs.createNFileTree_MultipleFileTypes(N, TEST_PATH, TEST_PATH + File.separator + N + "Set");

        service.shutdown();
        service.awaitTermination(10, TimeUnit.SECONDS);
    }

    /**
     * Create the ROOT and TEST directories and 3 xml files for test purposes
     * <p>
     * *               <BeforeClass>                |
     * *                                            |
     * *                     nio                    |
     * *     _________________|________________     |
     * *    |           |           |         |     |
     * *    A           B           C       a.xml   |
     * *    |           |           |               |
     * *    |-a.txt     BA          CA              |
     * *    |-a.xml     |           |               |
     * *    |-b.xml     |-a.xml     |-a.xml         |
     * *                                            |
     *
     * @throws IOException          if an error occurs while creating and deleting the files and directories
     * @throws InterruptedException if an error occurs while executing the ExecutorService thread pool
     */
    @Before
    public void start() throws IOException, InterruptedException {
        if (Files.exists(Paths.get(ROOT_PATH))) {
            cleanSystem();
        }

        /*
         * These fileLists should be synchronized sets because these are accessed by multiple threads concurrently
         * and the HashSet is not thread safe
         * *
         * - Java Doc says...
         * Note that this implementation is not synchronized. If multiple threads access a hash set concurrently,
         * and at least one of the threads modifies the set, it must be synchronized externally.
         */
        fileList1 = Collections.synchronizedSet(new HashSet<Path>());
        fileList2 = Collections.synchronizedSet(new HashSet<Path>());
        fileList3 = Collections.synchronizedSet(new HashSet<Path>());

        FileOperations.createDirs(Paths.get(ROOT_PATH),
                ROOT_PATH, ROOT_PATH + "/A", ROOT_PATH + "/B/BA", ROOT_PATH + "/C/CA");
        FileOperations.createDirs(Paths.get(TEST_PATH), TEST_PATH);
        FileOperations.createFiles(Paths.get(ROOT_PATH),
                "a.xml", "A/a.xml", "A/b.xml", "A/a.txt", "B/BA/a.xml", "C/CA/a.xml");
    }

    /**
     * Print all the result file set
     * Clean all directories and files created
     *
     * @throws IOException          if an error occurs while creating and deleting the files and directories in cleanSystem method
     * @throws InterruptedException if an error occurs while executing the thread sleep in cleanSystem method
     */
    @After
    public void finish() throws IOException, InterruptedException {
        System.err.println("Size of the file list 1 : " + getFileListSize(fileList1));
        System.err.println("Size of the file list 2 : " + getFileListSize(fileList2));
        System.err.println("Size of the file list 3 : " + getFileListSize(fileList3));

        switch (testName.getMethodName()) {
            case "testOverflow_FileTree_FileType":
                assureFile.assureFiles1(fileList1, fileList2, fileList3);
                break;
            case "testOverflow_FileTree_FileType_PatternType":
                assureFile.assureFiles2(fileList1, fileList2, fileList3);
                break;
            default:
                break;
        }

        if (Files.exists(Paths.get(ROOT_PATH))) {
            cleanSystem();
        }
    }

    /**
     * Clean all the directories
     *
     * @throws IOException          if an error occurs while creating and deleting the files and directories
     * @throws InterruptedException if an error occurs while executing the thread sleep
     */
    private void cleanSystem() throws IOException, InterruptedException {
        FileOperations.cleanDir(Paths.get(ROOT_PATH), true);
        Thread.sleep(DELAY);
        FileOperations.cleanDir(Paths.get(TEST_PATH), true);
        Thread.sleep(DELAY);
    }

    /**
     * Return the size of the file list in order to check the program
     *
     * @param fileList the file list which we want to get the size of
     * @return size of the file list
     */
    private int getFileListSize(Set<Path> fileList) {
        return fileList.size();
    }
}
