# Google Motion Photo 实现指南 (HEIC 格式) - 修订版

## 📋 概述

本文档描述了如何创建符合 **Google Motion Photo 官方规范** 的 HEIC 格式动态照片。

**官方规范**: https://developer.android.com/media/platform/motion-photo-format?hl=zh-cn

## 🎯 Google Motion Photo 规范要求

### 1. 文件结构（HEIC/AVIF 格式）

根据官方规范，HEIC Motion Photo 的正确结构如下：

```
HEIC Motion Photo 文件结构：
├── ftyp box - 文件类型声明
│   └── major_brand: heic/mif1
├── meta box - 图像元数据
│   ├── hdlr (Handler Reference Box)
│   ├── pitm (Primary Item Box)
│   ├── iloc (Item Location Box)
│   ├── iinf (Item Info Box)
│   ├── iprp (Item Properties Box)
│   └── XMP 元数据 (包含 Motion Photo 信息)
├── mdat box - 图像数据 (Media Data Box)
│   └── HEVC/JPEG 编码的图像数据
└── mpvd box - Motion Photo Video Data ⭐ 关键
    └── 完整的 MP4 视频文件
        ├── ftyp box (major_brand: isom/mp41/mp42)
        ├── moov box (Movie Box)
        │   ├── mvhd (Movie Header)
        │   ├── trak (Video Track - 必需)
        │   └── trak (Audio Track - 可选)
        └── mdat box (视频数据)
```

### 2. mpvd Box 规范

根据 ISO/IEC 14496-12:2015 定义：

```cpp
// Box as defined in ISO/IEC 14496-12:2015: 4.2
aligned(8) class MotionPhotoVideoData extends Box('mpvd') {
    bit(8) data[];
}
```

**关键要求：**
- ✅ Box 类型：`'mpvd'` (4 bytes)
- ✅ Box 大小：不能为 0
- ✅ Box 位置：**必须在所有 HEIC boxes 之后**
- ✅ Box 内容：完整的 MP4 视频文件（包含 ftyp, moov, mdat）

**Box 结构：**
```
[4 bytes: size] [4 bytes: 'mpvd'] [N bytes: 完整的 MP4 数据]
```

### 3. XMP 元数据要求

HEIC Motion Photo 的 XMP 元数据**必须**包含以下属性：

#### 3.1 相机元数据 (GCamera)

```xml
xmlns:GCamera="http://ns.google.com/photos/1.0/camera/"
```

| 属性 | 类型 | 值 | 说明 |
|------|------|-----|------|
| `GCamera:MotionPhoto` | Integer | 1 | 标识这是一个 Motion Photo |
| `GCamera:MotionPhotoVersion` | Integer | 1 | Motion Photo 格式版本 |
| `GCamera:MotionPhotoPresentationTimestampUs` | Long | 0 或具体值 | 静态图片对应的视频帧时间戳（微秒） |

#### 3.2 容器元数据 (Container)

```xml
xmlns:Container="http://ns.google.com/photos/1.0/container/"
xmlns:Item="http://ns.google.com/photos/1.0/container/item/"
```

**Container:Directory** - 媒体内容目录（有序数组）

**第一项（主图像）：**
| 属性 | 类型 | 值 | 说明 |
|------|------|-----|------|
| `Item:Semantic` | String | "Primary" | 主显示图片 |
| `Item:Mime` | String | "image/jpeg" 或 "image/heic" | 图像 MIME 类型 |
| `Item:Length` | Integer | 0 | 主图像长度（可选，设为 0） |
| `Item:Padding` | Integer | **8** | ⭐ **必需！mpvd box header 长度** |

**第二项（视频）：**
| 属性 | 类型 | 值 | 说明 |
|------|------|-----|------|
| `Item:Semantic` | String | "MotionPhoto" | 视频容器 |
| `Item:Mime` | String | "video/mp4" | 视频 MIME 类型 |
| `Item:Length` | Integer | 视频数据大小（字节） | MP4 数据的实际大小 |

#### 3.3 完整 XMP 示例

