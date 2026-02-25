package com.example.e68.app.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.e68.app.domain.entity.GpsPoint;
import com.example.e68.app.domain.entity.PatrolRoute;
import java.util.List;

public interface PatrolRepository {

    LiveData<List<PatrolRoute>> getAllRoutes();

    LiveData<PatrolRoute> getActiveRoute();

    long startRoute(String name);

    void addGpsPoint(long routeId, GpsPoint point);

    void finishRoute(long routeId);

    LiveData<List<GpsPoint>> getRoutePoints(long routeId);
}