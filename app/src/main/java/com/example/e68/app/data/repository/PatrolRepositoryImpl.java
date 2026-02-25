package com.example.e68.app.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.e68.app.domain.entity.GpsPoint;
import com.example.e68.app.domain.entity.PatrolRoute;
import com.example.e68.app.domain.repository.PatrolRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PatrolRepositoryImpl implements PatrolRepository {

    private final MutableLiveData<List<PatrolRoute>> allRoutes = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<PatrolRoute> activeRoute = new MutableLiveData<>();
    private final Map<Long, List<GpsPoint>> routePoints = new HashMap<>();

    @Inject
    public PatrolRepositoryImpl() {
        // Инициализация
    }

    @Override
    public LiveData<List<PatrolRoute>> getAllRoutes() {
        return allRoutes;
    }

    @Override
    public LiveData<PatrolRoute> getActiveRoute() {
        return activeRoute;
    }

    @Override
    public long startRoute(String name) {
        PatrolRoute route = new PatrolRoute();
        route.setId(System.currentTimeMillis());
        route.setName(name);
        route.setStartTime(System.currentTimeMillis());
        route.setEndTime(null);
        route.setDistance(0);
        route.setDefectsFound(0);

        List<PatrolRoute> routes = allRoutes.getValue();
        if (routes == null) {
            routes = new ArrayList<>();
        }
        routes.add(route);
        allRoutes.postValue(routes);
        activeRoute.postValue(route);

        routePoints.put(route.getId(), new ArrayList<>());

        return route.getId();
    }

    @Override
    public void addGpsPoint(long routeId, GpsPoint point) {
        List<GpsPoint> points = routePoints.get(routeId);
        if (points == null) {
            points = new ArrayList<>();
            routePoints.put(routeId, points);
        }
        points.add(point);
    }

    @Override
    public void finishRoute(long routeId) {
        PatrolRoute current = activeRoute.getValue();
        if (current != null && current.getId() == routeId) {
            current.setEndTime(System.currentTimeMillis());

            // Расчет расстояния (упрощенно)
            List<GpsPoint> points = routePoints.get(routeId);
            if (points != null && points.size() > 1) {
                double distance = 0;
                for (int i = 0; i < points.size() - 1; i++) {
                    GpsPoint p1 = points.get(i);
                    GpsPoint p2 = points.get(i + 1);
                    distance += calculateDistance(p1.getLatitude(), p1.getLongitude(),
                            p2.getLatitude(), p2.getLongitude());
                }
                current.setDistance(distance);
            }

            activeRoute.postValue(null);

            // Обновляем в списке
            List<PatrolRoute> routes = allRoutes.getValue();
            if (routes != null) {
                for (int i = 0; i < routes.size(); i++) {
                    if (routes.get(i).getId() == routeId) {
                        routes.set(i, current);
                        break;
                    }
                }
                allRoutes.postValue(routes);
            }
        }
    }

    @Override
    public LiveData<List<GpsPoint>> getRoutePoints(long routeId) {
        MutableLiveData<List<GpsPoint>> result = new MutableLiveData<>();
        result.postValue(routePoints.get(routeId));
        return result;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Упрощенный расчет расстояния (в реальности использовать Location.distanceTo)
        double R = 6371000; // радиус Земли в метрах
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}