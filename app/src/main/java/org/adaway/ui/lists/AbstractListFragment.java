package org.adaway.ui.lists;

import android.app.Activity;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProviders;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.adaway.R;
import org.adaway.db.entity.HostListItem;

import java.util.List;

/**
 * This class is a {@link Fragment} to display and manage lists of {@link ListsFragment}.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public abstract class AbstractListFragment extends Fragment implements ListsViewCallback {
    /**
     * The view model (<code>null</code> if view is not created).
     */
    protected ListsViewModel mViewModel;
    /**
     * The current activity (<code>null</code> if view is not created).
     */
    protected Activity mActivity;
    /**
     * The current action mode when item is selection (<code>null</code> if no action started).
     */
    private ActionMode mActionMode;
    /**
     * The action mode callback (<code>null</code> if view is not created).
     */
    private ActionMode.Callback mActionCallback;
    /**
     * The hosts list related to the current action (<code>null</code> if view is not created).
     */
    private HostListItem mActionItem;
    /**
     * The view related hosts source of the current action (<code>null</code> if view is not created).
     */
    private View mActionSourceView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Store activity
        mActivity = getActivity();
        // Create fragment view
        View view = inflater.inflate(R.layout.hosts_lists_fragment, container, false);
        /*
         * Configure recycler view.
         */
        // Store recycler view
        RecyclerView recyclerView = view.findViewById(R.id.hosts_lists_list);
        recyclerView.setHasFixedSize(true);
        // Defile recycler layout
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mActivity);
        recyclerView.setLayoutManager(linearLayoutManager);
        // Create recycler adapter
        ListAdapter adapter = new ListsAdapter(this, isTwoRowsItem());
        recyclerView.setAdapter(adapter);
        /*
         * Create action mode.
         */
        // Create action mode callback to display edit/delete menu
        mActionCallback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                // Get menu inflater
                MenuInflater inflater = actionMode.getMenuInflater();
                // Set action mode title
                actionMode.setTitle(R.string.checkbox_list_context_title);
                // Inflate edit/delete menu
                inflater.inflate(R.menu.checkbox_list_context, menu);
                // Return action created
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                // Nothing special to do
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem item) {
                // Check action item
                if (mActionItem == null) {
                    return false;
                }
                // Check item identifier
                switch (item.getItemId()) {
                    case R.id.checkbox_list_context_edit:
                        // Edit action item
                        editItem(mActionItem);
                        // Finish action mode
                        mActionMode.finish();
                        return true;
                    case R.id.checkbox_list_context_delete:
                        // Delete action item
                        deleteItem(mActionItem);
                        // Finish action mode
                        mActionMode.finish();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
                // Clear view background color
                if (mActionSourceView != null) {
                    mActionSourceView.setBackgroundColor(Color.TRANSPARENT);
                }
                // Clear current source and its view
                mActionItem = null;
                mActionSourceView = null;
                // Clear action mode
                mActionMode = null;
            }
        };
        /*
         * Load data.
         */
        // Get view model and bind it to the list view
        mViewModel = ViewModelProviders.of(this).get(ListsViewModel.class);
        getData().observe(this, adapter::submitList);
        // Return created view
        return view;
    }

    @Override
    public boolean startAction(HostListItem item, View sourceView) {
        // Check if there is already a current action
        if (mActionMode != null) {
            return false;
        }
        // Store current source and its view
        mActionItem = item;
        mActionSourceView = sourceView;
        // Get current item background color
        int currentItemBackgroundColor = getResources().getColor(R.color.selected_background);
        // Apply background color to view
        mActionSourceView.setBackgroundColor(currentItemBackgroundColor);
        // Start action mode and store it
        mActionMode = mActivity.startActionMode(mActionCallback);
        // Return event consumed
        return true;
    }

    /**
     * Ensure action mode is cancelled.
     */
    void ensureActionModeCanceled() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    protected abstract LiveData<List<HostListItem>> getData();

    protected boolean isTwoRowsItem() {
        return false;
    }

    protected abstract void addItem();

    protected abstract void editItem(HostListItem item);

    protected void deleteItem(HostListItem item) {
        mViewModel.removeListItem(item);
    }

    @Override
    public void toggleItemEnabled(HostListItem list) {
        mViewModel.toggleItemEnabled(list);
    }
}
