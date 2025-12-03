package com.nokia.miaf.gallery;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * HEIF XMP 元数据注入器
 * 
 * 用于向 HEIF/HEIC 文件的 meta box 中注入 XMP 元数据
 * 特别用于 Google Motion Photo 的 XMP 元数据
 * 
 * 参考规范：
 * - ISO/IEC 23008-12 (HEIF)
 * - ISO/IEC 14496-12 (ISOBMFF)
 * - Google Motion Photo Format 1.0
 */
public class HEIFXmpInjector {

    private static final String TAG = "HEIFXmpInjector";
    
    // XMP item 类型
    private static final String ITEM_TYPE_XMP = "mime";
    private static final String XMP_MIME_TYPE = "application/rdf+xml";
    
    /**
     * 向 HEIF 文件注入 Motion Photo XMP 元数据
     * 
     * @param heifData 原始 HEIF 文件数据
     * @param videoSize MP4 视频数据大小（字节）
     * @param imageMimeType 图像 MIME 类型 (image/heic 或 image/jpeg)
     * @return 注入 XMP 后的 HEIF 数据，失败返回原数据
     */
    public byte[] injectMotionPhotoXmp(byte[] heifData, int videoSize, String imageMimeType) {
        try {
            // 生成 Motion Photo XMP 内容
            String xmpContent = generateMotionPhotoXmp(videoSize, imageMimeType);
            
            // 注入 XMP 到 meta box
            return injectXmpToMetaBox(heifData, xmpContent);
            
        } catch (Exception e) {
            logError("Failed to inject XMP: " + e.getMessage());
            e.printStackTrace();
            return heifData; // 返回原数据
        }
    }
    
    /**
     * 生成符合 Google Motion Photo 规范的 XMP 元数据
     * 
     * @param videoSize 视频数据大小（字节）
     * @param imageMimeType 图像 MIME 类型
     * @return XMP XML 字符串
     */
    private String generateMotionPhotoXmp(int videoSize, String imageMimeType) {
        // 根据 Google Motion Photo 规范生成 XMP
        // https://developer.android.com/media/platform/motion-photo-format
        
        return "<?xpacket begin=\"\" id=\"W5M0MpCehiHzreSzNTczkc9d\"?>\n" +
               "<x:xmpmeta xmlns:x=\"adobe:ns:meta/\" x:xmptk=\"Adobe XMP Core 5.1.0-jc003\">\n" +
               "  <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
               "    <rdf:Description rdf:about=\"\"\n" +
               "        xmlns:GCamera=\"http://ns.google.com/photos/1.0/camera/\"\n" +
               "        xmlns:Container=\"http://ns.google.com/photos/1.0/container/\"\n" +
               "        xmlns:Item=\"http://ns.google.com/photos/1.0/container/item/\"\n" +
               "        GCamera:MotionPhoto=\"1\"\n" +
               "        GCamera:MotionPhotoVersion=\"1\"\n" +
               "        GCamera:MotionPhotoPresentationTimestampUs=\"0\">\n" +
               "      <Container:Directory>\n" +
               "        <rdf:Seq>\n" +
               "          <rdf:li rdf:parseType=\"Resource\">\n" +
               "            <Container:Item\n" +
               "                Item:Semantic=\"Primary\"\n" +
               "                Item:Mime=\"" + imageMimeType + "\"\n" +
               "                Item:Length=\"0\"\n" +
               "                Item:Padding=\"8\"/>\n" +
               "          </rdf:li>\n" +
               "          <rdf:li rdf:parseType=\"Resource\">\n" +
               "            <Container:Item\n" +
               "                Item:Semantic=\"MotionPhoto\"\n" +
               "                Item:Mime=\"video/mp4\"\n" +
               "                Item:Length=\"" + videoSize + "\"/>\n" +
               "          </rdf:li>\n" +
               "        </rdf:Seq>\n" +
               "      </Container:Directory>\n" +
               "    </rdf:Description>\n" +
               "  </rdf:RDF>\n" +
               "</x:xmpmeta>\n" +
               "<?xpacket end=\"w\"?>";
    }
    
    /**
     * 将 XMP 注入到 HEIF 的 meta box 中
     * 
     * 注意：这是一个简化实现
     * 完整实现需要解析和修改 meta box 的所有子 box (hdlr, pitm, iloc, iinf, iprp 等)
     * 
     * @param heifData 原始 HEIF 数据
     * @param xmpContent XMP XML 内容
     * @return 修改后的 HEIF 数据
     */
    private byte[] injectXmpToMetaBox(byte[] heifData, String xmpContent) throws IOException {
        // 解析顶层 boxes
        List<Box> boxes = ISOBMFFParser.parseTopLevelBoxes(heifData);
        
        // 查找 meta box
        Box metaBox = null;
        for (Box box : boxes) {
            if ("meta".equals(box.type)) {
                metaBox = box;
                break;
            }
        }
        
        if (metaBox == null) {
            logError("No meta box found in HEIF file");
            return heifData;
        }
        
        logInfo("Found meta box at offset " + metaBox.offset + ", size " + metaBox.size);
        
        // 简化方案：将 XMP 作为注释添加到文件末尾
        // 完整方案需要修改 meta box 内部结构
        return appendXmpAsComment(heifData, xmpContent);
    }
    
