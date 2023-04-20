package com.tinymq.core.utils;

import cn.hutool.core.lang.Assert;

import java.io.File;
import java.util.zip.CRC32;

public class UtilsAll {

    public static String getFileNameFromOffset(long offset) {
        int length = 20;

        StringBuilder sb = new StringBuilder();
        sb.append(offset);
        while(sb.length() < length) {
            sb.insert(0, 0);
        }
        return sb.toString();
    }

    public static String concatFileName(String storePath, int fileStartOffset) {
        Assert.notEmpty(storePath);
        return storePath + File.separator + fileStartOffset;
    }
    private static final ThreadLocal<CRC32> localCrc32 = new ThreadLocal<>();
    public static int crc32(final byte[] bytes) {
        if(localCrc32.get() == null) {
            localCrc32.set(new CRC32());
        }
        CRC32 local = localCrc32.get();
        local.update(bytes, 0, bytes.length);
        return (int) local.getValue();
    }

    public static String splitFileName(String filename) {
        Assert.notEmpty(filename);
        return filename.substring(filename.lastIndexOf('/') + 1);
    }
}
