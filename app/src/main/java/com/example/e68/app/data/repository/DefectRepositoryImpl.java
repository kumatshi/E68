package com.example.e68.app.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.e68.app.domain.entity.Defect;
import com.example.e68.app.domain.repository.DefectRepository;
import com.example.e68.app.util.Resource;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DefectRepositoryImpl implements DefectRepository {

    private final MutableLiveData<List<Defect>> allDefects = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Defect> currentDefect = new MutableLiveData<>();

    @Inject
    public DefectRepositoryImpl() {
        // Добавим тестовые данные
        addTestData();
    }

    private void addTestData() {
        List<Defect> defects = new ArrayList<>();

        Defect defect1 = new Defect();
        defect1.setId(1);
        defect1.setTitle("Выбоина на дороге");
        defect1.setDescription("Глубокая выбоина на проезжей части");
        defect1.setType("PH_001");
        defect1.setSeverity("HIGH");
        defect1.setStatus("OPEN");
        defect1.setAddress("ул. Ленина, 10");
        defect1.setLatitude(55.7558);
        defect1.setLongitude(37.6173);
        defect1.setCreatedAt(System.currentTimeMillis() - 86400000);
        defect1.setCreatedBy("Иванов И.И.");
        defects.add(defect1);

        Defect defect2 = new Defect();
        defect2.setId(2);
        defect2.setTitle("Поврежденный люк");
        defect2.setDescription("Люк открыт, требует ремонта");
        defect2.setType("MK_001");
        defect2.setSeverity("CRITICAL");
        defect2.setStatus("IN_PROGRESS");
        defect2.setAddress("ул. Пушкина, 5");
        defect2.setLatitude(55.7658);
        defect2.setLongitude(37.6273);
        defect2.setCreatedAt(System.currentTimeMillis() - 172800000);
        defect2.setCreatedBy("Петров П.П.");
        defects.add(defect2);

        allDefects.postValue(defects);
    }

    @Override
    public LiveData<List<Defect>> getAllDefects() {
        return allDefects;
    }

    @Override
    public LiveData<Defect> getDefectById(long id) {
        MutableLiveData<Defect> result = new MutableLiveData<>();
        List<Defect> defects = allDefects.getValue();
        if (defects != null) {
            for (Defect defect : defects) {
                if (defect.getId() == id) {
                    result.postValue(defect);
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public LiveData<Resource<Defect>> createDefect(Defect defect) {
        MutableLiveData<Resource<Defect>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        new android.os.Handler().postDelayed(() -> {
            List<Defect> defects = allDefects.getValue();
            if (defects == null) {
                defects = new ArrayList<>();
            }

            long newId = defects.size() + 1;
            defect.setId(newId);
            defect.setCreatedAt(System.currentTimeMillis());
            defect.setStatus("OPEN");

            defects.add(defect);
            allDefects.postValue(defects);

            result.postValue(Resource.success(defect));
        }, 1000);

        return result;
    }

    @Override
    public LiveData<Resource<Defect>> updateDefect(Defect defect) {
        MutableLiveData<Resource<Defect>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        List<Defect> defects = allDefects.getValue();
        if (defects != null) {
            for (int i = 0; i < defects.size(); i++) {
                if (defects.get(i).getId() == defect.getId()) {
                    defects.set(i, defect);
                    break;
                }
            }
            allDefects.postValue(defects);
        }

        result.postValue(Resource.success(defect));
        return result;
    }

    @Override
    public LiveData<Resource<Void>> deleteDefect(long id) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();

        List<Defect> defects = allDefects.getValue();
        if (defects != null) {
            defects.removeIf(defect -> defect.getId() == id);
            allDefects.postValue(defects);
        }

        result.postValue(Resource.success(null));
        return result;
    }

    @Override
    public LiveData<List<Defect>> getDefectsByStatus(String status) {
        MutableLiveData<List<Defect>> result = new MutableLiveData<>();
        List<Defect> filtered = new ArrayList<>();

        List<Defect> defects = allDefects.getValue();
        if (defects != null) {
            for (Defect defect : defects) {
                if (defect.getStatus().equals(status)) {
                    filtered.add(defect);
                }
            }
        }
        result.postValue(filtered);
        return result;
    }

    @Override
    public List<Defect> getUnsyncedDefects() {
        return new ArrayList<>(); // Для офлайн режима
    }
}