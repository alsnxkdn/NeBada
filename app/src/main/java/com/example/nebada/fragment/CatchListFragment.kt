package com.example.nebada.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nebada.adapter.CatchRecordAdapter
import com.example.nebada.databinding.FragmentCatchListBinding
import com.example.nebada.manager.CatchRecordManager
import com.example.nebada.model.CatchRecord

class CatchListFragment : Fragment() {

    private var _binding: FragmentCatchListBinding? = null
    private val binding get() = _binding!!

    private lateinit var catchManager: CatchRecordManager
    private lateinit var adapter: CatchRecordAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCatchListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        catchManager = CatchRecordManager(requireContext())
        setupRecyclerView()
        setupFab()
        loadCatchRecords()
    }

    private fun setupRecyclerView() {
        adapter = CatchRecordAdapter(
            onItemClick = { record ->
                // 상세 보기로 이동
                showRecordDetail(record)
            },
            onEditClick = { record ->
                // 수정 화면으로 이동
                editRecord(record)
            },
            onDeleteClick = { record ->
                // 삭제 확인 후 삭제
                deleteRecord(record)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@CatchListFragment.adapter
        }
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            // 새 어획 기록 추가 화면으로 이동
            val fragment = CatchRecordFragment()
            parentFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun loadCatchRecords() {
        val records = catchManager.getAllRecords().sortedByDescending { it.date }
        adapter.submitList(records)

        // 빈 상태 처리
        if (records.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
        }
    }

    private fun showRecordDetail(record: CatchRecord) {
        val fragment = CatchDetailFragment().apply {
            arguments = Bundle().apply {
                putString("record_id", record.id)
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun editRecord(record: CatchRecord) {
        val fragment = CatchRecordFragment().apply {
            arguments = Bundle().apply {
                putString("record_id", record.id)
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun deleteRecord(record: CatchRecord) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("어획 기록 삭제")
            .setMessage("'${record.fishType}' 어획 기록을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                catchManager.deleteCatchRecord(record.id)
                loadCatchRecords() // 목록 새로고침
            }
            .setNegativeButton("취소", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // 화면이 다시 표시될 때 목록 새로고침
        loadCatchRecords()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}