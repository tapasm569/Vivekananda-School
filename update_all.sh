#!/usr/bin/env bash
set -e

echo "=== [1/8] Setting up Directory Structure ==="
mkdir -p .github/workflows
mkdir -p app/src/main/res/values
mkdir -p app/src/main/res/layout
mkdir -p app/src/main/res/menu
mkdir -p app/src/main/res/xml
mkdir -p app/src/main/java/com/vidyamandir/schoolmanager/data
mkdir -p app/src/main/java/com/vidyamandir/schoolmanager/domain
mkdir -p app/src/main/java/com/vidyamandir/schoolmanager/util
mkdir -p app/src/main/java/com/vidyamandir/schoolmanager/ui

# Clean up any old files from previous package name if present
rm -rf app/src/main/java/in

echo "=== [2/8] Writing Build & Gradle Configurations ==="
cat << 'FILE_EOF' > gradle.properties
android.useAndroidX=true
android.nonTransitiveRClass=true
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
FILE_EOF

cat << 'FILE_EOF' > settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "Vivekananda School"
include(":app")
FILE_EOF

cat << 'FILE_EOF' > build.gradle.kts
plugins {
    id("com.android.application") version "8.4.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("org.jetbrains.kotlin.kapt") version "1.9.24" apply false
}
FILE_EOF

cat << 'FILE_EOF' > app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.vidyamandir.schoolmanager"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.vidyamandir.schoolmanager"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.core:core-ktx:1.13.1")

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
}
FILE_EOF

cat << 'FILE_EOF' > .github/workflows/build-apk.yml
name: Build School App APK