```xml
<?xpacket begin="" id="W5M0MpCehiHzreSzNTczkc9d"?>
<x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Adobe XMP Core 5.1.0-jc003">
  <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
    <rdf:Description rdf:about=""
        xmlns:GCamera="http://ns.google.com/photos/1.0/camera/"
        xmlns:Container="http://ns.google.com/photos/1.0/container/"
        xmlns:Item="http://ns.google.com/photos/1.0/container/item/"
        GCamera:MotionPhoto="1"
        GCamera:MotionPhotoVersion="1"
        GCamera:MotionPhotoPresentationTimestampUs="0">
      <Container:Directory>
        <rdf:Seq>
          <rdf:li rdf:parseType="Resource">
            <Container:Item
                Item:Semantic="Primary"
                Item:Mime="image/heic"
                Item:Length="0"
                Item:Padding="8"/>
          </rdf:li>
          <rdf:li rdf:parseType="Resource">
            <Container:Item
                Item:Semantic="MotionPhoto"
                Item:Mime="video/mp4"
                Item:Length="1234567"/>
          </rdf:li>
        </rdf:Seq>
      </Container:Directory>
    </rdf:Description>
  </rdf:RDF>
</x:xmpmeta>
<?xpacket end="w"?>
```

### 4. HEIF 品牌要求

```kotlin
heif.majorBrand = HEIF.BRAND_MIF1  // 或 HEIF.BRAND_HEIC
heif.compatibleBrands.add(HEIF.BRAND_MIF1)
heif.compatibleBrands.add(HEIF.BRAND_HEIC)
heif.compatibleBrands.add(HEIF.BRAND_HEVC)
heif.compatibleBrands.add(HEIF.BRAND_ISO8)
```

## 🔧 实现步骤

### 完整实现流程

```kotlin
fun create() {
    val heif = HEIF()
    val imageName = "image.jpg"
    val videoName = "video.mp4"
    
    // 步骤1: 设置主图像（JPEG 或 HEVC 编码）
    setupPrimaryImage(heif, imageName)
    
    // 步骤2: 配置 HEIF 文件属性
    configureHEIFFile(heif)
    
    // 步骤3: 保存基础 HEIF 文件（仅包含图像）
    val outputFile = saveHEIFFile(heif)
    
    // 步骤4: 追加 mpvd box（包含完整 MP4 视频）⭐ 关键步骤
    val mp4Data = readFileToByteArray("$MEDIA_ROOT_PATH/$videoName")
    appendMotionPhotoVideoDataBox(outputFile.absolutePath, mp4Data)
    
    // 步骤5: 添加/更新 XMP 元数据（必须包含 Padding=8）
    addMotionPhotoXMP(outputFile, imageName, videoName, mp4Data.size)
}
```

### 步骤 1: 设置主图像

```kotlin
private fun setupPrimaryImage(heif: HEIF, imageName: String) {
    val jpegByte = readFileToByteArray("$MEDIA_ROOT_PATH/$imageName")
    val imageSize = getImageSize(jpegByte)
    
    val jpegDecoderConfig = JPEGDecoderConfig(heif, jpegByte)
    val primaryImage = JPEGImageItem(heif, imageSize, jpegDecoderConfig, jpegByte)
    heif.primaryImage = primaryImage
}
```

### 步骤 2: 配置 HEIF 文件属性

```kotlin
private fun configureHEIFFile(heif: HEIF) {
    heif.majorBrand = HEIF.BRAND_MIF1
    heif.compatibleBrands.clear()
    heif.compatibleBrands.add(HEIF.BRAND_MIF1)
    heif.compatibleBrands.add(HEIF.BRAND_HEIC)
    heif.compatibleBrands.add(HEIF.BRAND_HEVC)
    heif.compatibleBrands.add(HEIF.BRAND_ISO8)
}
```

### 步骤 3: 保存基础 HEIF 文件

```kotlin
private fun saveHEIFFile(heif: HEIF): File {
    val outputDir = this.cacheDir.absolutePath + "/miaf-files-output"
    val outputFolder = File(outputDir)
    outputFolder.mkdirs()
    
    val tempOutputFile = File.createTempFile("motion-photo-", ".heic", outputFolder)
    heif.save(tempOutputFile.absolutePath)
    
    return tempOutputFile
}
```

### 步骤 4: 追加 mpvd Box ⭐ 关键实现