    /**
     * 简化方案：将 XMP 作为自定义 box 追加到文件
     * 
     * 注意：这不是标准的 HEIF XMP 嵌入方式
     * 标准方式需要在 meta box 内部添加 XMP item
     * 
     * 但这个方案可以让某些工具（如 exiftool）读取 XMP
     */
    private byte[] appendXmpAsComment(byte[] heifData, String xmpContent) throws IOException {
        // 创建一个自定义 'uuid' box 来存储 XMP
        // 或者创建 'xml ' box
        
        byte[] xmpBytes = xmpContent.getBytes(StandardCharsets.UTF_8);
        
        // 创建 'xml ' box
        ByteBuffer xmlBox = ByteBuffer.allocate(8 + xmpBytes.length);
        xmlBox.order(ByteOrder.BIG_ENDIAN);
        xmlBox.putInt(8 + xmpBytes.length);  // box size
        xmlBox.put("xml ".getBytes(StandardCharsets.US_ASCII));  // box type
        xmlBox.put(xmpBytes);  // XMP content
        
        // 追加到 HEIF 数据
        ByteArrayOutputStream output = new ByteArrayOutputStream(heifData.length + xmlBox.capacity());
        output.write(heifData);
        output.write(xmlBox.array());
        
        logInfo("Appended XMP as 'xml ' box (" + xmlBox.capacity() + " bytes)");
        
        return output.toByteArray();
    }
    
    /**
     * 标准方案：修改 meta box 内部结构（复杂实现）
     * 
     * 这需要：
     * 1. 解析 meta box 的所有子 box
     * 2. 添加新的 XMP item 到 iinf (item info)
     * 3. 更新 iloc (item location) 指向 XMP 数据
     * 4. 可能需要更新 iprp (item properties)
     * 5. 重新计算所有偏移和大小
     * 6. 重建整个 meta box
     */
    @SuppressWarnings("unused")
    private byte[] injectXmpToMetaBoxStandard(byte[] heifData, String xmpContent) throws IOException {
        // TODO: 完整实现
        // 这是一个非常复杂的过程，需要深入理解 HEIF 规范
        
        logInfo("Standard XMP injection not yet implemented");
        logInfo("Using simplified approach instead");
        
        return appendXmpAsComment(heifData, xmpContent);
    }
    
    /**
     * 解析 meta box 的子 boxes
     */
    @SuppressWarnings("unused")
    private List<Box> parseMetaBoxChildren(byte[] heifData, Box metaBox) {
        List<Box> children = new ArrayList<>();
        
        // meta box 的结构：
        // [size][type='meta'][version+flags][子 boxes...]
        
        long offset = metaBox.getPayloadOffset() + 4; // 跳过 version+flags
        long endOffset = metaBox.offset + metaBox.size;
        
        ByteBuffer buffer = ByteBuffer.wrap(heifData);
        buffer.order(ByteOrder.BIG_ENDIAN);
        
        while (offset + 8 <= endOffset) {
            buffer.position((int) offset);
            
            int size = buffer.getInt();
            byte[] typeBytes = new byte[4];
            buffer.get(typeBytes);
            String type = new String(typeBytes, StandardCharsets.US_ASCII);
            
            if (size < 8 || offset + size > endOffset) {
                break;
            }
            
            Box child = new Box(type, offset, size, 8);
            children.add(child);
            
            offset += size;
        }
        
        return children;
    }
    
    /**
     * 验证注入的 XMP 是否有效
     */
    public boolean verifyXmpInjection(byte[] heifData) {
        // 查找 'xml ' box
        List<Box> boxes = ISOBMFFParser.parseTopLevelBoxes(heifData);
        
        for (Box box : boxes) {
            if ("xml ".equals(box.type)) {
                logInfo("Found XMP box at offset " + box.offset);
                return true;
            }
        }
        
        logInfo("No XMP box found");
        return false;
    }
    
    /**
     * 提取 XMP 内容
     */
    public String extractXmp(byte[] heifData) {
        List<Box> boxes = ISOBMFFParser.parseTopLevelBoxes(heifData);
        
        for (Box box : boxes) {
            if ("xml ".equals(box.type)) {
                int offset = (int) box.getPayloadOffset();
                int size = (int) box.getPayloadSize();
                
                byte[] xmpBytes = new byte[size];
                System.arraycopy(heifData, offset, xmpBytes, 0, size);
                
                return new String(xmpBytes, StandardCharsets.UTF_8);
            }
        }
        
        return null;
    }
    
    private void logInfo(String message) {
        System.out.println(TAG + ": " + message);
    }
    
    private void logError(String message) {
        System.err.println(TAG + ": " + message);
    }
}
