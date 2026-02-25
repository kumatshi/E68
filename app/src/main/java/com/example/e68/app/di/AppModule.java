package com.example.e68.app.di;

import android.content.Context;
import androidx.room.Room;
import com.example.e68.app.data.local.db.AppDatabase;
import com.example.e68.app.domain.usecase.CreateDefectUseCase;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    @Provides
    @Singleton
    public static AppDatabase provideAppDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(
                context,
                AppDatabase.class,
                "e68_database"
        ).build();
    }

}