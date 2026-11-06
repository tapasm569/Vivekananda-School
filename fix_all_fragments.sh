#!/usr/bin/env bash
set -e

echo "=== [1/5] Ensuring All Directories Exist ==="
mkdir -p app/src/main/res/drawable
mkdir -p app/src/main/res/color
mkdir -p app/src/main/res/values
mkdir -p app/src/main/res/layout
mkdir -p app/src/main/res/menu
mkdir -p app/src/main/java/com/vidyamandir/schoolmanager/ui

echo "=== [2/5] Writing Vector Icons and Menu ==="
cat << 'FILE_EOF' > app/src/main/res/drawable/ic_nav_due.xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#0F3D6E"
        android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM13,17h-2v-2h2v2zM13,13h-2L11,7h2v6z"/>
</vector>
FILE_EOF

cat << 'FILE_EOF' > app/src/main/res/drawable/ic_nav_paid.xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#0F3D6E"
        android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM10,17l-5,-5 1.41,-1.41L10,14.17l7.59,-7.59L19,8l-9,9z"/>
</vector>
FILE_EOF

cat << 'FILE_EOF' > app/src/main/res/drawable/ic_nav_search.xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#0F3D6E"
        android:pathData="M15.5,14h-0.79l-0.28,-0.27A6.471,6.471 0,0 0,16 9.5 6.5,6.5 0,1 0,9.5 16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zm-6,0C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z"/>
</vector>
FILE_EOF

cat << 'FILE_EOF' > app/src/main/res/drawable/ic_nav_dashboard.xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#0F3D6E"
        android:pathData="M3,13h8L11,3L3,3v10zM3,21h8v-6L3,15v6zM13,21h8L21,11h-8v10zM13,3v6h8L21,3h-8z"/>
</vector>
FILE_EOF

cat << 'FILE_EOF' > app/src/main/res/menu/bottom_nav_menu.xml
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:id="@+id/nav_due"
        android:icon="@drawable/ic_nav_due"
        android:title="Due" />
    <item
        android:id="@+id/nav_paid"
        android:icon="@drawable/ic_nav_paid"
        android:title="Paid" />
    <item
        android:id="@+id/nav_search"
        android:icon="@drawable/ic_nav_search"
        android:title="Search" />
    <item
        android:id="@+id/nav_dashboard"
        android:icon="@drawable/ic_nav_dashboard"
        android:title="Dashboard" />
</menu>
FILE_EOF

echo "=== [3/5] Writing Fragment Layouts ==="
cat << 'FILE_EOF' > app/src/main/res/layout/fragment_due_list.xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="#F8FAFC"
    android:padding="12dp">

    <com.google.android.material.card.MaterialCardView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:cardCornerRadius="16dp"
        app:cardBackgroundColor="#FEF2F2"
        app:strokeWidth="1dp"
        app:strokeColor="#FECACA">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:padding="14dp">

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="TOTAL UNCOLLECTED DUES"
                    android:textSize="11sp"
                    android:letterSpacing="0.04"
                    android:textStyle="bold"
                    android:textColor="#991B1B" />

                <TextView
                    android:id="@+id/tvTotalDueHeader"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="2dp"
                    android:text="₹0.00"
                    android:textSize="20sp"
                    android:textStyle="bold"
                    android:textColor="#DC2626" />
            </LinearLayout>

            <TextView
                android:id="@+id/tvDueStudentCount"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:background="#FEE2E2"
                android:paddingHorizontal="10dp"
                android:paddingVertical="4dp"
                android:text="0 Students"
                android:textColor="#DC2626"
                android:textSize="12sp"
                android:textStyle="bold" />
        </LinearLayout>
    </com.google.android.material.card.MaterialCardView>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvDueList"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:layout_marginTop="10dp" />

</LinearLayout>
FILE_EOF

cat << 'FILE_EOF' > app/src/main/res/layout/fragment_paid_list.xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="#F8FAFC"
    android:padding="12dp">

    <com.google.android.material.card.MaterialCardView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:cardCornerRadius="16dp"
        app:cardBackgroundColor="#F0FDF4"
        app:strokeWidth="1dp"
        app:strokeColor="#BBF7D0">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:padding="14dp">

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="TOTAL COLLECTED BALANCE"
                    android:textSize="11sp"
                    android:letterSpacing="0.04"
                    android:textStyle="bold"
                    android:textColor="#166534" />

                <TextView
                    android:id="@+id/tvTotalPaidHeader"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="2dp"
                    android:text="₹0.00"
                    android:textSize="20sp"
                    android:textStyle="bold"
                    android:textColor="#16A34A" />
            </LinearLayout>

            <TextView
                android:id="@+id/tvPaidStudentCount"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:background="#DCFCE7"
                android:paddingHorizontal="10dp"
                android:paddingVertical="4dp"
                android:text="0 Students"
                android:textColor="#16A34A"
                android:textSize="12sp"
                android:textStyle="bold" />
        </LinearLayout>
    </com.google.android.material.card.MaterialCardView>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvPaidList"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:layout_marginTop="10dp" />

