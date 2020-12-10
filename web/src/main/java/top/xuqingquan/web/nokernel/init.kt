package top.xuqingquan.web.nokernel

import android.content.Context
import com.tencent.smtt.export.external.TbsCoreSettings
import com.tencent.smtt.sdk.QbSdk
import com.tencent.smtt.sdk.TbsListener
import org.adblockplus.libadblockplus.android.AdblockEngineProvider
import org.adblockplus.libadblockplus.android.settings.AdblockHelper
import top.xuqingquan.utils.Timber
import top.xuqingquan.web.R

/**
 * Created by 许清泉 on 2020/7/7 22:24
 */
/**
 * tbs初始化
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
            if (p0) {
                WebConfig.tbsStatus = true
            }
            Timber.d("QbSdk----onViewInitFinished--->$p0")
        }
    }
    //x5内核初始化接口
    QbSdk.initX5Environment(context, callback)
}

/**
 * 广告引擎初始化
 */
fun initAdblock(
    context: Context,
    urlToResourceIdMap: Map<String, Int>? = null,
    engineCreatedListener: AdblockEngineProvider.EngineCreatedListener? = null,
    engineDisposedListener: AdblockEngineProvider.EngineDisposedListener? = null
) {
    if (AdblockHelper.get().isInit) {
        return
    }
    val map = urlToResourceIdMap
        ?: mapOf(
            "https://easylist-downloads.adblockplus.org/easylistchina+easylist.txt" to R.raw.easylistchina_easylist,//中文规则
            "https://easylist-downloads.adblockplus.org/exceptionrules.txt" to R.raw.exceptionrules,//可接受广告
            "https://cdn.adblockcdn.com/filters/adblock_custom.txt" to R.raw.adblock_custom,//自定义规则
            "https://easylist-downloads.adblockplus.org/abp-filters-anti-cv.txt" to R.raw.abp_filters_anti_cv//反规避拦截规则
        )
    val provider =
        AdblockHelper.get().init(context, context.cacheDir.absolutePath, "adblock")
    provider.preloadSubscriptions("rules", map)
    if (engineCreatedListener != null) {
        provider.addEngineCreatedListener(engineCreatedListener)
    } else {
        provider.addEngineCreatedListener {
            Timber.d("addEngineCreatedListener--${it.isEnabled}")
        }
    }
    if (engineDisposedListener != null) {
        provider.addEngineDisposedListener(engineDisposedListener)
    } else {
        provider.addEngineDisposedListener {
            Timber.d("addEngineDisposedListener")
        }
    }
    provider.provider.retain(true)
}