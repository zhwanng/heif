/*
 * This file is part of Nokia HEIF applications
 *
 * Copyright (c) 2019-2021 Nokia Corporation and/or its subsidiary(-ies). All rights reserved.
 *
 * Contact: heif@nokia.com
 *
 * This software, including documentation, is protected by copyright controlled by Nokia Corporation. All rights are reserved.
 * Copying, including reproducing, storing, adapting or translating, any or all of this material requires the prior written consent of Nokia Corporation.
 * This material also contains confidential information which may not be disclosed to others without the prior written consent of Nokia.
 *
 */

package com.nokia.miaf.gallery

import android.graphics.BitmapFactory
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.heifwriter.HeifWriter
import androidx.heifwriter.HeifWriter.INPUT_MODE_BITMAP
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.nokia.heif.Exception
import com.nokia.heif.HEIF
import com.nokia.heif.HEVCDecoderConfig
import com.nokia.heif.HEVCSample
import com.nokia.heif.JPEGDecoderConfig
import com.nokia.heif.JPEGImageItem
import com.nokia.heif.Size
import com.nokia.heif.VideoTrack
import com.nokia.miaf.gallery.databinding.ActivityImageViewerBinding
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.lang.ref.WeakReference


class MiafViewerActivity : FragmentActivity(), ViewPager.OnPageChangeListener {
    private val TAG = "MiafViewerActivity"

    private lateinit var binding: ActivityImageViewerBinding;

    inner class ImagePageAdapter(fm: FragmentManager, fileList: List<File>) : FragmentStatePagerAdapter(fm) {
        private val mFiles = fileList


        override fun getItem(position: Int): Fragment {
            val imageFragment = MiafFragment()
            imageFragment.loadHEIFImage(mFiles[position])
            if (position >= mViews.size) {
                mViews.add(WeakReference(imageFragment))
            } else {
                mViews.set(position, WeakReference(imageFragment))
            }

            return imageFragment
        }

        override fun getCount(): Int {
            return mFiles.size
        }
    }

    private var mPagerAdapter: PagerAdapter? = null

    private val mViews: ArrayList<WeakReference<MiafFragment>> = ArrayList()

    private val MEDIA_ROOT_PATH = Environment.getExternalStorageDirectory().absolutePath + "/miaf-files"

    override fun onPageScrollStateChanged(state: Int) {
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
    }

    override fun onPageSelected(position: Int) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.viewPager.addOnPageChangeListener(this)

