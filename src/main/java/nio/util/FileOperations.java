/*
 * AdroitLogic UltraESB Enterprise Service Bus
 *
 * Copyright (c) 2010-2015 AdroitLogic Private Ltd. (http://adroitlogic.org). All Rights Reserved.
 *
 * GNU Affero General Public License Usage
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program (See LICENSE-AGPL.TXT).
 * If not, see http://www.gnu.org/licenses/agpl-3.0.html
 *
 * Commercial Usage
 *
 * Licensees holding valid UltraESB Commercial licenses may use this file in accordance with the UltraESB Commercial
 * License Agreement provided with the Software or, alternatively, in accordance with the terms contained in a written
 * agreement between you and AdroitLogic.
 *
 * If you are unsure which license is appropriate for your use, or have questions regarding the use of this file,
 * please contact AdroitLogic at info@adroitlogic.com
 */

package nio.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;


/**
 * Utility methods for tests
 *
 * @author Janaka
 */
public class FileOperations {

    /**
     * Length of a time step, in ms, when waiting for a condition
     */
    private static final long WAIT_STEP = 100;

    /**
     * Creates a set of subdirectories indicated by <code>paths</code> inside <code>rootDir</code>
     *
     * @param rootDir directory inside which to create subdirectories
     * @param paths   list of subdirectory pathnames to be created, relative to <code>rootDir</code>
     */
    public static void createDirs(Path rootDir, String... paths) throws IOException {
        for (String path : paths) {
            Path subdir = rootDir.resolve(path);
            Files.createDirectories(subdir);
            cleanDir(subdir, false);
        }
    }

    /**
     * Creates a set of files indicated by <code>paths</code> inside <code>rootDir</code>, containing the respective
     * relative pathnames as their content
     *
     * @param rootDir directory inside which to create files
     * @param paths   list of file pathnames to be created, relative to <code>rootDir</code>
     */
    public static void createFiles(Path rootDir, String... paths) throws IOException {
        // create file and write into it its own path, as content
        for (String path : paths) {
            Path file = rootDir.resolve(path);
            String filename = file.toString();
            createAndWrite(file, ByteBuffer.allocate(filename.length()).put(filename.getBytes()));
        }
    }

    /**
     * Creates the file indicated by <code>file</code> with <code>content</code> as its content
     *
     * @param file    pathname for the file
     * @param content initial content of the file
     */
    public static void createFile(String file, String content) throws Exception {
        createFile(Paths.get(file), content);
    }

    /**
     * Creates the file indicated by <code>file</code> with <code>content</code> as its content
     *
     * @param file    pathname for the file
     * @param content initial content of the file
     */
    private static void createFile(Path file, String content) throws IOException {
        createAndWrite(file, (ByteBuffer) ByteBuffer.allocate(content.length()).put(content.getBytes()).rewind());
    }

    /**
     * Waits for a maximum of <code>millis</code> ms for <code>dir</code> to get emptied. Fails if the directory is not
     * empty after the wait.
     *
     * @param dir    directory expected to be empty
     * @param millis maximum wait time (in ms)
     */
    public static void waitTillEmpty(Path dir, long millis) throws IOException, InterruptedException {
        boolean dirEmpty = false;
        for (long i = 0; i < millis; i += WAIT_STEP) {
            dirEmpty = dir.toFile().listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return true;
                }
            }).length == 0;
            if (dirEmpty) {
                break;
            }
            Thread.sleep(WAIT_STEP);
        }
        assertTrue(dir + " is not empty after " + millis + " ms", dirEmpty);
    }

    /**
     * Deletes all content of a directory (and optionally the directory itself)
     *
     * @param rootDir    directory to be emptied
     * @param deleteRoot whether to delete the root directory (<code>rootDir</code>) as well
     * @return number of entities deleted
     */
    public static int cleanDir(final Path rootDir, final boolean deleteRoot) throws IOException {
        final AtomicInteger count = new AtomicInteger();
        Files.walkFileTree(rootDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                count.incrementAndGet();
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) exc.printStackTrace(); //todo remove
                if (!rootDir.equals(dir) || deleteRoot) {
                    Files.delete(dir);
                    count.incrementAndGet();
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return count.intValue();
    }

    /**
     * Deletes all content of a directory (and optionally the directory itself)
     *
     * @param rootDir    directory to be emptied
     * @param deleteRoot whether to delete the root directory (<code>rootDir</code>) as well
     * @return number of entities deleted
     */
    public static int cleanDir(String rootDir, final boolean deleteRoot) throws IOException {
        return cleanDir(Paths.get(rootDir), deleteRoot);
    }

    /**
     * Asserts the existence or nonexistence of a set of files
     *
     * @param shouldExist <code>true</code> if the files should exist, <code>false</code> otherwise
     * @param rootDir     root directory to be checked
     * @param files       list of file pathnames to be checked
     */
    public static void assertFileExistence(boolean shouldExist, Path rootDir, String... files) throws IOException {
        for (String file : files) {
            assertEquals(shouldExist, Files.exists(rootDir.resolve(file)));
        }
    }

    /**
     * Creates the file indicated by <code>file</code> with <code>content</code> as its content
     *
     * @param file    pathname for the file
     * @param content a {@link ByteBuffer} containing the initial content of the file
     */
    private static void createAndWrite(Path file, ByteBuffer content) throws IOException {
        try {
            Files.createFile(file);
            ByteChannel channel = Files.newByteChannel(file, StandardOpenOption.WRITE);
            content.rewind();
            channel.write(content);
            channel.close();
        } catch (IOException e) {
            throw e;
        }
    }

    /**
     * Checks for the existence of a file <code>name</code> inside <code>dir</code> with content <code>content</code>
     *
     * @param dir     parent directory for the file
     * @param name    name of the file
     * @param content expected content of the file
     * @return <code>true</code> if the a file is found with the expected content, <code>false</code> otherwise
     */
    private static boolean doesFileExistWithContent(String dir, String name, String content) throws IOException {
        for (String file : new File(dir).list()) {
            if (file.endsWith(name)) {
                return content.equals(readFileAsString(dir + File.separator + file));
            }
        }
        return false;
    }

    /**
     * Reads and returns the content of <code>file</code> as a String
     *
     * @param file pathname of the file to be read
     * @return content of the file, as a String
     */
    private static String readFileAsString(String file) throws IOException {
        return readFileAsString(Paths.get(file));
    }

    /**
     * Reads and returns the content of <code>file</code> as a String
     *
     * @param file the file to be read
     * @return content of the file, as a String
     */
    private static String readFileAsString(Path file) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate((int) Files.size(file));
        ByteChannel channel = Files.newByteChannel(file, StandardOpenOption.READ);
        channel.read(buffer);
        channel.close();

        byte[] text = new byte[buffer.limit()];
        buffer.rewind();
        buffer.get(text);
        return new String(text);
    }
}
