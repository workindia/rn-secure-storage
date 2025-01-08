package com.taluttasgiran.rnsecurestorage.legacy;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public final class Storage {
    public static byte[] readValues(Context context, String filename) throws IOException {
        FileInputStream fis = context.openFileInput(filename);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        byte[] buffer = new byte[1024];
        int bytesRead = fis.read(buffer);
        while(bytesRead != -1) {
            baos.write(buffer, 0, bytesRead);
            bytesRead = fis.read(buffer);
        }
        return baos.toByteArray();
    }

    public static boolean exists(Context context, String filename) throws IOException {
        File file = context.getFileStreamPath(filename);
        return file != null && file.exists();
    }
}