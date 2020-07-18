package top.xuqingquan.web.nokernel

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

abstract class BaseIndicatorView<T : FrameLayout.LayoutParams> : FrameLayout, BaseIndicatorSpec, LayoutParamsOffer<T> {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun reset() {}

    override fun setProgress(newProgress: Int) {}

    override fun show() {}

    override fun hide() {}
}
