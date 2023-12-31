package io.astronout.core.di

import android.content.Context
import androidx.room.Room
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import com.skydoves.sandwich.coroutines.CoroutinesResponseCallAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.astronout.core.BuildConfig
import io.astronout.core.domain.repository.GamesRepository
import io.astronout.core.data.source.GamesStoryDataStore
import io.astronout.core.data.source.local.LocalDataSource
import io.astronout.core.data.source.local.LocalDataSourceImpl
import io.astronout.core.data.source.local.room.GameDatabase
import io.astronout.core.data.source.remote.web.ApiClient
import io.astronout.core.data.source.remote.web.ApiService
import io.astronout.core.domain.usecase.GameInteractor
import io.astronout.core.domain.usecase.GameUsecase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun provideChuckerInterceptor(@ApplicationContext context: Context): ChuckerInterceptor {
        return ChuckerInterceptor.Builder(context)
            .apply {
                collector(
                    ChuckerCollector(
                        context = context,
                        showNotification = BuildConfig.DEBUG,
                        retentionPeriod = RetentionManager.Period.ONE_DAY
                    )
                )
                maxContentLength(250_000L)
                alwaysReadResponseBody(false)
                if (!BuildConfig.DEBUG) {
                    redactHeaders("Authorization", "Bearer")
                    redactHeaders("Authorization", "Basic")
                }
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(chuckerInterceptor: ChuckerInterceptor) = if (BuildConfig.DEBUG) {
        OkHttpClient.Builder()
//            .addInterceptor(AuthInterceptor())
            .addInterceptor(chuckerInterceptor)
            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .build()
    } else {
        OkHttpClient.Builder()
//            .addInterceptor(AuthInterceptor())
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create())
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addCallAdapterFactory(CoroutinesResponseCallAdapterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)

    @Provides
    @Singleton
    fun provideIODispatcher(): CoroutineDispatcher {
        return Dispatchers.IO
    }

    @Provides
    @Singleton
    fun provideGameDatabase(@ApplicationContext context: Context): GameDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            GameDatabase::class.java,
            "game_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideLocalDataSource(gameDatabase: GameDatabase): LocalDataSource = LocalDataSourceImpl(gameDatabase)

    @Provides
    @Singleton
    fun provideRepository(apiClient: ApiClient, localDataSource: LocalDataSource, ioDispatcher: CoroutineDispatcher): GamesRepository {
        return GamesStoryDataStore(apiClient, localDataSource, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideGameUsecase(repo: GamesRepository, localDataSource: LocalDataSource): GameUsecase {
        return GameInteractor(repo, localDataSource)
    }

}