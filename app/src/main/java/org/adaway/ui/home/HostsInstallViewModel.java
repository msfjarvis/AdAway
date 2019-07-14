package org.adaway.ui.home;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import org.adaway.R;
import org.adaway.helper.PreferenceHelper;
import org.adaway.ui.AdAwayApplication;
import org.adaway.util.AppExecutors;
import org.adaway.util.ApplyUtils;
import org.adaway.util.Constants;
import org.adaway.util.Log;
import org.adaway.model.hostsinstall.HostsInstallError;
import org.adaway.model.hostsinstall.HostsInstallException;
import org.adaway.model.hostsinstall.HostsInstallModel;
import org.adaway.model.hostsinstall.HostsInstallStatus;

import java.util.Observer;

import static org.adaway.model.hostsinstall.HostsInstallStatus.INSTALLED;
import static org.adaway.model.hostsinstall.HostsInstallStatus.ORIGINAL;
import static org.adaway.model.hostsinstall.HostsInstallStatus.OUTDATED;
import static org.adaway.model.hostsinstall.HostsInstallStatus.WORK_IN_PROGRESS;

/**
 * This class is a {@link androidx.lifecycle.ViewModel} for home fragment UI.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class HostsInstallViewModel extends AndroidViewModel {
    private final HostsInstallModel model;
    private final MutableLiveData<HostsInstallStatus> status;
    private final MutableLiveData<String> state;
    private final MutableLiveData<String> details;
    private final MutableLiveData<HostsInstallError> error;
    private final Observer modelObserver;
    private boolean loaded;

    /**
     * Constructor.
     *
     * @param application The application context.
     */
    public HostsInstallViewModel(@NonNull Application application) {
        super(application);
        // Create model
        model = ((AdAwayApplication) application).getHostsInstallModel();
        // Initialize live data
        status = new MutableLiveData<>();
        state = new MutableLiveData<>();
        details = new MutableLiveData<>();
        error = new MutableLiveData<>();
        // Bind model to live data
        modelObserver = (o, a) -> {
            state.postValue(model.getState());
            details.postValue(model.getDetailedState());
        };
        model.addObserver(modelObserver);
        // Initialize model as not loaded
        loaded = false;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Unbind model to live data
        model.deleteObserver(modelObserver);
    }

    MutableLiveData<HostsInstallStatus> getStatus() {
        return status;
    }

    MutableLiveData<String> getState() {
        return state;
    }

    MutableLiveData<String> getDetails() {
        return details;
    }

    MutableLiveData<HostsInstallError> getError() {
        return error;
    }

    /**
     * Initialize the model with its current state.
     */
    void load() {
        // Check if model is already loaded
        if (loaded) {
            return;
        }
        loaded = true;
        // Check if hosts file is installed
        AppExecutors.getInstance().diskIO().execute(() -> {
            if (ApplyUtils.isHostsFileCorrect(Constants.ANDROID_SYSTEM_ETC_HOSTS)) {
                status.postValue(INSTALLED);
                setStateAndDetails(R.string.status_enabled, R.string.status_enabled_subtitle);
                // Check for update if needed
                if (PreferenceHelper.getUpdateCheck(getApplication())) {
                    checkForUpdate();
                }
            } else {
                status.postValue(ORIGINAL);
                setStateAndDetails(R.string.status_disabled, R.string.status_disabled_subtitle);
            }
        });
    }

    /**
     * Update the hosts file.
     */
    void update() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            HostsInstallStatus previousStatus = status.getValue();
            status.postValue(WORK_IN_PROGRESS);
            try {
                model.retrieveHostsSources();
                model.applyHostsFile();
                status.postValue(INSTALLED);
            } catch (HostsInstallException exception) {
                Log.e(Constants.TAG, "Failed to update hosts file.", exception);
                status.postValue(previousStatus);
                error.postValue(exception.getInstallError());
            }
        });
    }

    /**
     * Check if there is update available in hosts source.
     */
    void checkForUpdate() {
        AppExecutors.getInstance().networkIO().execute(() -> {
            // Update status
            status.postValue(WORK_IN_PROGRESS);
            try {
                // Check if update is available
                if (model.checkForUpdate()) {
                    status.postValue(OUTDATED);
                } else {
                    status.postValue(INSTALLED);
                }
            } catch (HostsInstallException exception) {
                Log.e(Constants.TAG, "Failed to check for update.", exception);
                status.postValue(INSTALLED);
                error.postValue(exception.getInstallError());
            }
        });
    }

    /**
     * Revert to the default hosts file.
     */
    void revert() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            status.postValue(WORK_IN_PROGRESS);
            try {
                model.revert();
                status.postValue(ORIGINAL);
            } catch (HostsInstallException exception) {
                Log.e(Constants.TAG, "Failed to revert hosts file.", exception);
                status.postValue(INSTALLED);
                error.postValue(exception.getInstallError());
            }
        });
    }

    private void setStateAndDetails(@StringRes int stateResId, @StringRes int detailsResId) {
        state.postValue(getApplication().getString(stateResId));
        details.postValue(getApplication().getString(detailsResId));
    }
}
