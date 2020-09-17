package top.xuqingquan.web.nokernel

/**
 * Created by 许清泉 on 2020/9/17 22:41
 */
interface AdblockCallback {

    /**
     * 增加一条拦截记录
     */
    fun addBlockCount(url: String)


}