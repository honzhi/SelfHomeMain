package com.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZIP {
    public static void zipFolder(String folderPath, String zipFilePath) throws IOException {
        FileOutputStream fos = null;
        ZipOutputStream zos = null;

        try {
            fos = new FileOutputStream(zipFilePath);
            zos = new ZipOutputStream(fos);
            addFolderToZip("", new File(folderPath), zos);
        } finally {
            if (zos != null) {
                zos.close();
            }

            if (fos != null) {
                fos.close();
            }
        }
    }

    private static void addFolderToZip(String parentPath, File folder, ZipOutputStream zos) throws FileNotFoundException, IOException {
        File[] var6;
        int var5 = (var6 = folder.listFiles()).length;

        for (int var4 = 0; var4 < var5; var4++) {
            File file = var6[var4];
            if (file.isDirectory()) {
                addFolderToZip(parentPath + folder.getName() + "/", file, zos);
            } else {
                FileInputStream fis = null;

                try {
                    fis = new FileInputStream(file);
                    ZipEntry zipEntry = new ZipEntry(parentPath + folder.getName() + "/" + file.getName());
                    zos.putNextEntry(zipEntry);
                    byte[] bytes = new byte[1024];

                    int length;
                    while ((length = fis.read(bytes)) >= 0) {
                        zos.write(bytes, 0, length);
                    }
                } finally {
                    if (fis != null) {
                        fis.close();
                    }
                }
            }
        }
    }
}
