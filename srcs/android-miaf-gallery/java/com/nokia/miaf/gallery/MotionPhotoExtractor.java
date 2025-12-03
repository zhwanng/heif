package com.nokia.miaf.gallery;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class MotionPhotoExtractor {

    private static final String TAG = "MotionPhotoExtractor";

    /**
     * 检测是否为 Motion Photo
     */
    public static boolean isMotionPhoto(byte[] data) {
        if (data == null || data.length < 100) {
            return false;
        }

        // 方法1: 检查 mpvd box
        if (hasMpvdBox(data)) {
            return true;
        }

        // 方法2: 在文件后半部分搜索视频签名
        return hasEmbeddedVideoSignature(data);
    }

    /**
     * 检测文件是否为 Motion Photo
     */
    public static boolean isMotionPhotoFile(String path) {
        try {
            byte[] data = Files.readAllBytes(Paths.get(path));
            return isMotionPhoto(data);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 检查是否有 mpvd box
     */
    public static boolean hasMpvdBox(byte[] data) {
        List<Box> boxes = ISOBMFFParser.parseTopLevelBoxes(data);
        for (Box box : boxes) {
            if ("mpvd".equals(box.type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取视频偏移量
     */
    public static long getVideoOffset(byte[] data) {
        // 方法1: 从 mpvd box 获取
        List<Box> boxes = ISOBMFFParser.parseTopLevelBoxes(data);
        for (Box box : boxes) {
            if ("mpvd".equals(box.type)) {
                return box.getPayloadOffset();
            }
        }

        // 方法2: 搜索视频签名
        return findVideoOffsetBySignature(data);
    }

    /**
     * 获取视频长度
     */
    public static long getVideoLength(byte[] data) {
        List<Box> boxes = ISOBMFFParser.parseTopLevelBoxes(data);
        for (Box box : boxes) {
            if ("mpvd".equals(box.type)) {
                return box.getPayloadSize();
            }
        }

        long offset = findVideoOffsetBySignature(data);
        if (offset > 0) {
            return data.length - offset;
        }

        return 0;
    }

    /**
     * 提取视频数据
     */
    public static byte[] extractVideo(byte[] data) {
        List<Box> boxes = ISOBMFFParser.parseTopLevelBoxes(data);

        for (Box box : boxes) {
            if ("mpvd".equals(box.type)) {
                int videoOffset = (int) box.getPayloadOffset();
                int videoLength = (int) box.getPayloadSize();

                if (videoOffset + videoLength <= data.length) {
                    byte[] videoData = new byte[videoLength];
                    System.arraycopy(data, videoOffset, videoData, 0, videoLength);
                    return videoData;
                }
            }
        }

        // 备用方法: 通过签名查找
        long offset = findVideoOffsetBySignature(data);
        if (offset > 0 && offset < data.length) {
            int videoLength = (int) (data.length - offset);
            byte[] videoData = new byte[videoLength];
            System.arraycopy(data, (int) offset, videoData, 0, videoLength);
            return videoData;
        }

        return null;
    }

    /**
     * 提取静态图像 (不含视频)
     */
    public static byte[] extractImage(byte[] data) {
        List<Box> boxes = ISOBMFFParser.parseTopLevelBoxes(data);

        long imageEndOffset = 0;
        for (Box box : boxes) {
            if ("mpvd".equals(box.type) || "uuid".equals(box.type)) {
                break;  // Motion Photo 扩展部分开始
            }
            imageEndOffset = box.offset + box.size;
        }

        if (imageEndOffset > 0 && imageEndOffset <= data.length) {
            byte[] imageData = new byte[(int) imageEndOffset];
            System.arraycopy(data, 0, imageData, 0, (int) imageEndOffset);
            return imageData;
        }

        return null;
    }

    /**
     * 搜索嵌入的视频签名
     */
    private static boolean hasEmbeddedVideoSignature(byte[] data) {
        return findVideoOffsetBySignature(data) > 0;
    }

    /**
     * 通过视频签名查找视频偏移
     */
    private static long findVideoOffsetBySignature(byte[] data) {
        // 视频 ftyp 签名
        String[] signatures = {
                "ftypisom", "ftypiso2", "ftypmp41", "ftypmp42",
                "ftypavc1", "ftyphvc1"
        };

        // 从文件后半部分开始搜索
        int searchStart = Math.max(50 * 1024, data.length / 2);

        for (String sig : signatures) {
            byte[] sigBytes = sig.getBytes(StandardCharsets.US_ASCII);
            int index = indexOf(data, sigBytes, searchStart);
            if (index != -1) {
                // ftyp box 开始于签名前 4 字节 (size 字段)
                return index - 4;
            }
        }

        return -1;
    }

    /**
     * 在字节数组中搜索子数组
     */
    private static int indexOf(byte[] data, byte[] pattern, int startFrom) {
        outer:
        for (int i = startFrom; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    /**
     * 分析 Motion Photo 结构
     */
    public static void logMotionPhotoStructure(byte[] data) {
        System.out.println("╔══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║          MOTION PHOTO STRUCTURE ANALYSIS                              ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════╣");
        System.out.println("║ Total file size: " + data.length + " bytes");

        List<Box> boxes = ISOBMFFParser.parseTopLevelBoxes(data);
        System.out.println("║ Found " + boxes.size() + " top-level boxes:");

        for (Box box : boxes) {
            System.out.printf("║   ├── '%s': offset=%d, size=%d%n",
                    box.type, box.offset, box.size);
        }

        System.out.println("║");
        System.out.println("║ Is Motion Photo: " + (isMotionPhoto(data) ? "YES ✓" : "NO ✗"));
        System.out.println("║ Has mpvd box: " + (hasMpvdBox(data) ? "YES ✓" : "NO ✗"));
        System.out.println("╚══════════════════════════════════════════════════════════════════════╝");
    }
}
