# Sunion BLE communication SDK for Android
## System requirements
* Android 8.0 (API level 26) or higher
* Bluetooth 4.2 or higher
* Android Studio
## Install
* Git clone core_ble_android as sub-module of your app project.
* In settings.gradle, add:
```gradle=
include ':core_ble_android'
```
* In app/build.gradle, add:
```gradle=
plugins {
    ...
    id 'kotlin-kapt'
    id 'com.google.dagger.hilt.android'
    ...
}

dependencies {
    ...
    implementation project(':core_ble_android')
    
    // timber
    implementation "com.jakewharton.timber:timber:$timber_version"
    
    // hilt
    implementation "com.google.dagger:hilt-android:$hilt_version"
    kapt "com.google.dagger:hilt-android-compiler:$hilt_version"
    kapt "androidx.hilt:hilt-compiler:1.0.0"
    
    // RxAndroidBLE
    implementation "com.polidea.rxandroidble2:rxandroidble:1.16.0"
    
    // Qrcode
    implementation "com.journeyapps:zxing-android-embedded:4.3.0"
    ...
}
```
* Define required dependencies version in project build.gradle:
```gradle=
buildscript {
    ...
    ext {
        kotlinVersion = '1.7.0'
        coreVersion = '1.8.0'
        timber_version = '5.0.1'
        hilt_version = '2.42'
    }
    ...
}

plugins {
    ...
    id 'org.jetbrains.kotlin.android' version "$kotlinVersion" apply false
    id 'com.google.dagger.hilt.android' version "$hilt_version" apply false
    ...
}

```
* Sync and done

## Setup the SDK
### Permissions
In your app's AndroidManifest.xml file, add following permissions:
```xml=
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission-sdk-23 android:name="android.permission.ACCESS_COARSE_LOCATION" android:maxSdkVersion="30" tools:node="replace" />
<uses-permission-sdk-23 android:name="android.permission.ACCESS_FINE_LOCATION" android:maxSdkVersion="30" tools:node="replace" />
```
### Hilt
It's recommended to use Hilt dependency injection library in your project. Following examples use Hilt to inject dependencies into Android classes. Creating a module class BleModule in your project with following content:
```kotlin=
import android.content.Context
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.internal.RxBleLog
import com.sunion.core.ble.StatefulConnection
import com.sunion.core.ble.ReactiveStatefulConnection
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext

@InstallIn(SingletonComponent::class)
@Module(includes = [BleModule.Bind::class])
object BleModule {

    @Provides
    @Singleton
    fun provideRxBleClient(@ApplicationContext context: Context): RxBleClient {
        val rxRleClient = RxBleClient.create(context)
        RxBleClient.setLogLevel(RxBleLog.DEBUG)
        return rxRleClient
    }

    @InstallIn(SingletonComponent::class)
    @Module
    abstract class Bind {
        @Binds
        @Singleton
        abstract fun bindRxStatefulConnection(reactiveStatefulConnection: ReactiveStatefulConnection): StatefulConnection
    }
}
```
* Creating a module class AppModule in your project with following content:
```kotlin=
import com.sunion.core.ble.AppSchedulers
import com.sunion.core.ble.Scheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module(includes = [AppModule.Bind::class])
object AppModule {
    @InstallIn(SingletonComponent::class)
    @Module
    abstract class Bind {
        @Binds
        abstract fun bindScheduler(appSchedulers: AppSchedulers): Scheduler
    }
}
```
* Creating a Application class HiltApplication in your project with following content:
```kotlin=
import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class HiltApplication: Application(){
    override fun onCreate() {
        super.onCreate()

        if(BuildConfig.DEBUG){
            Timber.plant(Timber.DebugTree())
        }
    }
}
```
* In MainActivity, add:
```kotlin=
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    ...
}
```
* In AndroidManifest, add:
```kotlin=
<application
    android:name=".HiltApplication"
    ...
    >
```
## SDK add lock flow (Android)
![](https://i.imgur.com/OUfyDD7.jpg)

## Version
| Version | Description | File Name |
| -------- | -------- | -------- |
| v1 | Preliminary version of Bluetooth commands | Version1 |
| v2 | Preliminary version of Bluetooth commands | Version2 |
| v3 | Includes Matter Device | Version3 |