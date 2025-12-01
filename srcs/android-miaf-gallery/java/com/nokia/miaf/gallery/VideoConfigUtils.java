package com.nokia.miaf.gallery;

import android.util.Log;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class VideoConfigUtils {
    private final static String TAG = "VideoConfigUtils";

    /**
     * 从MP4文件中提取视频解码器配置(AVC/HEVC)
     */
    public static byte[] extractVideoConfigFromMP4(String mp4FilePath) throws Exception {
        FileInputStream fis = new FileInputStream(mp4FilePath);
        byte[] mp4Data = new byte[fis.available()];
        fis.read(mp4Data);
        fis.close();

        return extractVideoConfigFromMP4Data(mp4Data);
    }

    /**
     * 从MP4字节数据中提取视频解码器配置
     */
    public static byte[] extractVideoConfigFromMP4Data(byte[] mp4Data) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(mp4Data);
        buffer.order(ByteOrder.BIG_ENDIAN);

        int position = 0;

        // 遍历MP4文件查找moov box
        while (position < buffer.capacity() - 8) {
            // 检查是否有足够的数据读取box大小和类型
            if (position + 8 > buffer.capacity()) {
                break;
            }

            // 读取box大小和类型
            int boxSize = buffer.getInt(position);
            int boxType = buffer.getInt(position + 4);

            // 验证box大小的有效性
            if (boxSize < 8 || boxSize > buffer.capacity() - position) {
                // 如果box大小无效，尝试查找下一个有效的box
                position++;
                continue;
            }

            // 将类型转换为字符串
            String boxTypeName = fourCCToString(boxType);

            Log.e(TAG, "发现box: " + boxTypeName + ", 大小: " + boxSize);

            // 查找moov box
            if ("moov".equals(boxTypeName)) {
                return parseMoovBox(buffer, position, boxSize);
            }

            // 移动到下一个box
            if (boxSize == 0) {
                break; // 无效大小
            } else if (boxSize == 1) {
                // 64位大小
                if (position + 16 > buffer.capacity()) {
                    break;
                }
                long largeSize = buffer.getLong(position + 8);
                if (largeSize < 0 || largeSize > Integer.MAX_VALUE) {
                    position++;
                    continue;
                }
                position += (int) largeSize;
            } else {
                position += boxSize;
            }
        }

        throw new Exception("未在MP4文件中找到moov box");
    }

    /**
     * 解析moov box以提取视频配置
     */
    private static byte[] parseMoovBox(ByteBuffer buffer, int startPosition, int boxSize) throws Exception {
        int position = startPosition + 8; // 跳过box头
        int endPosition = startPosition + boxSize;

        // 遍历moov内的box
        while (position < endPosition - 8 && position >= 0) {
            // 检查边界
            if (position + 8 > buffer.capacity()) {
                break;
            }

            int boxSizeInner = buffer.getInt(position);
            int boxTypeInner = buffer.getInt(position + 4);

            // 验证box大小的有效性
            if (boxSizeInner < 8 || boxSizeInner > buffer.capacity() - position) {
                position++;
                continue;
            }

            String boxTypeNameInner = fourCCToString(boxTypeInner);

            Log.e(TAG, "  发现子box: " + boxTypeNameInner + ", 大小: " + boxSizeInner);

            // 查找trak box
            if ("trak".equals(boxTypeNameInner)) {
                byte[] config = parseTrakBox(buffer, position, boxSizeInner);
                if (config != null) {
                    return config;
                }
            }

            // 移动到下一个box
            if (boxSizeInner == 0) {
                break;
            } else if (boxSizeInner == 1) {
                // 64位大小
                if (position + 16 > buffer.capacity()) {
                    break;
                }
                long largeSize = buffer.getLong(position + 8);
                if (largeSize < 0 || largeSize > Integer.MAX_VALUE) {
                    position++;
                    continue;
                }
                position += (int) largeSize;
            } else {
                position += boxSizeInner;
            }
        }

        // 如果没有找到配置信息，返回null而不是抛出异常
        Log.e(TAG, "警告: 未在moov box中找到视频配置信息");
        return null;
    }

    /**
     * 解析trak box以提取视频配置
     */
    private static byte[] parseTrakBox(ByteBuffer buffer, int startPosition, int boxSize) throws Exception {
        int position = startPosition + 8; // 跳过box头
        int endPosition = startPosition + boxSize;

        // 查找mdia box
        while (position < endPosition - 8 && position >= 0) {
            // 检查边界
            if (position + 8 > buffer.capacity()) {
                break;
            }

            int boxSizeInner = buffer.getInt(position);
            int boxTypeInner = buffer.getInt(position + 4);

            // 验证box大小的有效性
            if (boxSizeInner < 8 || boxSizeInner > buffer.capacity() - position) {
                position++;
                continue;
            }

            String boxTypeNameInner = fourCCToString(boxTypeInner);

            if ("mdia".equals(boxTypeNameInner)) {
                return parseMdiaBox(buffer, position, boxSizeInner);
            }

            // 移动到下一个box
            if (boxSizeInner == 0) {
                break;
            } else if (boxSizeInner == 1) {
                // 64位大小
                if (position + 16 > buffer.capacity()) {
                    break;
                }
                long largeSize = buffer.getLong(position + 8);
                if (largeSize < 0 || largeSize > Integer.MAX_VALUE) {
                    position++;
                    continue;
                }
                position += (int) largeSize;
            } else {
                position += boxSizeInner;
            }
        }

        return null;
    }

    /**
     * 解析mdia box以提取视频配置
     */
    private static byte[] parseMdiaBox(ByteBuffer buffer, int startPosition, int boxSize) throws Exception {
        int position = startPosition + 8; // 跳过box头
        int endPosition = startPosition + boxSize;

        // 查找minf box
        while (position < endPosition - 8 && position >= 0) {
            // 检查边界
            if (position + 8 > buffer.capacity()) {
                break;
            }

            int boxSizeInner = buffer.getInt(position);
            int boxTypeInner = buffer.getInt(position + 4);

            // 验证box大小的有效性
            if (boxSizeInner < 8 || boxSizeInner > buffer.capacity() - position) {
                position++;
                continue;
            }

            String boxTypeNameInner = fourCCToString(boxTypeInner);

            if ("minf".equals(boxTypeNameInner)) {
                return parseMinfBox(buffer, position, boxSizeInner);
            }

            // 移动到下一个box
            if (boxSizeInner == 0) {
                break;
            } else if (boxSizeInner == 1) {
                // 64位大小
                if (position + 16 > buffer.capacity()) {
                    break;
                }
                long largeSize = buffer.getLong(position + 8);
                if (largeSize < 0 || largeSize > Integer.MAX_VALUE) {
                    position++;
                    continue;
                }
                position += (int) largeSize;
            } else {
                position += boxSizeInner;
            }
        }

        return null;
    }

    /**
     * 解析minf box以提取视频配置
     */
    private static byte[] parseMinfBox(ByteBuffer buffer, int startPosition, int boxSize) throws Exception {
        int position = startPosition + 8; // 跳过box头
        int endPosition = startPosition + boxSize;

        // 查找stbl box
        while (position < endPosition - 8 && position >= 0) {
            // 检查边界
            if (position + 8 > buffer.capacity()) {
                break;
            }

            int boxSizeInner = buffer.getInt(position);
            int boxTypeInner = buffer.getInt(position + 4);

            // 验证box大小的有效性
            if (boxSizeInner < 8 || boxSizeInner > buffer.capacity() - position) {
                position++;
                continue;
            }

            String boxTypeNameInner = fourCCToString(boxTypeInner);

            if ("stbl".equals(boxTypeNameInner)) {
                return parseStblBox(buffer, position, boxSizeInner);
            }

            // 移动到下一个box
            if (boxSizeInner == 0) {
                break;
            } else if (boxSizeInner == 1) {
                // 64位大小
                if (position + 16 > buffer.capacity()) {
                    break;
                }
                long largeSize = buffer.getLong(position + 8);
                if (largeSize < 0 || largeSize > Integer.MAX_VALUE) {
                    position++;
                    continue;
                }
                position += (int) largeSize;
            } else {
                position += boxSizeInner;
            }
        }

        return null;
    }

    /**
     * 解析stbl box以提取视频配置
     */
    private static byte[] parseStblBox(ByteBuffer buffer, int startPosition, int boxSize) throws Exception {
        int position = startPosition + 8; // 跳过box头
        int endPosition = startPosition + boxSize;

        // 查找stsd box
        while (position < endPosition - 8 && position >= 0) {
            // 检查边界
            if (position + 8 > buffer.capacity()) {
                break;
            }

            int boxSizeInner = buffer.getInt(position);
            int boxTypeInner = buffer.getInt(position + 4);

            // 验证box大小的有效性
            if (boxSizeInner < 8 || boxSizeInner > buffer.capacity() - position) {
                position++;
                continue;
            }

            String boxTypeNameInner = fourCCToString(boxTypeInner);

            if ("stsd".equals(boxTypeNameInner)) {
                return parseStsdBox(buffer, position, boxSizeInner);
            }

            // 移动到下一个box
            if (boxSizeInner == 0) {
                break;
            } else if (boxSizeInner == 1) {
                // 64位大小
                if (position + 16 > buffer.capacity()) {
                    break;
                }
                long largeSize = buffer.getLong(position + 8);
                if (largeSize < 0 || largeSize > Integer.MAX_VALUE) {
                    position++;
                    continue;
                }
                position += (int) largeSize;
            } else {
                position += boxSizeInner;
            }
        }

        return null;
    }

    /**
     * 解析stsd box以提取视频配置
     */
    private static byte[] parseStsdBox(ByteBuffer buffer, int startPosition, int boxSize) throws Exception {
        int position = startPosition + 8; // 跳过box头
        int endPosition = startPosition + boxSize;

        // 检查边界
        if (position + 8 > buffer.capacity()) {
            return null;
        }

        // 跳过版本和标志字段(4字节)
        position += 4;

        // 跳过entry count字段(4字节)
        position += 4;

        // 查找视频样本描述符
        while (position < endPosition - 8 && position >= 0) {
            // 检查边界
            if (position + 8 > buffer.capacity()) {
                break;
            }

            int boxSizeInner = buffer.getInt(position);
            int boxTypeInner = buffer.getInt(position + 4);

            // 验证box大小的有效性
            if (boxSizeInner < 8 || boxSizeInner > buffer.capacity() - position) {
                position++;
                continue;
            }

            String boxTypeNameInner = fourCCToString(boxTypeInner);

            Log.e(TAG, "    发现样本描述符: " + boxTypeNameInner + ", 大小: " + boxSizeInner);

            // 检查是否为AVC或HEVC配置
            if ("avc1".equals(boxTypeNameInner) || "hvc1".equals(boxTypeNameInner) ||
                    "hev1".equals(boxTypeNameInner) || "avc3".equals(boxTypeNameInner)) {
                return parseVideoSampleEntry(buffer, position, boxSizeInner);
            }

            // 移动到下一个box
            if (boxSizeInner == 0) {
                break;
            } else if (boxSizeInner == 1) {
                // 64位大小
                if (position + 16 > buffer.capacity()) {
                    break;
                }
                long largeSize = buffer.getLong(position + 8);
                if (largeSize < 0 || largeSize > Integer.MAX_VALUE) {
                    position++;
                    continue;
                }
                position += (int) largeSize;
            } else {
                position += boxSizeInner;
            }
        }

        return null;
    }

    /**
     * 解析视频样本条目以提取配置信息
     */
    private static byte[] parseVideoSampleEntry(ByteBuffer buffer, int startPosition, int boxSize) throws Exception {
        int position = startPosition + 8; // 跳过box头
        int endPosition = startPosition + boxSize;

        // 检查边界
        if (position + 70 > buffer.capacity()) {
            return null;
        }

        // 跳过视频样本条目的固定字段
        // (6 reserved bytes + 2 data reference index + 2 version + 2 revision + 4 vendor + 
        //  4 temporal quality + 4 spatial quality + 2 width + 2 height + 
        //  4 horiz resolution + 4 vert resolution + 4 data size + 2 frame count)
        position += 70;

        // 检查是否有足够的空间跳过压缩器名称(32字节)
        if (position + 32 > buffer.capacity()) {
            return null;
        }

        // 跳过压缩器名称(32字节)
        position += 32;

        // 检查是否有足够的空间跳过剩余字段
        if (position + 4 > buffer.capacity()) {
            return null;
        }

        // 跳过剩余字段(2 depth + 2 color table id)
        position += 4;

        // 查找配置box (avcC for AVC, hvcC for HEVC)
        while (position < endPosition - 8 && position >= 0) {
            // 检查边界
            if (position + 8 > buffer.capacity()) {
                break;
            }

            int boxSizeInner = buffer.getInt(position);
            int boxTypeInner = buffer.getInt(position + 4);

            // 验证box大小的有效性
            if (boxSizeInner < 8 || boxSizeInner > buffer.capacity() - position) {
                position++;
                continue;
            }

            String boxTypeNameInner = fourCCToString(boxTypeInner);

            Log.e(TAG, "      发现配置box: " + boxTypeNameInner + ", 大小: " + boxSizeInner);

            // 检查是否为有效的配置box
            if ("avcC".equals(boxTypeNameInner) && boxSizeInner > 0) {
                return parseAVCConfigBox(buffer, position, boxSizeInner);
            } else if ("hvcC".equals(boxTypeNameInner) && boxSizeInner > 0) {
                return parseHEVCConfigBox(buffer, position, boxSizeInner);
            }

            // 移动到下一个box
            if (boxSizeInner == 0) {
                break;
            } else if (boxSizeInner == 1) {
                // 64位大小
                if (position + 16 > buffer.capacity()) {
                    break;
                }
                long largeSize = buffer.getLong(position + 8);
                if (largeSize < 0 || largeSize > Integer.MAX_VALUE) {
                    position++;
                    continue;
                }
                position += (int) largeSize;
            } else {
                position += boxSizeInner;
            }
        }

        return null;
    }

    /**
     * 解析AVC配置box
     */
    private static byte[] parseAVCConfigBox(ByteBuffer buffer, int startPosition, int boxSize) {
        // 确保box大小合理
        if (boxSize <= 0 || boxSize > buffer.capacity() - startPosition) {
            Log.e(TAG, "AVC配置box大小无效: " + boxSize);
            return null;
        }

        // 提取整个avcC box的内容作为配置数据
        byte[] configData = new byte[boxSize];
        System.arraycopy(buffer.array(), startPosition, configData, 0, boxSize);

        Log.e(TAG, "成功提取AVC配置数据，大小: " + boxSize + " 字节");
        return configData;
    }

    /**
     * 解析HEVC配置box
     */
    private static byte[] parseHEVCConfigBox(ByteBuffer buffer, int startPosition, int boxSize) {
        // 确保box大小合理
        if (boxSize <= 0 || boxSize > buffer.capacity() - startPosition) {
            Log.e(TAG, "HEVC配置box大小无效: " + boxSize);
            return null;
        }

        // 提取整个hvcC box的内容作为配置数据
        byte[] configData = new byte[boxSize];
        System.arraycopy(buffer.array(), startPosition, configData, 0, boxSize);

        Log.e(TAG, "成功提取HEVC配置数据，大小: " + boxSize + " 字节");
        return configData;
    }

    /**
     * 将FourCC整数转换为字符串
     */
    private static String fourCCToString(int fourCC) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) ((fourCC >> 24) & 0xFF);
        bytes[1] = (byte) ((fourCC >> 16) & 0xFF);
        bytes[2] = (byte) ((fourCC >> 8) & 0xFF);
        bytes[3] = (byte) (fourCC & 0xFF);

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            if (b >= 32 && b <= 126) { // 可打印字符范围
                sb.append((char) b);
            } else {
                // 如果不是可打印字符，返回空字符串
                return "";
            }
        }
        return sb.toString();
    }

    /**
     * 主方法演示如何使用
     */
    public static void main(String[] args) {
        try {
            String mp4FilePath = "/sdcard/xxx/miaf-files/o.mp4";
            byte[] configData = extractVideoConfigFromMP4(mp4FilePath);

            if (configData != null) {
                Log.e(TAG, "成功提取视频配置数据，大小: " + configData.length + " 字节");

                // 打印前几个字节作为示例
                System.out.print("配置数据前16字节: ");
                for (int i = 0; i < Math.min(16, configData.length); i++) {
                    System.out.printf("%02X ", configData[i]);
                }
                Log.e(TAG, "");
            } else {
                Log.e(TAG, "未能提取视频配置数据");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
