/*
 *  Copyright 2020 Johnny850807 (Waterball) 潘冠辰
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package tw.waterball.judgegirl.commons.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import tw.waterball.judgegirl.commons.models.files.StreamingResource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static java.util.Objects.requireNonNull;

/**
 * TODO Test Coverage and make it cleaner
 *
 * @author - johnny850807@gmail.com (Waterball)
 */
public class ZipUtils {
    public static ByteArrayInputStream zipClassPathResourcesToStream(String... resourcePaths) {
        return new ByteArrayInputStream(zipFilesFromResources(resourcePaths));
    }

    public static byte[] zipFilesFromResources(String... resourcePaths) {
        return zip(
                Arrays.stream(resourcePaths)
                        .map(path -> new StreamingResource(PathUtils.getFileName(path),
                                ResourceUtils.getResourceAsStream(path)))
                        .collect(Collectors.toList()));
    }

    public static <T extends StreamingResource> byte[] zip(List<T> streamingResources) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zipos = new ZipOutputStream(baos)) {
            for (StreamingResource streamingResource : streamingResources) {
                writeFileAsZipEntry(streamingResource.getFileName(), zipos,
                        streamingResource.getInputStream());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }


    public static byte[] zip(StreamingResource... streamingResources) {
        return zip(Arrays.asList(streamingResources));
    }

    public static ByteArrayInputStream zipToStream(StreamingResource... multipartFiles) {
        return new ByteArrayInputStream(zip(multipartFiles));
    }

    public static <T extends StreamingResource> ByteArrayInputStream zipToStream(List<T> multipartFiles) {
        return new ByteArrayInputStream(zip(multipartFiles));
    }

    public static byte[] zip(String fileName, InputStream in) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zipos = new ZipOutputStream(baos)) {
            writeFileAsZipEntry(fileName, zipos, in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    public static byte[] zip(String fileName, String str) {
        return zip(fileName, new ByteArrayInputStream(str.getBytes()));
    }

    public static ByteArrayInputStream zipToStream(String fileName, String str) {
        return new ByteArrayInputStream(zip(fileName, str));
    }

    @SuppressWarnings("RedundantIfStatement")
    public static void unzipToDestination(InputStream in,
                                          Path destinationPath) throws IOException {
        try (ZipInputStream zin = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                Path path = destinationPath.resolve(entry.getName());
                FileUtils.forceMkdir(path.getParent().toFile());
                if (entry.isDirectory()) {
                    Files.createDirectory(path);
                } else {
                    try (FileOutputStream out = new FileOutputStream(path.toFile())) {
                        IOUtils.copy(zin, out);
                    }
                }
                zin.closeEntry();
            }
        }
    }


    /**
     * @return the raw data of the first file in the zip
     */
    public static byte[] unzipFirst(InputStream in) throws IOException {
        return unzip(in).get(0);
    }

    public static List<byte[]> unzip(InputStream in) throws IOException {
        try (ZipInputStream zin = new ZipInputStream(in)) {
            List<byte[]> result = new ArrayList<>();
            while (zin.getNextEntry() != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                IOUtils.copy(zin, baos);
                result.add(baos.toByteArray());
            }
            return result;
        }
    }

    public static void zipToFile(File file, FileOutputStream fos, String... ignoredFileNames) {
        zipToFile(new File[]{file}, fos, ignoredFileNames);
    }

    /**
     * Zip the given files recursively (i.e. including all sub-directories)
     * through the given FileOutputStream, except those whose file names are contained in
     * ignoredFileNames.
     */
    public static void zipToFile(File[] files, FileOutputStream fos, String... ignoredFileNames) {
        try (ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (File file : files) {
                if (!ArrayUtils.contains(ignoredFileNames, file.getName())) {
                    writeZipEntry(file, zos, ignoredFileNames);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void writeZipEntry(File file, ZipOutputStream zipos,
                                      String... ignoredFileNames) throws IOException {
        writeZipEntry("", file, zipos, ignoredFileNames);
    }

    private static void writeZipEntry(String path, File file,
                                      ZipOutputStream zipos, String... ignoredFileNames) throws IOException {
        if (!ArrayUtils.contains(ignoredFileNames, file.getName())) {
            if (file.isDirectory()) {
                for (String fileName : requireNonNull(file.list())) {
                    writeZipEntry(path + file.getName() + "/",
                            new File(file, fileName), zipos, ignoredFileNames);
                }
            } else {
                writeFileAsZipEntry(path + file.getName(), zipos,
                        new FileInputStream(file));
            }
        }
    }


    private static void writeFileAsZipEntry(String fileName,
                                            ZipOutputStream zipos, InputStream in) throws IOException {
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipos.putNextEntry(zipEntry);
        IOUtils.copy(in, zipos);
        in.close();
        zipos.closeEntry();
    }

}
