package top.xuqingquan.web.publics

import android.webkit.WebView
import top.xuqingquan.web.nokernel.BaseIndicatorSpec

import com.tencent.smtt.sdk.WebView as X5WebView

class IndicatorHandler : IndicatorController {
    private var mBaseIndicatorSpec: BaseIndicatorSpec? = null

    override fun progress(v: WebView, newProgress: Int) {
        progress(newProgress)
    }

    override fun progress(v: X5WebView, newProgress: Int) {
        progress(newProgress)
    }

    private fun progress(newProgress: Int) {
        when (newProgress) {
            0 -> reset()
            in 1..10 -> showIndicator()
            in 11..94 -> setProgress(newProgress)
            else -> {
                setProgress(newProgress)
                finish()
            }
        }
    }

    override fun offerIndicator(): BaseIndicatorSpec {
        return this.mBaseIndicatorSpec!!
    }

    fun reset() {
        mBaseIndicatorSpec?.reset()
    }

    override fun finish() {
        mBaseIndicatorSpec?.hide()
    }

    override fun setProgress(newProgress: Int) {
        mBaseIndicatorSpec?.setProgress(newProgress)
    }

    override fun showIndicator() {
        mBaseIndicatorSpec?.show()
    }


    fun inJectIndicator(baseIndicatorSpec: BaseIndicatorSpec): IndicatorHandler {
        this.mBaseIndicatorSpec = baseIndicatorSpec
        return this
    }

    companion object {

        @JvmStatic
        val instance: IndicatorHandler
            get() = IndicatorHandler()
    }
}
