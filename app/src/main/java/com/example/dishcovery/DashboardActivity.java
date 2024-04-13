package com.example.dishcovery;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;

public class DashboardActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private GoogleSignInClient googleSignInClient;
    private TextView account_email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View headerView = navigationView.getHeaderView(0);
        account_email = headerView.findViewById(R.id.account_email);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_nav, R.string.close_nav);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
            navigationView.setCheckedItem(R.id.nav_home);
        }

        // Update account information
        updateAccountInformation();
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateAccountInformation();
    }

    private void updateAccountInformation() {
        // Retrieve the latest Google account information
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);

        if (acct != null) {
            String personEmail = acct.getEmail();

            account_email.setText(personEmail);

            SharedPreferences preferences = getSharedPreferences("user_preferences", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("userEmail", personEmail);
            editor.apply();
        } else {
            SharedPreferences preferences = getSharedPreferences("user_preferences", Context.MODE_PRIVATE);
            String email = preferences.getString("userEmail", "");

            account_email.setText(email);
        }
    }


    private void clearSharedPreferences() {
        // Access SharedPreferences using the context and specify a name for the preferences file
        SharedPreferences sharedPreferences = getSharedPreferences("user_preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    private void saveLoginStatus(boolean isLoggedIn) {
        // Access SharedPreferences using the context and specify a name for the preferences file
        SharedPreferences preferences = getSharedPreferences("user_preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isLoggedIn", isLoggedIn);
        editor.apply();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (!item.isChecked()) {
            item.setChecked(true);
        }

        if (id == R.id.nav_home) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
        } else if (id == R.id.nav_bookmarks) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new BookmarksFragment()).commit();
        } else if (id == R.id.nav_groups) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new GroupsFragment()).commit();
        } else if (id == R.id.nav_shared_recipes) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SharedRecipesFragment()).commit();
        } else if (id == R.id.nav_my_account) {
            MyAccountFragment myAccountFragment = new MyAccountFragment();

            Bundle args = new Bundle();
            String userEmail = account_email.getText().toString();
            args.putString("userEmail", userEmail);
            myAccountFragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, myAccountFragment)
                    .commit();
        } else if (id == R.id.nav_funds) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new FundsFragment()).commit();
        } else if (id == R.id.nav_logout) {
            showLogoutConfirmationDialog();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showLogoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout");
        builder.setMessage("Are you sure you want to logout?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                signOut();
            }
        });
        builder.setNegativeButton("No", null);
        builder.show();
    }

    private void signOut() {
        // Clear login status
        saveLoginStatus(false);

        // Clear shared preferences
        clearSharedPreferences();

        // Sign out from Google if signed in with Google
        if (googleSignInClient != null) {
            googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    // Sign out successful
                    Toast.makeText(DashboardActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                } else {
                    // Sign out failed
                    Toast.makeText(DashboardActivity.this, "Failed to logout", Toast.LENGTH_SHORT).show();
                }
                // Navigate back to LoginActivity
                Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            });
        } else {
            // If not signed in with Google, directly navigate back to LoginActivity
            Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
