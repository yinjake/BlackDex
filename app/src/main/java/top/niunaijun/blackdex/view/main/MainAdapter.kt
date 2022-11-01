package com.yinsheng.blackdex.view.main

import android.view.ViewGroup
import com.yinsheng.blackdex.data.entity.AppInfo
import com.yinsheng.blackdex.databinding.ItemPackageBinding
import com.yinsheng.blackdex.util.newBindingViewHolder
import com.yinsheng.blackdex.view.base.BaseAdapter

/**
 *
 * @Description: 软件显示界面适配器
 * @Author: wukaicheng
 * @CreateDate: 2021/4/29 21:52
 */

class MainAdapter : BaseAdapter<ItemPackageBinding, AppInfo>() {
    override fun getViewBinding(parent: ViewGroup): ItemPackageBinding {
        return newBindingViewHolder(parent, false)

    }

    override fun initView(binding: ItemPackageBinding, position: Int, data: AppInfo) {
        binding.icon.setImageDrawable(data.icon)
        binding.name.text = data.name
        binding.packageName.text = data.packageName
    }
}