on:
  push:
    branches: [ "main" ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle

      - name: Grant Execute Permission to Gradlew
        run: chmod +x gradlew

      - name: Build Debug APK
        run: ./gradlew assembleDebug --no-daemon -x test -x lint

      - name: Upload Debug APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: Vivekananda-Vidyamandir-Debug-APK
          path: app/build/outputs/apk/debug/app-debug.apk
FILE_EOF

echo "=== [3/8] Writing Manifest & Resources ==="
cat << 'FILE_EOF' > app/src/main/AndroidManifest.xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="true"
        android:icon="@android:drawable/sym_def_app_icon"
        android:label="Vivekananda Vidyamandir"
        android:roundIcon="@android:drawable/sym_def_app_icon"
        android:supportsRtl="true"
        android:theme="@style/Theme.SchoolManager">

        <activity
            android:name=".ui.LoginActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity android:name=".MainActivity" android:exported="false" />
        <activity android:name=".ui.TeacherPortalActivity" android:exported="false" />
        <activity android:name=".ui.FeeCollectionActivity" android:exported="false" />
        <activity android:name=".ui.ResultPublishActivity" android:exported="false" />
        <activity android:name=".ui.StudentManagementActivity" android:exported="false" />
        <activity android:name=".ui.AddStudentActivity" android:exported="false" />
        <activity android:name=".ui.ClassManagementActivity" android:exported="false" />
        <activity android:name=".ui.TeacherManagementActivity" android:exported="false" />
        <activity android:name=".ui.AddTeacherActivity" android:exported="false" />
        <activity android:name=".ui.StudentDuesListActivity" android:exported="false" />
        <activity android:name=".ui.TrackPaymentActivity" android:exported="false" />
        <activity android:name=".ui.MarksheetApprovalActivity" android:exported="false" />
        <activity android:name=".ui.WhatsAppMessageActivity" android:exported="false" />

        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>

    </application>

</manifest>
FILE_EOF

cat << 'FILE_EOF' > app/src/main/res/xml/file_paths.xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="report_cards" path="report_cards/" />
    <files-path name="profiles" path="profiles/" />
</paths>
FILE_EOF

cat << 'FILE_EOF' > app/src/main/res/values/themes.xml
<resources>
    <style name="Theme.SchoolManager" parent="Theme.Material3.DayNight.NoActionBar">
        <item name="colorPrimary">#0F3D6E</item>
        <item name="colorOnPrimary">#FFFFFF</item>
        <item name="colorSecondary">#D97706</item>
        <item name="android:statusBarColor">#0F3D6E</item>
        <item name="android:windowLightStatusBar">false</item>
    </style>
</resources>
FILE_EOF

cat << 'FILE_EOF' > app/src/main/res/values/strings.xml
<resources>
    <string name="app_name">Vivekananda Vidyamandir</string>
</resources>
FILE_EOF

cat << 'FILE_EOF' > app/src/main/res/menu/main_menu.xml
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">
    <item
        android:id="@+id/action_logout"
        android:title="Sign Out"
        android:icon="@android:drawable/ic_lock_power_off"
        app:showAsAction="never" />
</menu>
FILE_EOF

echo "=== [4/8] Writing XML Layouts ==="
cat << 'FILE_EOF' > app/src/main/res/layout/activity_login.xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.core.widget.NestedScrollView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true"
    android:background="#0F3D6E">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:padding="24dp"
        android:gravity="center">

        <com.google.android.material.card.MaterialCardView
            android:layout_width="76dp"
            android:layout_height="76dp"
            app:cardCornerRadius="38dp"
            app:cardElevation="6dp"
            app:cardBackgroundColor="#FEF3C7"
            app:strokeWidth="2dp"
            app:strokeColor="#D97706">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:gravity="center"
                android:text="VV"
                android:textColor="#0F3D6E"
                android:textSize="26sp"
                android:textStyle="bold" />
        </com.google.android.material.card.MaterialCardView>

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:text="VIVEKANANDA VIDYAMANDIR"
            android:textColor="@android:color/white"
            android:textSize="20sp"
            android:letterSpacing="0.04"
            android:textStyle="bold"
            android:textAlignment="center" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:text="Dhupguri, Jalpaiguri • Admin &amp; Teacher Portal"
            android:textColor="#93C5FD"
            android:textSize="13sp"
            android:textAlignment="center" />

        <com.google.android.material.card.MaterialCardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="32dp"
            app:cardCornerRadius="24dp"
            app:cardElevation="8dp"
            app:strokeWidth="0dp"
            app:cardBackgroundColor="@android:color/white">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="24dp">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Portal Sign In"
                    android:textColor="#0F3D6E"
                    android:textSize="18sp"
                    android:textStyle="bold" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="2dp"
                    android:text="Enter School Code &amp; your password"
                    android:textColor="#64748B"
                    android:textSize="12sp" />

                <com.google.android.material.textfield.TextInputLayout
                    android:id="@+id/tilSchoolCode"
                    style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="20dp"
                    android:hint="School Code"
                    app:boxCornerRadiusTopStart="12dp"
                    app:boxCornerRadiusTopEnd="12dp"
                    app:boxCornerRadiusBottomStart="12dp"
                    app:boxCornerRadiusBottomEnd="12dp">

                    <com.google.android.material.textfield.TextInputEditText
                        android:id="@+id/etSchoolCode"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="VV001"
                        android:inputType="textCapCharacters"
                        android:singleLine="true" />
                </com.google.android.material.textfield.TextInputLayout>

                <com.google.android.material.textfield.TextInputLayout
                    android:id="@+id/tilPassword"
                    style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="16dp"
                    android:hint="Password"
                    app:passwordToggleEnabled="true"
                    app:boxCornerRadiusTopStart="12dp"
                    app:boxCornerRadiusTopEnd="12dp"
                    app:boxCornerRadiusBottomStart="12dp"
                    app:boxCornerRadiusBottomEnd="12dp">

                    <com.google.android.material.textfield.TextInputEditText
                        android:id="@+id/etPassword"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:inputType="textPassword"
                        android:singleLine="true" />
                </com.google.android.material.textfield.TextInputLayout>

                <com.google.android.material.button.MaterialButton
                    android:id="@+id/btnLogin"
                    android:layout_width="match_parent"
                    android:layout_height="54dp"
                    android:layout_marginTop="26dp"
                    android:text="Sign In"
                    android:textStyle="bold"
                    app:cornerRadius="12dp"
                    app:backgroundTint="#0F3D6E" />

            </LinearLayout>
        </com.google.android.material.card.MaterialCardView>

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="28dp"
            android:text="Authorized Access Only • Session 2026"
            android:textColor="#93C5FD"
            android:textSize="11sp" />

    </LinearLayout>
</androidx.core.widget.NestedScrollView>
FILE_EOF

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

    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">

            <!-- COLLECT FEE (SINGLE CARD ROW) -->
            <com.google.android.material.card.MaterialCardView
                android:id="@+id/cardCollectFee"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                app:cardCornerRadius="18dp"
                app:cardElevation="3dp"
                app:cardBackgroundColor="#0F3D6E">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:gravity="center_vertical"
                    android:orientation="horizontal"
                    android:padding="18dp">

                    <LinearLayout
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:orientation="vertical">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="COLLECT FEE"
                            android:textColor="@android:color/white"
                            android:textSize="17sp"
                            android:letterSpacing="0.04"
                            android:textStyle="bold" />

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="2dp"
                            android:text="12-month fee counter, advances &amp; late fines"
                            android:textColor="#93C5FD"
                            android:textSize="12sp" />
                    </LinearLayout>

                    <com.google.android.material.button.MaterialButton
                        android:id="@+id/btnQuickCollect"
                        android:layout_width="wrap_content"
                        android:layout_height="44dp"
                        android:text="Open Counter →"
                        android:textColor="#0F3D6E"
                        android:textSize="12sp"
                        android:textStyle="bold"
                        app:backgroundTint="#FEF3C7"
                        app:cornerRadius="10dp" />
                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>

            <!-- ROW 1: STUDENTS, CLASS, TEACHER -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="14dp"
                android:orientation="horizontal">

                <com.google.android.material.card.MaterialCardView
                    android:id="@+id/cardStudents"
                    android:layout_width="0dp"
                    android:layout_height="110dp"
                    android:layout_weight="1"
                    android:layout_marginEnd="4dp"
                    app:cardCornerRadius="16dp"
                    app:cardElevation="2dp"
                    app:strokeWidth="1dp"
                    app:strokeColor="#CBD5E1"
                    app:cardBackgroundColor="@android:color/white">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="match_parent"
                        android:gravity="center"
                        android:orientation="vertical"
                        android:padding="8dp">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="STUDENTS"
                            android:textColor="#0F3D6E"
                            android:textSize="11sp"
                            android:letterSpacing="0.04"
                            android:textStyle="bold" />

                        <TextView
                            android:id="@+id/tvStudentCount"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="4dp"
                            android:text="0"
                            android:textColor="#0F3D6E"
                            android:textSize="22sp"
                            android:textStyle="bold" />

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Enrolled"
                            android:textColor="#64748B"
                            android:textSize="10sp" />
                    </LinearLayout>
                </com.google.android.material.card.MaterialCardView>

                <com.google.android.material.card.MaterialCardView
                    android:id="@+id/cardClasses"
                    android:layout_width="0dp"
                    android:layout_height="110dp"
                    android:layout_weight="1"
                    android:layout_marginHorizontal="4dp"
                    app:cardCornerRadius="16dp"
                    app:cardElevation="2dp"
                    app:strokeWidth="1dp"
                    app:strokeColor="#CBD5E1"
                    app:cardBackgroundColor="@android:color/white">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="match_parent"
                        android:gravity="center"
                        android:orientation="vertical"
                        android:padding="8dp">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="CLASS"
                            android:textColor="#0F3D6E"
                            android:textSize="11sp"
                            android:letterSpacing="0.04"
                            android:textStyle="bold" />

                        <TextView
                            android:id="@+id/tvClassCount"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="4dp"
                            android:text="0"
                            android:textColor="#0F3D6E"
                            android:textSize="22sp"
                            android:textStyle="bold" />

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Classes"
                            android:textColor="#64748B"
                            android:textSize="10sp" />
                    </LinearLayout>
                </com.google.android.material.card.MaterialCardView>

                <com.google.android.material.card.MaterialCardView
                    android:id="@+id/cardTeachers"
                    android:layout_width="0dp"
                    android:layout_height="110dp"
                    android:layout_weight="1"
                    android:layout_marginStart="4dp"
                    app:cardCornerRadius="16dp"
                    app:cardElevation="2dp"
                    app:strokeWidth="1dp"
                    app:strokeColor="#CBD5E1"
                    app:cardBackgroundColor="@android:color/white">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="match_parent"
                        android:gravity="center"
                        android:orientation="vertical"
                        android:padding="8dp">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="TEACHER"
                            android:textColor="#0F3D6E"
                            android:textSize="11sp"
                            android:letterSpacing="0.04"
                            android:textStyle="bold" />

                        <TextView
                            android:id="@+id/tvTeacherCount"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="4dp"
                            android:text="0"
                            android:textColor="#0F3D6E"
                            android:textSize="22sp"
                            android:textStyle="bold" />

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Staff"
                            android:textColor="#64748B"
                            android:textSize="10sp" />
                    </LinearLayout>
                </com.google.android.material.card.MaterialCardView>
            </LinearLayout>

            <!-- ROW 2: DUE BALANCE, PAID BALANCE -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="12dp"
                android:orientation="horizontal">

                <com.google.android.material.card.MaterialCardView
                    android:id="@+id/cardDueBalance"
                    android:layout_width="0dp"
                    android:layout_height="115dp"
                    android:layout_weight="1"
                    android:layout_marginEnd="6dp"
                    app:cardCornerRadius="18dp"
                    app:cardElevation="2dp"
                    app:cardBackgroundColor="#FEF2F2"
                    app:strokeWidth="1dp"
                    app:strokeColor="#FECACA">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="match_parent"
                        android:gravity="center_vertical"
                        android:orientation="vertical"
                        android:padding="16dp">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="DUE BALANCE"
                            android:textColor="#991B1B"
                            android:textSize="12sp"
                            android:textStyle="bold" />

                        <TextView
                            android:id="@+id/tvDueBalanceAmount"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="4dp"
                            android:text="₹0.00"
                            android:textColor="#DC2626"
                            android:textSize="19sp"
                            android:textStyle="bold" />

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="2dp"
                            android:text="Tap to view due list →"
                            android:textColor="#B91C1C"
                            android:textSize="11sp" />
                    </LinearLayout>
                </com.google.android.material.card.MaterialCardView>

                <com.google.android.material.card.MaterialCardView
                    android:id="@+id/cardPaidBalance"
                    android:layout_width="0dp"
                    android:layout_height="115dp"
                    android:layout_weight="1"
                    android:layout_marginStart="6dp"
                    app:cardCornerRadius="18dp"
                    app:cardElevation="2dp"
                    app:cardBackgroundColor="#F0FDF4"
                    app:strokeWidth="1dp"
                    app:strokeColor="#BBF7D0">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="match_parent"
                        android:gravity="center_vertical"
                        android:orientation="vertical"
                        android:padding="16dp">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="PAID BALANCE"
                            android:textColor="#166534"
                            android:textSize="12sp"
                            android:textStyle="bold" />

                        <TextView
                            android:id="@+id/tvPaidBalanceAmount"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="4dp"
                            android:text="₹0.00"
                            android:textColor="#16A34A"
                            android:textSize="19sp"
                            android:textStyle="bold" />

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="2dp"
                            android:text="Total collected →"
                            android:textColor="#15803D"
                            android:textSize="11sp" />
                    </LinearLayout>
                </com.google.android.material.card.MaterialCardView>
            </LinearLayout>

            <!-- ROW 3: TRACK PAYMENT, APPROVED MARKSHEET -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="12dp"
                android:orientation="horizontal">

                <com.google.android.material.card.MaterialCardView
                    android:id="@+id/cardTrackPayment"
                    android:layout_width="0dp"
                    android:layout_height="115dp"
                    android:layout_weight="1"
                    android:layout_marginEnd="6dp"
                    app:cardCornerRadius="18dp"
                    app:cardElevation="2dp"
                    app:cardBackgroundColor="#EFF6FF"
                    app:strokeWidth="1dp"
                    app:strokeColor="#BFDBFE">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="match_parent"
                        android:gravity="center_vertical"
                        android:orientation="vertical"
                        android:padding="16dp">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="TRACK PAYMENT"
                            android:textColor="#1E40AF"
                            android:textSize="12sp"
                            android:textStyle="bold" />

                        <TextView
                            android:id="@+id/tvDailyCollection"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="4dp"
                            android:text="₹0.00"
                            android:textColor="#2563EB"
                            android:textSize="19sp"
                            android:textStyle="bold" />

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="2dp"
                            android:text="Today's receipts →"
                            android:textColor="#1D4ED8"
                            android:textSize="11sp" />
                    </LinearLayout>
                </com.google.android.material.card.MaterialCardView>

                <com.google.android.material.card.MaterialCardView
                    android:id="@+id/cardApprovedMarksheet"
                    android:layout_width="0dp"
                    android:layout_height="115dp"
                    android:layout_weight="1"
                    android:layout_marginStart="6dp"
                    app:cardCornerRadius="18dp"
                    app:cardElevation="2dp"
                    app:cardBackgroundColor="#FFFBEB"
                    app:strokeWidth="1dp"
                    app:strokeColor="#FDE68A">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="match_parent"
                        android:gravity="center_vertical"
                        android:orientation="vertical"
                        android:padding="16dp">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="APPROVED MARKSHEET"
                            android:textColor="#92400E"
                            android:textSize="11sp"
                            android:textStyle="bold" />

                        <TextView
                            android:id="@+id/tvPendingMarksheetCount"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="4dp"
                            android:text="0 Pending"
                            android:textColor="#D97706"
                            android:textSize="19sp"
                            android:textStyle="bold" />

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="2dp"
                            android:text="Review &amp; Approve →"
                            android:textColor="#B45309"
                            android:textSize="11sp" />
                    </LinearLayout>
                </com.google.android.material.card.MaterialCardView>
            </LinearLayout>

            <!-- ROW 4: SET EXAMS, PUBLISH RESULT -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="12dp"
                android:orientation="horizontal">

                <com.google.android.material.card.MaterialCardView
                    android:id="@+id/cardSetExams"
                    android:layout_width="0dp"
                    android:layout_height="115dp"
                    android:layout_weight="1"
                    android:layout_marginEnd="6dp"
                    app:cardCornerRadius="18dp"
                    app:cardElevation="2dp"
                    app:cardBackgroundColor="#FAF5FF"
                    app:strokeWidth="1dp"
                    app:strokeColor="#E9D5FF">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="match_parent"
                        android:gravity="center_vertical"
                        android:orientation="vertical"
                        android:padding="16dp">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="SET EXAMS"
                            android:textColor="#6B21A8"
                            android:textSize="12sp"
                            android:textStyle="bold" />

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="4dp"
                            android:text="Schedule"
                            android:textColor="#9333EA"
                            android:textSize="18sp"
                            android:textStyle="bold" />

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="2dp"
                            android:text="Input &amp; configure tests →"
                            android:textColor="#7E22CE"
                            android:textSize="11sp" />
                    </LinearLayout>
                </com.google.android.material.card.MaterialCardView>

                <com.google.android.material.card.MaterialCardView
                    android:id="@+id/cardPublishResult"
                    android:layout_width="0dp"
                    android:layout_height="115dp"
                    android:layout_weight="1"
                    android:layout_marginStart="6dp"
                    app:cardCornerRadius="18dp"
                    app:cardElevation="2dp"
                    app:cardBackgroundColor="#F0FDFA"
                    app:strokeWidth="1dp"
                    app:strokeColor="#99F6E4">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="match_parent"
                        android:gravity="center_vertical"
                        android:orientation="vertical"
                        android:padding="16dp">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="PUBLISH RESULT"
                            android:textColor="#115E59"
                            android:textSize="12sp"
                            android:textStyle="bold" />

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="4dp"
                            android:text="Print 2-Page"
                            android:textColor="#0D9488"
                            android:textSize="18sp"
                            android:textStyle="bold" />

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="2dp"
                            android:text="Generate PDF marksheets →"
                            android:textColor="#0F766E"
                            android:textSize="11sp" />
                    </LinearLayout>
                </com.google.android.material.card.MaterialCardView>
            </LinearLayout>

            <!-- ROW 5: MESSAGE (FULL-WIDTH WHATSAPP BROADCAST) -->
            <com.google.android.material.card.MaterialCardView
                android:id="@+id/cardMessage"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="14dp"
                android:layout_marginBottom="24dp"
                app:cardCornerRadius="18dp"
                app:cardElevation="3dp"
                app:cardBackgroundColor="#0F3D6E">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:gravity="center_vertical"
                    android:orientation="horizontal"
                    android:padding="18dp">

                    <LinearLayout
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:orientation="vertical">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="MESSAGE"
                            android:textColor="@android:color/white"
                            android:textSize="16sp"
                            android:letterSpacing="0.04"
                            android:textStyle="bold" />

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="2dp"
                            android:text="Direct WhatsApp messaging to students &amp; parents"
                            android:textColor="#93C5FD"
                            android:textSize="12sp" />
                    </LinearLayout>

                    <com.google.android.material.button.MaterialButton
                        android:id="@+id/btnOpenMessage"
                        style="@style/Widget.Material3.Button.TonalButton"
                        android:layout_width="wrap_content"
                        android:layout_height="44dp"
                        android:text="Send WhatsApp"
                        android:textColor="#0F3D6E"
                        android:textSize="12sp"
                        android:textStyle="bold"
                        app:backgroundTint="#FEF3C7"
                        app:cornerRadius="10dp" />

                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>

        </LinearLayout>
    </androidx.core.widget.NestedScrollView>
</androidx.coordinatorlayout.widget.CoordinatorLayout>
FILE_EOF

cat << 'FILE_EOF' > app/src/main/res/layout/activity_fee_counter.xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F8FAFC">

    <com.google.android.material.appbar.AppBarLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            android:background="#0F3D6E"
            app:title="Fee Collection Counter"
            app:titleTextColor="@android:color/white"
            app:navigationIcon="@android:drawable/ic_menu_revert" />
    </com.google.android.material.appbar.AppBarLayout>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:padding="16dp"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <com.google.android.material.textfield.TextInputLayout
            android:id="@+id/tilStudentSelect"
            style="@style/Widget.Material3.TextInputLayout.OutlinedBox.ExposedDropdownMenu"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="Select Student">

            <AutoCompleteTextView
                android:id="@+id/actvStudentSelect"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="none" />
        </com.google.android.material.textfield.TextInputLayout>

        <com.google.android.material.card.MaterialCardView
            android:id="@+id/cardStudentInfo"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            app:cardCornerRadius="12dp"
            app:strokeWidth="1dp"
            app:strokeColor="#CBD5E1"
            app:cardBackgroundColor="@android:color/white">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="12dp">

                <TextView
                    android:id="@+id/tvSelectedStudentName"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Student: Please select above"
                    android:textColor="#0F3D6E"
                    android:textSize="15sp"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/tvSelectedStudentMeta"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="2dp"
                    android:text="Monthly Fee: ₹0.00"
                    android:textColor="#475569"
                    android:textSize="13sp" />
            </LinearLayout>
        </com.google.android.material.card.MaterialCardView>

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="14dp"
            android:layout_marginBottom="6dp"
            android:text="Academic Year Months (Select to Pay / Advance)"
            android:textColor="#1E293B"
            android:textSize="13sp"
            android:textStyle="bold" />

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/rvMonths"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1" />

        <com.google.android.material.card.MaterialCardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            app:cardCornerRadius="14dp"
            app:cardBackgroundColor="#FEF3C7">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="14dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="Base Fee:"
                        android:textColor="#78350F" />

                    <TextView
                        android:id="@+id/tvTotalBaseFee"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="₹0.00"
                        android:textColor="#78350F"
                        android:textStyle="bold" />
                </LinearLayout>

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="2dp"
                    android:orientation="horizontal">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="Late Fine:"
                        android:textColor="#DC2626" />

                    <TextView
                        android:id="@+id/tvTotalLateFee"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="₹0.00"
                        android:textColor="#DC2626"
                        android:textStyle="bold" />
                </LinearLayout>

                <View
                    android:layout_width="match_parent"
                    android:layout_height="1dp"
                    android:layout_marginVertical="6dp"
                    android:background="#FDE68A" />

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="Total Amount:"
                        android:textColor="#78350F"
                        android:textSize="16sp"
                        android:textStyle="bold" />

                    <TextView
                        android:id="@+id/tvTotalPayable"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="₹0.00"
                        android:textColor="#78350F"
                        android:textSize="18sp"
                        android:textStyle="bold" />
                </LinearLayout>

                <com.google.android.material.button.MaterialButton
                    android:id="@+id/btnSubmitPayment"
                    android:layout_width="match_parent"
                    android:layout_height="50dp"
                    android:layout_marginTop="10dp"
                    android:text="Confirm Payment &amp; Record"
                    android:textStyle="bold"
                    app:backgroundTint="#0F3D6E"
                    app:cornerRadius="10dp" />
            </LinearLayout>
        </com.google.android.material.card.MaterialCardView>

    </LinearLayout>
</androidx.coordinatorlayout.widget.CoordinatorLayout>
FILE_EOF

cat << 'FILE_EOF' > app/src/main/res/layout/item_month_fee.xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/cardMonth"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginHorizontal="6dp"
    android:layout_marginVertical="4dp"
    app:cardCornerRadius="12dp"
    app:strokeWidth="1dp"
    app:strokeColor="#E2E8F0"
    app:cardBackgroundColor="@android:color/white">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center_vertical"
        android:orientation="horizontal"
        android:padding="12dp">

        <com.google.android.material.checkbox.MaterialCheckBox
            android:id="@+id/cbSelectMonth"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content" />

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginStart="8dp"
            android:orientation="vertical">

            <TextView
                android:id="@+id/tvMonthName"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="January"
                android:textColor="#0F3D6E"
                android:textSize="15sp"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/tvFeeBreakdown"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="2dp"
                android:text="Fee: ₹0 | Fine: ₹0"
                android:textColor="#64748B"
                android:textSize="12sp" />
        </LinearLayout>

        <TextView
            android:id="@+id/tvStatusBadge"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:paddingHorizontal="10dp"
            android:paddingVertical="4dp"
            android:text="DUE"
            android:textSize="11sp"
            android:textStyle="bold"
            android:textColor="#DC2626"
            android:background="#FEE2E2" />

    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
FILE_EOF

cat << 'FILE_EOF' > app/src/main/res/layout/activity_student_management.xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F8FAFC">

    <com.google.android.material.appbar.AppBarLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            android:background="#0F3D6E"
            app:title="Student Directory"
            app:titleTextColor="@android:color/white"
            app:navigationIcon="@android:drawable/ic_menu_revert" />
    </com.google.android.material.appbar.AppBarLayout>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:padding="12dp"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <com.google.android.material.textfield.TextInputLayout
            android:id="@+id/tilClassFilter"
            style="@style/Widget.Material3.TextInputLayout.OutlinedBox.ExposedDropdownMenu"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="8dp"
            android:hint="Select Class Category">

            <AutoCompleteTextView
                android:id="@+id/actvClassFilter"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="none" />
        </com.google.android.material.textfield.TextInputLayout>

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/rvStudents"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1" />

    </LinearLayout>

    <com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
        android:id="@+id/fabAddStudent"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|end"
        android:layout_margin="16dp"
        android:text="Enroll Student"
        android:textColor="@android:color/white"
        app:icon="@android:drawable/ic_input_add"
        app:iconTint="@android:color/white"
        app:backgroundTint="#0F3D6E" />

</androidx.coordinatorlayout.widget.CoordinatorLayout>
FILE_EOF

cat << 'FILE_EOF' > app/src/main/res/layout/item_student_card.xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginVertical="5dp"
    app:cardCornerRadius="14dp"
    app:strokeWidth="1dp"
    app:strokeColor="#CBD5E1"
    app:cardBackgroundColor="@android:color/white">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center_vertical"
        android:orientation="horizontal"
        android:padding="12dp">

        <ImageView
            android:id="@+id/ivStudentAvatar"
            android:layout_width="50dp"
            android:layout_height="50dp"
            android:background="#E2E8F0"
            android:scaleType="centerCrop"
            android:src="@android:drawable/sym_def_app_icon" />

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginStart="12dp"
            android:orientation="vertical">

            <TextView
                android:id="@+id/tvStudentName"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Student Name"
                android:textSize="15sp"
                android:textStyle="bold"
                android:textColor="#0F3D6E" />

            <TextView
                android:id="@+id/tvStudentMeta"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="2dp"
                android:text="Class 5 • Roll: 1"
                android:textSize="12sp"
                android:textColor="#64748B" />

            <TextView
                android:id="@+id/tvParentsName"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Father: -- | Phone: --"
                android:textSize="11sp"
                android:textColor="#94A3B8" />
        </LinearLayout>

        <TextView
            android:id="@+id/tvMonthlyFeeBadge"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:background="#FEF3C7"
            android:paddingHorizontal="8dp"
            android:paddingVertical="4dp"
            android:text="₹450/m"
            android:textColor="#D97706"
            android:textSize="11sp"
            android:textStyle="bold" />

    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
FILE_EOF

cat << 'FILE_EOF' > app/src/main/res/layout/activity_add_student.xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F8FAFC">

    <com.google.android.material.appbar.AppBarLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            android:background="#0F3D6E"
            app:title="Enroll New Student"
            app:titleTextColor="@android:color/white"
            app:navigationIcon="@android:drawable/ic_menu_revert" />
    </com.google.android.material.appbar.AppBarLayout>

    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="20dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:gravity="center"
                android:orientation="vertical">

                <ImageView
                    android:id="@+id/ivStudentPhoto"
                    android:layout_width="90dp"
                    android:layout_height="90dp"
                    android:background="#E2E8F0"
                    android:scaleType="centerCrop"
                    android:src="@android:drawable/ic_menu_camera" />

                <com.google.android.material.button.MaterialButton
                    android:id="@+id/btnUploadPhoto"
                    style="@style/Widget.Material3.Button.TextButton"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Set Student Photo"
                    android:textColor="#0F3D6E" />
            </LinearLayout>

            <com.google.android.material.textfield.TextInputLayout
                style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:hint="Student Name">
                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/etStudentName"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content" />
            </com.google.android.material.textfield.TextInputLayout>

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:orientation="horizontal">

                <com.google.android.material.textfield.TextInputLayout
                    style="@style/Widget.Material3.TextInputLayout.OutlinedBox.ExposedDropdownMenu"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1.3"
                    android:layout_marginEnd="4dp"
                    android:hint="Class Unit">
                    <AutoCompleteTextView
                        android:id="@+id/actvClass"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:inputType="none" />
                </com.google.android.material.textfield.TextInputLayout>

                <com.google.android.material.textfield.TextInputLayout
                    style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="0.7"
                    android:layout_marginStart="4dp"
                    android:hint="Roll No">
                    <com.google.android.material.textfield.TextInputEditText
                        android:id="@+id/etRollNo"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:inputType="number" />
                </com.google.android.material.textfield.TextInputLayout>
            </LinearLayout>

            <com.google.android.material.textfield.TextInputLayout
                style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:hint="Father's Name">
                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/etFatherName"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content" />
            </com.google.android.material.textfield.TextInputLayout>

            <com.google.android.material.textfield.TextInputLayout
                style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:hint="Mother's Name">
                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/etMotherName"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content" />
            </com.google.android.material.textfield.TextInputLayout>

            <com.google.android.material.textfield.TextInputLayout
                style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:hint="Contact Number (WhatsApp)">
                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/etContactPhone"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="phone" />
            </com.google.android.material.textfield.TextInputLayout>

            <com.google.android.material.textfield.TextInputLayout
                style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:hint="Address (Dhupguri, Jalpaiguri...)">
                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/etAddress"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content" />
            </com.google.android.material.textfield.TextInputLayout>

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btnSaveStudent"
                android:layout_width="match_parent"
                android:layout_height="54dp"
                android:layout_marginTop="20dp"
                android:text="Enroll Student"
                android:textStyle="bold"
                app:cornerRadius="12dp"
                app:backgroundTint="#0F3D6E" />

        </LinearLayout>
    </androidx.core.widget.NestedScrollView>
</androidx.coordinatorlayout.widget.CoordinatorLayout>
FILE_EOF

cat << 'FILE_EOF' > app/src/main/res/layout/activity_class_management.xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F8FAFC">

    <com.google.android.material.appbar.AppBarLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            android:background="#0F3D6E"
            app:title="Manage School Classes"
            app:titleTextColor="@android:color/white"
            app:navigationIcon="@android:drawable/ic_menu_revert" />
    </com.google.android.material.appbar.AppBarLayout>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:padding="16dp"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <com.google.android.material.card.MaterialCardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            app:cardCornerRadius="16dp"
            app:cardBackgroundColor="@android:color/white"
            app:strokeWidth="1dp"
            app:strokeColor="#CBD5E1">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="16dp">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Create New Class"
                    android:textSize="16sp"
                    android:textStyle="bold"
                    android:textColor="#0F3D6E" />

                <com.google.android.material.textfield.TextInputLayout
                    style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="10dp"
                    android:hint="Class Name (e.g. Class 5, Nursery)">
                    <com.google.android.material.textfield.TextInputEditText
                        android:id="@+id/etClassName"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content" />
                </com.google.android.material.textfield.TextInputLayout>

                <com.google.android.material.textfield.TextInputLayout
                    style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp"
                    android:hint="Monthly Fee (₹)">
                    <com.google.android.material.textfield.TextInputEditText
                        android:id="@+id/etMonthlyFee"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:inputType="numberDecimal" />
                </com.google.android.material.textfield.TextInputLayout>

                <com.google.android.material.checkbox.MaterialCheckBox
                    android:id="@+id/cbDivideSection"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp"
                    android:text="Divide this class into sections (A, B...)"
                    android:textColor="#334155" />

                <com.google.android.material.textfield.TextInputLayout
                    android:id="@+id/tilSections"
                    style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="4dp"
                    android:hint="Sections (comma separated, e.g. A, B)"
                    android:visibility="gone">
                    <com.google.android.material.textfield.TextInputEditText
                        android:id="@+id/etSections"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="A, B" />
                </com.google.android.material.textfield.TextInputLayout>

                <com.google.android.material.button.MaterialButton
                    android:id="@+id/btnAddClass"
                    android:layout_width="match_parent"
                    android:layout_height="50dp"
                    android:layout_marginTop="14dp"
                    android:text="Save &amp; Register Class"
                    android:textStyle="bold"
                    app:cornerRadius="10dp"
                    app:backgroundTint="#0F3D6E" />

            </LinearLayout>
        </com.google.android.material.card.MaterialCardView>

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:layout_marginBottom="8dp"
            android:text="Registered Classes &amp; Units"
            android:textStyle="bold"
            android:textColor="#1E293B" />

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/rvClasses"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1" />
    </LinearLayout>
</androidx.coordinatorlayout.widget.CoordinatorLayout>
FILE_EOF

cat << 'FILE_EOF' > app/src/main/res/layout/activity_teacher_management.xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F8FAFC">

    <com.google.android.material.appbar.AppBarLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            android:background="#0F3D6E"
            app:title="Teacher Directory"
            app:titleTextColor="@android:color/white"
            app:navigationIcon="@android:drawable/ic_menu_revert" />
    </com.google.android.material.appbar.AppBarLayout>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvTeachers"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:padding="12dp"
        app:layout_behavior="@string/appbar_scrolling_view_behavior" />

    <com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
        android:id="@+id/fabAddTeacher"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|end"
        android:layout_margin="16dp"
        android:text="Add Teacher"
        android:textColor="@android:color/white"
        app:icon="@android:drawable/ic_input_add"
        app:iconTint="@android:color/white"
        app:backgroundTint="#0F3D6E" />

</androidx.coordinatorlayout.widget.CoordinatorLayout>
FILE_EOF

cat << 'FILE_EOF' > app/src/main/res/layout/item_teacher_card.xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginVertical="6dp"
    app:cardCornerRadius="14dp"
    app:strokeWidth="1dp"
    app:strokeColor="#CBD5E1"
    app:cardBackgroundColor="@android:color/white">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center_vertical"
        android:orientation="horizontal"
        android:padding="14dp">

        <ImageView
            android:id="@+id/ivTeacherAvatar"
            android:layout_width="52dp"
            android:layout_height="52dp"
            android:background="#E2E8F0"
            android:scaleType="centerCrop"
            android:src="@android:drawable/sym_def_app_icon" />

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginStart="12dp"
            android:orientation="vertical">

            <TextView
                android:id="@+id/tvTeacherName"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Teacher Name"
                android:textSize="15sp"
                android:textStyle="bold"
                android:textColor="#0F3D6E" />

            <TextView
                android:id="@+id/tvTeacherPhone"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="2dp"
                android:text="Contact: --"
                android:textSize="12sp"
                android:textColor="#64748B" />

            <TextView
                android:id="@+id/tvTeacherPassword"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="2dp"
                android:text="Password: --"
                android:textSize="11sp"
                android:textColor="#0F3D6E" />
        </LinearLayout>

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:background="#FEF3C7"
            android:paddingHorizontal="10dp"
            android:paddingVertical="4dp"
            android:text="TEACHER"
            android:textColor="#D97706"
            android:textSize="11sp"
            android:textStyle="bold" />

    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
FILE_EOF

cat << 'FILE_EOF' > app/src/main/res/layout/activity_add_teacher.xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F8FAFC">

    <com.google.android.material.appbar.AppBarLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            android:background="#0F3D6E"
            app:title="Add Teacher / Faculty"
            app:titleTextColor="@android:color/white"
            app:navigationIcon="@android:drawable/ic_menu_revert" />
    </com.google.android.material.appbar.AppBarLayout>

    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="20dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:gravity="center"
                android:orientation="vertical">

                <ImageView
                    android:id="@+id/ivTeacherPhoto"
                    android:layout_width="90dp"
                    android:layout_height="90dp"
                    android:background="#E2E8F0"
                    android:scaleType="centerCrop"
                    android:src="@android:drawable/ic_menu_camera" />

                <com.google.android.material.button.MaterialButton
                    android:id="@+id/btnUploadTeacherPhoto"
                    style="@style/Widget.Material3.Button.TextButton"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Set Profile Photo"
                    android:textColor="#0F3D6E" />
            </LinearLayout>

            <com.google.android.material.textfield.TextInputLayout
                style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:hint="Teacher Full Name">
                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/etTeacherName"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content" />
            </com.google.android.material.textfield.TextInputLayout>

            <com.google.android.material.textfield.TextInputLayout
                style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="12dp"
                android:hint="Contact Number (Used for Login ID)">
                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/etTeacherPhone"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="phone" />
            </com.google.android.material.textfield.TextInputLayout>

            <com.google.android.material.textfield.TextInputLayout
                style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="12dp"
                android:hint="Teacher Login Password"
                app:passwordToggleEnabled="true">
                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/etTeacherPassword"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="textPassword" />
            </com.google.android.material.textfield.TextInputLayout>

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btnSaveTeacher"
                android:layout_width="match_parent"
                android:layout_height="54dp"
                android:layout_marginTop="24dp"
                android:text="Save Teacher Account"
                android:textStyle="bold"
                app:cornerRadius="12dp"
                app:backgroundTint="#0F3D6E" />

        </LinearLayout>
    </androidx.core.widget.NestedScrollView>
</androidx.coordinatorlayout.widget.CoordinatorLayout>
FILE_EOF

cat << 'FILE_EOF' > app/src/main/res/layout/activity_teacher_portal.xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F8FAFC">

    <com.google.android.material.appbar.AppBarLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            android:background="#0F3D6E"
            app:title="Teacher Mark Entry Portal"
            app:titleTextColor="@android:color/white" />
    </com.google.android.material.appbar.AppBarLayout>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:padding="16dp"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <com.google.android.material.textfield.TextInputLayout
            style="@style/Widget.Material3.TextInputLayout.OutlinedBox.ExposedDropdownMenu"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="Select Student">

            <AutoCompleteTextView
                android:id="@+id/actvStudent"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="none" />
        </com.google.android.material.textfield.TextInputLayout>

        <com.google.android.material.textfield.TextInputLayout
            style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:hint="Exam Name">

            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/etExamName"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Annual Examination 2026" />
        </com.google.android.material.textfield.TextInputLayout>

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:layout_marginBottom="4dp"
            android:text="Enter Marks (Will be sent to Admin for approval)"
            android:textColor="#1E293B"
            android:textStyle="bold" />

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/rvMarks"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnSubmitToAdmin"
            android:layout_width="match_parent"
            android:layout_height="52dp"
            android:layout_marginTop="8dp"
            android:text="Submit Marks for Admin Approval"
            android:textStyle="bold"
            app:cornerRadius="10dp"
            app:backgroundTint="#0F3D6E" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnTeacherLogout"
            style="@style/Widget.Material3.Button.TextButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Sign Out"
            android:textColor="#DC2626" />

    </LinearLayout>
</androidx.coordinatorlayout.widget.CoordinatorLayout>
FILE_EOF

cat << 'FILE_EOF' > app/src/main/res/layout/activity_result_publish.xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F8FAFC">

    <com.google.android.material.appbar.AppBarLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            android:background="#0F3D6E"
            app:title="Publish Exam Results"
            app:titleTextColor="@android:color/white"
            app:navigationIcon="@android:drawable/ic_menu_revert" />
    </com.google.android.material.appbar.AppBarLayout>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:padding="16dp"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <com.google.android.material.textfield.TextInputLayout
            style="@style/Widget.Material3.TextInputLayout.OutlinedBox.ExposedDropdownMenu"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="Select Student">

            <AutoCompleteTextView
                android:id="@+id/actvStudentSelect"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="none" />
        </com.google.android.material.textfield.TextInputLayout>

        <com.google.android.material.textfield.TextInputLayout
            style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:hint="Exam Name">

            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/etExamName"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Annual Examination 2026" />
        </com.google.android.material.textfield.TextInputLayout>

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:layout_marginBottom="4dp"
            android:text="Enter Marks by Subject"
            android:textColor="#1E293B"
            android:textSize="13sp"
            android:textStyle="bold" />

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/rvSubjects"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1" />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="10dp"
            android:orientation="horizontal">

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btnSaveMarks"
                style="@style/Widget.Material3.Button.OutlinedButton"
                android:layout_width="0dp"
                android:layout_height="50dp"
                android:layout_weight="1"
                android:layout_marginEnd="6dp"
                android:text="Save Marks"
                android:textColor="#0F3D6E"
                app:cornerRadius="10dp"
                app:strokeColor="#0F3D6E" />

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btnGeneratePdf"
                android:layout_width="0dp"
                android:layout_height="50dp"
                android:layout_weight="1.3"
                android:layout_marginStart="6dp"
                android:text="Print 2-Page PDF"
                android:textStyle="bold"
                app:backgroundTint="#0F3D6E"
                app:cornerRadius="10dp" />
        </LinearLayout>

    </LinearLayout>
</androidx.coordinatorlayout.widget.CoordinatorLayout>
FILE_EOF

cat << 'FILE_EOF' > app/src/main/res/layout/item_subject_mark_input.xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginHorizontal="6dp"
    android:layout_marginVertical="4dp"
    app:cardCornerRadius="12dp"
    app:strokeWidth="1dp"
    app:strokeColor="#CBD5E1"
    app:cardBackgroundColor="@android:color/white">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="12dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical">

            <TextView
                android:id="@+id/tvSubjectName"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Subject Name"
                android:textColor="#0F3D6E"
                android:textSize="14sp"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/tvSubjectGradeBadge"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:background="#DCFCE7"
                android:paddingHorizontal="8dp"
                android:paddingVertical="2dp"
                android:text="Grade: --"
                android:textColor="#16A34A"
                android:textSize="11sp"
                android:textStyle="bold" />
        </LinearLayout>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:orientation="horizontal">

            <com.google.android.material.textfield.TextInputLayout
                style="@style/Widget.Material3.TextInputLayout.OutlinedBox.Dense"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:layout_marginEnd="4dp"
                android:hint="Full">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/etFullMarks"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="number"
                    android:text="100" />
            </com.google.android.material.textfield.TextInputLayout>

            <com.google.android.material.textfield.TextInputLayout
                style="@style/Widget.Material3.TextInputLayout.OutlinedBox.Dense"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:layout_marginHorizontal="4dp"
                android:hint="Written">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/etWrittenMarks"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="number"
                    android:text="0" />
            </com.google.android.material.textfield.TextInputLayout>

            <com.google.android.material.textfield.TextInputLayout
                style="@style/Widget.Material3.TextInputLayout.OutlinedBox.Dense"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:layout_marginStart="4dp"
                android:hint="Practical">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/etPracticalMarks"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="number"
                    android:text="0" />
            </com.google.android.material.textfield.TextInputLayout>
        </LinearLayout>

    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
FILE_EOF

echo "=== [5/8] Writing Database & Domain Kotlin Classes ==="
cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/data/Entities.kt
package com.vidyamandir.schoolmanager.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "classes")
data class SchoolClass(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val className: String,
    val monthlyFee: Double,
    val hasSections: Boolean = false,
    val sections: String = ""
) {
    fun getDisplayUnits(): List<Pair<String, String>> {
        if (!hasSections || sections.isBlank()) {
            return listOf(Pair(className, ""))
        }
        return sections.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { sec -> Pair(className, sec) }
    }
}

