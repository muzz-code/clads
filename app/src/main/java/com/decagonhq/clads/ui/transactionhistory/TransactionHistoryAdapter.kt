package com.decagonhq.clads.ui.transactionhistory

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.decagonhq.clads.data.domain.TransactionHistoryModel
import com.decagonhq.clads.databinding.TransactionHistoryFragmentRecyclerViewItemBinding
import com.decagonhq.clads.util.hideView
import com.decagonhq.clads.util.invisibleView
import com.decagonhq.clads.util.showView

class TransactionHistoryAdapter :
    ListAdapter<TransactionHistoryModel, TransactionHistoryAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(val binding: TransactionHistoryFragmentRecyclerViewItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(transactionHistoryModel: TransactionHistoryModel?) {
            binding.transactionHistoryTransactionTitleTextView.text =
                transactionHistoryModel?.transactionTitle
            binding.transactionHistoryTransactionTimeTextView.text =
                transactionHistoryModel?.transactionTime
            binding.transactionHistoryTransactionCustomerNameTextView.text =
                transactionHistoryModel?.userName

            transactionHistoryModel?.userImage?.let {
                binding.transactionHistoryFragmentTransactionImageView.setImageResource(
                    it
                )
            }
            binding.transactionHistoryTransactionFeeTextView.text =
                transactionHistoryModel?.transactionFee
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = TransactionHistoryFragmentRecyclerViewItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
        // toggle buttons onclick
        holder.binding.apply {
            transactionHistoryTransactionAcceptButton.setOnClickListener {
                transactionHistoryTransactionAcceptButton.invisibleView()
                transactionHistoryTransactionAcceptedTextView.showView()
                transactionHistoryTransactionDeclineButton.hideView()
                transactionHistoryTransactionCompleteButton.showView()
//                transactionHistoryTransactionDeclineButton.setBackgroundResource(R.drawable.transaction_history_accept_button_background)
            }

            transactionHistoryTransactionCompleteButton.setOnClickListener {
                transactionHistoryTransactionCompleteButton.hideView()
                transactionHistoryTransactionAcceptedTextView.invisibleView()
                transactionHistoryTransactionCompletedTextView.showView()
                transactionHistoryTransactionAcceptButton.invisibleView()
            }

            transactionHistoryTransactionDeclineButton.setOnClickListener {
                transactionHistoryTransactionDeclineButton.hideView()
                transactionHistoryTransactionDeclinedTextView.showView()
                transactionHistoryTransactionAcceptButton.invisibleView()
                transactionHistoryUserImageView.invisibleView()
                transactionHistoryChatWithTailorTextView.invisibleView()
            }
        }
    }
}

object DiffCallback : DiffUtil.ItemCallback<TransactionHistoryModel>() {
    override fun areItemsTheSame(
        oldItem: TransactionHistoryModel,
        newItem: TransactionHistoryModel
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: TransactionHistoryModel,
        newItem: TransactionHistoryModel
    ): Boolean {
        return oldItem == newItem
    }
}
