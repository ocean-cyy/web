package top.xuqingquan.web.nokernel

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import top.xuqingquan.utils.Timber
import top.xuqingquan.utils.getPath
import top.xuqingquan.web.nokernel.WebUtils.getCommonFileIntentCompat
import java.io.File

class DownLoadBroadcast : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            val downloadManager =
                ContextCompat.getSystemService(context, DownloadManager::class.java)
            val uri = downloadManager?.getUriForDownloadedFile(downloadId)
            Timber.d("url===>$uri")
            uri?.let {
                val path = getPath(context, uri)
                if (!path.isNullOrEmpty()) {
                    val mIntent = getCommonFileIntentCompat(context, File((path)))
                    mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(mIntent)
                }
            }
            context.unregisterReceiver(this)
        }
    }
}
