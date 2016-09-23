package nio.test;

import org.apache.commons.collections.CollectionUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;

/**
 * This class is for assuring the fileList which is given as the output after the process
 *
 * @author Chanaka Lakmal
 */
@SuppressWarnings("WeakerAccess")
public class AssureFile {

    private static final String TEST_PATH = System.getProperty("java.io.tmpdir") + File.separator + "ram" + File.separator + "test";

    /**
     * Assure the list contains the files which we wanted
     * Sometimes a file may created in a certain path but the event may be called from a different path
     * This assures that it will not happen for @Test method testOverflow_FileTree_FileType()
     *
     * @param XMLFileList XML file list
     * @param TXTFileList TXT file list
     * @param TXTDirList  TXT file list with directory structure
     */
    @SuppressWarnings("WeakerAccess")
    public void assureFiles1(Set<Path> XMLFileList, Set<Path> TXTFileList, Set<Path> TXTDirList) {
        Set<Path> testXMLList = new HashSet<>();
        Set<Path> testTXTList = new HashSet<>();
        Set<Path> testTXTDirList = new HashSet<>();

        int N = 25000;
        for (int i = 1; i <= N; i++) {
            if (i % 10 == 0) {
                testTXTList.add(Paths.get(TEST_PATH + "/" + N + "Set/" + i + ".txt"));
            } else {
                testXMLList.add(Paths.get(TEST_PATH + "/" + N + "Set/" + i + ".xml"));
            }
        }

        N = 10000;
        for (int i = 1; i <= N; i++) {
            if (i % 10 == 0) {
                testTXTList.add(Paths.get(TEST_PATH + "/" + N + "Set/" + i + ".txt"));
            } else {
                testXMLList.add(Paths.get(TEST_PATH + "/" + N + "Set/" + i + ".xml"));
            }
        }


        N = 1000;
        for (int i = 1; i <= 10; i++) {
            for (int j = 1; j <= 10; j++) {
                for (int k = 1; k <= 10; k++) {
                    if (k % 3 == 0) {
                        testTXTList.add(Paths.get(TEST_PATH + "/" + N + "Set/" + i + "/" + j + "A/" + k + ".txt"));
                        testTXTDirList.add(Paths.get(TEST_PATH + "/" + N + "Set/" + i + "/" + j + "A/" + k + ".txt"));
                    } else {
                        testXMLList.add(Paths.get(TEST_PATH + "/" + N + "Set/" + i + "/" + j + "A/" + k + ".xml"));
                    }
                }
            }
        }

        assertTrue(CollectionUtils.isEqualCollection(XMLFileList, testXMLList));
        assertTrue(CollectionUtils.isEqualCollection(TXTFileList, testTXTList));
        assertTrue(CollectionUtils.isEqualCollection(TXTDirList, testTXTDirList));

    }

    /**
     * Assure the list contains the files which we wanted
     * Sometimes a file may created in a certain path but the event may be called from a different path
     * This assures that it will not happen for @Test method testOverflow_FileTree_FileType()
     *
     * @param XMLFileList1 XML file list 1 with regex pattern
     * @param XMLFileList2 XML file list 2  with glob pattern
     * @param TXTDirList   TXT file list with directory structure with regex pattern
     */
    @SuppressWarnings("WeakerAccess")
    public void assureFiles2(Set<Path> XMLFileList1, Set<Path> XMLFileList2, Set<Path> TXTDirList) {
        Set<Path> testXMLList1 = new HashSet<>();
        Set<Path> testXMLList2 = new HashSet<>();
        Set<Path> testTXTDirList = new HashSet<>();

        int N = 25000;
        for (int i = 1; i <= N; i++) {
            if (i % 10 != 0) {
                testXMLList1.add(Paths.get(TEST_PATH + "/" + N + "Set/" + i + ".xml"));
                testXMLList2.add(Paths.get(TEST_PATH + "/" + N + "Set/" + i + ".xml"));
            }
        }

        N = 10000;
        for (int i = 1; i <= N; i++) {
            if (i % 10 != 0) {
                testXMLList1.add(Paths.get(TEST_PATH + "/" + N + "Set/" + i + ".xml"));
                testXMLList2.add(Paths.get(TEST_PATH + "/" + N + "Set/" + i + ".xml"));
            }
        }


        N = 1000;
        for (int i = 1; i <= 10; i++) {
            for (int j = 1; j <= 10; j++) {
                for (int k = 1; k <= 10; k++) {
                    int t = (i - 1) * 100 + (j - 1) * 10 + k;
                    if (k % 3 == 0) {
                        testTXTDirList.add(Paths.get(TEST_PATH + "/" + N + "Set/" + i + "/" + j + "A/" + t + ".txt"));
                    } else {
                        testXMLList2.add(Paths.get(TEST_PATH + "/" + N + "Set/" + i + "/" + j + "A/" + t + ".xml"));
                    }
                }
            }
        }

        assertTrue(CollectionUtils.isEqualCollection(XMLFileList1, testXMLList1));
        assertTrue(CollectionUtils.isEqualCollection(XMLFileList2, testXMLList2));
        assertTrue(CollectionUtils.isEqualCollection(TXTDirList, testTXTDirList));
    }
}
