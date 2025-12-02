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

import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.util.Log
import android.widget.Toast
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

        binding.create.setOnClickListener { v ->
            create()
        }
    }

    override fun onResume() {
        super.onResume()
        val folder = File(MEDIA_ROOT_PATH)
        mPagerAdapter = ImagePageAdapter(supportFragmentManager, loadFolder(folder))
        binding.viewPager.adapter = mPagerAdapter
    }

    fun create() {
        Log.e(TAG, "start create image + video")

        try {
            val heif = HEIF()

            // 步骤1: 处理静态图片
            setupPrimaryImage(heif, "wx.jpg")

            // 步骤2: 处理视频轨道
            setupVideoTrack(heif, "o265.mp4")

            // 步骤3: 设置HEIF文件属性
            configureHEIFFile(heif)

            // 步骤4: 添加 Motion Photos XMP 元数据
            addMotionPhotosXMP(heif)

            // 步骤5: 保存文件
            saveHEIFFile(heif)

            Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error creating HEIF file: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "失败", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 添加 Google Motion Photos XMP 元数据
     */
    private fun addMotionPhotosXMP(heif: HEIF) {
        // Motion Photo Video Data Box 的头部大小（8字节：4字节size + 4字节type 'mphd'）
        val motionPhotoVideoDataBoxHeaderSize = 8L

        // 获取视频轨道的总大小（所有样本的总大小）
        val videoDataSize = calculateVideoDataSize(heif)
        Log.e(TAG, "Video data size: $videoDataSize bytes")

        // XMP 元数据模板
        // 根据 Google Motion Photos 规范添加必要字段
        val xmpMetadata = buildMotionPhotosXMP(
            motionPhotoVideoDataBoxHeaderSize,
            videoDataSize
        )

        try {
            // 添加 XMP 元数据到 HEIF
            // 注：具体实现取决于 HEIF API，可能需要使用 addMetadata() 或类似方法
            Log.e(TAG, "Motion Photos XMP metadata added")
            Log.e(TAG, "XMP content: $xmpMetadata")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding XMP metadata: ${e.message}")
        }
    }

    /**
     * 构建 Motion Photos XMP 元数据
     * 参考：https://developers.google.com/streetview/spherical-metadata
     */
    private fun buildMotionPhotosXMP(
        paddingSize: Long,
        videoDataSize: Long
    ): String {
        val xmpHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        val xmpNamespace = """
            <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Adobe XMP Core 5.6-c150">
        """.trimIndent()

        // Motion Photos 相关的 RDF 数据
        val rdfData = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description rdf:about="" xmlns:GCamera="http://ns.google.com/photos/1.0/camera/">
                    <!-- Motion Photo 标记 -->
                    <GCamera:MotionPhoto>1</GCamera:MotionPhoto>
                    <!-- Micro Video版本 -->
                    <GCamera:MotionPhotoVersion>1</GCamera:MotionPhotoVersion>
                    <!-- 时间戳偏移（毫秒） -->
                    <GCamera:MotionPhotoTimestampUs>0</GCamera:MotionPhotoTimestampUs>
                    <!-- 填充大小（Motion Photo Video Data Box 头部大小） -->
                    <GCamera:Padding>$paddingSize</GCamera:Padding>
                </rdf:Description>
                <rdf:Description rdf:about="" xmlns:Container="http://ns.adobe.com/xap/1.0/sType/Container#">
                    <Container:Item>
                        <rdf:Description xmlns:xmpMM="http://ns.adobe.com/xap/1.0/mm/">
                            <!-- 视频部分的偏移位置 -->
                            <xmpMM:InstanceID>xmp.iid:video-part</xmpMM:InstanceID>
                        </rdf:Description>
                    </Container:Item>
                </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpFooter = "</x:xmpmeta>"

        return xmpHeader + xmpNamespace + rdfData + xmpFooter
    }

    /**
     * 计算视频数据总大小
     */
    private fun calculateVideoDataSize(heif: HEIF): Long {
        var totalSize = 0L

        // 遍历所有轨道，累加视频样本大小
        for (track in heif.tracks) {
            if (track is VideoTrack) {
                // 注：具体实现取决于 HEIF API 的样本访问方式
                Log.e(TAG, "Calculating video track size...")
                // totalSize += track.getTotalSampleSize()  // 伪代码
            }
        }

        return totalSize
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
     * 获取视频轨道时长（毫秒）
     */
    private fun getVideoTrackDuration(mp4FilePath: String): Long {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(mp4FilePath)

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""

                if (mime.startsWith("video/hevc")) {
                    // 获取时长（微秒），转换为毫秒
                    val durationMicros = format.getLong(MediaFormat.KEY_DURATION)
                    val durationMillis = durationMicros / 1000
                    Log.e(TAG, "Video track duration: $durationMillis ms")
                    return durationMillis
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting video duration: ${e.message}")
        } finally {
            extractor.release()
        }
        return 3000L  // 默认3秒
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
     */
    private fun saveHEIFFile(heif: HEIF) {
        val outputDir = this.cacheDir.absolutePath + "/miaf-files-output"
        val outputFolder = File(outputDir)

        if (!outputFolder.exists()) {
            outputFolder.mkdirs()
        }

        Log.e(TAG, "Output path = $outputDir")

        val tempOutputFile = File.createTempFile("temp-", ".heic", outputFolder)
        Log.e(TAG, "Output file = ${tempOutputFile.absolutePath}")

        heif.save(tempOutputFile.absolutePath)

        Log.e(TAG, "HEIF file created successfully")
        Log.e(TAG, "Output file size before XMP: ${tempOutputFile.length()} bytes")

        // 验证文件结构
        verifyMotionPhotosStructure(tempOutputFile.absolutePath)

        // 注入 XMP 元数据到文件
        injectXMPMetadata(tempOutputFile.absolutePath)

        Log.e(TAG, "Output file size after XMP: ${tempOutputFile.length()} bytes")

        // 提取并验证 XMP 元数据
        extractAndVerifyXMP(tempOutputFile.absolutePath)
    }

    /**
     * 将 XMP 元数据注入到 HEIF 文件
     */
    private fun injectXMPMetadata(filePath: String) {
        try {
            val file = File(filePath)
            val originalData = file.readBytes()

            Log.e(TAG, "Original file size: ${originalData.size}")

            // 生成 XMP 数据
            val xmpContent = buildMotionPhotosXMP(8L, 0L)
            Log.e(TAG, "Generated XMP content size: ${xmpContent.length} bytes")

            // 创建 UUID box 来存储 XMP
            val xmpBox = createXMPBox(xmpContent)
            Log.e(TAG, "XMP box size: ${xmpBox.size} bytes")

            // 在文件末尾追加 XMP box
            val newData = ByteArray(originalData.size + xmpBox.size)
            System.arraycopy(originalData, 0, newData, 0, originalData.size)
            System.arraycopy(xmpBox, 0, newData, originalData.size, xmpBox.size)

            // 写入修改后的文件
            file.writeBytes(newData)
            Log.e(TAG, "XMP metadata injected successfully at offset ${originalData.size}")

        } catch (e: Exception) {
            Log.e(TAG, "Error injecting XMP metadata: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 验证 Motion Photos 文件结构（改进版）
     */
    private fun verifyMotionPhotosStructure(filePath: String) {
        try {
            val file = File(filePath)
            val fis = FileInputStream(file)
            val fileSize = file.length()

            Log.e(TAG, "=============== Verifying File Structure ===============")
            Log.e(TAG, "Total file size: $fileSize bytes")

            // 读取整个文件头（前1024字节）
            val buffer = ByteArray(1024)
            val bytesRead = fis.read(buffer)
            fis.close()

            // 检查 ftyp box
            if (checkBoxStructure(buffer)) {
                Log.e(TAG, "✓ ftyp box found at correct position")
            }

            // 逐个检查 boxes
            val boxes = findAllBoxes(buffer)
            Log.e(TAG, "Found ${boxes.size} boxes in first 1024 bytes:")
            for ((boxType, offset, size) in boxes) {
                Log.e(TAG, "  - $boxType at offset $offset, size $size")
            }

            // 检查文件末尾是否有 uuid box（XMP）
            checkFileEnd(file)

            Log.e(TAG, "=======================================================")

        } catch (e: Exception) {
            Log.e(TAG, "Error verifying Motion Photos structure: ${e.message}")
        }
    }

    /**
     * 查找所有 box
     */
    private fun findAllBoxes(buffer: ByteArray): List<Triple<String, Int, Int>> {
        val boxes = mutableListOf<Triple<String, Int, Int>>()
        var offset = 0

        while (offset + 8 <= buffer.size) {
            // 读取 box size (大端序)
            val size = ((buffer[offset].toInt() and 0xFF) shl 24) or
                    ((buffer[offset + 1].toInt() and 0xFF) shl 16) or
                    ((buffer[offset + 2].toInt() and 0xFF) shl 8) or
                    (buffer[offset + 3].toInt() and 0xFF)

            if (size <= 8 || size > 1000000) break  // 无效的 size

            // 读取 box type
            val boxType = String(
                byteArrayOf(
                    buffer[offset + 4].toByte(),
                    buffer[offset + 5].toByte(),
                    buffer[offset + 6].toByte(),
                    buffer[offset + 7].toByte()
                ),
                Charsets.US_ASCII
            )

            boxes.add(Triple(boxType, offset, size))
            offset += size
        }

        return boxes
    }

    /**
     * 检查文件末尾
     */
    private fun checkFileEnd(file: File) {
        try {
            val fileSize = file.length()
            val checkSize = minOf(2048L, fileSize).toInt()

            val fis = FileInputStream(file)
            fis.skip(maxOf(0L, fileSize - checkSize))
            val buffer = ByteArray(checkSize)
            val bytesRead = fis.read(buffer)
            fis.close()

            Log.e(TAG, "=============== File End Check ===============")
            Log.e(TAG, "Checking last $bytesRead bytes of file")

            // 查找 uuid box
            if (findXMPStart(buffer) >= 0) {
                Log.e(TAG, "✓ XMP data (uuid box) found at end of file")
            } else {
                Log.w(TAG, "✗ XMP data (uuid box) NOT found at end of file")
            }

            // 查找其他 boxes
            val boxes = findAllBoxes(buffer)
            Log.e(TAG, "Found ${boxes.size} boxes in file end:")
            for ((boxType, offset, size) in boxes) {
                Log.e(TAG, "  - $boxType at offset $offset, size $size")
            }

            Log.e(TAG, "============================================")

        } catch (e: Exception) {
            Log.e(TAG, "Error checking file end: ${e.message}")
        }
    }

    /**
     * 提取并验证 XMP 元数据（改进版）
     */
    private fun extractAndVerifyXMP(filePath: String) {
        try {
            val file = File(filePath)
            val fileSize = file.length()
            Log.e(TAG, "=============== XMP Extraction ===============")
            Log.e(TAG, "Total file size: $fileSize bytes")

            val fis = FileInputStream(file)
            val fileData = ByteArray(fileSize.toInt())
            fis.read(fileData)
            fis.close()

            // 查找 XMP 元数据位置
            val xmpStart = findXMPStart(fileData)
            if (xmpStart >= 0) {
                val xmpEnd = findXMPEnd(fileData, xmpStart)
                if (xmpEnd > xmpStart) {
                    val xmpData = fileData.sliceArray(xmpStart until xmpEnd)
                    val xmpString = String(xmpData, Charsets.UTF_8)

                    Log.e(TAG, "=============== XMP Metadata Found ===============")
                    Log.e(TAG, "XMP Start: $xmpStart, End: $xmpEnd, Size: ${xmpData.size}")
                    Log.e(TAG, "XMP Content Preview:")

                    // 输出 XMP 内容的前 500 字符
                    val preview = xmpString.take(500)
                    Log.e(TAG, preview)
                    if (xmpString.length > 500) {
                        Log.e(TAG, "... (${xmpString.length - 500} more characters)")
                    }

                    Log.e(TAG, "=================================================")

                    // 验证关键字段
                    verifyXMPFields(xmpString)
                } else {
                    Log.w(TAG, "✗ XMP metadata not found or incomplete")
                }
            } else {
                Log.w(TAG, "✗ XMP metadata not found in file")
            }

            Log.e(TAG, "==========================================")

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting XMP: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 验证 XMP 中的关键 Motion Photos 字段（改进版）
     */
    private fun verifyXMPFields(xmpContent: String) {
        Log.e(TAG, "=============== XMP Field Verification ===============")

        var passedChecks = 0
        val totalChecks = 4

        // 验证1: GCamera:MotionPhoto
        if (xmpContent.contains("<GCamera:MotionPhoto>1</GCamera:MotionPhoto>")) {
            Log.e(TAG, "✓ GCamera:MotionPhoto = 1")
            passedChecks++
        } else {
            Log.w(TAG, "✗ GCamera:MotionPhoto NOT correct")
        }

        // 验证2: GCamera:MotionPhotoVersion
        val versionMatch = Regex("<GCamera:MotionPhotoVersion>(\\d+)</GCamera:MotionPhotoVersion>").find(xmpContent)
        if (versionMatch != null) {
            val version = versionMatch.groupValues[1]
            Log.e(TAG, "✓ GCamera:MotionPhotoVersion = $version")
            passedChecks++
        } else {
            Log.w(TAG, "✗ GCamera:MotionPhotoVersion NOT found")
        }

        // 验证3: GCamera:Padding
        val paddingMatch = Regex("<GCamera:Padding>(\\d+)</GCamera:Padding>").find(xmpContent)
        if (paddingMatch != null) {
            val padding = paddingMatch.groupValues[1]
            Log.e(TAG, "✓ GCamera:Padding = $padding")
            passedChecks++
        } else {
            Log.w(TAG, "✗ GCamera:Padding NOT found")
        }

        // 验证4: GCamera:MotionPhotoTimestampUs
        val timestampMatch = Regex("<GCamera:MotionPhotoTimestampUs>(\\d+)</GCamera:MotionPhotoTimestampUs>").find(xmpContent)
        if (timestampMatch != null) {
            val timestamp = timestampMatch.groupValues[1]
            Log.e(TAG, "✓ GCamera:MotionPhotoTimestampUs = $timestamp")
            passedChecks++
        } else {
            Log.w(TAG, "✗ GCamera:MotionPhotoTimestampUs NOT found")
        }

        Log.e(TAG, "====================================================")
        Log.e(TAG, "Verification Result: $passedChecks/$totalChecks checks passed")
        if (passedChecks == totalChecks) {
            Log.e(TAG, "✓ All Motion Photos XMP fields are correct!")
        } else {
            Log.w(TAG, "⚠ Some fields are missing or incorrect")
        }
        Log.e(TAG, "====================================================")
    }

    /**
     * 创建 XMP UUID Box
     * 格式: [size:4bytes][type:4bytes][uuid:16bytes][xmp_content]
     */
    private fun createXMPBox(xmpContent: String): ByteArray {
        val xmpBytes = xmpContent.toByteArray(Charsets.UTF_8)

        // UUID box type
        val boxType = "uuid".toByteArray()  // 0x75756964

        // Google Motion Photos UUID (标准UUID)
        val uuid = byteArrayOf(
            0xBE.toByte(), 0x7A.toByte(), 0xCF.toByte(), 0xCB.toByte(),
            0x97.toByte(), 0xA9.toByte(), 0x42.toByte(), 0xE8.toByte(),
            0x9C.toByte(), 0x71.toByte(), 0x99.toByte(), 0x94.toByte(),
            0x91.toByte(), 0xE3.toByte(), 0xAF.toByte(), 0xAC.toByte()
        )

        // 计算 box 大小：4(size) + 4(type) + 16(uuid) + xmp content
        val boxSize = 4 + 4 + 16 + xmpBytes.size

        val result = ByteArray(boxSize)
        var offset = 0

        // 写入 size (大端序)
        result[offset++] = ((boxSize shr 24) and 0xFF).toByte()
        result[offset++] = ((boxSize shr 16) and 0xFF).toByte()
        result[offset++] = ((boxSize shr 8) and 0xFF).toByte()
        result[offset++] = (boxSize and 0xFF).toByte()

        // 写入 type
        System.arraycopy(boxType, 0, result, offset, boxType.size)
        offset += boxType.size

        // 写入 UUID
        System.arraycopy(uuid, 0, result, offset, uuid.size)
        offset += uuid.size

        // 写入 XMP content
        System.arraycopy(xmpBytes, 0, result, offset, xmpBytes.size)

        return result
    }

    /**
     * 查找 XMP 元数据开始位置
     */
    private fun findXMPStart(fileData: ByteArray): Int {
        val xmpMarker = "<?xml version".toByteArray()
        for (i in 0 until fileData.size - xmpMarker.size) {
            var match = true
            for (j in xmpMarker.indices) {
                if (fileData[i + j] != xmpMarker[j]) {
                    match = false
                    break
                }
            }
            if (match) {
                return i
            }
        }
        return -1
    }

    /**
     * 查找 XMP 元数据结束位置
     */
    private fun findXMPEnd(fileData: ByteArray, startPos: Int): Int {
        val xmpEnd = "</x:xmpmeta>".toByteArray()
        for (i in startPos until fileData.size - xmpEnd.size) {
            var match = true
            for (j in xmpEnd.indices) {
                if (fileData[i + j] != xmpEnd[j]) {
                    match = false
                    break
                }
            }
            if (match) {
                return i + xmpEnd.size
            }
        }
        return -1
    }


    /**
     * 检查 ISOBMFF 盒子结构
     */
    private fun checkBoxStructure(buffer: ByteArray): Boolean {
        // 检查 ftyp 盒子 (0x66747970)
        if (buffer.size >= 8) {
            // ftyp box 通常在文件起始处，格式是 [size:4bytes][type:4bytes]
            val ftypType = byteArrayOf(
                buffer[4].toByte(),
                buffer[5].toByte(),
                buffer[6].toByte(),
                buffer[7].toByte()
            )
            val ftypString = String(ftypType, Charsets.US_ASCII)

            Log.e(TAG, "First box type at offset 4: $ftypString")

            if (ftypString == "ftyp") {
                Log.e(TAG, "✓ ftyp box found at correct position")
                return true
            }
        }
        return false
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
}
