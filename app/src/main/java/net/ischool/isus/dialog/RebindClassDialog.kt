package net.ischool.isus.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import net.ischool.isus.R
import net.ischool.isus.databinding.DialogRebindClassBinding
import net.ischool.isus.model.Organization
import net.ischool.isus.preference.PreferenceManager

/**
 * 重新绑定班级对话框
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2024/1/16
 */
class RebindClassDialog (
    context: Context,
    private val organizations: List<Organization>
) : Dialog(context, R.style.AlertDialogStyle) {
    companion object {
        private const val TAG = "RebindClassDialog"
    }

    init {
        setCanceledOnTouchOutside(false)
    }

    private lateinit var binding: DialogRebindClassBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogRebindClassBinding.inflate(layoutInflater, null, false)
        setContentView(binding.root)
        window?.attributes?.width = ViewGroup.LayoutParams.MATCH_PARENT
        window?.attributes?.height = ViewGroup.LayoutParams.MATCH_PARENT
        initUI()
    }

    private fun initUI() {
        binding.save.setOnClickListener {  }
        binding.cancel.setOnClickListener { dismiss() }
        organizations.firstOrNull { "${it.id}" == PreferenceManager.instance.getClassId() }?.let {
            binding.currentClass.text = it.name
        }
    }
}