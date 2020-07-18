@file:JvmName("SystemDownloadUtils")

package top.xuqingquan.web.utils

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.support.v4.content.ContextCompat
import top.xuqingquan.utils.getCacheFilePath
import top.xuqingquan.web.nokernel.DownLoadBroadcast
import top.xuqingquan.web.nokernel.WebUtils
import java.io.File

/**
 * Created by 许清泉 on 2020/6/22 14:22
 */

@Throws(Throwable::class)
fun download(context: Context, fileName: String, url: String) {
    val downloadPath = getCacheFilePath(context)
    val downloadFile = File(downloadPath, fileName)
    if (downloadFile.exists()) {
        val mIntent = WebUtils.getCommonFileIntentCompat(context, downloadFile)
        mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(mIntent)
        return
    }
    val downloadManager = ContextCompat.getSystemService(context, DownloadManager::class.java)
    if (downloadManager != null) {
        val request = DownloadManager.Request(Uri.parse(url))
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalFilesDir(context, "/", fileName)
        downloadManager.enqueue(request)
        val intentFilter = IntentFilter()
        intentFilter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        context.registerReceiver(DownLoadBroadcast(), intentFilter)
    }
}