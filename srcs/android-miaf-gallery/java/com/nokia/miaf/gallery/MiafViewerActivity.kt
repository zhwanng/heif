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

import android.os.Bundle
import android.os.Environment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.util.Log
import com.nokia.heif.Exception
import com.nokia.heif.HEIF
import com.nokia.heif.HEVCDecoderConfig
import com.nokia.heif.HEVCImageItem
import com.nokia.heif.HEVCSample
import com.nokia.heif.ImageSequence
import com.nokia.heif.JPEGDecoderConfig
import com.nokia.heif.JPEGImageItem
import com.nokia.heif.Size
import com.nokia.heif.VideoSample
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

    private val MEDIA_ROOT_PATH = Environment.getExternalStorageDirectory().absolutePath + "/miaf-files/"

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
            createImageVideo()
        }
    }

    override fun onResume() {
        super.onResume()
        val folder = File(MEDIA_ROOT_PATH)
        mPagerAdapter = ImagePageAdapter(supportFragmentManager, loadFolder(folder))
        binding.viewPager.adapter = mPagerAdapter
    }


    fun createImageVideo() {
        Log.e(TAG, "start create image sequence")
        val outputFolderPath = "$MEDIA_ROOT_PATH/miaf-files-output/"
        val outputFolder = File(outputFolderPath)
        if (!outputFolder.exists()) {
            outputFolder.mkdir()
        }
        Log.e(TAG, "output path = ${outputFolderPath}")

        val outputFile = "$outputFolderPath/image_video.heic"

        val heif = HEIF()
        val byte0 = byteArrayOf(0)
        val jpegByte = readFileToByteArray("$MEDIA_ROOT_PATH/o.jpg")
        val jpegDecoderConfig = JPEGDecoderConfig(heif, byte0);

        val primaryImage = JPEGImageItem(heif, Size(1080, 1920), jpegDecoderConfig, jpegByte)
        heif.primaryImage = primaryImage;

        val mp4Data = readFileToByteArray("$MEDIA_ROOT_PATH/o.mp4")
        val timescale = 1000;
        val videoTrack = VideoTrack(heif, timescale)
        val videoDecoderConfig = extractVideoConfigFromMP4(mp4Data)
        val hevcDecoderConfig = HEVCDecoderConfig(heif, videoDecoderConfig)

        val videoSample = HEVCSample(heif, hevcDecoderConfig, mp4Data, 1000)
        videoTrack.addSample(videoSample)

        videoTrack.timescale = timescale
        heif.tracks.add(videoTrack)

        heif.save(outputFile)
        Log.e(TAG, "create image video success")

    }

    fun createImageSequence() {
        Log.e(TAG, "start create image sequence")
        val outputFolderPath = "$MEDIA_ROOT_PATH/miaf-files-output/"
        val outputFolder = File(outputFolderPath)
        if (!outputFolder.exists()) {
            outputFolder.mkdir()
        }
        Log.e(TAG, "output path = ${outputFolderPath}")

        val outputFile = "$outputFolderPath/imageSeq.heic"


        // These should contain the encoded image data and the corresponding decoder config data
        val width = 1080
        val height = 1920

        // Sizes are just placeholders
        val decoderConfigData = ByteArray(1024) // fake data
        val imageData = ByteArray(50000) // fake data

        // !!!! this is fake data - initiate list of fake image sequence sample data, normally you would get this from video encoder.
        val imageSequenceSampleDatas: MutableList<ByteArray?> = java.util.ArrayList<ByteArray?>()
        imageSequenceSampleDatas.add(imageData) // first image sequence sample is same as still image
        for (i in 0..6) {
            val imageSequenceSampleData = ByteArray(50001) // fake data
            imageSequenceSampleDatas.add(imageSequenceSampleData)
        }

        val sampleDuration = 200
        val timescale = 1000

        // Create an instance of the HEIF library,
        val heif = HEIF()
        try {
            // This example assumes that the data is HEVC

            val imageSeq = ImageSequence(heif, timescale)
            val decoderConfig = HEVCDecoderConfig(heif, decoderConfigData)
            for (i in 0..7) {
                val imageSeqSample = HEVCSample(heif, decoderConfig, imageSequenceSampleDatas.get(i), sampleDuration.toLong())
                imageSeq.addSample(imageSeqSample)
            }

            // The constructor requires the HEIF instance, the size of the image,
            // the decoder config data and the image data
            val imageItem = HEVCImageItem(heif, Size(width, height), decoderConfig, imageData)
            // Every HEIF image should have a primary image
            heif.setPrimaryImage(imageItem)

            // The brands need to be set
            heif.setMajorBrand(HEIF.BRAND_MSF1)
            heif.addCompatibleBrand(HEIF.BRAND_HEVC)
            heif.addCompatibleBrand(HEIF.BRAND_HEIC)
            heif.addCompatibleBrand(HEIF.BRAND_MIF1)
            heif.addCompatibleBrand(HEIF.BRAND_ISO8)

            // And we save the file
            heif.save(outputFile) // this will fail with fake data above - with real data it generates output file.
        } // All exceptions thrown by the HEIF library are of the same type
        // Check the error code to see what happened
        catch (e: Exception) {
            e.printStackTrace()
        }
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
     * 从MP4文件中提取视频解码器配置
     * 注意：这只是一个占位符方法，在实际应用中需要正确解析MP4文件以提取配置
     */
    private fun extractVideoConfigFromMP4(mp4Data: ByteArray?): ByteArray {
        // 这里应该解析MP4文件并提取AVC/HEVC配置信息
        // 返回示例配置数据
        return VideoConfigUtils.extractVideoConfigFromMP4Data(mp4Data);
    }

}