</LinearLayout>
FILE_EOF

cat << 'FILE_EOF' > app/src/main/res/layout/fragment_search.xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="#F8FAFC"
    android:padding="12dp">

    <com.google.android.material.textfield.TextInputLayout
        style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Search Student Name, Class, or Roll..."
        app:startIconDrawable="@drawable/ic_nav_search"
        app:startIconTint="#0F3D6E"
        app:boxCornerRadiusTopStart="14dp"
        app:boxCornerRadiusTopEnd="14dp"
        app:boxCornerRadiusBottomStart="14dp"
        app:boxCornerRadiusBottomEnd="14dp">

        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/etSearchInput"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="text"
            android:singleLine="true" />
    </com.google.android.material.textfield.TextInputLayout>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvSearchResults"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:layout_marginTop="10dp" />

</LinearLayout>
FILE_EOF

cat << 'FILE_EOF' > app/src/main/res/layout/fragment_dashboard.xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.core.widget.NestedScrollView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F1F5F9">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="14dp">

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
                android:padding="16dp">

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
                        android:text="12-month advance &amp; due payment counter"
                        android:textColor="#93C5FD"
                        android:textSize="12sp" />
                </LinearLayout>

                <com.google.android.material.button.MaterialButton
                    android:id="@+id/btnQuickCollect"
                    android:layout_width="wrap_content"
                    android:layout_height="42dp"
                    android:text="Open Counter →"
                    android:textColor="#0F3D6E"
                    android:textSize="11sp"
                    android:textStyle="bold"
                    app:backgroundTint="#FEF3C7"
                    app:cornerRadius="10dp" />
            </LinearLayout>
        </com.google.android.material.card.MaterialCardView>

        <!-- ROW 1: STUDENTS, CLASS, TEACHER -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:orientation="horizontal">

            <com.google.android.material.card.MaterialCardView
                android:id="@+id/cardStudents"
                android:layout_width="0dp"
                android:layout_height="105dp"
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
                    android:padding="6dp">

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
                        android:layout_marginTop="2dp"
                        android:text="0"
                        android:textColor="#0F3D6E"
                        android:textSize="20sp"
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
                android:layout_height="105dp"
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
                    android:padding="6dp">

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
                        android:layout_marginTop="2dp"
                        android:text="0"
                        android:textColor="#0F3D6E"
                        android:textSize="20sp"
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
                android:layout_height="105dp"
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
                    android:padding="6dp">

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
                        android:layout_marginTop="2dp"
                        android:text="0"
                        android:textColor="#0F3D6E"
                        android:textSize="20sp"
                        android:textStyle="bold" />

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Faculty"
                        android:textColor="#64748B"
                        android:textSize="10sp" />
                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>
        </LinearLayout>

        <!-- ROW 2: TRACK PAYMENT & APPROVED MARKSHEET -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:orientation="horizontal">

            <com.google.android.material.card.MaterialCardView
                android:id="@+id/cardTrackPayment"
                android:layout_width="0dp"
                android:layout_height="110dp"
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
                    android:padding="14dp">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="TRACK PAYMENT"
                        android:textColor="#1E40AF"
                        android:textSize="11sp"
                        android:textStyle="bold" />

                    <TextView
                        android:id="@+id/tvDailyCollection"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="4dp"
                        android:text="₹0.00"
                        android:textColor="#2563EB"
                        android:textSize="18sp"
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
                android:layout_height="110dp"
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
                    android:padding="14dp">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="APPROVED MARKSHEET"
                        android:textColor="#92400E"
                        android:textSize="10sp"
                        android:textStyle="bold" />

                    <TextView
                        android:id="@+id/tvPendingMarksheetCount"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="4dp"
                        android:text="0 Pending"
                        android:textColor="#D97706"
                        android:textSize="18sp"
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

        <!-- ROW 3: SET EXAMS & PUBLISH RESULT -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:orientation="horizontal">

            <com.google.android.material.card.MaterialCardView
                android:id="@+id/cardSetExams"
                android:layout_width="0dp"
                android:layout_height="110dp"
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
                    android:padding="14dp">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="SET EXAMS"
                        android:textColor="#6B21A8"
                        android:textSize="11sp"
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
                android:layout_height="110dp"
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
                    android:padding="14dp">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="PUBLISH RESULT"
                        android:textColor="#115E59"
                        android:textSize="11sp"
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

        <!-- ROW 4: MESSAGE (WHATSAPP BROADCAST) -->
        <com.google.android.material.card.MaterialCardView
            android:id="@+id/cardMessage"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:layout_marginBottom="16dp"
            app:cardCornerRadius="18dp"
            app:cardElevation="3dp"
            app:cardBackgroundColor="#0F3D6E">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:gravity="center_vertical"
                android:orientation="horizontal"
                android:padding="16dp">

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
                        android:textSize="15sp"
                        android:letterSpacing="0.04"
                        android:textStyle="bold" />

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="2dp"
                        android:text="Direct WhatsApp messaging to students &amp; parents"
                        android:textColor="#93C5FD"
                        android:textSize="11sp" />
                </LinearLayout>

                <com.google.android.material.button.MaterialButton
                    android:id="@+id/btnOpenMessage"
                    style="@style/Widget.Material3.Button.TonalButton"
                    android:layout_width="wrap_content"
                    android:layout_height="40dp"
                    android:text="Send WhatsApp"
                    android:textColor="#0F3D6E"
                    android:textSize="11sp"
                    android:textStyle="bold"
                    app:backgroundTint="#FEF3C7"
                    app:cornerRadius="10dp" />

            </LinearLayout>
        </com.google.android.material.card.MaterialCardView>

    </LinearLayout>
