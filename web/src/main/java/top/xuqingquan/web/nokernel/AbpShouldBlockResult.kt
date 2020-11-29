package top.xuqingquan.web.nokernel

/**
 * Created by 许清泉 on 11/29/20 7:51 PM
 */
enum class AbpShouldBlockResult {
    // FilterEngine is released or
    // ABP enabled state is unknown or
    // ABP enabled state is "disabled"
    NOT_ENABLED,  // Allow loading (with further sitekey-related routines)
    ALLOW_LOAD,  // Allow loading
    ALLOW_LOAD_NO_SITEKEY_CHECK,  // Block loading
    BLOCK_LOAD
}