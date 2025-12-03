package com.nokia.miaf.gallery;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class MotionPhotoComposer {

    private static final String TAG = "MotionPhotoComposer";

    /**
     * 合成 Motion Photo
     *
     * @param imageData HEIC 或 JPEG 图片数据
     * @param videoData MP4 视频数据
     * @return Motion Photo 数据，失败返回 null
     */
    public byte[] composeMotionPhoto(byte[] imageData, byte[] videoData) {
        // 验证输入
        if (imageData == null || imageData.length == 0) {
            logError("Image data is null or empty");
            return null;
        }
        if (videoData == null || videoData.length == 0) {
            logError("Video data is null or empty");
            return null;
        }

        // 验证视频格式 (必须是 MP4)
        if (!ISOBMFFParser.isMP4(videoData)) {
            logError("Video is not MP4 format");
            return null;
        }

        // 根据图片格式选择处理方式
        if (ISOBMFFParser.isHEIC(imageData)) {
            return composeHEICMotionPhoto(imageData, videoData);
        } else if (ISOBMFFParser.isJPEG(imageData)) {
            return composeJPEGMotionPhoto(imageData, videoData);
        } else {
            logError("Unsupported image format");
            return null;
        }
    }

    /**
     * 从文件合成 Motion Photo
     */
    public boolean composeMotionPhotoToFile(String imagePath, String videoPath, String outputPath) {
        try {
            byte[] imageData = Files.readAllBytes(Paths.get(imagePath));
            byte[] videoData = Files.readAllBytes(Paths.get(videoPath));

            byte[] result = composeMotionPhoto(imageData, videoData);
            if (result == null) {
                return false;
            }

            Files.write(Paths.get(outputPath), result);
            return true;

        } catch (IOException e) {
            logError("Failed to compose Motion Photo: " + e.getMessage());
            return false;
        }
    }

    /**
     * 合成 HEIC Motion Photo
     * <p>
     * 结构: 保持原 HEIC 结构 + 追加 mpvd box + 注入 XMP 元数据
     * ftyp + meta + mdat + mpvd + xml (XMP)
     */
    private byte[] composeHEICMotionPhoto(byte[] imageData, byte[] videoData) {
        logInfo("Building HEIC Motion Photo (Google spec compliant)...");

        // 解析 HEIC 结构
        List<Box> boxes = ISOBMFFParser.parseTopLevelBoxes(imageData);
        if (boxes.isEmpty()) {
            logError("Failed to parse HEIC structure");
            return null;
        }

        // 验证必要的 boxes
        boolean hasFtyp = false;
        for (Box box : boxes) {
            if ("ftyp".equals(box.type)) {
                hasFtyp = true;
                break;
            }
        }
        if (!hasFtyp) {
            logError("Missing ftyp box in HEIC");
            return null;
        }

        // 创建 mpvd box
        byte[] mpvdBox = createMpvdBox(videoData);

        // 计算总大小
        int totalSize = imageData.length + mpvdBox.length;

        // 构建结果
        ByteArrayOutputStream output = new ByteArrayOutputStream(totalSize);
        try {
            // 1. 复制原始 HEIC 数据
            output.write(imageData);
            logInfo("Added original HEIC data (" + imageData.length + " bytes)");

            // 2. 追加 mpvd box
            output.write(mpvdBox);
            logInfo("Added mpvd box (" + mpvdBox.length + " bytes)");

        } catch (IOException e) {
            logError("Failed to write data: " + e.getMessage());
            return null;
        }

        byte[] result = output.toByteArray();
        
        // 3. 注入 XMP 元数据 (Google Motion Photo 规范要求)
        logInfo("Injecting Motion Photo XMP metadata...");
        HEIFXmpInjector xmpInjector = new HEIFXmpInjector();
        result = xmpInjector.injectMotionPhotoXmp(result, videoData.length, "image/heic");
        
        // 验证 XMP 注入
        if (xmpInjector.verifyXmpInjection(result)) {
            logInfo("✓ XMP metadata injected successfully");
        } else {
            logInfo("⚠ XMP injection may have failed (file still valid without XMP)");
        }
        
        logInfo("Motion Photo composed successfully. Total: " + result.length + " bytes");
        logInfo("Structure: ftyp + meta + mdat + mpvd + xml (Google spec compliant)");

        return result;
    }

    /**
     * 合成 JPEG Motion Photo
     * <p>
     * 结构: JPEG 数据 + 视频数据
     * 注意: JPEG Motion Photo 通常使用 XMP 元数据标记视频位置
     */
    private byte[] composeJPEGMotionPhoto(byte[] jpegData, byte[] videoData) {
        logInfo("Building JPEG Motion Photo...");

        // 简单追加方式
        int totalSize = jpegData.length + videoData.length;

        ByteArrayOutputStream output = new ByteArrayOutputStream(totalSize);
        try {
            output.write(jpegData);
            output.write(videoData);
        } catch (IOException e) {
            logError("Failed to write data: " + e.getMessage());
            return null;
        }

        logInfo("JPEG Motion Photo composed. Total: " + output.size() + " bytes");
        return output.toByteArray();
    }

    /**
     * 创建 mpvd box
     * <p>
     * 结构:
     * ├── size: 4 bytes (big-endian)
     * ├── type: "mpvd" (4 bytes)
     * └── data: 完整的 MP4 视频
     * <p>
     * 如果视频大于 4GB-8，使用扩展大小格式:
     * ├── size: 4 bytes (值为 1)
     * ├── type: "mpvd" (4 bytes)
     * ├── extended_size: 8 bytes (big-endian)
     * └── data: 视频数据
     */
    public byte[] createMpvdBox(byte[] videoData) {
        boolean useExtendedSize = videoData.length > (Integer.MAX_VALUE - 8);

        int headerSize = useExtendedSize ? 16 : 8;
        int totalSize = headerSize + videoData.length;

        ByteBuffer box = ByteBuffer.allocate(totalSize);
        box.order(ByteOrder.BIG_ENDIAN);

        if (useExtendedSize) {
            box.putInt(1);  // size = 1 表示使用扩展大小
            box.put("mpvd".getBytes(StandardCharsets.US_ASCII));
            box.putLong(16L + videoData.length);  // 扩展大小
        } else {
            box.putInt(8 + videoData.length);
            box.put("mpvd".getBytes(StandardCharsets.US_ASCII));
        }

        box.put(videoData);

        return box.array();
    }

    /**
     * 流式合成 Motion Photo (用于大文件)
     */
    public boolean composeMotionPhotoStreaming(
            InputStream imageStream,
            long imageSize,
            InputStream videoStream,
            long videoSize,
            OutputStream outputStream) throws IOException {

        // 1. 复制图片数据
        copyStream(imageStream, outputStream, imageSize);

        // 2. 写入 mpvd header
        writeMpvdHeader(outputStream, videoSize);

        // 3. 复制视频数据
        copyStream(videoStream, outputStream, videoSize);

        return true;
    }

    private void writeMpvdHeader(OutputStream output, long videoSize) throws IOException {
        boolean useExtendedSize = videoSize > (Integer.MAX_VALUE - 8);

        ByteBuffer header;
        if (useExtendedSize) {
            header = ByteBuffer.allocate(16);
            header.order(ByteOrder.BIG_ENDIAN);
            header.putInt(1);
            header.put("mpvd".getBytes(StandardCharsets.US_ASCII));
            header.putLong(16 + videoSize);
        } else {
            header = ByteBuffer.allocate(8);
            header.order(ByteOrder.BIG_ENDIAN);
            header.putInt((int) (8 + videoSize));
            header.put("mpvd".getBytes(StandardCharsets.US_ASCII));
        }

        output.write(header.array());
    }

    private void copyStream(InputStream input, OutputStream output, long length) throws IOException {
        byte[] buffer = new byte[8192];
        long remaining = length;

        while (remaining > 0) {
            int toRead = (int) Math.min(buffer.length, remaining);
            int read = input.read(buffer, 0, toRead);
            if (read == -1) break;
            output.write(buffer, 0, read);
            remaining -= read;
        }
    }

    private void logInfo(String message) {
        System.out.println(TAG + ": " + message);
    }

    private void logError(String message) {
        System.err.println(TAG + ": " + message);
    }
}
