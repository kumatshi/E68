package com.example.e68.app.di;

import com.example.e68.app.data.repository.DefectRepositoryImpl;
import com.example.e68.app.data.repository.PatrolRepositoryImpl;
import com.example.e68.app.data.repository.UserRepositoryImpl;
import com.example.e68.app.domain.repository.DefectRepository;
import com.example.e68.app.domain.repository.PatrolRepository;
import com.example.e68.app.domain.repository.UserRepository;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public abstract class RepositoryModule {

    @Binds
    @Singleton
    public abstract UserRepository bindUserRepository(UserRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract DefectRepository bindDefectRepository(DefectRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract PatrolRepository bindPatrolRepository(PatrolRepositoryImpl impl);
}