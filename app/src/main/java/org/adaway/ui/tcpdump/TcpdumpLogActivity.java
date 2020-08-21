/*
 * Copyright (C) 2011-2012 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This file is part of AdAway.
 *
 * AdAway is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AdAway is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AdAway.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.adaway.ui.tcpdump;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProviders;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.adaway.R;
import org.adaway.db.entity.ListType;
import org.adaway.helper.ThemeHelper;
import org.adaway.ui.MainActivity;
import org.adaway.ui.dialog.AlertDialogValidator;
import org.adaway.ui.hostsinstall.HostsInstallSnackbar;
import org.adaway.util.RegexUtils;

/**
 * This class is an {@link android.app.Activity} to show tcpdump log entries.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class TcpdumpLogActivity extends AppCompatActivity implements TcpdumpLogViewCallback {
    /**
     * The view model (<code>null</code> if activity is not created).
     */
    private TcpdumpLogViewModel mViewModel;
    /**
     * The snackbar notification (<code>null</code> if activity is not created).
     */
    private HostsInstallSnackbar mInstallSnackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*
         * Create activity.
         */
        super.onCreate(savedInstanceState);
        ThemeHelper.applyTheme(this);
        setContentView(R.layout.tcpdump_log_activity);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        /*
         * Configure swipe layout.
         */
        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(() -> mViewModel.updateDnsRequests());
        /*
         * Configure recycler view.
         */
        // Get recycler view
        RecyclerView recyclerView = findViewById(R.id.tcpdump_log_list);
        recyclerView.setHasFixedSize(true);
        // Defile recycler layout
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        // Get view model
        mViewModel = ViewModelProviders.of(this).get(TcpdumpLogViewModel.class);
        // Create recycler adapter
        ListAdapter adapter = new TcpdumpLogAdapter(this);
        recyclerView.setAdapter(adapter);
        /*
         * Configure snackbar.
         */
        // Create install snackbar
        mInstallSnackbar = new HostsInstallSnackbar(swipeRefreshLayout);
        /*
         * Load data.
         */
        // Bind view model to the list view
        mViewModel.getLogEntries().observe(this, logEntries -> {
            adapter.submitList(logEntries);
            swipeRefreshLayout.setRefreshing(false);
        });
        // Mark as loading data
        swipeRefreshLayout.setRefreshing(true);
        // Load initial data
        mViewModel.updateDnsRequests();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.tcpdump_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in Action Bar clicked; go home
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.sort:
                mViewModel.toggleSort();
                return true;
            case R.id.delete:
                TcpdumpUtils.clearLogFile(this);
                mViewModel.updateDnsRequests();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void addListItem(@NonNull String hostName, @NonNull ListType type) {
        // Check view model and snackbar notification
        if (mViewModel == null || mInstallSnackbar == null) {
            return;
        }
        // Check type other than redirection
        if (type != ListType.REDIRECTION_LIST) {
            // Insert list item
            mViewModel.addListItem(hostName, type, null);
            // Display snackbar notification
            mInstallSnackbar.notifyUpdateAvailable();
        } else {
            // Create dialog view
            LayoutInflater factory = LayoutInflater.from(this);
            View view = factory.inflate(R.layout.tcpdump_redirect_dialog, null);
            EditText ipEditText = view.findViewById(R.id.tcpdump_redirect_ip);
            // Create dialog
            AlertDialog alertDialog = new MaterialAlertDialogBuilder(this)
                    .setCancelable(true)
                    .setTitle(R.string.tcpdump_redirect_dialog_title)
                    .setView(view)
                    // Setup buttons
                    .setPositiveButton(
                            R.string.button_add,
                            (dialog, which) -> {
                                // Close dialog
                                dialog.dismiss();
                                // Check IP is valid
                                String ip = ipEditText.getText().toString();
                                if (RegexUtils.isValidIP(ip)) {
                                    // Insert list item
                                    mViewModel.addListItem(hostName, type, ip);
                                    // Display snackbar notification
                                    mInstallSnackbar.notifyUpdateAvailable();
                                }
                            }
                    )
                    .setNegativeButton(
                            R.string.button_cancel,
                            (dialog, which) -> dialog.dismiss()
                    )
                    .create();
            // Show dialog
            alertDialog.show();
            // Set button validation behavior
            ipEditText.addTextChangedListener(
                    new AlertDialogValidator(alertDialog, RegexUtils::isValidIP, false)
            );
        }
    }

    @Override
    public void removeListItem(@NonNull String hostName) {
        if (mViewModel != null && mInstallSnackbar != null) {
            mViewModel.removeListItem(hostName);
            mInstallSnackbar.notifyUpdateAvailable();
        }
    }

    @Override
    public void openHostInBrowser(@NonNull String hostName) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("http://" + hostName));
        startActivity(intent);
    }
}
