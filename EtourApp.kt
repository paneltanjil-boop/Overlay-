package com.etour.app

import android.app.Application
import com.onesignal.OneSignal

class EtourApp : Application() {
    override fun onCreate() {
        super.onCreate()
        OneSignal.initWithContext(this, "cc3ee16e-2486-46b4-86de-f5118a66f03a")
    }
}
