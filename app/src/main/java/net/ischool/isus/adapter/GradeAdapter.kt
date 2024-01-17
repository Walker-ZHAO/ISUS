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
 * 年级适配器
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2024/1/16
 */
class GradeAdapter(val context: Context, val onItemClick: (List<Organization>, Organization?) -> Unit): RecyclerView.Adapter<GradeAdapter.ViewHolder>() {
    private val inflater = LayoutInflater.from(context)

    // 年级列表
    private val grades: MutableList<Pair<Int, List<Organization>>> = mutableListOf()
    // 当前选中的年级
    private var currentGrade: Int = 0

    inner class ViewHolder(private val binding: ViewTextItemBinding): RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(item: Pair<Int, List<Organization>>) {
            binding.content.text = "${item.first}级"
            if (item.first == currentGrade) {
                binding.bg.setBackgroundResource(R.drawable.blue_corner_rect)
                binding.content.setTextColor(context.resources.getColor(R.color.white))
            } else {
                binding.bg.setBackgroundResource(R.drawable.purple_corner_rect)
                binding.content.setTextColor(context.resources.getColor(R.color.title_gray))
            }
            binding.rootView.setOnClickListener {
                currentGrade = item.first
                onItemClick(item.second, null)
                notifyDataSetChanged()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GradeAdapter.ViewHolder {
        return ViewHolder(ViewTextItemBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: GradeAdapter.ViewHolder, position: Int) {
        holder.bind(grades[position])
    }

    override fun getItemCount() = grades.size

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<Pair<Int, List<Organization>>>, initialOrganization: Organization?) {
        grades.clear()
        grades.addAll(data)
        currentGrade = initialOrganization?.beginYear ?: 0
        data.firstOrNull { it.first == currentGrade }?.let {
            onItemClick(it.second, initialOrganization)
        }
        notifyDataSetChanged()
    }
}