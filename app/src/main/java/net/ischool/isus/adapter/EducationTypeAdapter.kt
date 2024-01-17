package net.ischool.isus.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.ischool.isus.R
import net.ischool.isus.databinding.ViewTextItemBinding
import net.ischool.isus.model.Organization

/**
 * 学段适配器
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2024/1/16
 */
class EducationTypeAdapter(val context: Context, val onItemClick: (List<Pair<Int, List<Organization>>>, Organization?) -> Unit): RecyclerView.Adapter<EducationTypeAdapter.ViewHolder>() {
    private val inflater = LayoutInflater.from(context)

    // 学段列表
    private val educationTypes: MutableList<Pair<String, List<Pair<Int, List<Organization>>>>> = mutableListOf()
    // 当前选中的学段
    private var currentEducationType: String = ""

    inner class ViewHolder(private val binding: ViewTextItemBinding): RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(item: Pair<String, List<Pair<Int, List<Organization>>>>) {
            binding.content.text = item.first
            if (item.first == currentEducationType) {
                binding.bg.setBackgroundResource(R.drawable.blue_corner_rect)
                binding.content.setTextColor(context.resources.getColor(R.color.white))
            } else {
                binding.bg.setBackgroundResource(R.drawable.purple_corner_rect)
                binding.content.setTextColor(context.resources.getColor(R.color.title_gray))
            }
            binding.rootView.setOnClickListener {
                currentEducationType = item.first
                onItemClick(item.second, null)
                notifyDataSetChanged()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EducationTypeAdapter.ViewHolder {
        return ViewHolder(ViewTextItemBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: EducationTypeAdapter.ViewHolder, position: Int) {
        holder.bind(educationTypes[position])
    }

    override fun getItemCount() = educationTypes.size

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<Pair<String, List<Pair<Int, List<Organization>>>>>, initialOrganization: Organization?) {
        educationTypes.clear()
        educationTypes.addAll(data)
        // 设置初始选中的学段
        currentEducationType = initialOrganization?.readebleEducationType ?: ""
        data.firstOrNull { it.first == currentEducationType }?.let {
            onItemClick(it.second, initialOrganization)
        }
        notifyDataSetChanged()
    }
}