package top.xuqingquan.web.nokernel

import android.widget.FrameLayout

interface LayoutParamsOffer<T : FrameLayout.LayoutParams> {

    fun offerLayoutParams(): T

}
