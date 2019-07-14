package org.adaway.ui.hostsinstall;

import androidx.lifecycle.Observer;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import org.adaway.R;
import org.adaway.model.hostsinstall.HostsInstallException;
import org.adaway.model.hostsinstall.HostsInstallModel;
import org.adaway.ui.AdAwayApplication;
import org.adaway.util.AppExecutors;

import java.util.Collection;

import static com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE;
import static com.google.android.material.snackbar.Snackbar.LENGTH_LONG;

/**
 * This class is a {@link Snackbar} to notify about hosts install need.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class HostsInstallSnackbar {
    /**
     * The view to bind the snackbar to.
     */
    private View mView;
    /**
     * The current hosts update available status ({@code true} if update available, {@code false} otherwise).
     */
    private boolean update;
    /**
     * Whether or not ignore the next update event ({@code true} to ignore, {@code false} otherwise).
     */
    private boolean skipUpdate;
    /**
     * Whether or not ignore update events during the install ({@code true} to ignore, {@code false} otherwise).
     */
    private boolean ignoreEventDuringInstall;
    /**
     * The notify snackbar when hosts update available ({@code null} if no hosts update).
     */
    private Snackbar notifySnackbar;
    /**
     * The wait snackbar during hosts install ({@code null} if no pending hosts install).
     */
    private Snackbar waitSnackbar;

    /**
     * Constructor.
     *
     * @param view The view to bind the snackbar to.
     */
    public HostsInstallSnackbar(@NonNull View view) {
        mView = view;
        update = false;
        skipUpdate = false;
        ignoreEventDuringInstall = false;
    }

    /**
     * Set whether or not ignore update events during the install.
     *
     * @param ignore {@code true} to ignore events, {@code false} otherwise.
     */
    public void setIgnoreEventDuringInstall(boolean ignore) {
        ignoreEventDuringInstall = ignore;
    }

    /**
     * Create {@link Observer} which ignores first (initialization) event.
     *
     * @param <T> The type of data to observe.
     * @return The observer instance.
     */
    public <T> Observer<T> createObserver() {
        return new Observer<T>() {
            boolean firstUpdate = true;

            @Override
            public void onChanged(@Nullable T t) {
                // Check new data
                if (t == null || (t instanceof Collection && ((Collection) t).isEmpty())) {
                    return;
                }
                // First update
                if (firstUpdate) {
                    firstUpdate = false;
                    return;
                }
                notifyUpdateAvailable();
            }
        };
    }

    /**
     * Notify update available.
     */
    public void notifyUpdateAvailable() {
        // Check if notify snackbar is already displayed
        if (notifySnackbar != null) {
            return;
        }
        // Check if wait snackbar is displayed
        if (waitSnackbar != null) {
            // Mark update available
            update = true;
            return;
        }
        // Check if update event should be skipped
        if (skipUpdate) {
            skipUpdate = false;
            return;
        }
        // Show notify snackbar
        notifySnackbar = Snackbar.make(mView, R.string.notification_configuration_changed, LENGTH_INDEFINITE)
                .setAction(R.string.notification_configuration_changed_action, v -> install());
        notifySnackbar.show();
        // Mark update as notified
        update = false;
    }

    private void install() {
        showLoading();
        AppExecutors.getInstance().diskIO().execute(() -> {
            AdAwayApplication application = (AdAwayApplication) mView.getContext().getApplicationContext();
            HostsInstallModel model = application.getHostsInstallModel();
            try {
                model.retrieveHostsSources();
                model.applyHostsFile();
                endLoading(true);
            } catch (HostsInstallException exception) {
                endLoading(false);
            }
        });
    }

    private void showLoading() {
        // Clear notify snackbar
        if (notifySnackbar != null) {
            notifySnackbar.dismiss();
            notifySnackbar = null;
        }
        // Create and show wait snackbar
        waitSnackbar = Snackbar.make(mView, R.string.notification_configuration_installing, LENGTH_INDEFINITE);
        appendViewToSnackbar(waitSnackbar, new ProgressBar(mView.getContext()));
        waitSnackbar.show();
    }

    private void endLoading(boolean successfulInstall) {
        // Clear wait snackbar
        if (waitSnackbar != null) {
            waitSnackbar.dismiss();
            waitSnackbar = null;
        }
        // Check install failure
        if (!successfulInstall) {
            Snackbar failureSnackbar = Snackbar.make(mView, R.string.notification_configuration_failed, LENGTH_LONG);
            ImageView view = new ImageView(mView.getContext());
            view.setImageResource(R.drawable.status_fail);
            appendViewToSnackbar(failureSnackbar, view);
            failureSnackbar.show();
        }
        // Check pending update notification
        else if (update) {
            // Ignore next update event if events should be ignored
            if (ignoreEventDuringInstall) {
                skipUpdate = true;
            } else {
                // Otherwise display update notification
                notifyUpdateAvailable();
            }
        }
    }

    private void appendViewToSnackbar(Snackbar snackbar, View view) {
        ViewGroup viewGroup = (ViewGroup) snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text).getParent();
        viewGroup.addView(view);
    }
}
