package com.vidyamandir.schoolmanager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.vidyamandir.schoolmanager.databinding.ActivityMainBinding
import com.vidyamandir.schoolmanager.ui.DashboardFragment
import com.vidyamandir.schoolmanager.ui.DueListFragment
import com.vidyamandir.schoolmanager.ui.LoginActivity
import com.vidyamandir.schoolmanager.ui.PaidListFragment
import com.vidyamandir.schoolmanager.ui.SearchFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViewPagerWithBottomNav()
    }

    private fun setupToolbar() {
        binding.topAppBar.setNavigationOnClickListener { anchor ->
            val popup = PopupMenu(this, anchor)
            popup.menu.add(0, R.id.action_logout, 0, "Sign out")
            popup.setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.action_logout) {
                    val prefs = getSharedPreferences("vv_auth_session", Context.MODE_PRIVATE)
                    prefs.edit().clear().apply()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    true
                } else false
            }
            popup.show()
        }
    }

    private fun setupViewPagerWithBottomNav() {
        // Tab 0: Due List, Tab 1: Paid List, Tab 2: Search, Tab 3: Dashboard
        val pagerAdapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 4
            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> DueListFragment()
                    1 -> PaidListFragment()
                    2 -> SearchFragment()
                    3 -> DashboardFragment()
                    else -> DashboardFragment()
                }
            }
        }

        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = 3

        // Swiping Left/Right updates Footer Icon
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> binding.bottomNav.selectedItemId = R.id.nav_due
                    1 -> binding.bottomNav.selectedItemId = R.id.nav_paid
                    2 -> binding.bottomNav.selectedItemId = R.id.nav_search
                    3 -> binding.bottomNav.selectedItemId = R.id.nav_dashboard
                }
            }
        })

        // Tapping Footer Icon scrolls ViewPager2
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_due -> {
                    if (binding.viewPager.currentItem != 0) binding.viewPager.setCurrentItem(0, true)
                    true
                }
                R.id.nav_paid -> {
                    if (binding.viewPager.currentItem != 1) binding.viewPager.setCurrentItem(1, true)
                    true
                }
                R.id.nav_search -> {
                    if (binding.viewPager.currentItem != 2) binding.viewPager.setCurrentItem(2, true)
                    true
                }
                R.id.nav_dashboard -> {
                    if (binding.viewPager.currentItem != 3) binding.viewPager.setCurrentItem(3, true)
                    true
                }
                else -> false
            }
        }
    }
}
