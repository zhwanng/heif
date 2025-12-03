package com.nokia.miaf.gallery;

public class Box {
    public final String type;      // Box 类型 (4字符)
    public final long offset;      // 在文件中的偏移
    public final long size;        // Box 总大小 (含 header)
    public final int headerSize;   // Header 大小 (8 或 16)

    public Box(String type, long offset, long size, int headerSize) {
        this.type = type;
        this.offset = offset;
        this.size = size;
        this.headerSize = headerSize;
    }

    public long getPayloadOffset() {
        return offset + headerSize;
    }

    public long getPayloadSize() {
        return size - headerSize;
    }

    @Override
    public String toString() {
        return String.format("Box{type='%s', offset=%d, size=%d, headerSize=%d}",
                type, offset, size, headerSize);
    }
}
