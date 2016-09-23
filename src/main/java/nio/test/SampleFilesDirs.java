package nio.test;

import nio.util.FileOperations;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * This class is for creating sample files and directories in order to test the test cases in LocalFileTest class.
 *
 * @author Chanaka Lakmal
 */
@SuppressWarnings("WeakerAccess")
public class SampleFilesDirs {

    private static final int DELAY = 1000;
    private static final int TOLERANCE = 1000;
    private static final int PROCESS_WAIT = DELAY + TOLERANCE;

    /**
     * Create N no of XML files under the folder structure of /[0-9]/[0-9]A/[0-9].xml
     *
     * @param N    no of test files
     * @param root root path
     * @param path inner path
     * @throws IOException if an error occurs while creating and deleting the files and directories
     */
    @SuppressWarnings("WeakerAccess")
    public void createNFileTree_MultipleFileTypes(int N, String root, String path) throws IOException {
        int countXML = 0;
        int countTXT = 0;
        FileOperations.createDirs(Paths.get(root), path);
        switch (N) {
            case 100:
                for (int j = 1; j <= 10; j++) {
                    FileOperations.createDirs(Paths.get(root), path + "/" + j + "A");
                    for (int k = 1; k <= 10; k++) {
                        int t = (j - 1) * 10 + k;
                        if (j % 5 == 0) {
                            FileOperations.createFiles(Paths.get(root), path + "/" + j + "A/" + t + ".txt");
                            countTXT++;
                        } else {
                            FileOperations.createFiles(Paths.get(root), path + "/" + j + "A/" + t + ".xml");
                            countXML++;
                        }
                    }
                }
                break;
            case 1000:
                for (int i = 1; i <= 10; i++) {
                    FileOperations.createDirs(Paths.get(root), path + "/" + i);
                    for (int j = 1; j <= 10; j++) {
                        FileOperations.createDirs(Paths.get(root), path + "/" + i + "/" + j + "A");
                        for (int k = 1; k <= 10; k++) {
                            int t = (i - 1) * 100 + (j - 1) * 10 + k;
                            if (k % 3 == 0) {
                                FileOperations.createFiles(Paths.get(root), path + "/" + i + "/" + j + "A/" + t + ".txt");
                                countTXT++;
                            } else {
                                FileOperations.createFiles(Paths.get(root), path + "/" + i + "/" + j + "A/" + t + ".xml");
                                countXML++;
                            }
                        }
                    }
                }
                break;
            default:
                break;
        }
        System.err.println(countXML + " XML files created !");
        System.err.println(countTXT + " TXT files created !");
    }

    /**
     * Create N no of XML files under the folder structure of /[0-9]/[0-9]A/[0-9].xml
     *
     * @param N    no of test files
     * @param root root path
     * @param path inner path
     * @throws IOException if an error occurs while creating and deleting the files and directories
     */
    @SuppressWarnings("WeakerAccess")
    public void createNFileTree(int N, String root, String path) throws IOException {
        int count = 0;
        FileOperations.createDirs(Paths.get(root), path);
        switch (N) {
            case 100:
                for (int j = 1; j <= 10; j++) {
                    FileOperations.createDirs(Paths.get(root), path + "/" + j + "A");
                    for (int k = 1; k <= 10; k++) {
                        int t = (j - 1) * 10 + k;
                        FileOperations.createFiles(Paths.get(root), path + "/" + j + "A/" + t + ".xml");
                        count++;
                    }
                }
                break;
            case 1000:
                for (int i = 1; i <= 10; i++) {
                    FileOperations.createDirs(Paths.get(root), path + "/" + i);
                    for (int j = 1; j <= 10; j++) {
                        FileOperations.createDirs(Paths.get(root), path + "/" + i + "/" + j + "A");
                        for (int k = 1; k <= 10; k++) {
                            int t = (i - 1) * 100 + (j - 1) * 10 + k;
                            FileOperations.createFiles(Paths.get(root), path + "/" + i + "/" + j + "A/" + t + ".xml");
                            count++;
                        }
                    }
                }
                break;
            default:
                break;
        }
        System.err.println(count + " files created !");
    }

    /**
     * Create N no of XML file set in the given path for testing purposes
     *
     * @param N    no of test files
     * @param root root path
     * @param path inner path
     * @throws IOException if an error occurs while creating and deleting the files and directories
     */
    @SuppressWarnings("WeakerAccess")
    public void createNFiles_MultipleFileTypes(int N, String root, String path) throws IOException {
        int countXML = 0;
        int countTXT = 0;
        FileOperations.createDirs(Paths.get(root), path);
        for (int i = 1; i <= N; i++) {
            if (i % 10 == 0) {
                FileOperations.createFiles(Paths.get(root), path + "/" + i + ".txt");
                countTXT++;
            } else {
                FileOperations.createFiles(Paths.get(root), path + "/" + i + ".xml");
                countXML++;
            }
        }
        System.err.println(countXML + " XML files created !");
        System.err.println(countTXT + " TXT files created !");
    }

    /**
     * Create N no of XML file set in the given path for testing purposes
     *
     * @param N    no of test files
     * @param root root path
     * @param path inner path
     * @throws IOException if an error occurs while creating and deleting the files and directories
     */
    @SuppressWarnings("WeakerAccess")
    public void createNFiles(int N, String root, String path) throws IOException {
        int count = 0;
        FileOperations.createDirs(Paths.get(root), path);
        for (int i = 1; i <= N; i++) {
            FileOperations.createFiles(Paths.get(root), path + "/" + i + ".xml");
            count++;
        }
        System.err.println(count + " files created !");
    }

    /**
     * Create some sample files and directories for the testing purpose
     * <p>
     * *               <BeforeClass>                |       <TESTING>
     * *                                            |
     * *                     nio                    |
     * *     _________________|_____________________|__________________________
     * *    |           |           |         |     |   |         |           |
     * *    A           B           C       a.xml   |   T       t.xml       t.txt
     * *    |           |           |               |   |
     * *    |-a.txt     BA          CA              |   |-t.xml
     * *    |-a.xml     |           |               |   |-TA
     * *    |-b.xml     |-a.xml     |-a.xml         |       |
     * *                |-b.xml     |-b.xml         |       |-t.xml
     * *
     *
     * @param root root path
     * @throws IOException          if an error occurs while creating and deleting the files and directories
     * @throws InterruptedException if an error occurs while executing the thread sleep
     */
    @SuppressWarnings("WeakerAccess")
    public void createTestFiles(String root) throws IOException, InterruptedException {
        Path rootPath = Paths.get(root);
        ArrayList<String> dirList = new ArrayList<>();
        ArrayList<String> fileList = new ArrayList<>();
        dirList.add(root + "/T");
        dirList.add(root + "/T/TA");
        fileList.add("t.xml");
        fileList.add("t.txt");
        fileList.add("T/t.xml");
        fileList.add("T/TA/t.xml");
        fileList.add("B/BA/b.xml");
        fileList.add("C/CA/b.xml");

        for (String dir : dirList) {
            System.out.println("CREATED  : " + dir);
            FileOperations.createDirs(rootPath, dir);
            Thread.sleep(PROCESS_WAIT);
        }

        for (String file : fileList) {
            System.out.println("CREATED  : " + file);
            FileOperations.createFiles(rootPath, file);
            Thread.sleep(PROCESS_WAIT);
        }
    }
}