```kotlin
/**
 * 追加 Motion Photo Video Data Box (mpvd)
 * 
 * 根据 Google Motion Photo 规范：
 * - Box 类型: 'mpvd' (Motion Photo Video Data)
 * - Box 结构: [size(4 bytes)][type(4 bytes)][完整的 MP4 数据]
 * - 必须位于所有 HEIC boxes 之后
 * - size 不能为 0
 */
private fun appendMotionPhotoVideoDataBox(filePath: String, mp4Data: ByteArray) {
    val file = File(filePath)
    val fos = FileOutputStream(file, true) // 追加模式
    
    // 计算 box 大小 (8 bytes header + MP4 data size)
    val boxSize = 8 + mp4Data.size
    
    // 使用 ByteBuffer 写入 box header (大端序)
    val header = ByteBuffer.allocate(8).apply {
        order(ByteOrder.BIG_ENDIAN)
        putInt(boxSize)              // 写入 box 大小 (4 bytes)
        put('m'.code.toByte())       // 写入 box 类型 'mpvd' (4 bytes)
        put('p'.code.toByte())
        put('v'.code.toByte())
        put('d'.code.toByte())
    }
    
    // 写入 header 和 MP4 数据
    fos.write(header.array())
    fos.write(mp4Data)
    fos.close()
}
```

**关键点：**
1. ✅ 使用追加模式 (`FileOutputStream(file, true)`)
2. ✅ 使用大端序 (`ByteOrder.BIG_ENDIAN`)
3. ✅ Box 大小 = 8 (header) + MP4 数据大小
4. ✅ 写入完整的 MP4 文件数据

### 步骤 5: 添加 XMP 元数据

```kotlin
private fun generateMotionPhotoXMP(
    fileSize: Int, 
    imageName: String, 
    videoName: String, 
    videoSize: Int
): String {
    return """<?xpacket begin="" id="W5M0MpCehiHzreSzNTczkc9d"?>
<x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Adobe XMP Core 5.1.0-jc003">
  <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
    <rdf:Description rdf:about=""
        xmlns:GCamera="http://ns.google.com/photos/1.0/camera/"
        xmlns:Container="http://ns.google.com/photos/1.0/container/"
        xmlns:Item="http://ns.google.com/photos/1.0/container/item/"
        GCamera:MotionPhoto="1"
        GCamera:MotionPhotoVersion="1"
        GCamera:MotionPhotoPresentationTimestampUs="0">
      <Container:Directory>
        <rdf:Seq>
          <rdf:li rdf:parseType="Resource">
            <Container:Item
                Item:Semantic="Primary"
                Item:Mime="${getMimeType(imageName)}"
                Item:Length="0"
                Item:Padding="8"/>
          </rdf:li>
          <rdf:li rdf:parseType="Resource">
            <Container:Item
                Item:Semantic="MotionPhoto"
                Item:Mime="video/mp4"
                Item:Length="$videoSize"/>
          </rdf:li>
        </rdf:Seq>
      </Container:Directory>
    </rdf:Description>
  </rdf:RDF>
</x:xmpmeta>
<?xpacket end="w"?>"""
}
```

**关键点：**
1. ✅ `Item:Padding="8"` - **必需！**（mpvd box header 长度）
2. ✅ `Item:Length="$videoSize"` - 视频数据的实际大小
3. ✅ `GCamera:MotionPhoto="1"` - 标识为 Motion Photo

## ⚠️ 重要注意事项

### 1. Padding 属性的重要性

根据官方规范：

> **[基于 HEIC/AVIF 的动态照片]**  
> 主媒体项的必填项。值必须等于 8，即动态照片视频数据（"mpvd"）框的标题长度。

**为什么是 8？**
- mpvd box header = 4 bytes (size) + 4 bytes (type) = 8 bytes

**作用：**
- 允许读取器从文件末尾快速定位视频数据
- 计算方式：`文件末尾 - Item:Length - Item:Padding = 视频起始位置`

### 2. mpvd Box 必须在最后

根据规范：

> "mpvd"文本框必须在所有 HEIC 图片文件的框之后。

**实现方式：**
1. 先保存完整的 HEIC 文件（ftyp + meta + mdat）
2. 然后追加 mpvd box

### 3. MP4 视频要求

**必需：**
- ✅ 至少一个主视频轨道
- ✅ 视频编码：AVC (H.264) / HEVC (H.265) / AV1

**可选：**
- 次要视频轨道（更高分辨率）
- 音频轨道（AAC 编码，16位，44/48/96 kHz）

### 4. XMP 嵌入方法

目前的实现将 XMP 保存为单独的 `.xmp` 文件。要创建完全符合规范的 Motion Photo，需要将 XMP 嵌入到 HEIF 的 meta box 中。

**选项 A: 使用 HEIF 库的 XMP API**
```kotlin
// 如果 Nokia HEIF 库支持
heif.setXMPMetadata(xmpData)
```

