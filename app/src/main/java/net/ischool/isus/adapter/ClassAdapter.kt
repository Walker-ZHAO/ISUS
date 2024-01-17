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
 * 班级适配器
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2024/1/16
 */
class ClassAdapter(val context: Context, val onItemClick: (Organization) -> Unit): RecyclerView.Adapter<ClassAdapter.ViewHolder>() {
    private val inflater = LayoutInflater.from(context)

    // 班级列表
    private val classes: MutableList<Organization> = mutableListOf()
    // 当前选中的班级ID
    private var currentClassId: Int = 0

    inner class ViewHolder(private val binding: ViewTextItemBinding): RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(item: Organization) {
            binding.content.text = item.name
            if (item.id == currentClassId) {
                binding.bg.setBackgroundResource(R.drawable.blue_corner_rect)
                binding.content.setTextColor(context.resources.getColor(R.color.white))
            } else {
                binding.bg.setBackgroundResource(R.drawable.purple_corner_rect)
                binding.content.setTextColor(context.resources.getColor(R.color.title_gray))
            }
            binding.rootView.setOnClickListener {
                currentClassId = item.id
                onItemClick(item)
                notifyDataSetChanged()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassAdapter.ViewHolder {
        return ViewHolder(ViewTextItemBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ClassAdapter.ViewHolder, position: Int) {
        holder.bind(classes[position])
    }

    override fun getItemCount() = classes.size

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<Organization>, initialOrganization: Organization?) {
        classes.clear()
        classes.addAll(data)
        currentClassId = initialOrganization?.id ?: 0
        data.firstOrNull { it.id == currentClassId }?.let {
            onItemClick(it)
        }
        notifyDataSetChanged()
    }
}