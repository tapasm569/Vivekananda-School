#!/usr/bin/env bash
set -e

# 1. Ensure res/color and res/values exist
mkdir -p app/src/main/res/color
mkdir -p app/src/main/res/values

# 2. Icon color selector (Active: Deep Navy #0F3D6E, Inactive: Slate #94A3B8)
cat << 'FILE_EOF' > app/src/main/res/color/nav_icon_color.xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:color="#0F3D6E" android:state_checked="true" />
    <item android:color="#94A3B8" />
</selector>
FILE_EOF

# 3. Configure Material 3 Theme with #FEF3C7 as the active indicator container color
cat << 'FILE_EOF' > app/src/main/res/values/themes.xml
<resources>
    <style name="Theme.SchoolManager" parent="Theme.Material3.DayNight.NoActionBar">
        <item name="colorPrimary">#0F3D6E</item>
        <item name="colorOnPrimary">#FFFFFF</item>
        <item name="colorSecondary">#D97706</item>
        <!-- Material 3 BottomNavigationView uses colorSecondaryContainer for active pill indicator -->
        <item name="colorSecondaryContainer">#FEF3C7</item>
        <item name="colorOnSecondaryContainer">#0F3D6E</item>
        <item name="android:statusBarColor">#0F3D6E</item>
        <item name="android:windowLightStatusBar">false</item>
    </style>
</resources>
FILE_EOF

# 4. Clean activity_main.xml using only valid BottomNavigationView attributes
cat << 'FILE_EOF' > app/src/main/res/layout/activity_main.xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F1F5F9">

    <com.google.android.material.appbar.AppBarLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/topAppBar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            android:background="#0F3D6E"
            app:title="Vivekananda Vidyamandir"
            app:subtitle="Dhupguri, Jalpaiguri • Admin Panel"
            app:titleTextColor="@android:color/white"
            app:subtitleTextColor="#93C5FD"
            app:menu="@menu/main_menu" />
    </com.google.android.material.appbar.AppBarLayout>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <!-- WhatsApp-Style Horizontal Swiper -->
        <androidx.viewpager2.widget.ViewPager2
            android:id="@+id/viewPager"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1" />

        <!-- Icon-Only Bottom Navigation Footer -->
        <com.google.android.material.bottomnavigation.BottomNavigationView
            android:id="@+id/bottomNav"
            android:layout_width="match_parent"
            android:layout_height="60dp"
            android:background="@android:color/white"
            app:elevation="8dp"
            app:labelVisibilityMode="unlabeled"
            
app:menu="@menu/bottom_nav_menu"
            app:itemIconTint="@color/nav_icon_color" />

    </LinearLayout>
</androidx.coordinatorlayout.widget.CoordinatorLayout>
FILE_EOF

# 5. Commit and push the fix
git add app/src/main/res/color/nav_icon_color.xml
git add app/src/main/res/values/themes.xml
git add app/src/main/res/layout/activity_main.xml
git commit -m "fix: remove invalid itemActiveIndicatorColor attribute and set active indicator color via M3 theme"
git push origin main

echo "Pushed successfully. GitHub Actions will now compile cleanly."
