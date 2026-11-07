#!/usr/bin/env bash
set -e

echo "=== Updating MainActivity.kt with Safe Posted Initialization ==="
cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/MainActivity.kt
package com.vidyamandir.schoolmanager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.vidyamandir.schoolmanager.databinding.ActivityMainBinding
import com.vidyamandir.schoolmanager.ui.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isSyncing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViewPagerWithBottomNav()
    }

    private fun setupToolbar() {
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_logout -> {
                    val prefs = getSharedPreferences("vv_auth_session", Context.MODE_PRIVATE)
                    prefs.edit().clear().apply()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupViewPagerWithBottomNav() {
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
        binding.viewPager.offscreenPageLimit = 1

        // Sync ViewPager2 swipe -> BottomNav
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (isSyncing) return

                val targetId = when (position) {
                    0 -> R.id.nav_due
                    1 -> R.id.nav_paid
                    2 -> R.id.nav_search
                    3 -> R.id.nav_dashboard
                    else -> R.id.nav_dashboard
                }

                if (binding.bottomNav.selectedItemId != targetId) {
                    isSyncing = true
                    try {
                        binding.bottomNav.selectedItemId = targetId
                    } catch (_: Exception) {}
                    isSyncing = false
                }
            }
        })

        // Sync BottomNav click -> ViewPager2
        binding.bottomNav.setOnItemSelectedListener { item ->
            if (isSyncing) return@setOnItemSelectedListener true

            val targetPage = when (item.itemId) {
                R.id.nav_due -> 0
                R.id.nav_paid -> 1
                R.id.nav_search -> 2
                R.id.nav_dashboard -> 3
                else -> 3
            }

            if (binding.viewPager.currentItem != targetPage) {
                isSyncing = true
                binding.viewPager.setCurrentItem(targetPage, false)
                isSyncing = false
            }
            true
        }

        // Safely land on Tab 4 (Dashboard) after layout measurement is complete
        binding.viewPager.post {
            if (!isFinishing && !isDestroyed) {
                isSyncing = true
                try {
                    binding.viewPager.setCurrentItem(3, false)
                    binding.bottomNav.selectedItemId = R.id.nav_dashboard
                } catch (_: Exception) {}
                isSyncing = false
            }
        }
    }
}
FILE_EOF

echo "=== Committing and Pushing to GitHub ==="
git add app/src/main/java/com/vidyamandir/schoolmanager/MainActivity.kt
git commit -m "fix: defer initial BottomNavigationView selection to viewPager.post to prevent startup crash"
git push origin main

echo "=========================================================================="
echo " SUCCESS! MainActivity updated with deferred initialization and pushed.   "
echo " Download your compiled APK from GitHub Actions and test login.           "
echo "=========================================================================="
