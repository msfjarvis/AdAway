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

package org.adaway.ui;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.adaway.R;
import org.adaway.helper.NotificationHelper;
import org.adaway.helper.ThemeHelper;
import org.adaway.ui.help.HelpActivity;
import org.adaway.ui.home.HomeFragment;
import org.adaway.ui.hosts.HostsSourcesFragment;
import org.adaway.ui.hostscontent.HostsContentFragment;
import org.adaway.ui.lists.ListsFragment;
import org.adaway.ui.prefs.PrefsFragment;
import org.adaway.ui.tcpdump.TcpdumpFragment;
import org.adaway.util.Constants;
import org.adaway.util.Log;
import org.adaway.util.SentryLog;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

/**
 * This class is the application main activity.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class MainActivity extends AppCompatActivity {
    /**
     * The back stack state name of the secondary fragment.
     * ({@link HomeFragment} is always the primary fragment.)
     */
    public static final String STACK_STATE_NAME = "secondary-fragment";
    /**
     * The selected menu item key for saved instance {@link Bundle}.
     */
    public static final String SELECTED_MENU_ITEM_KEY = "SELECTED_MENU_ITEM";
    /**
     * The project link.
     */
    private static final String PROJECT_LINK = "https://github.com/AdAway/AdAway";
    /**
     * The support link.
     */
    private static final String SUPPORT_LINK = "https://paypal.me/BruceBUJON";
    /*
     * Application navigation related.
     */
    /**
     * The navigation drawer layout.
     */
    private DrawerLayout mDrawerLayout;
    /**
     * The navigation drawer toggle.
     */
    private ActionBarDrawerToggle mDrawerToggle;
    /**
     * The navigation drawer.
     */
    private View mDrawer;
    /**
     * The navigation drawer list.
     */
    private ListView mDrawerList;
    /**
     * The activity title.
     */
    private CharSequence mTitle;
    /**
     * The navigation drawer title.
     */
    private CharSequence mDrawerTitle;
    /**
     * The position of selected menu item of drawer list.
     */
    private int mSelectedMenuItem;

    /**
     * Instantiate View and initialize fragments for this Activity
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.applyTheme(this);
        NotificationHelper.clearUpdateHostsNotification(this);
        Log.i(Constants.TAG, "Starting main activity");
        setContentView(R.layout.base_activity_drawer);
        /*
         * Configure navigation drawer.
         */
        mDrawer = findViewById(R.id.left_drawer);
        // Configure drawer items
        String[] drawerItems = getResources().getStringArray(R.array.drawer_items);
        mDrawerList = findViewById(R.id.left_drawer_list);
        mDrawerList.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_list_item, drawerItems));
        // Set drawer item listener
        mDrawerList.setOnItemClickListener((parent, view, position, id) -> selectDrawerMenuItem(position));
        // Configure drawer toggle
        mTitle = mDrawerTitle = getTitle();
        mSelectedMenuItem = 0;
        mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
        ) {
            /** Called when a drawer has settled in a completely closed state. */
            @Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setTitle(mTitle);
                }
            }

            /** Called when a drawer has settled in a completely open state. */
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setTitle(mDrawerTitle);
                }
            }

        };
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        updateSelectedMenuItem();
        // Set version number and click listener
        TextView versionNumberTextView = findViewById(R.id.version_number);
        versionNumberTextView.setText(getApplicationVersion());
        versionNumberTextView.setOnClickListener(this::showProjectPage);
        // Set support text view click listener
        TextView supportTextView = findViewById(R.id.support_text);
        supportTextView.setOnClickListener(this::showSupportPage);
        /*
         * Configure actionbar.
         */
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
        /*
         * Insert main fragment.
         */
        // Check activity initialization
        if (savedInstanceState == null) {
            // Get fragment manager
            FragmentManager fragmentManager = getSupportFragmentManager();
            // Add selected menu item as back stack change listener
            fragmentManager.addOnBackStackChangedListener(this::updateSelectedMenuItem);
            // Insert fragment as main content
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.content_frame, new HomeFragment());
            fragmentTransaction.commit();
            // Get shortcut extra if defined
            Intent intent = getIntent();
            Bundle extras = intent.getExtras();
            if (extras != null) {
                String shortcut = extras.getString("shortcut", "");
                // Check shortcut to start
                switch (shortcut) {
                    case "your_lists":
                        selectDrawerMenuItem(2);
                        break;
                    case "dns_requests":
                        selectDrawerMenuItem(4);
                        break;
                    case "preferences":
                        selectDrawerMenuItem(6);
                        break;
                    default:
                        // Shortcut not defined
                        break;
                }
            }
        } else {
            // Restore selected menu item
            mSelectedMenuItem = savedInstanceState.getInt(SELECTED_MENU_ITEM_KEY, 0);
            // Restore activity title
            updateSelectedMenuItem();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
        // Check user telemetry consent
        SentryLog.requestUserConsent(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_MENU_ITEM_KEY, mSelectedMenuItem);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle
        // If it returns true then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Define HomeFragment as selected menu item
        mSelectedMenuItem = 0;
        // Delegate back pressed
        super.onBackPressed();
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(mTitle);
        }
    }

    /**
     * Swaps fragments in the main content view.
     *
     * @param position The position of the selected item in drawer menu.
     */
    private void selectDrawerMenuItem(int position) {
        Fragment fragment = null;
        switch (position) {
            case 0:
                // HomeFragment, no fragment to instantiate
                break;
            case 1:
                fragment = new HostsSourcesFragment();
                break;
            case 2:
                fragment = new ListsFragment();
                break;
            case 3:
                fragment = new HostsContentFragment();
                break;
            case 4:
                fragment = new TcpdumpFragment();
                break;
            case 5:
                fragment = new PrefsFragment();
                break;
            case 6:
                // Restore drawer selected item
                mDrawerList.setItemChecked(mSelectedMenuItem, true);
                // Start help activity
                startActivity(new Intent(this, HelpActivity.class));
                // Do nothing more
                return;
            default:
                // Position is not supported. Exit.
                return;
        }
        // Update selected menu item
        mSelectedMenuItem = position;
        // Pop back stack up to HomeFragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.popBackStack(STACK_STATE_NAME, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        // Check there is new fragment to insert
        if (fragment != null) {
            // Insert the fragment by replacing any existing fragment
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .addToBackStack(STACK_STATE_NAME)
                    .commit();
        }
        // Close the drawer
        mDrawerLayout.closeDrawer(mDrawer);
    }

    /**
     * Update the activity title and selected menu item in drawer list.
     */
    private void updateSelectedMenuItem() {
        // Get position of selected menu item
        int position = mSelectedMenuItem;
        // Set selected item in drawer list
        mDrawerList.setItemChecked(position, true);
        // Get item name
        String itemName;
        if (position == 0) {
            itemName = getString(R.string.app_name);
        } else {
            itemName = getResources().getStringArray(R.array.drawer_items)[position];
        }
        // Update title with item name
        setTitle(itemName);
        // Record breadcrumb
        SentryLog.recordBreadcrumb("Using \"" + itemName + "\" feature");
    }

    /**
     * Get application version.
     *
     * @return The application version number.
     */
    private String getApplicationVersion() {
        try {
            PackageManager manager = getPackageManager();
            PackageInfo info = manager.getPackageInfo(getPackageName(), 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException exception) {
            Log.w(Constants.TAG, "Unable to get application version: " + exception.getMessage());
            return "";
        }
    }

    /**
     * Show development page.
     *
     * @param view The source event view.
     */
    private void showProjectPage(@SuppressWarnings("unused") View view) {
        // Close the drawer
        mDrawerLayout.closeDrawer(mDrawer);
        // Show development page
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(PROJECT_LINK));
        startActivity(browserIntent);
    }

    /**
     * Show support page.
     *
     * @param view The source event view.
     */
    private void showSupportPage(@SuppressWarnings("unused") View view) {
        // Close the drawer
        mDrawerLayout.closeDrawer(mDrawer);
        // Show support dialog
        new MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.baseline_favorite_24)
                .setTitle(R.string.drawer_support_dialog_title)
                .setMessage(R.string.drawer_support_dialog_text)
                .setPositiveButton(R.string.drawer_support_dialog_button, (d, which) -> {
                    // Show support page
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(SUPPORT_LINK));
                    startActivity(browserIntent);
                })
                .create()
                .show();
    }
}
