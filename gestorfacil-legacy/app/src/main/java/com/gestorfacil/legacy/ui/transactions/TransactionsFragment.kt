package com.gestorfacil.legacy.ui.transactions

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gestorfacil.legacy.GestorFacilLegacyApp
import com.gestorfacil.legacy.R
import com.gestorfacil.legacy.data.database.TransactionEntity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TransactionsFragment : Fragment() {

    private lateinit var adapter: TransactionAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchEdit: EditText
    private var allTransactions: List<TransactionEntity> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val app = requireContext().applicationContext as GestorFacilLegacyApp
        val repo = app.repository

        recyclerView = view.findViewById(R.id.transactions_list)
        searchEdit = view.findViewById(R.id.search_bar)

        adapter = TransactionAdapter(
            onDelete = { item ->
                lifecycleScope.launch { repo.deleteTransaction(item.id) }
            },
            onEdit = { item -> showTransactionDialog(app, repo, item) }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        view.findViewById<FloatingActionButton>(R.id.fab_add).setOnClickListener {
            showTransactionDialog(app, repo, null)
        }

        searchEdit.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s?.toString()?.lowercase() ?: ""
                val filtered = if (query.isEmpty()) allTransactions
                else allTransactions.filter {
                    it.description.lowercase().contains(query) ||
                    it.category.lowercase().contains(query)
                }
                adapter.updateList(filtered)
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            repo.allTransactions.collect { list ->
                allTransactions = list
                val query = searchEdit.text.toString().lowercase()
                val filtered = if (query.isEmpty()) list
                else list.filter {
                    it.description.lowercase().contains(query) ||
                    it.category.lowercase().contains(query)
                }
                adapter.updateList(filtered)
            }
        }
    }

    private fun showTransactionDialog(app: GestorFacilLegacyApp, repo: com.gestorfacil.legacy.data.repository.FinanceRepository, editItem: TransactionEntity?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_transaction, null)
        val descInput = dialogView.findViewById<EditText>(R.id.dialog_description)
        val amountInput = dialogView.findViewById<EditText>(R.id.dialog_amount)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.dialog_category)
        val typeSpinner = dialogView.findViewById<Spinner>(R.id.dialog_type)
        val dateBtn = dialogView.findViewById<TextView>(R.id.dialog_date_btn)

        val categories = listOf("Alimentación", "Transporte", "Ocio", "Hogar", "Salud", "Educación", "Salario", "Freelance", "Inversión", "Otros")
        val types = listOf("Gasto", "Ingreso")

        categorySpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        typeSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, types)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        var selectedDate = sdf.format(java.util.Date())

        if (editItem != null) {
            descInput.setText(editItem.description)
            amountInput.setText(kotlin.math.abs(editItem.amount).toString())
            val catIdx = categories.indexOf(editItem.category)
            if (catIdx >= 0) categorySpinner.setSelection(catIdx)
            typeSpinner.setSelection(if (editItem.type == "income") 1 else 0)
            selectedDate = editItem.date
        }

        dateBtn.text = selectedDate
        dateBtn.setOnClickListener {
            val parts = selectedDate.split("-")
            val y = parts[0].toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
            val m = (parts.getOrNull(1)?.toIntOrNull() ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)) - 1
            val d = parts.getOrNull(2)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            DatePickerDialog(requireContext(), { _, yr, mo, dy ->
                selectedDate = String.format(Locale.US, "%04d-%02d-%02d", yr, mo + 1, dy)
                dateBtn.text = selectedDate
            }, y, m, d).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (editItem != null) "Editar transacción" else "Nueva transacción")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val desc = descInput.text.toString().trim()
                val amount = amountInput.text.toString().toDoubleOrNull() ?: 0.0
                val category = categories[categorySpinner.selectedItemPosition]
                val type = if (typeSpinner.selectedItemPosition == 1) "income" else "expense"
                if (desc.isEmpty()) {
                    Toast.makeText(requireContext(), "Descripción requerida", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val tx = TransactionEntity(
                    id = editItem?.id ?: 0,
                    date = selectedDate,
                    description = desc,
                    amount = amount,
                    category = category,
                    type = type
                )
                lifecycleScope.launch {
                    if (editItem != null) repo.updateTransaction(tx)
                    else repo.addTransaction(tx)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
