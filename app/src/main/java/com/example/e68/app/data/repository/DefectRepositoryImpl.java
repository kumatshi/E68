package com.example.e68.app.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.e68.app.domain.entity.Defect;
import com.example.e68.app.domain.repository.DefectRepository;
import com.example.e68.app.util.Resource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DefectRepositoryImpl implements DefectRepository {

    private static final String TAG = "DefectRepository";
    private static final String COL = "defects";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private final MutableLiveData<List<Defect>> _allDefects = new MutableLiveData<>(new ArrayList<>());
    private ListenerRegistration listenerReg;

    @Inject
    public DefectRepositoryImpl(FirebaseFirestore db, FirebaseAuth auth) {
        this.db = db;
        this.auth = auth;
        startRealtimeListener();
    }

    private void startRealtimeListener() {
        listenerReg = db.collection(COL)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    List<Defect> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Defect d = docToDefect(doc);
                        if (d != null) list.add(d);
                    }
                    _allDefects.postValue(list);
                    Log.d(TAG, "Loaded " + list.size() + " defects from Firestore");
                });
    }

    @Override
    public LiveData<List<Defect>> getAllDefects() {
        return _allDefects;
    }

    @Override
    public LiveData<Defect> getDefectById(long id) {
        MutableLiveData<Defect> result = new MutableLiveData<>();
        List<Defect> all = _allDefects.getValue();
        if (all != null) {
            for (Defect d : all) {
                if (d.getId() == id) { result.setValue(d); return result; }
            }
        }
        result.setValue(null);
        return result;
    }

    @Override
    public LiveData<List<Defect>> getDefectsByStatus(String status) {
        MutableLiveData<List<Defect>> result = new MutableLiveData<>();
        List<Defect> all = _allDefects.getValue();
        List<Defect> filtered = new ArrayList<>();
        if (all != null) {
            for (Defect d : all) {
                if (status.equals(d.getStatus())) filtered.add(d);
            }
        }
        result.setValue(filtered);
        return result;
    }

    @Override
    public LiveData<Resource<Defect>> createDefect(Defect defect) {
        MutableLiveData<Resource<Defect>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        String currentUid = auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid() : "unknown";
        String currentEmail = auth.getCurrentUser() != null
                ? auth.getCurrentUser().getEmail() : "unknown";

        defect.setStatus("OPEN");
        defect.setCreatedAt(System.currentTimeMillis());
        defect.setCreatedBy(currentEmail);

        Map<String, Object> data = defectToMap(defect);
        data.put("inspectorUid", currentUid);

        Log.d(TAG, "Creating defect: " + data.toString());

        db.collection(COL).add(data)
                .addOnSuccessListener(ref -> {
                    defect.setId(ref.getId().hashCode());
                    defect.setLocalUuid(ref.getId());
                    Log.d(TAG, "Defect created with ID: " + ref.getId());
                    result.setValue(Resource.success(defect));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create defect: " + e.getMessage());
                    result.setValue(Resource.error(e.getMessage(), null));
                });

        return result;
    }

    @Override
    public LiveData<Resource<Defect>> updateDefect(Defect defect) {
        MutableLiveData<Resource<Defect>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        if (defect.getLocalUuid() == null) {
            result.setValue(Resource.error("Нет Firestore ID", null));
            return result;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("title",       defect.getTitle());
        updates.put("type",        defect.getType());
        updates.put("address",     defect.getAddress());
        updates.put("description", defect.getDescription());
        updates.put("severity",    defect.getSeverity());
        updates.put("status",      defect.getStatus());
        updates.put("updatedAt",   System.currentTimeMillis());

        final String localUuid = defect.getLocalUuid();

        db.collection(COL).document(localUuid)
                .update(updates)
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "Defect updated: " + localUuid);
                    result.setValue(Resource.success(defect));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update defect: " + e.getMessage());
                    result.setValue(Resource.error(e.getMessage(), null));
                });

        return result;
    }

    @Override
    public LiveData<Resource<Void>> deleteDefect(long id) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();

        List<Defect> all = _allDefects.getValue();
        String firestoreId = null;
        if (all != null) {
            for (Defect d : all) {
                if (d.getId() == id) {
                    firestoreId = d.getLocalUuid();
                    break;
                }
            }
        }

        if (firestoreId == null) {
            result.setValue(Resource.error("Дефект не найден", null));
            return result;
        }

        final String finalFirestoreId = firestoreId;

        db.collection(COL).document(finalFirestoreId).delete()
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "Defect deleted: " + finalFirestoreId);
                    result.setValue(Resource.success(null));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete defect: " + e.getMessage());
                    result.setValue(Resource.error(e.getMessage(), null));
                });

        return result;
    }

    @Override
    public List<Defect> getUnsyncedDefects() {
        return new ArrayList<>();
    }

    private Defect docToDefect(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        Defect d = new Defect();
        d.setId(doc.getId().hashCode());
        d.setLocalUuid(doc.getId());
        d.setTitle(doc.getString("title"));
        d.setDescription(doc.getString("description"));
        d.setType(doc.getString("type"));
        d.setSeverity(doc.getString("severity"));
        d.setStatus(doc.getString("status"));
        d.setAddress(doc.getString("address"));
        d.setPhotoBase64(doc.getString("photoBase64")); // ← ДОБАВЬТЕ ЭТУ СТРОКУ
        Double lat = doc.getDouble("latitude");
        Double lng = doc.getDouble("longitude");
        if (lat != null) d.setLatitude(lat);
        if (lng != null) d.setLongitude(lng);
        d.setCreatedBy(doc.getString("createdBy"));
        Long createdAt = doc.getLong("createdAt");
        if (createdAt != null) d.setCreatedAt(createdAt);
        d.setPhotoPath(doc.getString("photoPath"));
        return d;
    }

    private Map<String, Object> defectToMap(Defect d) {
        Map<String, Object> m = new HashMap<>();
        m.put("title",       d.getTitle());
        m.put("description", d.getDescription());
        m.put("type",        d.getType());
        m.put("severity",    d.getSeverity());
        m.put("status",      d.getStatus());
        m.put("address",     d.getAddress());
        m.put("latitude",    d.getLatitude());
        m.put("longitude",   d.getLongitude());
        m.put("createdBy",   d.getCreatedBy());
        m.put("createdAt",   d.getCreatedAt());
        m.put("updatedAt",   d.getCreatedAt());
        m.put("photoPath",   d.getPhotoPath());
        m.put("photoBase64", d.getPhotoBase64());
        return m;
    }
}