**选项 B: 使用 ExifTool**
```bash
exiftool -XMP-GCamera:MotionPhoto=1 \
         -XMP-GCamera:MotionPhotoVersion=1 \
         -overwrite_original motion-photo.heic
```

**选项 C: 手动嵌入**
需要解析 HEIF 文件结构，在 meta box 中添加 XMP item。

## 🔍 验证方法

### 1. 使用 ExifTool 检查

```bash
# 查看所有元数据
exiftool -a -G1 motion-photo.heic

# 查看 Motion Photo 相关属性
exiftool -GCamera:all motion-photo.heic

# 查看 Container 信息
exiftool -Container:all motion-photo.heic

# 检查 Padding 属性
exiftool -XMP:Padding motion-photo.heic
```

**预期输出：**
```
[XMP-GCamera] Motion Photo              : 1
[XMP-GCamera] Motion Photo Version      : 1
[XMP-Container] Directory               : (Binary data)
[XMP-Item] Padding                      : 8
```

### 2. 使用 mp4box 分析结构

```bash
# 分析 HEIC 文件结构
mp4box -info motion-photo.heic

# 提取 mpvd box
mp4box -dump-item 1:path=extracted.mp4 motion-photo.heic
```

### 3. 手动验证 mpvd Box

```bash
# 查看文件末尾的 mpvd box
hexdump -C motion-photo.heic | tail -n 20

# 应该看到类似：
# ... 00 12 34 56 6d 70 76 64 ...
#     ^^^^^^^^^^^ ^^^^^^^^^^^
#     box size    'mpvd'
```

### 4. 在 Android 设备上测试

```kotlin
// 检查是否被识别为 Motion Photo
val uri = Uri.fromFile(File("/path/to/motion-photo.heic"))
val cursor = contentResolver.query(
    uri, 
    arrayOf(MediaStore.Images.Media.IS_MOTION_PHOTO), 
    null, null, null
)
```

## 📊 文件大小计算

```
总文件大小 = HEIC 图像大小 + mpvd box 大小
mpvd box 大小 = 8 (header) + MP4 视频大小
```

**示例：**
- HEIC 图像：2 MB
- MP4 视频：5 MB
- mpvd box header：8 bytes
- **总大小：7 MB + 8 bytes**

## 🐛 常见问题

### Q1: Motion Photo 在 Google Photos 中不显示动画？

**检查清单：**
- [ ] XMP 元数据是否正确嵌入到 meta box
- [ ] `GCamera:MotionPhoto` 是否设置为 "1"
- [ ] `Item:Padding` 是否设置为 "8"
- [ ] `Item:Length` 是否等于 MP4 数据大小
- [ ] mpvd box 是否在文件末尾
- [ ] MP4 视频是否包含有效的视频轨道

### Q2: 如何验证 mpvd box 是否正确？

```bash
# 方法1: 使用 hexdump
tail -c +$(stat -f%z motion-photo.heic | awk '{print $1-100}') motion-photo.heic | hexdump -C | grep "mpvd"

# 方法2: 提取并验证 MP4
dd if=motion-photo.heic of=extracted.mp4 bs=1 skip=<HEIC_SIZE+8>
ffprobe extracted.mp4
```

### Q3: Padding=8 的作用是什么？

**作用：**
允许读取器从文件末尾快速定位视频数据，无需解析整个 HEIC 文件。

**计算公式：**
```
视频起始位置 = 文件总大小 - Item:Length - Item:Padding
             = 文件总大小 - MP4大小 - 8
```

## 📚 参考资料

1. **Google Motion Photo 官方规范**
   - https://developer.android.com/media/platform/motion-photo-format?hl=zh-cn

2. **ISO Base Media File Format (ISOBMFF)**
   - ISO/IEC 14496-12:2015
   - https://www.iso.org/standard/68960.html

3. **HEIF 规范**
   - ISO/IEC 23008-12
   - https://www.iso.org/standard/83656.html

4. **XMP 规范**
   - https://www.adobe.com/devnet/xmp.html
   - https://github.com/adobe/xmp-docs

## 📝 更新日志

- **2025-12-03 (修订版)**: 
  - ✅ 更正了 Motion Photo 实现方式（使用 mpvd box 而非 video track）
  - ✅ 添加了 Padding=8 的详细说明
  - ✅ 更新了完整的 XMP 元数据示例
  - ✅ 添加了 mpvd box 的详细实现
  - ✅ 更新了验证方法和常见问题

---

**作者**: Nokia HEIF Team  
**最后更新**: 2025-12-03  
**规范版本**: Motion Photo Format 1.0