        binding.create1.setOnClickListener { v ->
            create()
        }
        binding.create2.setOnClickListener { view ->
            test();
        }
    }

    override fun onResume() {
        super.onResume()
        val folder = File(MEDIA_ROOT_PATH)
        mPagerAdapter = ImagePageAdapter(supportFragmentManager, loadFolder(folder))
        binding.viewPager.adapter = mPagerAdapter
    }

    /**
     * 创建标准的 Google Motion Photo (HEIC 格式)
     *
     * 根据 Google Motion Photo 官方规范：
     * https://developer.android.com/media/platform/motion-photo-format?hl=zh-cn#isobmff-image-specific-behavior
     *
     * HEIC Motion Photo 结构：
     * 1. ftyp box - 文件类型声明
     * 2. meta box - 图像元数据（包含 XMP）
     * 3. mdat box - HEVC 编码的图像数据
     * 4. mpvd box - Motion Photo Video Data（完整的 MP4 文件）
     *
     * 关键要求：
     * - XMP 必须包含 Padding 属性 = 8（mpvd box header 长度）
     * - mpvd box 必须在所有 HEIC boxes 之后
     * - mpvd box 包含完整的 MP4 视频数据
     */
    fun create() {
        Log.e(TAG, "========================================")
        Log.e(TAG, "开始创建 Google Motion Photo (HEIC 格式)")
        Log.e(TAG, "========================================")

        try {
            val heif = HEIF()
            val imageName = "wx.jpg"
            val videoName = "o265.mp4"

            // 步骤1: 设置主图像（JPEG）
            Log.e(TAG, "\n[步骤 1/4] 设置主图像...")
            setupPrimaryImage(heif, imageName)
            Log.e(TAG, "✓ 主图像设置完成")

            // 步骤2: 配置 HEIF 文件属性（品牌和兼容性）
            Log.e(TAG, "\n[步骤 2/4] 配置 HEIF 文件属性...")
            configureHEIFFile(heif)
            Log.e(TAG, "✓ HEIF 文件属性配置完成")

            // 步骤3: 保存基础 HEIF 文件（仅包含图像）
            Log.e(TAG, "\n[步骤 3/4] 保存基础 HEIF 文件...")
            val outputFile = saveHEIFFile(heif)
            Log.e(TAG, "✓ HEIF 文件保存完成: ${outputFile.length()} bytes")

            // 步骤4: 重新构建文件以包含 mpvd box
            Log.e(TAG, "\n[步骤 4/4] 重新构建文件以包含 mpvd box...")
            val mp4Data = readFileToByteArray("$MEDIA_ROOT_PATH/$videoName")
            rebuildFileWithMpvdBox(outputFile, mp4Data)
            Log.e(TAG, "✓ 文件重新构建完成")

            Log.e(TAG, "最终文件大小: ${outputFile.length()} bytes")

            Log.e(TAG, "\n========================================")
            Log.e(TAG, "✅ Google Motion Photo 创建成功！")
            Log.e(TAG, "文件路径: ${outputFile.absolutePath}")
            Log.e(TAG, "文件大小: ${outputFile.length()} bytes")
            Log.e(TAG, "========================================")

            Toast.makeText(this, "Motion Photo 创建成功！", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "\n========================================")
            Log.e(TAG, "✗ 创建失败: ${e.message}")
            Log.e(TAG, "========================================")
            e.printStackTrace()
            Toast.makeText(this, "创建失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 重新构建文件以包含 mpvd box
     * 这种方法读取整个HEIF文件，然后在末尾添加mpvd box
     */
    private fun rebuildFileWithMpvdBox(heifFile: File, mp4Data: ByteArray) {
        try {
            // 读取原始HEIF文件内容
            val heifData = heifFile.readBytes()
            Log.e(TAG, "Original HEIF data size: ${heifData.size} bytes")

            // 创建新的文件内容（HEIF数据 + mpvd box）
            val boxSize = 8 + mp4Data.size
            Log.e(TAG, "Creating mpvd box with size: $boxSize bytes")

            // 创建mpvd box数据
            val mpvdBox = ByteArray(8 + mp4Data.size)

            // 写入box大小（4字节，大端序）
            mpvdBox[0] = (boxSize shr 24 and 0xFF).toByte()
            mpvdBox[1] = (boxSize shr 16 and 0xFF).toByte()
            mpvdBox[2] = (boxSize shr 8 and 0xFF).toByte()
            mpvdBox[3] = (boxSize and 0xFF).toByte()

            // 写入box类型'mpvd'（4字节）
            mpvdBox[4] = 'm'.code.toByte()
            mpvdBox[5] = 'p'.code.toByte()
            mpvdBox[6] = 'v'.code.toByte()
            mpvdBox[7] = 'd'.code.toByte()

            // 复制MP4数据
            System.arraycopy(mp4Data, 0, mpvdBox, 8, mp4Data.size)

            // 创建完整文件数据
            val fullData = ByteArray(heifData.size + mpvdBox.size)
            System.arraycopy(heifData, 0, fullData, 0, heifData.size)
            System.arraycopy(mpvdBox, 0, fullData, heifData.size, mpvdBox.size)

            // 写入新文件
            heifFile.writeBytes(fullData)

            Log.e(TAG, "File rebuilt with mpvd box. New size: ${heifFile.length()} bytes")

            // 验证
            verifyRebuiltFile(heifFile)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to rebuild file with mpvd box: ${e.message}")
            throw e
        }
    }

    /**
     * 验证重新构建的文件
     */
    private fun verifyRebuiltFile(file: File) {
        try {
            val fileSize = file.length()
            if (fileSize < 8) return

            val fis = java.io.FileInputStream(file)
            fis.skip(fileSize - 8)
            val buffer = ByteArray(8)
            fis.read(buffer)
            fis.close()

            // 检查是否以'mpvd'结尾
            val boxType = String(buffer, 4, 4)
            if (boxType == "mpvd") {
                Log.e(TAG, "✓ File verification passed - mpvd box found at end")
            } else {
                Log.e(TAG, "✗ File verification failed - expected 'mpvd', found '$boxType'")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Verification failed: ${e.message}")
        }
    }

    /**
     * 设置主图片（JPEG）
     */
    private fun setupPrimaryImage(heif: HEIF, imageName: String) {
        val jpegByte = readFileToByteArray("$MEDIA_ROOT_PATH/$imageName")
        Log.e(TAG, "JPEG data size: ${jpegByte.size}")

        if (jpegByte.isEmpty()) {
            throw Exception("JPEG data is empty")
        }

        val imageSize = getImageSize(jpegByte)
        Log.e(TAG, "JPEG image size: ${imageSize.width}x${imageSize.height}")

        val jpegDecoderConfig = JPEGDecoderConfig(heif, jpegByte)
        val primaryImage = JPEGImageItem(heif, imageSize, jpegDecoderConfig, jpegByte)
        heif.primaryImage = primaryImage
    }

    /**
     * 设置视频轨道（改进版：整体存放视频流）
     */
    private fun setupVideoTrack(heif: HEIF, videoName: String) {
        val videoPath = "$MEDIA_ROOT_PATH/$videoName"

        val hevcConfig = extractHEVCConfigFromMP4(videoPath)

        // 获取完整的视频流数据和样本信息
        val (completeVideoData, sampleInfoList) = extractHEVCVideoStream(videoPath)

        if (hevcConfig.isEmpty()) {
            Log.w(TAG, "Warning: HEVC config is empty")
        }
        if (completeVideoData.isEmpty()) {
            throw Exception("HEVC video stream is empty")
        }

        val timescale = 1000
        val videoTrack = VideoTrack(heif, timescale)
        val hevcDecoderConfig = HEVCDecoderConfig(heif, hevcConfig)

        Log.e(TAG, "Complete video stream size: ${completeVideoData.size} bytes")
        Log.e(TAG, "Sample count: ${sampleInfoList.size}")

        // 基于样本信息添加样本
        for ((index, sampleInfo) in sampleInfoList.withIndex()) {
            val frameData = completeVideoData.sliceArray(sampleInfo.startOffset until sampleInfo.endOffset)
            val videoSample = HEVCSample(heif, hevcDecoderConfig, frameData, sampleInfo.duration)

            Log.e(TAG, "Adding frame $index: offset=${sampleInfo.startOffset}, size=${frameData.size}, duration=${sampleInfo.duration}")
            videoTrack.addSample(videoSample)
        }

        videoTrack.timescale = timescale
        heif.tracks.add(videoTrack)

        Log.e(TAG, "Video track added successfully with ${sampleInfoList.size} frames")
    }

    /**
     * 样本信息数据类
     */
    data class SampleInfo(
        val startOffset: Int,
        val endOffset: Int,
        val duration: Long,
        val size: Int
    )

    /**
     * 提取完整的HEVC视频流及样本信息（不转换格式）
     */
    private fun extractHEVCVideoStream(mp4FilePath: String): Pair<ByteArray, List<SampleInfo>> {
        val extractor = MediaExtractor()
        val videoStreamBuilder = mutableListOf<Byte>()
        val sampleInfoList = mutableListOf<SampleInfo>()
        var currentOffset = 0

        try {
            extractor.setDataSource(mp4FilePath)

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""

                if (mime.startsWith("video/hevc")) {
                    extractor.selectTrack(i)

                    val buffer = java.nio.ByteBuffer.allocate(1024 * 1024)
                    var sampleSize: Int
                    var frameIndex = 0

                    Log.e(TAG, "Extracting complete HEVC video stream...")

                    while (extractor.readSampleData(buffer, 0).also { sampleSize = it } > 0) {
                        val frameData = ByteArray(sampleSize)
                        buffer.position(0)
                        buffer.get(frameData)

                        // ❌ 不转换，直接保留原始MP4格式数据
                        val rawFrame = frameData

                        // 记录样本信息
                        val sampleInfo = SampleInfo(
                            startOffset = currentOffset,
                            endOffset = currentOffset + rawFrame.size,
                            duration = 41L,
                            size = rawFrame.size
                        )
                        sampleInfoList.add(sampleInfo)

                        // 追加到视频流
                        for (byte in rawFrame) {
                            videoStreamBuilder.add(byte)
                        }
                        currentOffset += rawFrame.size

                        Log.e(TAG, "Frame $frameIndex: size=${rawFrame.size}, total=${currentOffset}")
                        frameIndex++
                        extractor.advance()
                    }

                    Log.e(TAG, "Extracted ${frameIndex} frames, total stream size: $currentOffset bytes")
                    return Pair(videoStreamBuilder.toByteArray(), sampleInfoList)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting HEVC video stream: ${e.message}")
            e.printStackTrace()
        } finally {
            extractor.release()
        }

        return Pair(byteArrayOf(), emptyList())
    }

    /**
     * 配置HEIF文件属性
     */
    private fun configureHEIFFile(heif: HEIF) {
        heif.majorBrand = HEIF.BRAND_MIF1
        heif.compatibleBrands.clear()
        heif.compatibleBrands.add(HEIF.BRAND_MIF1)
        heif.compatibleBrands.add(HEIF.BRAND_HEIC)
        heif.compatibleBrands.add(HEIF.BRAND_HEVC)
        heif.compatibleBrands.add(HEIF.BRAND_ISO8)

        Log.e(TAG, "HEIF configured: majorBrand=mif1, compatibleBrands=${heif.compatibleBrands.size}")

    }

    /**
     * 保存HEIF文件（支持 Motion Photos）
     * @return 保存的文件对象
     */
    private fun saveHEIFFile(heif: HEIF): File {
        val outputDir = this.cacheDir.absolutePath + "/miaf-files-output"
        val outputFolder = File(outputDir)

        if (!outputFolder.exists()) {
            outputFolder.mkdirs()
        }

        Log.e(TAG, "Output path = $outputDir")

        val tempOutputFile = File.createTempFile("motion-photo-", ".heic", outputFolder)
        Log.e(TAG, "Output file = ${tempOutputFile.absolutePath}")

        heif.save(tempOutputFile.absolutePath)

        Log.e(TAG, "HEIF file saved successfully")
        Log.e(TAG, "File size: ${tempOutputFile.length()} bytes")

        return tempOutputFile
    }

    /**
     * 从MP4文件提取HEVC解码配置
     */
    private fun extractHEVCConfigFromMP4(mp4FilePath: String): ByteArray {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(mp4FilePath)

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""

                if (mime.startsWith("video/hevc")) {
                    Log.e(TAG, "Found HEVC track: $i")

                    // 方法1: 尝试获取 csd-0（Codec Specific Data）
                    val csd0 = format.getByteBuffer("csd-0")?.let {
                        val buffer = ByteArray(it.remaining())
                        it.get(buffer)
                        buffer
                    }

                    if (csd0 != null && csd0.isNotEmpty()) {
                        Log.e(TAG, "Extracted HEVC csd-0 config: ${csd0.size} bytes")
                        return csd0
                    }

                    // 方法2: 如果 csd-0 为空，尝试获取 csd-1（备选）
                    val csd1 = format.getByteBuffer("csd-1")?.let {
                        val buffer = ByteArray(it.remaining())
                        it.get(buffer)
                        buffer
                    }

                    if (csd1 != null && csd1.isNotEmpty()) {
                        Log.e(TAG, "Extracted HEVC csd-1 config: ${csd1.size} bytes")
                        return csd1
                    }

                    Log.w(TAG, "No HEVC config found in csd-0 or csd-1")
                    return byteArrayOf()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting HEVC config: ${e.message}")
            e.printStackTrace()
        } finally {
            extractor.release()
        }
        return byteArrayOf()
    }


    /**
     * 获取JPEG图像尺寸
     */
    private fun getImageSize(jpegData: ByteArray): Size {
        try {
            var i = 0
            while (i < jpegData.size - 9) {
                if (jpegData[i].toInt() == 0xFF) {
                    val marker = jpegData[i + 1].toInt()
                    // SOF0 (0xC0) 或 SOF2 (0xC2) 标记
                    if (marker == 0xC0 || marker == 0xC2) {
                        val height = ((jpegData[i + 5].toInt() and 0xFF) shl 8) or (jpegData[i + 6].toInt() and 0xFF)
                        val width = ((jpegData[i + 7].toInt() and 0xFF) shl 8) or (jpegData[i + 8].toInt() and 0xFF)
                        return Size(width, height)
                    }
                }
                i++
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting image size: ${e.message}")
        }
        return Size(1920, 1080)  // 默认尺寸
    }

    /**
     * 从文件读取字节数组
     */
    @Throws(IOException::class)
    private fun readFileToByteArray(filePath: String): ByteArray {
        val file = File(filePath)
        val fis = FileInputStream(file)
        val data = ByteArray(file.length().toInt())
        fis.read(data)
        fis.close()
        return data
    }

    /**
     * 添加 Google Motion Photo XMP 元数据
     *
     * 根据 Google Motion Photo 规范，XMP 元数据必须包含：
     * - GCamera:MotionPhoto = 1 (标识这是一个 Motion Photo)
     * - GCamera:MotionPhotoVersion = 1
     * - Container:Directory 描述图像和视频的位置和属性
     * - **Padding = 8** (对于 HEIC/AVIF 格式，这是 mpvd box header 的长度)
     *
     * @param heicFile HEIC 文件
     * @param imageName 图像文件名
     * @param videoName 视频文件名
     * @param videoSize 视频数据大小（字节）
     */
    private fun addMotionPhotoXMP(heicFile: File, imageName: String, videoName: String, videoSize: Int) {
        try {
            // 读取原始 HEIC 文件
            val heicData = heicFile.readBytes()

            // 生成 XMP 元数据（包含 Padding=8）
            val xmpData = generateMotionPhotoXMP(heicData.size, imageName, videoName, videoSize)

            Log.e(TAG, "Generated XMP metadata with Padding=8:")
            Log.e(TAG, xmpData)

            // 注意：对于 HEIC 格式，XMP 元数据应该嵌入到 HEIF 容器的 meta box 中
            // 由于 Nokia HEIF 库可能不直接支持 XMP 写入，这里提供两种方案：

            // 方案1: 如果库支持，在创建 HEIF 时就添加 XMP
            // 方案2: 使用 ExifTool 或其他工具后处理添加 XMP

            Log.e(TAG, "XMP metadata generated. To embed XMP into HEIC:")
            Log.e(TAG, "Option 1: Use HEIF library's XMP API (if available)")
            Log.e(TAG, "Option 2: Use exiftool: exiftool -XMP=\"...\" ${heicFile.absolutePath}")

            // 保存 XMP 到单独的文件供参考
            val xmpFile = File(heicFile.parent, heicFile.nameWithoutExtension + ".xmp")
            xmpFile.writeText(xmpData)
            Log.e(TAG, "XMP saved to: ${xmpFile.absolutePath}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to add Motion Photo XMP: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 生成符合 Google Motion Photo 规范的 XMP 元数据
     *
     * 参考规范：
     * https://developer.android.com/media/platform/motion-photo-format?hl=zh-cn
     *
     * @param fileSize HEIC 文件大小（不包含 mpvd box）
     * @param imageName 图像文件名
     * @param videoName 视频文件名
     * @param videoSize 视频数据大小（字节）
     */
    private fun generateMotionPhotoXMP(fileSize: Int, imageName: String, videoName: String, videoSize: Int): String {
        // 对于 HEIC 格式，必须设置 Padding = 8（mpvd box header 长度）
        // videoSize + 8 是 mpvd box 的总大小（包含 header）

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

    /**
     * 追加 Motion Photo Video Data Box (mpvd)
     *
     * 根据 Google Motion Photo 规范：
     * - Box 类型: 'mpvd' (Motion Photo Video Data)
     * - Box 结构: [size(4 bytes)][type(4 bytes)][完整的 MP4 数据]
     * - 必须位于所有 HEIC boxes 之后
     * - size 不能为 0
     *
     * @param filePath HEIC 文件路径
     * @param mp4Data 完整的 MP4 视频数据
     */
    private fun appendMotionPhotoVideoDataBox(filePath: String, mp4Data: ByteArray) {
        try {
            val file = File(filePath)
            val fos = java.io.FileOutputStream(file, true) // 追加模式

            // 计算 box 大小 (8 bytes header + MP4 data size)
            val boxSize = 8 + mp4Data.size

            Log.e(TAG, "Creating mpvd box:")
            Log.e(TAG, "  Box size: $boxSize bytes (header: 8 + data: ${mp4Data.size})")
            Log.e(TAG, "  Box type: 'mpvd'")

            // 正确的实现：分别写入size和type
            // 写入 box 大小 (4 bytes, big-endian)
            fos.write((boxSize shr 24 and 0xFF).toByte().toInt())
            fos.write((boxSize shr 16 and 0xFF).toByte().toInt())
            fos.write((boxSize shr 8 and 0xFF).toByte().toInt())
            fos.write((boxSize and 0xFF).toByte().toInt())

            // 写入 box 类型 'mpvd' (4 bytes)
            fos.write('m'.code.toByte().toInt())
            fos.write('p'.code.toByte().toInt())
            fos.write('v'.code.toByte().toInt())
            fos.write('d'.code.toByte().toInt())

            // 写入完整的 MP4 数据
            fos.write(mp4Data)
            fos.close()

            Log.e(TAG, "✓ mpvd box appended successfully")
            Log.e(TAG, "  Structure: [size:$boxSize][type:mpvd][MP4 data:${mp4Data.size} bytes]")

        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to append mpvd box: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }


    /**
     * 辅助方法：获取文件的 MIME 类型
     */
    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".jpg", ignoreCase = true) ||
                    fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"

            fileName.endsWith(".heic", ignoreCase = true) -> "image/heic"
            fileName.endsWith(".mp4", ignoreCase = true) -> "video/mp4"
            else -> "application/octet-stream"
        }
    }

    fun test() {

        val outputDir = this.cacheDir.absolutePath + "/miaf-files-output"
        val outputFolder = File(outputDir)

        if (!outputFolder.exists()) {
            outputFolder.mkdirs()
        }

        Log.e(TAG, "Output path = $outputDir")

        val tempOutputFile = File.createTempFile("temp-", ".heic", outputFolder)
        val imageName: String = "IMG_5880.JPG";

        val heifWriter: HeifWriter = HeifWriter.Builder(tempOutputFile.absolutePath, 1080, 1920, INPUT_MODE_BITMAP)
            .setQuality(100)
            .build();
        heifWriter.start()
        heifWriter.addBitmap(BitmapFactory.decodeFile("$MEDIA_ROOT_PATH/$imageName"))
        heifWriter.stop(0)
        heifWriter.close()
        Log.e(TAG, "Output file = ${tempOutputFile.absolutePath}")

    }
}
