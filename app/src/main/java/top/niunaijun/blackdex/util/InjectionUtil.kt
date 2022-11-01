package com.yinsheng.blackdex.util

import com.yinsheng.blackdex.data.DexDumpRepository
import com.yinsheng.blackdex.view.main.MainFactory


/**
 *
 * @Description:
 * @Author: wukaicheng
 * @CreateDate: 2021/4/29 22:38
 */
object InjectionUtil {

    private val dexDumpRepository = DexDumpRepository()


    fun getMainFactory() : MainFactory {
        return MainFactory(dexDumpRepository)
    }

}