package com.gestorfacil.legacy.ui.transactions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gestorfacil.legacy.R
import com.gestorfacil.legacy.data.database.TransactionEntity
import com.gestorfacil.legacy.data.model.Currency
import java.text.NumberFormat
import java.util.Locale

class TransactionAdapter(
    private var items: List<TransactionEntity> = emptyList(),
    private val onDelete: (TransactionEntity) -> Unit,
    private val onEdit: (TransactionEntity) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    private val nf = NumberFormat.getNumberInstance(Locale("es", "VE")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    fun updateList(newItems: List<TransactionEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.description.text = item.description
        holder.category.text = item.category
        holder.date.text = item.date

        val amountText = "Bs. ${nf.format(item.amount)}"
        holder.amount.text = amountText
        holder.amount.setTextColor(
            if (item.type == "income") holder.itemView.context.getColor(R.color.income)
            else holder.itemView.context.getColor(R.color.expense)
        )

        holder.itemView.setOnClickListener { onEdit(item) }
        holder.deleteBtn.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount() = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val description: TextView = view.findViewById(R.id.item_description)
        val category: TextView = view.findViewById(R.id.item_category)
        val date: TextView = view.findViewById(R.id.item_date)
        val amount: TextView = view.findViewById(R.id.item_amount)
        val deleteBtn: View = view.findViewById(R.id.btn_delete)
    }
}
