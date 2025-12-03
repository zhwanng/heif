package com.nokia.miaf.gallery;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ISOBMFFParser {

    /**
     * 解析顶层 boxes
     *
     * Box 结构:
     * ├── size: 4 bytes (big-endian), 如果 = 1 则使用扩展大小
     * ├── type: 4 bytes ASCII
     * ├── extended_size: 8 bytes (仅当 size = 1)
     * └── data: size - header_size bytes
     */
    public static List<Box> parseTopLevelBoxes(byte[] data) {
        List<Box> boxes = new ArrayList<>();
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.BIG_ENDIAN);

        long offset = 0;
        while (offset + 8 <= data.length) {
            buffer.position((int) offset);

            // 读取 size
            int size32 = buffer.getInt();

            // 读取 type
            byte[] typeBytes = new byte[4];
            buffer.get(typeBytes);
            String type = new String(typeBytes, StandardCharsets.US_ASCII);

            long size;
            int headerSize = 8;

            if (size32 == 1) {
                // 扩展大小
                size = buffer.getLong();
                headerSize = 16;
            } else if (size32 == 0) {
                // 延伸到文件末尾
                size = data.length - offset;
            } else {
                size = size32;
            }

            // 验证 box 大小
            if (size < headerSize || offset + size > data.length) {
                break;
            }

            Box box = new Box(type, offset, size, headerSize);
            boxes.add(box);

            offset += size;
        }

        return boxes;
    }

    /**
     * 查找指定类型的 box
     */
    public static Box findBox(byte[] data, String type) {
        List<Box> boxes = parseTopLevelBoxes(data);
        for (Box box : boxes) {
            if (type.equals(box.type)) {
                return box;
            }
        }
        return null;
    }

    /**
     * 检查是否为有效的 ISOBMFF 格式
     */
    public static boolean isValidISOBMFF(byte[] data) {
        if (data == null || data.length < 12) {
            return false;
        }

        ByteBuffer buffer = ByteBuffer.wrap(data, 4, 4);
        byte[] typeBytes = new byte[4];
        buffer.get(typeBytes);
        String type = new String(typeBytes, StandardCharsets.US_ASCII);

        return "ftyp".equals(type);
    }

    /**
     * 检查是否为 HEIC 格式
     */
    public static boolean isHEIC(byte[] data) {
        if (!isValidISOBMFF(data)) {
            return false;
        }

        ByteBuffer buffer = ByteBuffer.wrap(data, 8, 4);
        byte[] brandBytes = new byte[4];
        buffer.get(brandBytes);
        String brand = new String(brandBytes, StandardCharsets.US_ASCII);

        // HEIC brands
        Set<String> heicBrands = new HashSet<>(Arrays.asList(
                "heic", "heix", "hevc", "hevx", "mif1", "msf1"
        ));

        if (heicBrands.contains(brand)) {
            return true;
        }

        // 检查兼容 brands
        Box ftypBox = findBox(data, "ftyp");
        if (ftypBox != null && ftypBox.size > 16) {
            int offset = 16;
            while (offset + 4 <= ftypBox.size) {
                buffer = ByteBuffer.wrap(data, (int) ftypBox.offset + offset, 4);
                byte[] compatBrand = new byte[4];
                buffer.get(compatBrand);
                String compat = new String(compatBrand, StandardCharsets.US_ASCII);
                if (heicBrands.contains(compat)) {
                    return true;
                }
                offset += 4;
            }
        }

        return false;
    }

    /**
     * 检查是否为 JPEG 格式
     */
    public static boolean isJPEG(byte[] data) {
        if (data == null || data.length < 4) {
            return false;
        }
        // JPEG 以 FF D8 FF 开头
        return (data[0] & 0xFF) == 0xFF &&
                (data[1] & 0xFF) == 0xD8 &&
                (data[2] & 0xFF) == 0xFF;
    }

    /**
     * 检查是否为 MP4 格式
     */
    public static boolean isMP4(byte[] data) {
        if (!isValidISOBMFF(data)) {
            return false;
        }

        ByteBuffer buffer = ByteBuffer.wrap(data, 8, 4);
        byte[] brandBytes = new byte[4];
        buffer.get(brandBytes);
        String brand = new String(brandBytes, StandardCharsets.US_ASCII);

        // MP4 brands
        Set<String> mp4Brands = new HashSet<>(Arrays.asList(
                "isom", "iso2", "iso3", "iso4", "iso5", "iso6",
                "mp41", "mp42", "mp71", "avc1", "hvc1", "hev1"
        ));

        return mp4Brands.contains(brand);
    }

    /**
     * 检查是否为 QuickTime MOV 格式
     */
    public static boolean isQuickTime(byte[] data) {
        if (!isValidISOBMFF(data)) {
            // 可能是旧版 MOV 格式 (moov 在开头)
            if (data != null && data.length >= 8) {
                ByteBuffer buffer = ByteBuffer.wrap(data, 4, 4);
                byte[] typeBytes = new byte[4];
                buffer.get(typeBytes);
                String type = new String(typeBytes, StandardCharsets.US_ASCII);
                return "moov".equals(type) || "mdat".equals(type) || "wide".equals(type);
            }
            return false;
        }

        ByteBuffer buffer = ByteBuffer.wrap(data, 8, 4);
        byte[] brandBytes = new byte[4];
        buffer.get(brandBytes);
        String brand = new String(brandBytes, StandardCharsets.US_ASCII);

        return "qt  ".equals(brand) || "M4V ".equals(brand);
    }
}
