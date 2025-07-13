package com.example.nebada

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.nebada.databinding.ActivityCatchManagementBinding
import com.example.nebada.fragment.CatchListFragment
import com.example.nebada.fragment.CatchRecordFragment
import com.example.nebada.fragment.CatchStatisticsFragment

class CatchManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCatchManagementBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCatchManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupBottomNavigation()

        // 직접 기록 화면으로 이동하는 경우 확인
        val directToRecord = intent.getBooleanExtra("direct_to_record", false)

        if (savedInstanceState == null) {
            if (directToRecord) {
                // 어획 기록 화면으로 직접 이동
                showFragment(CatchRecordFragment())
                binding.bottomNavigation.selectedItemId = R.id.nav_add_catch
            } else {
                // 기본으로 어획 목록 화면 표시
                showFragment(CatchListFragment())
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "어획 관리"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_catch_list -> {
                    showFragment(CatchListFragment())
                    true
                }
                R.id.nav_add_catch -> {
                    showFragment(CatchRecordFragment())
                    true
                }
                R.id.nav_statistics -> {
                    showFragment(CatchStatisticsFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}