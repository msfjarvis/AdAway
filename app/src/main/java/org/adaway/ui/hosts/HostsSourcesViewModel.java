package org.adaway.ui.hosts;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.annotation.NonNull;

import org.adaway.db.AppDatabase;
import org.adaway.db.dao.HostsSourceDao;
import org.adaway.db.entity.HostsSource;
import org.adaway.util.AppExecutors;

import java.util.List;

/**
 * This class is an {@link AndroidViewModel} for the {@link HostsSourcesFragment}.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class HostsSourcesViewModel extends AndroidViewModel {

    private final HostsSourceDao hostsSourceDao;

    public HostsSourcesViewModel(@NonNull Application application) {
        super(application);
        hostsSourceDao = AppDatabase.getInstance(getApplication()).hostsSourceDao();
    }

    public LiveData<List<HostsSource>> getHostsSources() {
        return hostsSourceDao.loadAll();
    }

    public void toggleSourceEnabled(HostsSource source) {
        source.setEnabled(!source.isEnabled());
        AppExecutors.getInstance().diskIO().execute(() -> hostsSourceDao.update(source));
    }

    public void addSourceFromUrl(String url) {
        HostsSource source = new HostsSource();
        source.setUrl(url);
        source.setEnabled(true);
        AppExecutors.getInstance().diskIO().execute(() -> hostsSourceDao.insert(source));
    }

    public void updateSourceUrl(HostsSource source, String url) {
        HostsSource newSource = new HostsSource();
        newSource.setUrl(url);
        newSource.setEnabled(source.isEnabled());
        AppExecutors.getInstance().diskIO().execute(() -> {
            hostsSourceDao.delete(source);
            hostsSourceDao.insert(newSource);
        });
    }

    public void removeSource(HostsSource source) {
        AppExecutors.getInstance().diskIO().execute(() -> hostsSourceDao.delete(source));
    }
}
