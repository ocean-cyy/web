package top.xuqingquan.web.nokernel

interface Provider<T> {
    fun provide(): T?
}
