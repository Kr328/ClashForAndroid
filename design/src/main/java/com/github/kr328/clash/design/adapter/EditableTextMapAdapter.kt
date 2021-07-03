package com.github.kr328.clash.design.adapter

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.databinding.AdapterEditableTextMapBinding
import com.github.kr328.clash.design.databinding.DialogEditableMapTextFieldBinding
import com.github.kr328.clash.design.preference.TextAdapter
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class EditableTextMapAdapter<K, V>(
    private val context: Context,
    val title: CharSequence,
    val values: MutableList<Pair<K, V>>,
    private val keyAdapter: TextAdapter<K>,
    private val valueAdapter: TextAdapter<V>,
) : RecyclerView.Adapter<EditableTextMapAdapter.Holder>() {
    class Holder(val binding: AdapterEditableTextMapBinding) : RecyclerView.ViewHolder(binding.root)

    fun addElement(key: String, value: String) {
        val keyValue = keyAdapter.to(key)
        val valueValue = valueAdapter.to(value)

        notifyItemInserted(values.size)
        values.add(keyValue to valueValue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            AdapterEditableTextMapBinding
                .inflate(context.layoutInflater, parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val current = values[position]

        holder.binding.keyView.text = keyAdapter.from(current.first)
        holder.binding.valueView.text = valueAdapter.from(current.second)
        holder.binding.root.setOnClickListener {
            val index = values.indexOf(current)
            requestEditTextMap(
                context,
                title,
                keyAdapter.from(current.first),
                valueAdapter.from(current.second)
            ) {
                if (index >= 0) {
                    values.removeAt(index)
                    values.add(index, Pair(keyAdapter.to(it.first), valueAdapter.to(it.second)))
                    notifyItemChanged(index)
                }
            }
        }
        holder.binding.deleteView.setOnClickListener {
            val index = values.indexOf(current)

            if (index >= 0) {
                values.removeAt(index)
                notifyItemRemoved(index)
            }
        }
    }

    override fun getItemCount(): Int {
        return values.size
    }
}

private fun requestEditTextMap(
    context: Context,
    title: CharSequence,
    key: CharSequence,
    value: CharSequence,
    callback: (Pair<String, String>) -> Unit
) {
    val binding = DialogEditableMapTextFieldBinding
        .inflate(context.layoutInflater, context.root, false)

    binding.keyView.setText(key)
    binding.valueView.setText(value)

    val dialog = MaterialAlertDialogBuilder(context)
        .setTitle(title)
        .setNegativeButton(R.string.cancel) { _, _ -> }
        .setPositiveButton(R.string.ok) { _, _ ->
            val k = binding.keyView.text?.toString()?.trim() ?: ""
            val v = binding.valueView.text?.toString()?.trim() ?: ""

            if (k.isNotEmpty() && v.isNotEmpty()) {
                callback.invoke(Pair(k, v))
            }
        }
        .setView(binding.root)
        .create()

    dialog.setOnCancelListener {

    }

    dialog.show()
}