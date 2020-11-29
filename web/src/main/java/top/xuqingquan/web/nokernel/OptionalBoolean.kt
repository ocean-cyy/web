package top.xuqingquan.web.nokernel

/**
 * Created by 许清泉 on 11/29/20 7:40 PM
 * Optional boolean value.
 * Puts 2 dimensions (having value/no value + true/false) into 1 dimension
 * to achieve atomic comparisons and null-safety.
 */
enum class OptionalBoolean {
    /**
     * No value (equal to "null")
     */
    UNDEFINED,

    /**
     * Having a value and it's True
     */
    TRUE,

    /**
     * Having a value and it's False
     */
    FALSE;

    companion object {
        /**
         * Convenience method to get enum value from boolean value
         *
         * @param value boolean value
         * @return enum value
         */
        @JvmStatic
        fun from(value: Boolean): OptionalBoolean {
            return if (value) TRUE else FALSE
        }
    }
}