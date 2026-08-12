package com.waynelinnn.voiceagent.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * App-wide Hilt bindings. Concrete providers land in later modules.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule
