package com.kamakarov.musicshelf.core;

import com.kamakarov.musicshelf.web.IMusicApi;
import com.kamakarov.musicshelf.web.MusicApiImpl;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class MusicshelfModule {
    @Provides
    @Singleton
    public IMusicApi provideApi() {
        return new MusicApiImpl();
    }

}