package top.xuqingquan.web.nokernel

interface BaseIndicatorSpec {

    fun show()

    fun hide()

    fun reset()

    fun setProgress(newProgress: Int)
}