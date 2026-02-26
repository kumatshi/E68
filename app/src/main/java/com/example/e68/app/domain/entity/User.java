package com.example.e68.app.domain.entity;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {

    private String uid;
    private String name;
    private String email;
    private String role;
    private String department;
    private boolean isActive;

    // Обязательный пустой конструктор для Firestore
    public User() {}

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }

    public boolean isInspector() { return "INSPECTOR".equals(role); }
    public boolean isManager()   { return "MANAGER".equals(role); }
    public boolean isAdmin()     { return "ADMIN".equals(role); }
}