</androidx.core.widget.NestedScrollView>
FILE_EOF

echo "=== [4/5] Writing Fragment Kotlin Source Code ==="

# 1. DueListFragment (Tab 1)
cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/ui/DueListFragment.kt
package com.vidyamandir.schoolmanager.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.databinding.FragmentDueListBinding
import com.vidyamandir.schoolmanager.databinding.ItemStudentCardBinding
import com.vidyamandir.schoolmanager.util.ImageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.util.Calendar

data class DueStudentCardItem(
    val student: Student,
    val totalDue: Double,
    val details: String
)

class DueListFragment : Fragment() {

    private var _binding: FragmentDueListBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDueListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)
        binding.rvDueList.layoutManager = LinearLayoutManager(requireContext())
        loadDues()
    }

    override fun onResume() {
        super.onResume()
        loadDues()
    }

    private fun loadDues() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val students = db.schoolDao().getAllActiveStudents().first()
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
            val dueList = mutableListOf<DueStudentCardItem>()
            var grandTotalDue = 0.0

            for (student in students) {
                val payments = db.schoolDao().getStudentPayments(student.id, 2026).first()
                val paidMonths = payments.flatMap { p ->
                    p.paidMonths.split(",").filter { it.isNotBlank() }.map { it.trim().toInt() }
                }.toSet()

                val dueCount = (student.admissionMonth..currentMonth).count { !paidMonths.contains(it) }
                if (dueCount > 0) {
                    val amount = dueCount * student.monthlyFee
                    grandTotalDue += amount
                    dueList.add(DueStudentCardItem(student, amount, "Due: $dueCount Month(s) unpaid"))
                }
            }

            withContext(Dispatchers.Main) {
                if (_binding != null) {
                    binding.tvTotalDueHeader.text = "₹${String.format("%.2f", grandTotalDue)}"
                    binding.tvDueStudentCount.text = "${dueList.size} Students"
                    binding.rvDueList.adapter = DueAdapter(dueList) { student, dueAmt ->
                        sendReminder(student, dueAmt)
                    }
                }
            }
        }
    }

    private fun sendReminder(student: Student, dueAmt: Double) {
        try {
            val phone = if (student.parentPhone.startsWith("+91")) student.parentPhone else "+91${student.parentPhone}"
            val msg = "Namaskar, Vivekananda Vidyamandir Fee Reminder: Outstanding monthly fee of ₹$dueAmt is due for ${student.fullName} (${student.displayClass}). Please clear the dues soon."
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=${URLEncoder.encode(msg, "UTF-8")}"))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "WhatsApp not found or invalid phone number", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class DueAdapter(
        private val list: List<DueStudentCardItem>,
        private val onReminder: (Student, Double) -> Unit
    ) : RecyclerView.Adapter<DueAdapter.ViewHolder>() {

        class ViewHolder(val b: ItemStudentCardBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemStudentCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val entry = list[position]
            val s = entry.student
            holder.b.tvStudentName.text = s.fullName
            holder.b.tvStudentMeta.text = "${s.displayClass} • Roll: ${s.rollNo}"
            holder.b.tvParentsName.text = "${entry.details} • 📞 ${s.parentPhone}"

            holder.b.tvMonthlyFeeBadge.text = "Due: ₹${entry.totalDue}"
            holder.b.tvMonthlyFeeBadge.setTextColor(android.graphics.Color.parseColor("#DC2626"))
            holder.b.tvMonthlyFeeBadge.setBackgroundColor(android.graphics.Color.parseColor("#FEE2E2"))

            ImageHelper.loadProfilePhoto(s.photoUri, holder.b.ivStudentAvatar, android.R.drawable.sym_def_app_icon)

            holder.b.root.setOnClickListener {
                onReminder(s, entry.totalDue)
            }
        }

        override fun getItemCount() = list.size
    }
}
FILE_EOF

