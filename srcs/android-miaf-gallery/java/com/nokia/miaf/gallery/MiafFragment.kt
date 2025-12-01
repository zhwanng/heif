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
import android.support.v4.app.Fragment
import android.view.*

import com.nokia.heif.*
import com.nokia.heif.utility.miaf.MIAFReader
import com.nokia.miaf.gallery.databinding.FragmentMiafBinding
import java.io.File
import java.util.HashSet

class MiafFragment : Fragment() {
    private val mMIAF: MIAFReader = MIAFReader()
    private var mLoaded = false
    private var mFilename: String? = null
    private var mCurrentDisplayed: Base? = null

    private var mIsVisible = false
    private lateinit var binding: FragmentMiafBinding;

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMiafBinding.inflate(inflater, container, false)
        return binding.root;
    }

    override fun onResume() {
        super.onResume()
        if (!mLoaded) {
            loadImage()
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        mIsVisible = isVisibleToUser
    }

    private fun updateDisplayImages() {
        val constraints = MIAFReader.Constraints()
        constraints.preference = MIAFReader.Preference.TRACK

        val roles = HashSet<MIAFReader.OutputRole>()
        roles.add(MIAFReader.OutputRole.MASTER)

        val content = mMIAF.getContent(constraints, roles)
        binding.miafView.setContent(content.master)
        mCurrentDisplayed = content.master
    }

    fun loadHEIFImage(file: File) {
        mFilename = file.absolutePath
        mMIAF.load(mFilename)
    }

    private fun loadImage() {
        updateDisplayImages()
        registerForContextMenu(binding.miafView)
        binding.miafView.setOnClickListener { togglePlayback() }
        binding.miafView.setOnLongClickListener {
            binding.miafView.showContextMenu()
            return@setOnLongClickListener true
        }
    }

    private fun togglePlayback() {
        if (mCurrentDisplayed is VideoTrack) {
            binding.miafView.togglePlayback()
        }
    }
}
