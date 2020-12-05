package top.xuqingquan.sample

import android.app.Application
import top.xuqingquan.utils.Timber
import top.xuqingquan.web.nokernel.initAdblock
import top.xuqingquan.web.nokernel.initTbs

/**
 * Create by 许清泉 on 2020/8/22 22:17
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
//        initAdblock(this)
//        initTbs(this)
    }

}