@Entity(tableName = "teachers")
data class Teacher(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val password: String,
    val photoUri: String? = null
)

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val admissionNo: String,
    val fullName: String,
    val className: String,
    val section: String = "",
    val rollNo: Int,
    val fatherName: String = "",
    val motherName: String = "",
    val parentPhone: String,
    val address: String = "",
    val photoUri: String? = null,
    val admissionMonth: Int = 1,
    val monthlyFee: Double,
    val isActive: Boolean = true
) {
    val displayClass: String
        get() = if (section.isNotBlank()) "$className (Sec $section)" else className
}

@Entity(
    tableName = "fee_payments",
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FeePayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val studentName: String = "",
    val receiptNo: String,
    val academicYear: Int,
    val paidMonths: String,
    val baseAmount: Double,
    val lateFeeAmount: Double,
    val totalAmount: Double,
    val paymentDate: Long
)

@Entity(tableName = "late_fee_rules")
data class LateFeeRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayFrom: Int,
    val dayTo: Int,
    val fineAmount: Double
)

@Entity(
    tableName = "exam_results",
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExamResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val examName: String,
    val academicYear: Int,
    val subjectName: String,
    val fullMarks: Int,
    val writtenMarks: Int,
    val practicalMarks: Int,
    val grade: String,
    val submittedByTeacher: String = "Admin",
    val approvalStatus: String = "APPROVED"
) {
    val obtainedMarks: Int get() = writtenMarks + practicalMarks
}
FILE_EOF

cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/data/SchoolDao.kt
package com.vidyamandir.schoolmanager.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolDao {

    @Query("SELECT COUNT(*) FROM students WHERE isActive = 1")
    fun getStudentCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM classes")
    fun getClassCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM teachers")
    fun getTeacherCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM exam_results WHERE approvalStatus = 'PENDING'")
    fun getPendingMarksheetCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(schoolClass: SchoolClass): Long

    @Query("SELECT * FROM classes ORDER BY id ASC")
    fun getAllClasses(): Flow<List<SchoolClass>>

    @Query("SELECT * FROM classes WHERE className = :name LIMIT 1")
    suspend fun getClassByName(name: String): SchoolClass?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: Teacher): Long

    @Query("SELECT * FROM teachers ORDER BY name ASC")
    fun getAllTeachers(): Flow<List<Teacher>>

    @Query("SELECT * FROM teachers WHERE password = :pass LIMIT 1")
    suspend fun getTeacherByPassword(pass: String): Teacher?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Query("SELECT * FROM students WHERE isActive = 1 ORDER BY className ASC, rollNo ASC")
    fun getAllActiveStudents(): Flow<List<Student>>

    @Insert
    suspend fun recordPayment(payment: FeePayment): Long

    @Query("SELECT * FROM fee_payments ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<FeePayment>>

    @Query("SELECT * FROM fee_payments WHERE paymentDate >= :startOfDayTimestamp ORDER BY paymentDate DESC")
    fun getTodayPayments(startOfDayTimestamp: Long): Flow<List<FeePayment>>

    @Query("SELECT * FROM fee_payments WHERE studentId = :studentId AND academicYear = :year ORDER BY paymentDate DESC")
    fun getStudentPayments(studentId: Long, year: Int): Flow<List<FeePayment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLateFeeRule(rule: LateFeeRule): Long

    @Query("SELECT * FROM late_fee_rules ORDER BY dayFrom ASC")
    suspend fun getAllLateFeeRules(): List<LateFeeRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(results: List<ExamResult>)

    @Query("SELECT * FROM exam_results WHERE approvalStatus = 'PENDING'")
    fun getPendingMarksheets(): Flow<List<ExamResult>>

    @Query("UPDATE exam_results SET approvalStatus = :status WHERE id = :id")
    suspend fun updateMarksheetStatus(id: Long, status: String)
}
FILE_EOF

cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/data/AppDatabase.kt
package com.vidyamandir.schoolmanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SchoolClass::class,
        Teacher::class,
        Student::class,
        FeePayment::class,
        LateFeeRule::class,
        ExamResult::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun schoolDao(): SchoolDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vivekananda_vidyamandir.db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        val dao = database.schoolDao()
                        // Zero demo classes inserted. Starts completely fresh.
                        dao.insertLateFeeRule(LateFeeRule(dayFrom = 1, dayTo = 10, fineAmount = 0.0))
                        dao.insertLateFeeRule(LateFeeRule(dayFrom = 11, dayTo = 20, fineAmount = 50.0))
                        dao.insertLateFeeRule(LateFeeRule(dayFrom = 21, dayTo = 30, fineAmount = 100.0))
                    }
                }
            }
        }
    }
}
FILE_EOF

cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/domain/FeeCalculator.kt
package com.vidyamandir.schoolmanager.domain

import com.vidyamandir.schoolmanager.data.LateFeeRule
import com.vidyamandir.schoolmanager.data.Student
import java.util.Calendar

data class MonthDueStatus(
    val monthIndex: Int,
    val monthName: String,
    val baseFee: Double,
    val lateFee: Double,
    val isDue: Boolean,
    val isPaid: Boolean
)

object FeeCalculator {
    private val MONTH_NAMES = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    fun calculateDueMonths(
        student: Student,
        paidMonths: Set<Int>,
        activeRules: List<LateFeeRule>,
        currentCalendar: Calendar = Calendar.getInstance()
    ): List<MonthDueStatus> {
        val currentMonth = currentCalendar.get(Calendar.MONTH) + 1
        val currentDay = currentCalendar.get(Calendar.DAY_OF_MONTH)
        val statusList = mutableListOf<MonthDueStatus>()

        for (m in 1..12) {
            val name = MONTH_NAMES[m - 1]
            if (paidMonths.contains(m)) {
                statusList.add(MonthDueStatus(m, name, student.monthlyFee, 0.0, isDue = false, isPaid = true))
                continue
            }

            val isEligibleDue = m >= student.admissionMonth && m <= currentMonth
            val lateFee = if (isEligibleDue) {
                val overdueDays = if (m == currentMonth) (currentDay - 1) else 30
                computeLateFine(overdueDays, activeRules)
            } else {
                0.0
            }

            statusList.add(
                MonthDueStatus(
                    monthIndex = m,
                    monthName = name,
                    baseFee = student.monthlyFee,
                    lateFee = lateFee,
                    isDue = isEligibleDue,
                    isPaid = false
                )
            )
        }
        return statusList
    }

    private fun computeLateFine(overdueDays: Int, rules: List<LateFeeRule>): Double {
        if (overdueDays <= 10) return 0.0
        val matchedRule = rules.firstOrNull { overdueDays in it.dayFrom..it.dayTo }
        return matchedRule?.fineAmount ?: 0.0
    }
}
FILE_EOF

echo "=== [6/8] Writing Utilities ==="
cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/util/ImageHelper.kt
package com.vidyamandir.schoolmanager.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import java.io.File
import java.io.FileOutputStream

object ImageHelper {

