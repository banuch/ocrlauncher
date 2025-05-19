// ReadingHistoryAdapter.kt
package com.example.ocrlauncher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ReadingHistoryAdapter(private val readingHistory: List<ReadingEntry>) :
    RecyclerView.Adapter<ReadingHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val serviceIdTextView: TextView = view.findViewById(R.id.historyServiceId)
        val valueTypeTextView: TextView = view.findViewById(R.id.historyValueType)
        val readingTextView: TextView = view.findViewById(R.id.historyReading)
        val timestampTextView: TextView = view.findViewById(R.id.historyTimestamp)
        val editedTextView: TextView = view.findViewById(R.id.historyEdited)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.history_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = readingHistory[position]
        holder.serviceIdTextView.text = "Service ID: ${entry.serviceId}"
        holder.valueTypeTextView.text = "Value Type: ${entry.valueType}"
        holder.readingTextView.text = "Reading: ${entry.reading}"
        holder.timestampTextView.text = entry.timestamp
        holder.editedTextView.text = if (entry.isEdited) "Manual Entry" else "Auto Detected"
    }

    override fun getItemCount() = readingHistory.size
}