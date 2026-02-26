package com.example.e68.app.presentation.patrol;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.e68.app.domain.entity.GpsPoint;
import com.example.e68.app.domain.entity.PatrolRoute;
import com.example.e68.app.domain.repository.PatrolRepository;
import com.example.e68.app.presentation.common.BaseViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;
import java.util.List;

@HiltViewModel
public class PatrolViewModel extends BaseViewModel {

    private final PatrolRepository patrolRepository;
    private final MutableLiveData<Boolean> _isActive = new MutableLiveData<>(false);
    private long activeRouteId = -1;

    @Inject
    public PatrolViewModel(PatrolRepository patrolRepository) {
        this.patrolRepository = patrolRepository;
    }

    public LiveData<Boolean> isPatrolActive() { return _isActive; }

    public LiveData<PatrolRoute> getActiveRoute() {
        return patrolRepository.getActiveRoute();
    }

    public LiveData<List<GpsPoint>> getRoutePoints() {
        if (activeRouteId == -1) return new MutableLiveData<>();
        return patrolRepository.getRoutePoints(activeRouteId);
    }

    public void startPatrol(String name) {
        activeRouteId = patrolRepository.startRoute(name);
        _isActive.postValue(true);
    }

    public void stopPatrol() {
        if (activeRouteId != -1) {
            patrolRepository.finishRoute(activeRouteId);
            activeRouteId = -1;
        }
        _isActive.postValue(false);
    }
}