    fun saveImageToInternalStorage(context: Context, uri: Uri, prefix: String): String? {
        return try {
            val directory = File(context.filesDir, "profiles").apply { if (!exists()) mkdirs() }
            val file = File(directory, "${prefix}_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun loadProfilePhoto(filePath: String?, imageView: ImageView, defaultResId: Int) {
        if (filePath.isNullOrBlank()) {
            imageView.setImageResource(defaultResId)
            return
        }
        try {
            val file = File(filePath)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    return
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        imageView.setImageResource(defaultResId)
    }
}
FILE_EOF

cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/util/ResultPdfGenerator.kt
package com.vidyamandir.schoolmanager.util

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.vidyamandir.schoolmanager.data.ExamResult
import com.vidyamandir.schoolmanager.data.Student
import java.io.File
import java.io.FileOutputStream

class ResultPdfGenerator(private val context: Context) {

    private val a4Width = 595
    private val a4Height = 842

    fun generateReportCard(
        student: Student,
        examName: String,
        results: List<ExamResult>,
        outputFile: File
    ) {
        val document = PdfDocument()

        val pageInfo1 = PdfDocument.PageInfo.Builder(a4Width, a4Height, 1).create()
        val page1 = document.startPage(pageInfo1)
        drawPageOne(page1.canvas, student, examName)
        document.finishPage(page1)

        val pageInfo2 = PdfDocument.PageInfo.Builder(a4Width, a4Height, 2).create()
        val page2 = document.startPage(pageInfo2)
        drawPageTwo(page2.canvas, student, results)
        document.finishPage(page2)

        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()
    }

    private fun drawPageOne(canvas: Canvas, student: Student, examName: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = Color.parseColor("#0F3D6E")
        canvas.drawRect(24f, 24f, (a4Width - 24).toFloat(), (a4Height - 24).toFloat(), paint)

        paint.strokeWidth = 1f
        canvas.drawRect(28f, 28f, (a4Width - 28).toFloat(), (a4Height - 28).toFloat(), paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#0F3D6E")
        paint.textSize = 24f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("VIVEKANANDA VIDYAMANDIR", (a4Width / 2).toFloat(), 95f, paint)

        paint.textSize = 12f
        paint.isFakeBoldText = false
        paint.color = Color.DKGRAY
        canvas.drawText("Dhupguri, Jalpaiguri, West Bengal - 735210", (a4Width / 2).toFloat(), 120f, paint)

        paint.textSize = 14f
        paint.isFakeBoldText = true
        paint.color = Color.parseColor("#D97706")
        canvas.drawText(examName.uppercase(), (a4Width / 2).toFloat(), 160f, paint)

        val centerX = (a4Width / 2).toFloat()
        val centerY = 310f

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#FEF3C7")
        canvas.drawCircle(centerX, centerY, 100f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.parseColor("#D97706")
        canvas.drawCircle(centerX, centerY, 102f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#0F3D6E")
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText("SWAMI VIVEKANANDA", centerX, centerY - 15f, paint)
        paint.textSize = 10f
        paint.color = Color.parseColor("#B45309")
        canvas.drawText("★ MOTTO OF EDUCATION ★", centerX, centerY + 10f, paint)

        val quoteTop = 460f
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(45f, quoteTop, (a4Width - 45).toFloat(), quoteTop + 95f, 10f, 10f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawRoundRect(45f, quoteTop, (a4Width - 45).toFloat(), quoteTop + 95f, 10f, 10f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#0F3D6E")
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText(
            "\"Education is the manifestation of the perfection already in man.\"",
            centerX,
            quoteTop + 40f,
            paint
        )
        paint.textSize = 11f
        paint.isFakeBoldText = false
        paint.color = Color.DKGRAY
        canvas.drawText("— Swami Vivekananda", centerX, quoteTop + 68f, paint)

        val profileTop = 590f
        paint.color = Color.parseColor("#FFFFFF")
        canvas.drawRoundRect(45f, profileTop, (a4Width - 45).toFloat(), profileTop + 140f, 10f, 10f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawRoundRect(45f, profileTop, (a4Width - 45).toFloat(), profileTop + 140f, 10f, 10f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#0F3D6E")
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("STUDENT PROFILE", centerX, profileTop + 30f, paint)

        paint.textSize = 12f
        paint.color = Color.BLACK
        paint.isFakeBoldText = false
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Student Name: ${student.fullName}", 70f, profileTop + 65f, paint)
        canvas.drawText("Admission ID: ${student.admissionNo}", 70f, profileTop + 95f, paint)
        canvas.drawText("Class: ${student.displayClass}", 320f, profileTop + 65f, paint)
        canvas.drawText("Roll Number: ${student.rollNo}", 320f, profileTop + 95f, paint)
    }

    private fun drawPageTwo(canvas: Canvas, student: Student, results: List<ExamResult>) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = Color.parseColor("#0F3D6E")
        paint.textSize = 18f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("ACADEMIC PROGRESS STATEMENT", (a4Width / 2).toFloat(), 55f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.LTGRAY
        canvas.drawRect(40f, 75f, (a4Width - 40).toFloat(), 115f, paint)

        paint.style = Paint.Style.FILL
        paint.textSize = 10f
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.BLACK
        paint.isFakeBoldText = false
        canvas.drawText("Student: ${student.fullName}", 50f, 98f, paint)
        canvas.drawText("Class: ${student.displayClass}", 260f, 98f, paint)
        canvas.drawText("Roll: ${student.rollNo}", 440f, 98f, paint)

        var y = 145f
        paint.color = Color.parseColor("#0F3D6E")
        canvas.drawRect(40f, y - 18f, (a4Width - 40).toFloat(), y + 8f, paint)

        paint.color = Color.WHITE
        paint.isFakeBoldText = true
        paint.textSize = 10f
        canvas.drawText("Subject Name", 50f, y - 4f, paint)
        canvas.drawText("Full", 230f, y - 4f, paint)
        canvas.drawText("Written", 290f, y - 4f, paint)
        canvas.drawText("Practical", 360f, y - 4f, paint)
        canvas.drawText("Obtained", 440f, y - 4f, paint)
        canvas.drawText("Grade", 510f, y - 4f, paint)

        paint.color = Color.BLACK
        paint.isFakeBoldText = false
        var grandFull = 0
        var grandObtained = 0
        var isPassed = true

        y += 24f
        for (res in results) {
            grandFull += res.fullMarks
            grandObtained += res.obtainedMarks
            if (res.obtainedMarks < (res.fullMarks * 0.35)) isPassed = false

            canvas.drawText(res.subjectName, 50f, y, paint)
            canvas.drawText(res.fullMarks.toString(), 230f, y, paint)
            canvas.drawText(res.writtenMarks.toString(), 290f, y, paint)
            canvas.drawText(res.practicalMarks.toString(), 360f, y, paint)
            canvas.drawText(res.obtainedMarks.toString(), 440f, y, paint)
            canvas.drawText(res.grade, 510f, y, paint)

            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawLine(40f, y + 5f, (a4Width - 40).toFloat(), y + 5f, paint)
            paint.color = Color.BLACK
            y += 24f
        }

        y += 20f
        val percentage = if (grandFull > 0) (grandObtained.toDouble() / grandFull * 100) else 0.0
        val finalStatus = if (isPassed) "PASSED" else "FAILED"
        val overallGrade = when {
            percentage >= 90 -> "AA"
            percentage >= 80 -> "A+"
            percentage >= 70 -> "A"
            percentage >= 60 -> "B+"
            percentage >= 50 -> "B"
            percentage >= 35 -> "C"
            percentage >= 25 -> "D"
            else -> "F"
        }

        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRoundRect(40f, y, (a4Width - 40).toFloat(), y + 60f, 6f, 6f, paint)

        paint.color = Color.parseColor("#0F3D6E")
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas.drawText("Grand Total: $grandObtained / $grandFull", 55f, y + 26f, paint)
        canvas.drawText(String.format("Percentage: %.2f%%", percentage), 55f, y + 46f, paint)
        canvas.drawText("Overall Grade: $overallGrade", 270f, y + 36f, paint)

        paint.color = if (isPassed) Color.parseColor("#16A34A") else Color.parseColor("#DC2626")
        paint.textSize = 14f
        canvas.drawText("Result: $finalStatus", 420f, y + 36f, paint)

        val signY = a4Height - 90f
        paint.color = Color.GRAY
        canvas.drawLine(50f, signY, 180f, signY, paint)
        canvas.drawLine((a4Width - 190).toFloat(), signY, (a4Width - 50).toFloat(), signY, paint)

        paint.color = Color.BLACK
        paint.textSize = 10f
        paint.isFakeBoldText = false
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Class Teacher Signature", 115f, signY + 18f, paint)
        canvas.drawText("Headmaster / Seal", (a4Width - 120).toFloat(), signY + 18f, paint)
    }
}
FILE_EOF

echo "=== [7/8] Writing UI Activities ==="
cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/ui/LoginActivity.kt
package com.vidyamandir.schoolmanager.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vidyamandir.schoolmanager.MainActivity
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.databinding.ActivityLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var db: AppDatabase

    companion object {
        private const val PREFS_NAME = "vv_auth_session"
        private const val KEY_ROLE = "session_role"
        private const val KEY_TEACHER_NAME = "teacher_name"
        private const val SCHOOL_CODE = "VV001"
        private const val ADMIN_PASSWORD = "India360@"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val role = prefs.getString(KEY_ROLE, null)
        if (role == "ADMIN") {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        } else if (role == "TEACHER") {
            startActivity(Intent(this, TeacherPortalActivity::class.java))
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this, lifecycleScope)

        binding.btnLogin.setOnClickListener {
            authenticate()
        }
    }

    private fun authenticate() {
        val inputCode = binding.etSchoolCode.text?.toString()?.trim() ?: ""
        val inputPassword = binding.etPassword.text?.toString()?.trim() ?: ""

        if (!inputCode.equals(SCHOOL_CODE, ignoreCase = true)) {
            binding.tilSchoolCode.error = "Invalid School Code (Must be VV001)"
            return
        }
        binding.tilSchoolCode.error = null

        if (inputPassword.isEmpty()) {
            binding.tilPassword.error = "Please enter password"
            return
        }

        if (inputPassword == ADMIN_PASSWORD) {
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_ROLE, "ADMIN")
                .apply()
            Toast.makeText(this, "Logged in as Administrator", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val teacher = db.schoolDao().getTeacherByPassword(inputPassword)
            withContext(Dispatchers.Main) {
                if (teacher != null) {
                    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                        .putString(KEY_ROLE, "TEACHER")
                        .putString(KEY_TEACHER_NAME, teacher.name)
                        .apply()
                    Toast.makeText(this@LoginActivity, "Welcome Teacher ${teacher.name}!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, TeacherPortalActivity::class.java))
                    finish()
                } else {
                    binding.tilPassword.error = "Incorrect Password"
                    Toast.makeText(this@LoginActivity, "Authentication Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
FILE_EOF

cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/ui/MonthAdapter.kt
package com.vidyamandir.schoolmanager.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.databinding.ItemMonthFeeBinding
import com.vidyamandir.schoolmanager.domain.MonthDueStatus

class MonthAdapter(
    private val onSelectionChanged: (selectedMonths: List<MonthDueStatus>) -> Unit
) : RecyclerView.Adapter<MonthAdapter.MonthViewHolder>() {

    private var items: List<MonthDueStatus> = emptyList()
    private val selectedMonthIndices = mutableSetOf<Int>()

    fun submitList(newList: List<MonthDueStatus>) {
        items = newList
        selectedMonthIndices.clear()
        notifyDataSetChanged()
        onSelectionChanged(emptyList())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
        val binding = ItemMonthFeeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MonthViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class MonthViewHolder(private val binding: ItemMonthFeeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MonthDueStatus) {
            binding.tvMonthName.text = item.monthName

            if (item.isPaid) {
                binding.cbSelectMonth.isChecked = false
                binding.cbSelectMonth.isEnabled = false
                binding.tvFeeBreakdown.text = "Monthly fee already settled"
                binding.tvStatusBadge.text = "PAID"
                binding.tvStatusBadge.setTextColor(Color.parseColor("#16A34A"))
                binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#DCFCE7"))
                binding.root.setOnClickListener(null)
            } else {
                binding.cbSelectMonth.isEnabled = true
                binding.cbSelectMonth.isChecked = selectedMonthIndices.contains(item.monthIndex)

                if (item.isDue) {
                    binding.tvStatusBadge.text = "DUE"
                    binding.tvStatusBadge.setTextColor(Color.parseColor("#DC2626"))
                    binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#FEE2E2"))
                    binding.tvFeeBreakdown.text = "Fee: ₹${item.baseFee} | Fine: ₹${item.lateFee}"
                } else {
                    binding.tvStatusBadge.text = "ADVANCE"
                    binding.tvStatusBadge.setTextColor(Color.parseColor("#D97706"))
                    binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#FEF3C7"))
                    binding.tvFeeBreakdown.text = "Fee: ₹${item.baseFee} | Fine: ₹0"
                }

                val toggleAction = {
                    if (selectedMonthIndices.contains(item.monthIndex)) {
                        selectedMonthIndices.remove(item.monthIndex)
                    } else {
                        selectedMonthIndices.add(item.monthIndex)
                    }
                    notifyItemChanged(adapterPosition)
                    val selectedItems = items.filter { selectedMonthIndices.contains(it.monthIndex) }
                    onSelectionChanged(selectedItems)
                }

                binding.cbSelectMonth.setOnClickListener { toggleAction() }
                binding.root.setOnClickListener { toggleAction() }
            }
        }
    }
}
FILE_EOF

cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/ui/FeeCollectionActivity.kt
package com.vidyamandir.schoolmanager.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.FeePayment
import com.vidyamandir.schoolmanager.data.LateFeeRule
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.databinding.ActivityFeeCounterBinding
import com.vidyamandir.schoolmanager.domain.FeeCalculator
import com.vidyamandir.schoolmanager.domain.MonthDueStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeeCollectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeeCounterBinding
    private lateinit var adapter: MonthAdapter
    private lateinit var db: AppDatabase

    private var studentsList: List<Student> = emptyList()
    private var selectedStudent: Student? = null
    private var activeRules: List<LateFeeRule> = emptyList()
    private var currentlySelectedMonths: List<MonthDueStatus> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeeCounterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this, lifecycleScope)

        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = MonthAdapter { selectedMonths ->
            currentlySelectedMonths = selectedMonths
            calculateTotals(selectedMonths)
        }
        binding.rvMonths.layoutManager = LinearLayoutManager(this)
        binding.rvMonths.adapter = adapter

        loadInitialData()

        binding.actvStudentSelect.setOnItemClickListener { _, _, position, _ ->
            selectedStudent = studentsList[position]
            displayStudentInfo(selectedStudent!!)
            loadStudentFeeMonths(selectedStudent!!)
        }

        binding.btnSubmitPayment.setOnClickListener {
            submitPayment()
        }
    }

    private fun loadInitialData() {
        lifecycleScope.launch(Dispatchers.IO) {
            activeRules = db.schoolDao().getAllLateFeeRules()
            studentsList = db.schoolDao().getAllActiveStudents().first()

            withContext(Dispatchers.Main) {
                if (studentsList.isNotEmpty()) {
                    val names = studentsList.map { "${it.fullName} (${it.displayClass}) - Roll ${it.rollNo}" }
                    val arrayAdapter = ArrayAdapter(this@FeeCollectionActivity, android.R.layout.simple_dropdown_item_1line, names)
                    binding.actvStudentSelect.setAdapter(arrayAdapter)
                }
            }
        }
    }

    private fun displayStudentInfo(student: Student) {
        binding.tvSelectedStudentName.text = "${student.fullName} (Roll: ${student.rollNo})"
        binding.tvSelectedStudentMeta.text = "Class: ${student.displayClass} | Monthly Fee: ₹${student.monthlyFee}"
    }

    private fun loadStudentFeeMonths(student: Student) {
        lifecycleScope.launch(Dispatchers.IO) {
            val payments = db.schoolDao().getStudentPayments(student.id, 2026).first()
            val paidMonthsSet = mutableSetOf<Int>()

            payments.forEach { p ->
                p.paidMonths.split(",").filter { it.isNotBlank() }.forEach { m ->
                    paidMonthsSet.add(m.trim().toInt())
                }
            }

            val statusList = FeeCalculator.calculateDueMonths(student, paidMonthsSet, activeRules)

            withContext(Dispatchers.Main) {
                adapter.submitList(statusList)
            }
        }
    }

    private fun calculateTotals(selectedMonths: List<MonthDueStatus>) {
        val baseSum = selectedMonths.sumOf { it.baseFee }
        val lateSum = selectedMonths.sumOf { it.lateFee }
        val payableTotal = baseSum + lateSum

        binding.tvTotalBaseFee.text = "₹${String.format("%.2f", baseSum)}"
        binding.tvTotalLateFee.text = "₹${String.format("%.2f", lateSum)}"
        binding.tvTotalPayable.text = "₹${String.format("%.2f", payableTotal)}"
    }

    private fun submitPayment() {
        val student = selectedStudent
        if (student == null) {
            Toast.makeText(this, "Please select a student first", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentlySelectedMonths.isEmpty()) {
            Toast.makeText(this, "Please select at least one month to pay", Toast.LENGTH_SHORT).show()
            return
        }

        val baseSum = currentlySelectedMonths.sumOf { it.baseFee }
        val lateSum = currentlySelectedMonths.sumOf { it.lateFee }
        val total = baseSum + lateSum
        val monthsString = currentlySelectedMonths.joinToString(",") { it.monthIndex.toString() }
        val receipt = "REC-${System.currentTimeMillis().toString().takeLast(6)}"

        lifecycleScope.launch(Dispatchers.IO) {
            val payment = FeePayment(
                studentId = student.id,
                studentName = student.fullName,
                receiptNo = receipt,
                academicYear = 2026,
                paidMonths = monthsString,
                baseAmount = baseSum,
                lateFeeAmount = lateSum,
                totalAmount = total,
                paymentDate = System.currentTimeMillis()
            )
            db.schoolDao().recordPayment(payment)

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@FeeCollectionActivity,
                    "Payment Recorded Successfully! Receipt: $receipt",
                    Toast.LENGTH_LONG
                ).show()
                loadStudentFeeMonths(student)
            }
        }
    }
}
FILE_EOF

cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/ui/SubjectMarkAdapter.kt
package com.vidyamandir.schoolmanager.ui

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.databinding.ItemSubjectMarkInputBinding

data class SubjectScoreItem(
    val subjectName: String,
    var fullMarks: Int = 100,
    var writtenMarks: Int = 0,
    var practicalMarks: Int = 0
) {
    val obtainedMarks: Int get() = writtenMarks + practicalMarks

    val grade: String
        get() {
            val pct = if (fullMarks > 0) (obtainedMarks.toDouble() / fullMarks * 100) else 0.0
            return when {
                pct >= 90 -> "AA"
                pct >= 80 -> "A+"
                pct >= 70 -> "A"
                pct >= 60 -> "B+"
                pct >= 50 -> "B"
                pct >= 35 -> "C"
                pct >= 25 -> "D"
                else -> "F"
            }
        }
}

class SubjectMarkAdapter(
    private val items: List<SubjectScoreItem>
) : RecyclerView.Adapter<SubjectMarkAdapter.SubjectViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val binding = ItemSubjectMarkInputBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SubjectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class SubjectViewHolder(private val binding: ItemSubjectMarkInputBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SubjectScoreItem) {
            binding.tvSubjectName.text = item.subjectName
            binding.etFullMarks.setText(item.fullMarks.toString())
            binding.etWrittenMarks.setText(item.writtenMarks.toString())
            binding.etPracticalMarks.setText(item.practicalMarks.toString())
            updateBadge(item)

            binding.etFullMarks.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    item.fullMarks = s.toString().toIntOrNull() ?: 100
                    updateBadge(item)
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            binding.etWrittenMarks.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    item.writtenMarks = s.toString().toIntOrNull() ?: 0
                    updateBadge(item)
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            binding.etPracticalMarks.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    item.practicalMarks = s.toString().toIntOrNull() ?: 0
                    updateBadge(item)
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }

        private fun updateBadge(item: SubjectScoreItem) {
            binding.tvSubjectGradeBadge.text = "Grade: ${item.grade} (${item.obtainedMarks}/${item.fullMarks})"
        }
    }
}
FILE_EOF

cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/ui/ResultPublishActivity.kt
package com.vidyamandir.schoolmanager.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.ExamResult
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.databinding.ActivityResultPublishBinding
import com.vidyamandir.schoolmanager.util.ResultPdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ResultPublishActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultPublishBinding
    private lateinit var db: AppDatabase
    private lateinit var adapter: SubjectMarkAdapter

    private var studentsList: List<Student> = emptyList()
    private var selectedStudent: Student? = null

    private val subjectScoreItems = mutableListOf(
        SubjectScoreItem("First Language (Bengali)", 100, 75, 20),
        SubjectScoreItem("Second Language (English)", 100, 68, 18),
        SubjectScoreItem("Mathematics", 100, 78, 20),
        SubjectScoreItem("Physical Science", 100, 62, 19),
        SubjectScoreItem("Life Science", 100, 70, 18),
        SubjectScoreItem("History", 100, 65, 17),
        SubjectScoreItem("Geography", 100, 72, 18)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultPublishBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this, lifecycleScope)
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = SubjectMarkAdapter(subjectScoreItems)
        binding.rvSubjects.layoutManager = LinearLayoutManager(this)
        binding.rvSubjects.adapter = adapter

        loadStudents()

        binding.actvStudentSelect.setOnItemClickListener { _, _, position, _ ->
            selectedStudent = studentsList[position]
        }

        binding.btnSaveMarks.setOnClickListener {
            saveMarksToDatabase {
                Toast.makeText(this, "Marks saved successfully!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnGeneratePdf.setOnClickListener {
            saveMarksToDatabase {
                generateAndOpenPdf()
            }
        }
    }

    private fun loadStudents() {
        lifecycleScope.launch(Dispatchers.IO) {
            studentsList = db.schoolDao().getAllActiveStudents().first()
            withContext(Dispatchers.Main) {
                if (studentsList.isNotEmpty()) {
                    val names = studentsList.map { "${it.fullName} (${it.displayClass}) - Roll ${it.rollNo}" }
                    val arrayAdapter = ArrayAdapter(this@ResultPublishActivity, android.R.layout.simple_dropdown_item_1line, names)
                    binding.actvStudentSelect.setAdapter(arrayAdapter)
                    binding.actvStudentSelect.setText(names[0], false)
                    selectedStudent = studentsList[0]
                }
            }
        }
    }

    private fun saveMarksToDatabase(onSuccess: () -> Unit) {
        val student = selectedStudent
        if (student == null) {
            Toast.makeText(this, "Please select a student", Toast.LENGTH_SHORT).show()
            return
        }

        val examName = binding.etExamName.text.toString().trim().ifEmpty { "Annual Examination 2026" }

        val examResults = subjectScoreItems.map {
            ExamResult(
                studentId = student.id,
                examName = examName,
                academicYear = 2026,
                subjectName = it.subjectName,
                fullMarks = it.fullMarks,
                writtenMarks = it.writtenMarks,
                practicalMarks = it.practicalMarks,
                grade = it.grade,
                approvalStatus = "APPROVED"
            )
        }

        lifecycleScope.launch(Dispatchers.IO) {
            db.schoolDao().insertResults(examResults)
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    private fun generateAndOpenPdf() {
        val student = selectedStudent ?: return
        val examName = binding.etExamName.text.toString().trim().ifEmpty { "Annual Examination 2026" }

        val examResults = subjectScoreItems.map {
            ExamResult(
                studentId = student.id,
                examName = examName,
                academicYear = 2026,
                subjectName = it.subjectName,
                fullMarks = it.fullMarks,
                writtenMarks = it.writtenMarks,
                practicalMarks = it.practicalMarks,
                grade = it.grade,
                approvalStatus = "APPROVED"
            )
        }

        try {
            val pdfDir = File(cacheDir, "report_cards").apply { if (!exists()) mkdirs() }
            val sanitizedName = student.fullName.replace(" ", "_")
            val pdfFile = File(pdfDir, "Report_${sanitizedName}_2026.pdf")

            val generator = ResultPdfGenerator(this)
            generator.generateReportCard(student, examName, examResults, pdfFile)

            val fileUri: Uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY
            }

            startActivity(Intent.createChooser(intent, "Open 2-Page Vivekananda Report Card"))
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to create PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
FILE_EOF

cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/ui/ClassManagementActivity.kt
package com.vidyamandir.schoolmanager.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.SchoolClass
import com.vidyamandir.schoolmanager.databinding.ActivityClassManagementBinding
import com.vidyamandir.schoolmanager.databinding.ItemMonthFeeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClassManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClassManagementBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClassManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this, lifecycleScope)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.rvClasses.layoutManager = LinearLayoutManager(this)

        binding.cbDivideSection.setOnCheckedChangeListener { _, isChecked ->
            binding.tilSections.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        binding.btnAddClass.setOnClickListener {
            saveClass()
        }

        observeClasses()
    }

    private fun saveClass() {
        val name = binding.etClassName.text?.toString()?.trim() ?: ""
        val feeStr = binding.etMonthlyFee.text?.toString()?.trim() ?: ""
        val hasSections = binding.cbDivideSection.isChecked
        val sections = if (hasSections) {
            binding.etSections.text?.toString()?.trim() ?: "A, B"
        } else {
            ""
        }

        if (name.isEmpty() || feeStr.isEmpty()) {
            Toast.makeText(this, "Please enter Class Name and Monthly Fee", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val newClass = SchoolClass(
                className = name,
                monthlyFee = feeStr.toDoubleOrNull() ?: 500.0,
                hasSections = hasSections,
                sections = sections
            )
            db.schoolDao().insertClass(newClass)

            withContext(Dispatchers.Main) {
                binding.etClassName.text?.clear()
                binding.etMonthlyFee.text?.clear()
                binding.cbDivideSection.isChecked = false
                Toast.makeText(this@ClassManagementActivity, "Class registered successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeClasses() {
        lifecycleScope.launch {
            db.schoolDao().getAllClasses().collect { list ->
                binding.rvClasses.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                        val v = ItemMonthFeeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                        return object : RecyclerView.ViewHolder(v.root) {}
                    }

                    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                        val cls = list[pos]
                        val b = ItemMonthFeeBinding.bind(holder.itemView)
                        b.tvMonthName.text = cls.className

                        if (cls.hasSections && cls.sections.isNotBlank()) {
                            val units = cls.getDisplayUnits().joinToString(", ") { "${cls.className} (${it.second})" }
                            b.tvFeeBreakdown.text = "Fee: ₹${cls.monthlyFee}/m • Units: $units"
                            b.tvStatusBadge.text = "SECTIONS: ${cls.sections}"
                        } else {
                            b.tvFeeBreakdown.text = "Fee: ₹${cls.monthlyFee}/m • Single Unit"
                            b.tvStatusBadge.text = "CLASS"
                        }

                        b.cbSelectMonth.visibility = View.GONE
                    }

                    override fun getItemCount() = list.size
                }
            }
        }
    }
}
FILE_EOF

cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/ui/AddStudentActivity.kt
package com.vidyamandir.schoolmanager.ui

import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.databinding.ActivityAddStudentBinding
import com.vidyamandir.schoolmanager.util.ImageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

data class ClassUnitOption(
    val className: String,
    val section: String,
    val fee: Double
) {
    val displayText: String
        get() = if (section.isNotBlank()) "$className (Sec $section)" else className
}

class AddStudentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddStudentBinding
    private lateinit var db: AppDatabase
    private var internalPhotoPath: String? = null
    private val classUnitOptions = mutableListOf<ClassUnitOption>()

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedPath = ImageHelper.saveImageToInternalStorage(this, it, "student")
            if (savedPath != null) {
                internalPhotoPath = savedPath
                ImageHelper.loadProfilePhoto(savedPath, binding.ivStudentPhoto, android.R.drawable.ic_menu_camera)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddStudentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this, lifecycleScope)
        binding.toolbar.setNavigationOnClickListener { finish() }

        loadClasses()

        binding.btnUploadPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnSaveStudent.setOnClickListener {
            saveStudent()
        }
    }

    private fun loadClasses() {
        lifecycleScope.launch(Dispatchers.IO) {
            val classes = db.schoolDao().getAllClasses().first()
            classUnitOptions.clear()

            for (cls in classes) {
                for (unit in cls.getDisplayUnits()) {
                    classUnitOptions.add(ClassUnitOption(unit.first, unit.second, cls.monthlyFee))
                }
            }

            withContext(Dispatchers.Main) {
                if (classUnitOptions.isNotEmpty()) {
                    val names = classUnitOptions.map { it.displayText }
                    val adapter = ArrayAdapter(this@AddStudentActivity, android.R.layout.simple_dropdown_item_1line, names)
                    binding.actvClass.setAdapter(adapter)
                    binding.actvClass.setText(names[0], false)
                } else {
                    Toast.makeText(this@AddStudentActivity, "Please add a class first in 'CLASS' menu.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun saveStudent() {
        val selectedText = binding.actvClass.text?.toString()?.trim() ?: ""
        val matchedUnit = classUnitOptions.firstOrNull { it.displayText.equals(selectedText, ignoreCase = true) }

        if (matchedUnit == null) {
            Toast.makeText(this, "Please select a valid Class", Toast.LENGTH_SHORT).show()
            return
        }

        val name = binding.etStudentName.text?.toString()?.trim() ?: ""
        val rollStr = binding.etRollNo.text?.toString()?.trim() ?: "1"
        val father = binding.etFatherName.text?.toString()?.trim() ?: ""
        val mother = binding.etMotherName.text?.toString()?.trim() ?: ""
        val phone = binding.etContactPhone.text?.toString()?.trim() ?: ""
        val address = binding.etAddress.text?.toString()?.trim() ?: ""

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please enter Student Name and Contact Number", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val admissionNo = "VV-${System.currentTimeMillis().toString().takeLast(5)}"
            val admissionMonth = Calendar.getInstance().get(Calendar.MONTH) + 1

            val student = Student(
                admissionNo = admissionNo,
                fullName = name,
                className = matchedUnit.className,
                section = matchedUnit.section,
                rollNo = rollStr.toIntOrNull() ?: 1,
                fatherName = father,
                motherName = mother,
                parentPhone = phone,
                address = address,
                photoUri = internalPhotoPath,
                admissionMonth = admissionMonth,
                monthlyFee = matchedUnit.fee
            )

            db.schoolDao().insertStudent(student)

            withContext(Dispatchers.Main) {
                Toast.makeText(this@AddStudentActivity, "Student enrolled successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
FILE_EOF

cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/ui/StudentManagementActivity.kt
package com.vidyamandir.schoolmanager.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.databinding.ActivityStudentManagementBinding
import com.vidyamandir.schoolmanager.databinding.ItemStudentCardBinding
import com.vidyamandir.schoolmanager.util.ImageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudentManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentManagementBinding
    private lateinit var db: AppDatabase
    private var allStudents: List<Student> = emptyList()
    private var selectedClassFilter = "All Classes"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this, lifecycleScope)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.rvStudents.layoutManager = LinearLayoutManager(this)

        binding.fabAddStudent.setOnClickListener {
            startActivity(Intent(this, AddStudentActivity::class.java))
        }

        loadClassFilters()
        observeStudents()
    }

    private fun loadClassFilters() {
        lifecycleScope.launch(Dispatchers.IO) {
            val classes = db.schoolDao().getAllClasses().first()
            val filterOptions = mutableListOf("All Classes")
            classes.forEach { cls ->
                cls.getDisplayUnits().forEach { unit ->
                    val text = if (unit.second.isNotBlank()) "${unit.first} (Sec ${unit.second})" else unit.first
                    filterOptions.add(text)
                }
            }

            withContext(Dispatchers.Main) {
                val adapter = ArrayAdapter(this@StudentManagementActivity, android.R.layout.simple_dropdown_item_1line, filterOptions)
                binding.actvClassFilter.setAdapter(adapter)
                binding.actvClassFilter.setText("All Classes", false)

                binding.actvClassFilter.setOnItemClickListener { _, _, position, _ ->
                    selectedClassFilter = filterOptions[position]
                    applyFilter()
                }
            }
        }
    }

    private fun observeStudents() {
        lifecycleScope.launch {
            db.schoolDao().getAllActiveStudents().collect { list ->
                allStudents = list
                applyFilter()
            }
        }
    }

    private fun applyFilter() {
        val filtered = if (selectedClassFilter == "All Classes") {
            allStudents
        } else {
            allStudents.filter { it.displayClass.equals(selectedClassFilter, ignoreCase = true) }
        }
        binding.rvStudents.adapter = StudentAdapter(filtered)
    }

    class StudentAdapter(private val list: List<Student>) :
        RecyclerView.Adapter<StudentAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemStudentCardBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemStudentCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val s = list[position]
            holder.binding.tvStudentName.text = s.fullName
            holder.binding.tvStudentMeta.text = "${s.displayClass} • Roll: ${s.rollNo}"
            holder.binding.tvParentsName.text = "Father: ${s.fatherName} • 📞 ${s.parentPhone}"
            holder.binding.tvMonthlyFeeBadge.text = "₹${s.monthlyFee}/m"

            ImageHelper.loadProfilePhoto(
                s.photoUri,
                holder.binding.ivStudentAvatar,
                android.R.drawable.sym_def_app_icon
            )
        }

        override fun getItemCount() = list.size
    }
}
FILE_EOF

cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/ui/AddTeacherActivity.kt
package com.vidyamandir.schoolmanager.ui

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.Teacher
import com.vidyamandir.schoolmanager.databinding.ActivityAddTeacherBinding
import com.vidyamandir.schoolmanager.util.ImageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddTeacherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTeacherBinding
    private lateinit var db: AppDatabase
    private var internalPhotoPath: String? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedPath = ImageHelper.saveImageToInternalStorage(this, it, "teacher")
            if (savedPath != null) {
                internalPhotoPath = savedPath
                ImageHelper.loadProfilePhoto(savedPath, binding.ivTeacherPhoto, android.R.drawable.ic_menu_camera)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTeacherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this, lifecycleScope)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnUploadTeacherPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnSaveTeacher.setOnClickListener {
            saveTeacher()
        }
    }

    private fun saveTeacher() {
        val name = binding.etTeacherName.text?.toString()?.trim() ?: ""
        val phone = binding.etTeacherPhone.text?.toString()?.trim() ?: ""
        val password = binding.etTeacherPassword.text?.toString()?.trim() ?: ""

        if (name.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter Name, Contact Number, and Password", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val teacher = Teacher(
                name = name,
                phone = phone,
                password = password,
                photoUri = internalPhotoPath
            )
            db.schoolDao().insertTeacher(teacher)

            withContext(Dispatchers.Main) {
                Toast.makeText(this@AddTeacherActivity, "Teacher registered successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
FILE_EOF

cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/ui/TeacherManagementActivity.kt
package com.vidyamandir.schoolmanager.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.Teacher
import com.vidyamandir.schoolmanager.databinding.ActivityTeacherManagementBinding
import com.vidyamandir.schoolmanager.databinding.ItemTeacherCardBinding
import com.vidyamandir.schoolmanager.util.ImageHelper
import kotlinx.coroutines.launch

class TeacherManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTeacherManagementBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        db = AppDatabase.getDatabase(this, lifecycleScope)

        binding.rvTeachers.layoutManager = LinearLayoutManager(this)

        binding.fabAddTeacher.setOnClickListener {
            startActivity(Intent(this, AddTeacherActivity::class.java))
        }

        lifecycleScope.launch {
            db.schoolDao().getAllTeachers().collect { list ->
                binding.rvTeachers.adapter = TeacherCardAdapter(list)
            }
        }
    }

    class TeacherCardAdapter(private val list: List<Teacher>) :
        RecyclerView.Adapter<TeacherCardAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemTeacherCardBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemTeacherCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val t = list[position]
            holder.binding.tvTeacherName.text = t.name
            holder.binding.tvTeacherPhone.text = "📞 ${t.phone}"
            holder.binding.tvTeacherPassword.text = "Password: ${t.password}"

            ImageHelper.loadProfilePhoto(
                t.photoUri,
                holder.binding.ivTeacherAvatar,
                android.R.drawable.sym_def_app_icon
            )
        }

        override fun getItemCount() = list.size
    }
}
FILE_EOF

cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/ui/TeacherPortalActivity.kt
package com.vidyamandir.schoolmanager.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.ExamResult
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.databinding.ActivityTeacherPortalBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TeacherPortalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTeacherPortalBinding
    private lateinit var db: AppDatabase
    private lateinit var adapter: SubjectMarkAdapter

    private var studentsList: List<Student> = emptyList()
    private var selectedStudent: Student? = null

    private val scoreItems = mutableListOf(
        SubjectScoreItem("First Language (Bengali)", 100, 70, 18),
        SubjectScoreItem("Second Language (English)", 100, 65, 17),
        SubjectScoreItem("Mathematics", 100, 75, 19),
        SubjectScoreItem("Physical Science", 100, 60, 18),
        SubjectScoreItem("Life Science", 100, 68, 18),
        SubjectScoreItem("History", 100, 62, 17),
        SubjectScoreItem("Geography", 100, 70, 18)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherPortalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this, lifecycleScope)

        adapter = SubjectMarkAdapter(scoreItems)
        binding.rvMarks.layoutManager = LinearLayoutManager(this)
        binding.rvMarks.adapter = adapter

        loadStudents()

        binding.btnSubmitToAdmin.setOnClickListener {
            submitMarks()
        }

        binding.btnTeacherLogout.setOnClickListener {
            getSharedPreferences("vv_auth_session", Context.MODE_PRIVATE).edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun loadStudents() {
        lifecycleScope.launch(Dispatchers.IO) {
            studentsList = db.schoolDao().getAllActiveStudents().first()
            withContext(Dispatchers.Main) {
                if (studentsList.isNotEmpty()) {
                    val names = studentsList.map { "${it.fullName} (${it.displayClass}) - Roll ${it.rollNo}" }
                    val arrayAdapter = ArrayAdapter(this@TeacherPortalActivity, android.R.layout.simple_dropdown_item_1line, names)
                    binding.actvStudent.setAdapter(arrayAdapter)
                    binding.actvStudent.setText(names[0], false)
                    selectedStudent = studentsList[0]

                    binding.actvStudent.setOnItemClickListener { _, _, position, _ ->
                        selectedStudent = studentsList[position]
                    }
                }
            }
        }
    }

    private fun submitMarks() {
        val student = selectedStudent
        if (student == null) {
            Toast.makeText(this, "Please select a student", Toast.LENGTH_SHORT).show()
            return
        }

        val examName = binding.etExamName.text?.toString()?.trim() ?: "Annual Examination 2026"
        val teacherName = getSharedPreferences("vv_auth_session", Context.MODE_PRIVATE)
            .getString("teacher_name", "Teacher") ?: "Teacher"

        val results = scoreItems.map {
            ExamResult(
                studentId = student.id,
                examName = examName,
                academicYear = 2026,
                subjectName = it.subjectName,
                fullMarks = it.fullMarks,
                writtenMarks = it.writtenMarks,
                practicalMarks = it.practicalMarks,
                grade = it.grade,
                submittedByTeacher = teacherName,
                approvalStatus = "PENDING"
            )
        }

        lifecycleScope.launch(Dispatchers.IO) {
            db.schoolDao().insertResults(results)
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@TeacherPortalActivity,
                    "Marks submitted for Admin Approval!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
FILE_EOF

cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/ui/StudentDuesListActivity.kt
package com.vidyamandir.schoolmanager.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.databinding.ActivityStudentManagementBinding
import com.vidyamandir.schoolmanager.databinding.ItemStudentCardBinding
import com.vidyamandir.schoolmanager.util.ImageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

data class StudentDueEntry(
    val student: Student,
    val totalAmount: Double,
    val detailsText: String
)

class StudentDuesListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentManagementBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mode = intent.getStringExtra("VIEW_MODE") ?: "DUE"
        binding.toolbar.title = if (mode == "DUE") "Due Balance Defaulters" else "Paid Students"
        binding.fabAddStudent.hide()
        binding.tilClassFilter.visibility = android.view.View.GONE
        binding.toolbar.setNavigationOnClickListener { finish() }

        db = AppDatabase.getDatabase(this, lifecycleScope)
        binding.rvStudents.layoutManager = LinearLayoutManager(this)

        loadDuesData(mode)
    }

    private fun loadDuesData(mode: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val students = db.schoolDao().getAllActiveStudents().first()
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
            val displayList = mutableListOf<StudentDueEntry>()

            for (student in students) {
                val payments = db.schoolDao().getStudentPayments(student.id, 2026).first()
                val paidMonths = payments.flatMap { p ->
                    p.paidMonths.split(",").filter { it.isNotBlank() }.map { it.trim().toInt() }
                }.toSet()

                val startMonth = student.admissionMonth.coerceAtMost(12)
                val dueMonthIndices = mutableListOf<Int>()

                for (m in startMonth..currentMonth) {
                    if (!paidMonths.contains(m)) {
                        dueMonthIndices.add(m)
                    }
                }

                if (mode == "DUE" && dueMonthIndices.isNotEmpty()) {
                    val dueAmt = dueMonthIndices.size * student.monthlyFee
                    val text = "Unpaid: ${dueMonthIndices.size} Month(s) [₹${student.monthlyFee}/m]"
                    displayList.add(StudentDueEntry(student, dueAmt, text))
                } else if (mode == "PAID" && dueMonthIndices.isEmpty()) {
                    val text = "All dues cleared up to current month"
                    displayList.add(StudentDueEntry(student, student.monthlyFee, text))
                }
            }

            withContext(Dispatchers.Main) {
                binding.rvStudents.adapter = DuesAdapter(displayList, mode)
            }
        }
    }

    class DuesAdapter(
        private val list: List<StudentDueEntry>,
        private val mode: String
    ) : RecyclerView.Adapter<DuesAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemStudentCardBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemStudentCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val entry = list[position]
            val s = entry.student
            holder.binding.tvStudentName.text = s.fullName
            holder.binding.tvStudentMeta.text = "${s.displayClass} • Roll: ${s.rollNo}"
            holder.binding.tvParentsName.text = "${entry.detailsText} • 📞 ${s.parentPhone}"

            if (mode == "DUE") {
                holder.binding.tvMonthlyFeeBadge.text = "Due: ₹${entry.totalAmount}"
                holder.binding.tvMonthlyFeeBadge.setTextColor(android.graphics.Color.parseColor("#DC2626"))
                holder.binding.tvMonthlyFeeBadge.setBackgroundColor(android.graphics.Color.parseColor("#FEE2E2"))
            } else {
                holder.binding.tvMonthlyFeeBadge.text = "PAID"
                holder.binding.tvMonthlyFeeBadge.setTextColor(android.graphics.Color.parseColor("#16A34A"))
                holder.binding.tvMonthlyFeeBadge.setBackgroundColor(android.graphics.Color.parseColor("#DCFCE7"))
            }

            ImageHelper.loadProfilePhoto(
                s.photoUri,
                holder.binding.ivStudentAvatar,
                android.R.drawable.sym_def_app_icon
            )
        }

        override fun getItemCount() = list.size
    }
}
FILE_EOF

cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/ui/TrackPaymentActivity.kt
package com.vidyamandir.schoolmanager.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.databinding.ActivityStudentManagementBinding
import com.vidyamandir.schoolmanager.databinding.ItemMonthFeeBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TrackPaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentManagementBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.title = "Today's Tracked Payments"
        binding.fabAddStudent.hide()
        binding.tilClassFilter.visibility = android.view.View.GONE
        binding.toolbar.setNavigationOnClickListener { finish() }

        db = AppDatabase.getDatabase(this, lifecycleScope)
        binding.rvStudents.layoutManager = LinearLayoutManager(this)

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        lifecycleScope.launch {
            db.schoolDao().getTodayPayments(cal.timeInMillis).collect { list ->
                binding.rvStudents.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                        val b = ItemMonthFeeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                        return object : RecyclerView.ViewHolder(b.root) {}
                    }

                    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                        val p = list[pos]
                        val b = ItemMonthFeeBinding.bind(holder.itemView)
                        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(p.paymentDate))
                        b.tvMonthName.text = "${p.studentName} (${p.receiptNo})"
                        b.tvFeeBreakdown.text = "Base: ₹${p.baseAmount} | Fine: ₹${p.lateFeeAmount} | $timeStr"
                        b.tvStatusBadge.text = "₹${p.totalAmount}"
                        b.cbSelectMonth.isEnabled = false
                        b.cbSelectMonth.isChecked = true
                    }

                    override fun getItemCount() = list.size
                }
            }
        }
    }
}
FILE_EOF

cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/ui/MarksheetApprovalActivity.kt
package com.vidyamandir.schoolmanager.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.ExamResult
import com.vidyamandir.schoolmanager.databinding.ActivityStudentManagementBinding
import com.vidyamandir.schoolmanager.databinding.ItemSubjectMarkInputBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MarksheetApprovalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentManagementBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.title = "Approve Marksheets"
        binding.fabAddStudent.hide()
        binding.tilClassFilter.visibility = android.view.View.GONE
        binding.toolbar.setNavigationOnClickListener { finish() }

        db = AppDatabase.getDatabase(this, lifecycleScope)
        binding.rvStudents.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            db.schoolDao().getPendingMarksheets().collect { pendingList ->
                binding.rvStudents.adapter = ApprovalAdapter(pendingList) { id, status ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        db.schoolDao().updateMarksheetStatus(id, status)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MarksheetApprovalActivity, "Marksheet $status!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    class ApprovalAdapter(
        private val list: List<ExamResult>,
        private val onAction: (Long, String) -> Unit
    ) : RecyclerView.Adapter<ApprovalAdapter.ApprovalViewHolder>() {

        class ApprovalViewHolder(val binding: ItemSubjectMarkInputBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApprovalViewHolder {
            val binding = ItemSubjectMarkInputBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ApprovalViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ApprovalViewHolder, position: Int) {
            val item = list[position]
            holder.binding.tvSubjectName.text = "${item.subjectName} (${item.examName})"
            holder.binding.tvSubjectGradeBadge.text = "Tap to Approve (${item.obtainedMarks}/${item.fullMarks})"
            holder.binding.etFullMarks.setText(item.fullMarks.toString())
            holder.binding.etWrittenMarks.setText(item.writtenMarks.toString())
            holder.binding.etPracticalMarks.setText(item.practicalMarks.toString())

            holder.binding.root.setOnClickListener {
                onAction(item.id, "APPROVED")
            }
        }

        override fun getItemCount() = list.size
    }
}
FILE_EOF

cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/ui/WhatsAppMessageActivity.kt
package com.vidyamandir.schoolmanager.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.databinding.ActivityStudentManagementBinding
import com.vidyamandir.schoolmanager.databinding.ItemStudentCardBinding
import kotlinx.coroutines.launch
import java.net.URLEncoder

class WhatsAppMessageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentManagementBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.title = "WhatsApp Parent Broadcast"
        binding.fabAddStudent.hide()
        binding.tilClassFilter.visibility = android.view.View.GONE
        binding.toolbar.setNavigationOnClickListener { finish() }

        db = AppDatabase.getDatabase(this, lifecycleScope)
        binding.rvStudents.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            db.schoolDao().getAllActiveStudents().collect { list ->
                binding.rvStudents.adapter = WhatsAppAdapter(list) { student ->
                    sendWhatsApp(student)
                }
            }
        }
    }

    private fun sendWhatsApp(student: Student) {
        try {
            val phone = if (student.parentPhone.startsWith("+91")) student.parentPhone else "+91${student.parentPhone}"
            val text = "Namaskar, this is Vivekananda Vidyamandir (Dhupguri). Notice regarding student: ${student.fullName} (Class: ${student.displayClass}, Roll: ${student.rollNo})."
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=${URLEncoder.encode(text, "UTF-8")}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp not installed or invalid phone number", Toast.LENGTH_SHORT).show()
        }
    }

    class WhatsAppAdapter(
        private val list: List<Student>,
        private val onClick: (Student) -> Unit
    ) : RecyclerView.Adapter<WhatsAppAdapter.WhatsAppViewHolder>() {

        class WhatsAppViewHolder(val binding: ItemStudentCardBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WhatsAppViewHolder {
            val binding = ItemStudentCardBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return WhatsAppViewHolder(binding)
        }

        override fun onBindViewHolder(holder: WhatsAppViewHolder, position: Int) {
            val s = list[position]
            holder.binding.tvStudentName.text = s.fullName
            holder.binding.tvStudentMeta.text = "${s.displayClass} • 📞 ${s.parentPhone}"
            holder.binding.tvParentsName.text = "Tap to send WhatsApp alert"
            holder.binding.tvMonthlyFeeBadge.text = "CHAT"
            holder.binding.root.setOnClickListener { onClick(s) }
        }

        override fun getItemCount() = list.size
    }
}
FILE_EOF

cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/MainActivity.kt
package com.vidyamandir.schoolmanager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.databinding.ActivityMainBinding
import com.vidyamandir.schoolmanager.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this, lifecycleScope)

        setupToolbar()
        observeDashboardStats()
        setupCardClicks()
    }