# 2. PaidListFragment (Tab 2)
cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/ui/PaidListFragment.kt
package com.vidyamandir.schoolmanager.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.databinding.FragmentPaidListBinding
import com.vidyamandir.schoolmanager.databinding.ItemStudentCardBinding
import com.vidyamandir.schoolmanager.util.ImageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class PaidListFragment : Fragment() {

    private var _binding: FragmentPaidListBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPaidListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)
        binding.rvPaidList.layoutManager = LinearLayoutManager(requireContext())
        loadPaidStudents()
    }

    override fun onResume() {
        super.onResume()
        loadPaidStudents()
    }

    private fun loadPaidStudents() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val students = db.schoolDao().getAllActiveStudents().first()
            val allPayments = db.schoolDao().getAllPayments().first()
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
            val paidList = mutableListOf<Student>()

            for (student in students) {
                val studentPayments = db.schoolDao().getStudentPayments(student.id, 2026).first()
                val paidMonths = studentPayments.flatMap { p ->
                    p.paidMonths.split(",").filter { it.isNotBlank() }.map { it.trim().toInt() }
                }.toSet()

                val dueCount = (student.admissionMonth..currentMonth).count { !paidMonths.contains(it) }
                if (dueCount == 0) {
                    paidList.add(student)
                }
            }

            val grandTotalPaid = allPayments.sumOf { it.totalAmount }

            withContext(Dispatchers.Main) {
                if (_binding != null) {
                    binding.tvTotalPaidHeader.text = "₹${String.format("%.2f", grandTotalPaid)}"
                    binding.tvPaidStudentCount.text = "${paidList.size} Cleared"
                    binding.rvPaidList.adapter = PaidAdapter(paidList)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class PaidAdapter(private val list: List<Student>) :
        RecyclerView.Adapter<PaidAdapter.ViewHolder>() {

        class ViewHolder(val b: ItemStudentCardBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemStudentCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val s = list[position]
            holder.b.tvStudentName.text = s.fullName
            holder.b.tvStudentMeta.text = "${s.displayClass} • Roll: ${s.rollNo}"
            holder.b.tvParentsName.text = "Father: ${s.fatherName} • 📞 ${s.parentPhone}"

            holder.b.tvMonthlyFeeBadge.text = "PAID"
            holder.b.tvMonthlyFeeBadge.setTextColor(android.graphics.Color.parseColor("#16A34A"))
            holder.b.tvMonthlyFeeBadge.setBackgroundColor(android.graphics.Color.parseColor("#DCFCE7"))

            ImageHelper.loadProfilePhoto(s.photoUri, holder.b.ivStudentAvatar, android.R.drawable.sym_def_app_icon)
        }

        override fun getItemCount() = list.size
    }
}
FILE_EOF

# 3. SearchFragment (Tab 3)
cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/ui/SearchFragment.kt
package com.vidyamandir.schoolmanager.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.data.Student
import com.vidyamandir.schoolmanager.databinding.FragmentSearchBinding
import com.vidyamandir.schoolmanager.databinding.ItemStudentCardBinding
import com.vidyamandir.schoolmanager.util.ImageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    private var allStudents: List<Student> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)
        binding.rvSearchResults.layoutManager = LinearLayoutManager(requireContext())

        loadStudents()

        binding.etSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                filter(s?.toString()?.trim() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadStudents() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            allStudents = db.schoolDao().getAllActiveStudents().first()
            withContext(Dispatchers.Main) {
                if (_binding != null) {
                    filter(binding.etSearchInput.text?.toString()?.trim() ?: "")
                }
            }
        }
    }

    private fun filter(query: String) {
        val filtered = if (query.isEmpty()) {
            allStudents
        } else {
            allStudents.filter { s ->
                s.fullName.contains(query, ignoreCase = true) ||
                s.className.contains(query, ignoreCase = true) ||
                s.section.contains(query, ignoreCase = true) ||
                s.rollNo.toString() == query ||
                s.parentPhone.contains(query)
            }
        }

        binding.rvSearchResults.adapter = SearchAdapter(filtered) { selectedStudent ->
            val intent = Intent(requireContext(), FeeCollectionActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class SearchAdapter(
        private val list: List<Student>,
        private val onClick: (Student) -> Unit
    ) : RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

        class ViewHolder(val b: ItemStudentCardBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemStudentCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val s = list[position]
            holder.b.tvStudentName.text = s.fullName
            holder.b.tvStudentMeta.text = "${s.displayClass} • Roll: ${s.rollNo}"
            holder.b.tvParentsName.text = "Father: ${s.fatherName} • 📞 ${s.parentPhone}"
            holder.b.tvMonthlyFeeBadge.text = "₹${s.monthlyFee}/m"

            ImageHelper.loadProfilePhoto(s.photoUri, holder.b.ivStudentAvatar, android.R.drawable.sym_def_app_icon)

            holder.b.root.setOnClickListener {
                onClick(s)
            }
        }

        override fun getItemCount() = list.size
    }
}
FILE_EOF

# 4. DashboardFragment (Tab 4)
cat << 'FILE_EOF' > app/src/main/java/com/vidyamandir/schoolmanager/ui/DashboardFragment.kt
package com.vidyamandir.schoolmanager.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.vidyamandir.schoolmanager.data.AppDatabase
import com.vidyamandir.schoolmanager.databinding.FragmentDashboardBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)

        observeStats()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        refreshTodayPayment()
    }

    private fun observeStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            db.schoolDao().getStudentCount().collect { count ->
                _binding?.tvStudentCount?.text = count.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            db.schoolDao().getClassCount().collect { count ->
                _binding?.tvClassCount?.text = count.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            db.schoolDao().getTeacherCount().collect { count ->
                _binding?.tvTeacherCount?.text = count.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            db.schoolDao().getPendingMarksheetCount().collect { count ->
                _binding?.tvPendingMarksheetCount?.text = "$count Pending"
            }
        }
    }

    private fun refreshTodayPayment() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayList = db.schoolDao().getTodayPayments(cal.timeInMillis).first()
            val todayTotal = todayList.sumOf { it.totalAmount }

            withContext(Dispatchers.Main) {
                _binding?.tvDailyCollection?.text = "₹${String.format("%.2f", todayTotal)}"
            }
        }
    }

    private fun setupListeners() {
        binding.cardCollectFee.setOnClickListener {
            startActivity(Intent(requireContext(), FeeCollectionActivity::class.java))
        }
        binding.btnQuickCollect.setOnClickListener {
            startActivity(Intent(requireContext(), FeeCollectionActivity::class.java))
        }

        binding.cardStudents.setOnClickListener {
            startActivity(Intent(requireContext(), StudentManagementActivity::class.java))
        }
        binding.cardClasses.setOnClickListener {
            startActivity(Intent(requireContext(), ClassManagementActivity::class.java))
        }
        binding.cardTeachers.setOnClickListener {
            startActivity(Intent(requireContext(), TeacherManagementActivity::class.java))
        }

        binding.cardTrackPayment.setOnClickListener {
            startActivity(Intent(requireContext(), TrackPaymentActivity::class.java))
        }
        binding.cardApprovedMarksheet.setOnClickListener {
            startActivity(Intent(requireContext(), MarksheetApprovalActivity::class.java))
        }

        binding.cardSetExams.setOnClickListener {
            startActivity(Intent(requireContext(), ResultPublishActivity::class.java))
        }
        binding.cardPublishResult.setOnClickListener {
            startActivity(Intent(requireContext(), ResultPublishActivity::class.java))
        }

        binding.cardMessage.setOnClickListener {
            startActivity(Intent(requireContext(), WhatsAppMessageActivity::class.java))
        }
        binding.btnOpenMessage.setOnClickListener {
            startActivity(Intent(requireContext(), WhatsAppMessageActivity::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
FILE_EOF

echo "=== [5/5] Staging All Files, Committing and Pushing ==="
git add .
git commit -m "fix: commit all fragment layouts, source files, and vector icons"
git push origin main

echo "All files committed and pushed to GitHub main successfully."
