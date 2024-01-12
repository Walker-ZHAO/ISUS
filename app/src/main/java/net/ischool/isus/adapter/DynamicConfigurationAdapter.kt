package net.ischool.isus.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.ischool.isus.databinding.ViewDynamicConfigurationItemBinding

/**
 * 动态配置适配器
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2024/1/12
 */
class DynamicConfigurationAdapter(val context: Context): RecyclerView.Adapter<DynamicConfigurationAdapter.ConfigHolder>() {
    private val inflater = LayoutInflater.from(context)

    // 配置项列表
    private val configurations = mutableListOf<Pair<String, String>>()

    inner class ConfigHolder(private val binding: ViewDynamicConfigurationItemBinding): RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(item: Pair<String, String>) {
            binding.title.text = item.first
            binding.content.text = item.second
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DynamicConfigurationAdapter.ConfigHolder {
        return ConfigHolder(ViewDynamicConfigurationItemBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: DynamicConfigurationAdapter.ConfigHolder, position: Int) {
        holder.bind(configurations[position])
    }

    override fun getItemCount() = configurations.size

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<Pair<String, String>>) {
        configurations.clear()
        configurations.addAll(data)
        notifyDataSetChanged()
    }
}