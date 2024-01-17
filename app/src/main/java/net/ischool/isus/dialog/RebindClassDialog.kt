package net.ischool.isus.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import net.ischool.isus.R
import net.ischool.isus.adapter.ClassAdapter
import net.ischool.isus.adapter.EducationTypeAdapter
import net.ischool.isus.adapter.GradeAdapter
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

    private val educationTypeAdapter by lazy { EducationTypeAdapter(context, ::onEducationTypeChoose) }
    private val gradeAdapter by lazy { GradeAdapter(context, ::onGradeChoose) }
    private val classAdapter by lazy { ClassAdapter(context, ::onClassChoose) }

    init {
        setCanceledOnTouchOutside(false)
    }

    private lateinit var binding: DialogRebindClassBinding
    // 当前选中的组织
    private var currentOrganization: Organization? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogRebindClassBinding.inflate(layoutInflater, null, false)
        setContentView(binding.root)
        window?.attributes?.width = ViewGroup.LayoutParams.MATCH_PARENT
        window?.attributes?.height = ViewGroup.LayoutParams.MATCH_PARENT

        initUI()
        parseData()
    }

    @SuppressLint("SetTextI18n")
    private fun initUI() {
        binding.save.setOnClickListener {
            Log.i(TAG, "choose organization: $currentOrganization")
        }
        binding.cancel.setOnClickListener { dismiss() }

        currentOrganization = organizations.firstOrNull { "${it.id}" == PreferenceManager.instance.getClassId() }
        currentOrganization?.let {
            binding.currentClass.text = "${it.readebleEducationType} ${it.name}"
        }

        // 学段信息
        binding.educationRv.apply {
            layoutManager = GridLayoutManager(context, 8, LinearLayoutManager.VERTICAL, false)
            adapter = educationTypeAdapter
        }
        // 年级信息
        binding.gradeRv.apply {
            layoutManager = GridLayoutManager(context, 8, LinearLayoutManager.VERTICAL, false)
            adapter = gradeAdapter
        }
        // 班级信息
        binding.classRv.apply {
            layoutManager = GridLayoutManager(context, 8, LinearLayoutManager.VERTICAL, false)
            adapter = classAdapter
        }
    }

    private fun parseData() {
        val mapping: MutableList<Pair<String, List<Pair<Int, List<Organization>>>>> = mutableListOf()
        // 先按学段分组
        organizations.groupBy { it.readebleEducationType }.toList().forEach { pair ->
            // 再按年级分组
            val gradeGroup = pair.second.sortedBy { it.beginYear }.groupBy { it.beginYear }.toList()
            // 添加到映射表中
            mapping.add(Pair(pair.first, gradeGroup))
        }
        educationTypeAdapter.setData(mapping, currentOrganization)
    }

    private fun onEducationTypeChoose(grades: List<Pair<Int, List<Organization>>>, initialOrganization: Organization?) {
        currentOrganization = null
        gradeAdapter.setData(grades, initialOrganization)
    }

    private fun onGradeChoose(classes: List<Organization>, initialOrganization: Organization?) {
        currentOrganization = null
        classAdapter.setData(classes, initialOrganization)
    }

    private fun onClassChoose(organization: Organization) {
        currentOrganization = organization
    }
}