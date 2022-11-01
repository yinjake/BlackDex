package com.yinsheng.blackdex.view.setting

import android.os.Bundle
import com.yinsheng.blackdex.R
import com.yinsheng.blackdex.databinding.ActivitySettingBinding
import com.yinsheng.blackdex.util.inflate
import com.yinsheng.blackdex.view.base.BaseActivity
import com.yinsheng.blackdex.view.base.PermissionActivity

class SettingActivity : PermissionActivity() {

    private val viewBinding: ActivitySettingBinding by inflate()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        initToolbar(viewBinding.toolbarLayout.toolbar, R.string.app_setting,true)
        supportFragmentManager.beginTransaction().replace(R.id.fragment,SettingFragment()).commit()
    }

    fun setRequestCallback(callback:((Boolean)->Unit)?){
        this.requestPermissionCallback = callback
        requestStoragePermission()
    }
}