package top.xuqingquan.sample

import android.app.Application
import top.xuqingquan.utils.Timber
import top.xuqingquan.web.nokernel.initAdblock

/**
 * Create by 许清泉 on 2020/8/22 22:17
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
//        initTbs(this)
        initAdblock(this, engineCreatedListener = {
            val url = arrayListOf<String>()
            it.listedSubscriptions.forEach { sub ->
                url.add(sub.url)
            }
            url.add("https://cdn.adblockcdn.com/filters/adblock_custom.txt")
            it.setSubscriptions(url)
        })
    }

}