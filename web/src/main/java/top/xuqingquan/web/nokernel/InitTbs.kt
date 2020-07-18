package top.xuqingquan.web.nokernel

import android.content.Context
import com.tencent.smtt.export.external.TbsCoreSettings
import com.tencent.smtt.sdk.QbSdk
import com.tencent.smtt.sdk.TbsListener
import top.xuqingquan.utils.Timber

/**
 * Created by 许清泉 on 2020/7/7 22:24
 */
fun initTbs(
    context: Context,
    downloadWithoutWifi: Boolean = true,
    cb: QbSdk.PreInitCallback? = null
) {
    Timber.d("QbSdk----Thread.currentThread()===${Thread.currentThread()}")
    QbSdk.initTbsSettings(
        mapOf(
            TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER to true,
            TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE to true
        )
    )
    QbSdk.setDownloadWithoutWifi(downloadWithoutWifi)
    QbSdk.setTbsListener(object : TbsListener {
        override fun onInstallFinish(p0: Int) {
            Timber.d("QbSdk----onInstallFinish--->$p0")
        }

        override fun onDownloadFinish(p0: Int) {
            Timber.d("QbSdk----onDownloadFinish--->$p0")
        }

        override fun onDownloadProgress(p0: Int) {
            Timber.d("QbSdk----onDownloadProgress--->$p0")
        }
    })
    val callback = cb ?: object : QbSdk.PreInitCallback {
        override fun onCoreInitFinished() {
            Timber.d("QbSdk----onCoreInitFinished")

        }

        override fun onViewInitFinished(p0: Boolean) {
            WebConfig.x5 = p0
            Timber.d("QbSdk----onViewInitFinished--->$p0")
        }
    }
    //x5内核初始化接口
    QbSdk.initX5Environment(context, callback)
}