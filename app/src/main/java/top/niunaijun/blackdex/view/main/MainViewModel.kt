package com.yinsheng.blackdex.view.main

import androidx.lifecycle.MutableLiveData
import com.yinsheng.blackdex.data.DexDumpRepository
import com.yinsheng.blackdex.data.entity.AppInfo
import com.yinsheng.blackdex.data.entity.DumpInfo
import com.yinsheng.blackdex.view.base.BaseViewModel

/**
 *
 * @Description:
 * @Author: wukaicheng
 * @CreateDate: 2021/5/23 14:29
 */
class MainViewModel(private val repo: DexDumpRepository) : BaseViewModel() {

    val mAppListLiveData = MutableLiveData<List<AppInfo>>()

    val mDexDumpLiveData = MutableLiveData<DumpInfo>()


    fun getAppList() {
        launchOnUI {
            repo.getAppList(mAppListLiveData)
        }
    }

    fun startDexDump(source: String) {
        launchOnUI {
            repo.dumpDex(source, mDexDumpLiveData)
        }
    }

    fun dexDumpSuccess() {
        launchOnUI {
            repo.dumpSuccess()
        }
    }

}