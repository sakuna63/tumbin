package com.sakuna63.tumbin.application.util

import android.text.TextUtils
import android.view.View

object DataBindingUtils {
    @JvmStatic
    fun goneIfEmpty(str: String?): Int = if (TextUtils.isEmpty(str)) View.GONE else View.VISIBLE

    @JvmStatic
    fun visibleIfNull(obj: Any?): Int = if (obj == null) View.VISIBLE else View.GONE

    @JvmStatic
    fun goneIfNull(obj: Any?): Int = if (obj == null) View.GONE else View.VISIBLE

    @JvmStatic
    fun goneIfZero(value: Int): Int = if (value == 0) View.GONE else View.VISIBLE
}
