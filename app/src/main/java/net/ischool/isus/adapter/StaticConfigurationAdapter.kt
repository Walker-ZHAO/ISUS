package net.ischool.isus.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.ischool.isus.databinding.ViewStaticConfigurationItemBinding

/**
 * 静态配置适配器
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2024/1/12
 */
class StaticConfigurationAdapter(val context: Context): RecyclerView.Adapter<StaticConfigurationAdapter.ConfigHolder>() {
    private val inflater = LayoutInflater.from(context)

    // 配置项列表
    private val configurations = mutableListOf<String>()

    inner class ConfigHolder(private val binding: ViewStaticConfigurationItemBinding): RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(item: String) {
            binding.config.text = item
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StaticConfigurationAdapter.ConfigHolder {
        return ConfigHolder(ViewStaticConfigurationItemBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: StaticConfigurationAdapter.ConfigHolder, position: Int) {
        holder.bind(configurations[position])
    }

    override fun getItemCount() = configurations.size

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<String>) {
        configurations.clear()
        configurations.addAll(data)
        notifyDataSetChanged()
    }
}