    override fun onResume() {
        super.onResume()
        refreshBalances()
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

    private fun observeDashboardStats() {
        lifecycleScope.launch {
            db.schoolDao().getStudentCount().collect { count ->
                binding.tvStudentCount.text = count.toString()
            }
        }

        lifecycleScope.launch {
            db.schoolDao().getClassCount().collect { count ->
                binding.tvClassCount.text = count.toString()
            }
        }

        lifecycleScope.launch {
            db.schoolDao().getTeacherCount().collect { count ->
                binding.tvTeacherCount.text = count.toString()
            }
        }

        lifecycleScope.launch {
            db.schoolDao().getPendingMarksheetCount().collect { count ->
                binding.tvPendingMarksheetCount.text = "$count Pending"
            }
        }

        lifecycleScope.launch {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            db.schoolDao().getTodayPayments(cal.timeInMillis).collect { todayPayments ->
                val todaySum = todayPayments.sumOf { it.totalAmount }
                binding.tvDailyCollection.text = "₹${String.format("%.2f", todaySum)}"
            }
        }

        lifecycleScope.launch {
            db.schoolDao().getAllPayments().collect { allPayments ->
                val paidSum = allPayments.sumOf { it.totalAmount }
                binding.tvPaidBalanceAmount.text = "₹${String.format("%.2f", paidSum)}"
            }
        }
    }

    private fun refreshBalances() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val students = db.schoolDao().getAllActiveStudents().first()
                val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
                var totalDues = 0.0

                for (student in students) {
                    val payments = db.schoolDao().getStudentPayments(student.id, 2026).first()
                    val paidMonths = payments.flatMap { p ->
                        p.paidMonths.split(",").filter { it.isNotBlank() }.map { it.trim().toInt() }
                    }.toSet()

                    val startMonth = student.admissionMonth.coerceAtMost(12)
                    var dueMonthsCount = 0
                    for (m in startMonth..currentMonth) {
                        if (!paidMonths.contains(m)) dueMonthsCount++
                    }
                    totalDues += (dueMonthsCount * student.monthlyFee)
                }

                withContext(Dispatchers.Main) {
                    binding.tvDueBalanceAmount.text = "₹${String.format("%.2f", totalDues)}"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupCardClicks() {
        // Collect Fee Action
        binding.cardCollectFee.setOnClickListener {
            startActivity(Intent(this, FeeCollectionActivity::class.java))
        }
        binding.btnQuickCollect.setOnClickListener {
            startActivity(Intent(this, FeeCollectionActivity::class.java))
        }

        // Row 1
        binding.cardStudents.setOnClickListener {
            startActivity(Intent(this, StudentManagementActivity::class.java))
        }
        binding.cardClasses.setOnClickListener {
            startActivity(Intent(this, ClassManagementActivity::class.java))
        }
        binding.cardTeachers.setOnClickListener {
            startActivity(Intent(this, TeacherManagementActivity::class.java))
        }

        // Row 2
        binding.cardDueBalance.setOnClickListener {
            val intent = Intent(this, StudentDuesListActivity::class.java)
            intent.putExtra("VIEW_MODE", "DUE")
            startActivity(intent)
        }
        binding.cardPaidBalance.setOnClickListener {
            val intent = Intent(this, StudentDuesListActivity::class.java)
            intent.putExtra("VIEW_MODE", "PAID")
            startActivity(intent)
        }

        // Row 3
        binding.cardTrackPayment.setOnClickListener {
            startActivity(Intent(this, TrackPaymentActivity::class.java))
        }
        binding.cardApprovedMarksheet.setOnClickListener {
            startActivity(Intent(this, MarksheetApprovalActivity::class.java))
        }

        // Row 4
        binding.cardSetExams.setOnClickListener {
            startActivity(Intent(this, ResultPublishActivity::class.java))
        }
        binding.cardPublishResult.setOnClickListener {
            startActivity(Intent(this, ResultPublishActivity::class.java))
        }

        // Row 5
        binding.cardMessage.setOnClickListener {
            startActivity(Intent(this, WhatsAppMessageActivity::class.java))
        }
        binding.btnOpenMessage.setOnClickListener {
            startActivity(Intent(this, WhatsAppMessageActivity::class.java))
        }
    }
}
FILE_EOF

echo "=== [8/8] Staging, Committing and Pushing to GitHub ==="
chmod +x gradlew 2>/dev/null || true
git add .
git commit -m "feat: complete unified school app update with all screens, zero demo classes, and full dashboard"
git push origin main

echo "========================================================"
echo " SUCCESS! All files updated and pushed to GitHub main. "
echo " Check GitHub Actions to download your compiled APK.    "
echo "========================================================"
