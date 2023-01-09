package com.decagonhq.clads.ui.transactionhistory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.decagonhq.clads.R
import com.decagonhq.clads.data.domain.TransactionHistoryModel
import com.decagonhq.clads.databinding.TransactionHistoryFragmentBinding

class TransactionHistory : Fragment() {
    private var _binding: TransactionHistoryFragmentBinding? = null
    val binding get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = TransactionHistoryFragmentBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val transactionHistoryList = listOf(
            TransactionHistoryModel(
                "1",
                R.drawable.navy_blue_suit,
                "Ankara High Slitted Maxi Skirt",
                "Monday, 7 May 2020, 07:45 am  ",
                "Ifeyinwa Machala",
                "\$150.00"
            ),
            TransactionHistoryModel(
                "1",
                R.drawable.jenifa_stiches,
                "Ankara High Slitted Maxi Skirt",
                "Monday, 7 May 2020, 07:45 am  ",
                "Ifeyinwa Machala",
                "\$150.00"
            ),
            TransactionHistoryModel(
                "1",
                R.drawable.elegant_suit_photo,
                "Ankara High Slitted Maxi Skirt",
                "Monday, 7 May 2020, 07:45 am  ",
                "Ifeyinwa Machala",
                "\$150.00"
            ),
            TransactionHistoryModel(
                "1",
                R.drawable.description,
                "Ankara High Slitted Maxi Skirt",
                "Monday, 7 May 2020, 07:45 am  ",
                "Ifeyinwa Machala",
                "\$150.00"
            )
        )
        val transactionRV = binding?.transactionHistoryRecyclerView
        val transactionHistoryAdapter = TransactionHistoryAdapter()
        transactionRV?.adapter = transactionHistoryAdapter
        transactionHistoryAdapter.submitList(transactionHistoryList)
    }
}
