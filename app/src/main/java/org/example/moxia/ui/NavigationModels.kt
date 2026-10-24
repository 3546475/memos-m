package org.example.moxia.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MemoKey(val id: String, val fromSearch: Boolean = false) : Parcelable
