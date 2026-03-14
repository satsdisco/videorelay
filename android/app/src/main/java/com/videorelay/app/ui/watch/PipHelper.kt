package com.videorelay.app.ui.watch

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational

/**
 * Picture-in-Picture support for video playback.
 */
object PipHelper {
    fun enterPip(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            activity.enterPictureInPictureMode(params)
        }
    }
}
