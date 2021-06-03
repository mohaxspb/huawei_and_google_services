package ru.kuchanov.huaweiandgoogleservices

import android.util.Log
import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage

class DemoHmsMessageService : HmsMessageService() {

    override fun onNewToken(token: String?) {
        super.onNewToken(token)
        Log.d(TAG, "onNewToken: $token")
    }

    override fun onMessageReceived(message: RemoteMessage?) {
        super.onMessageReceived(message)
        Log.i(TAG, "onMessageReceived() data: ")
        message?.dataOfMap?.let {
            for (data in it) {
                Log.i(TAG, "key: ${data.key} - value: ${data.value};")
            }
        }
    }

    companion object {
        const val TAG = "DemoHmsMessageService"
    }
}