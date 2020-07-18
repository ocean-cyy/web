package top.xuqingquan.web.nokernel

interface IWebIndicator<T : BaseIndicatorSpec> {

    fun offer(): T
}
