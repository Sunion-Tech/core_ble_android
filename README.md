# Sunion BLE communication SDK for Android
## System requirements
* Android 8.0 (API level 26) or higher
* Bluetooth 4.2 or higher
* Android Studio
## Install
* Git clone core_ble_android as sub-module of your app project.
* In settings.gradle, add:
```
include ':core_ble_android'
```
* In app/build.gradle, add:
```
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
```
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
```
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission-sdk-23 android:name="android.permission.ACCESS_COARSE_LOCATION" android:maxSdkVersion="30" tools:node="replace" />
<uses-permission-sdk-23 android:name="android.permission.ACCESS_FINE_LOCATION" android:maxSdkVersion="30" tools:node="replace" />
```
### Hilt
It's recommended to use Hilt dependency injection library in your project. Following examples use Hilt to inject dependencies into Android classes. Creating a module class BleModule in your project with following content:
```
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
```
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
```
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
```
...
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    ...
}
```
* In AndroidManifest, add:
```
<application
    android:name=".HiltApplication"
    ...
    >
```
## Quick start
### Pairing with lock
To pair with lock, you can get lock connection information by scanning QR code of lock. The content of QR code is encryted with BARCODE_KEY, you can decrypt the contet with the following example code:
```
/*
 * content: QR code content (base64 encoded string)
 * _barcodeKey: decryption key
 * /
val qrCodeContent =
    runCatching { lockQRCodeUseCase.parseQRCodeContent(_barcodeKey, content) }.getOrNull()
        ?: runCatching { lockQRCodeUseCase.parseWifiQRCodeContent(_barcodeKey, content) }.getOrNull()

if (qrCodeContent == null) {
    // parsing content failed
}
```
Before connecting to the lock, you can setup connection state observer:
```
private var _bleConnectionStateListener: Job? = null
// Setup BLE connection state observer
_bleConnectionStateListener?.cancel()
_bleConnectionStateListener = statefulConnection.connState
    .onEach { event ->
        when (event.status) {
            // something wrong
            EventState.ERROR -> {
                when (event.message) {
                    TimeoutException::class.java.simpleName -> {
                        // Connection timeout
                    }
                    else -> {
                        unless(
                            event.data != null
                        ) {
                            /*
                             * Possible errors:
                             *     BleDisconnectedException
                             *     IllegalTokenException
                             *     DeviceRefusedException
                             * /
                        }
                    }
                }
            }
            // RxBleConnection is ready.
            EventState.READY -> {
                // Setup incoming device status observer, please refer to IncomingDeviceStatusUseCase
            }
            // connected
            EventState.SUCCESS -> {
                // You should save permanent token after paring with lock.
                // You can get full lock connection information from statefulConnection.lockConnectionInfo and save it.
            }
            EventState.LOADING -> {}
            else -> {}
        }
    }
    .catch { Timber.e(it) }
    .flowOn(Dispatchers.Default)
    .launchIn(viewModelScope)
```
Connecting to lock
```
statefulConnection.establishConnection(
    macAddress = macAddress,     // get from QR code
    keyOne = keyOne,             // get from QR code
    oneTimeToken = oneTimeToken, // get from QR code
    permanentToken = null,
    isSilentlyFail = false
)
```
### Connecting to paired lock
To connect to paired lock, you should use the saved LockConnectionInfo to connect to lock.
```
statefulConnection.establishConnection(
    macAddress = _lockConnectionInfo!!.macAddress!!,
    keyOne = _lockConnectionInfo!!.keyOne!!,
    oneTimeToken = _lockConnectionInfo!!.oneTimeToken!!,
    permanentToken = _lockConnectionInfo!!.permanentToken,
    isSilentlyFail = false
)
```
## UseCases
### IncomingSunionBleNotificationUseCase
IncomingSunionBleNotificationUseCase collects device status notified by lock. You can setup the observer when lock connection is ready:
```
private var _bleDeviceStatusListener: Job? = null
// Setup incoming device status observer
_bleSunionBleNotificationListener?.cancel()
_bleSunionBleNotificationListener = IncomingSunionBleNotificationUseCase()
    .map { sunionBleNotification ->
        when (sunionBleNotification) {
            is DeviceStatus.DeviceStatusD6 -> {
            }
            is DeviceStatus.DeviceStatusA2 -> {
            }
            is Alert.AlertAF -> {
            }
            is Access.AccessA9 -> {             
            }
            else -> { 
                _currentDeviceStatus = DeviceStatus.UNKNOWN 
                _currentSunionBleNotification = SunionBleNotification.UNKNOWN
            }
        }
    }
    .catch { e -> Timber.e("Incoming SunionBleNotification exception $e") }
    .flowOn(Dispatchers.IO)
    .launchIn(viewModelScope)
```
### DeviceStatusD6UseCase
#### Query device status from lock
```
operator fun invoke(): Flow<DeviceStatus.DeviceStatusD6>
```
Example
```
deviceStatusD6UseCase()
    .map { deviceStatus ->
        updateStatus(deviceStatus)
    }
    .flowOn(Dispatchers.IO)
    .catch { Timber.e(it) }
    .launchIn(viewModelScope)
```
Exceptions

| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Set LockState
You should determine LockDirection before setting LockState. Please refer to LockDirectionUseCase.
```
setLockState(desiredState: Int): Flow<DeviceStatus.DeviceStatusD6>
```
Parameter


| Parameter | Type | Description |
| -------- | -------- | -------- |
| desiredState     | Int     | 0: Unlocked ,<BR> 1: Locked     |

Example
```
deviceStatusD6UseCase.setLockState(desiredState)
    .map { deviceStatus ->
        updateStatus(deviceStatus)
    }
    .flowOn(Dispatchers.IO)
    .catch { Timber.e(it) }
    .launchIn(viewModelScope)
```

Exceptions

| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| IllegalArgumentException     | Unknown desired lock state.     |

### DeviceStatusA2UseCase
#### Query device status from lock
```
operator fun invoke(): Flow<DeviceStatus.DeviceStatusA2>
```
Example
```
deviceStatusA2UseCase()
    .map { deviceStatus ->
        updateStatus(deviceStatus)
    }
    .flowOn(Dispatchers.IO)
    .catch { Timber.e(it) }
    .launchIn(viewModelScope)
```
Exceptions

| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Set LockState
```
setLockState(state: Int): Flow<DeviceStatus.DeviceStatusA2>
```
Parameter


| Parameter | Type | Description |
| -------- | -------- | -------- |
| state     | Int     | 0: Unlocked ,<BR> 1: Locked     |

Example
```
deviceStatusA2UseCase.setLockState(state)
    .map { deviceStatus ->
        updateStatus(deviceStatus)
    }
    .flowOn(Dispatchers.IO)
    .catch { Timber.e(it) }
    .launchIn(viewModelScope)
```

Exceptions

| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| IllegalArgumentException     | Unknown desired lock state.     |

#### Set SecurityBolt
```
setSecurityBolt(state: Int): Flow<DeviceStatus.DeviceStatusA2>
```
Parameter


| Parameter | Type | Description |
| -------- | -------- | -------- |
| state     | Int     | 0: NotProtrue ,<BR> 1: Protrude     |

Example
```
deviceStatusA2UseCase.setSecurityBolt(state)
    .map { deviceStatus ->
        updateStatus(deviceStatus)
    }
    .flowOn(Dispatchers.IO)
    .catch { Timber.e(it) }
    .launchIn(viewModelScope)
```

Exceptions

| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

### AdminCodeUseCase
#### Check if admin code has been set
```
isAdminCodeExists(): Flow<Boolean>
```
Example
```
adminCodeUseCase.isAdminCodeExists()
    .map { result ->
        // result = true when admin code exists
    }
    .flowOn(Dispatchers.IO)
    .catch { Timber.e(it) }
    .launchIn(viewModelScope)
```
Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |

#### Create admin code
```
createAdminCode(code: String): Flow<Boolean>
```
Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| code     | String     | The admin code you want to create.    |

Example
```
adminCodeUseCase.createAdminCode("0000")
    .map { result ->
        // result = true when succeed
    }
    .flowOn(Dispatchers.IO)
    .catch { Timber.e(it) }
    .launchIn(viewModelScope)
```

Exceptions
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| IllegalArgumentException     | Admin code must be 4-8 digits.     |

#### Update admin code
```
updateAdminCode(oldCode: String, newCode: String): Flow<Boolean>
```

Parameters
| Parameter | Type | Description |
| -------- | -------- | -------- |
| oldCode     | String     | The original admin code.    |
| newCode     | String     | The new admin code.    |

Example
```
adminCodeUseCase.updateAdminCode(oldCode, newCode)
    .map { result ->
        // result = true when succeed
    }
    .flowOn(Dispatchers.IO)
    .catch { Timber.e(it) }
    .launchIn(viewModelScope)
```

Exceptions
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| IllegalArgumentException     | Admin code must be 4-8 digits.     |

### LockDirectionUseCase
LockDirectionUseCase requests lock to determine LockDirection.
```
operator fun invoke(): Flow<DeviceStatus.DeviceStatus>
```

Example
```
lockDirectionUseCase()
    .map { deviceStatus ->
        when (deviceStatus) {
            is DeviceStatus.DeviceStatusD6 -> {
                updateStatus(deviceStatus)
            }
            is DeviceStatus.DeviceStatusA2 -> {
                if(deviceStatus.direction == BleV2Lock.Direction.NOT_SUPPORT.value) {
                    throw LockStatusException.LockFunctionNotSupportException()
                }
                updateStatus(deviceStatus)
            }
        }
    }
    .flowOn(Dispatchers.IO)
    .catch { Timber.e(it) }
    .launchIn(viewModelScope)
```

Exceptions
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException <BR>(for DeviceStatusA2)     | Lock function not support.|

### LockTimeUseCase
#### Get time of lock
```
getTime(): Flow<Int>
```

Example
```
lockTimeUseCase.getTime()
    .map { timeStamp ->
        // return unix time stamp
    }
    .flowOn(Dispatchers.IO)
    .catch { Timber.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |

#### Set time of lock
```
setTime(timeStamp: Long): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| timeStamp     | Long     | Unix time stamp    |

Example
```
// Set lock time to now
lockTimeUseCase.setTime(Instant.now().atZone(ZoneId.systemDefault()).toEpochSecond())
    .map { result ->
        // result = true when succeed
    }
    .flowOn(Dispatchers.IO)
    .catch { Timber.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |

#### Set timezone of lock
```
setTimeZone(timeZone: String): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| timezone     | String     | Time-zone ID, such as Asia/Taipei.    |

Example
```
// Set timezone to system default
lockTimeUseCase.setTimeZone(ZoneId.systemDefault().id)
    .map { result ->
        // result = true when succeed
    }
    .onStart { _uiState.update { it.copy(isLoading = true) } }
    .onCompletion { _uiState.update { it.copy(isLoading = false) } }
    .flowOn(Dispatchers.IO)
    .catch { e -> showLog("setLockTimeZone exception $e \n") }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |

### LockNameUsecase
#### Get lock name
```
getName(): Flow<String>
```

Example
```
lockNameUseCase.getName()
    .map { name ->
        // return lock name
    }
    .flowOn(Dispatchers.IO)
    .catch { Timber.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |

#### Set lock name
```
setName(name: String): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| name     | String     | Lock name    |

Example
```
lockNameUseCase.setName(name)
    .map { result ->
        // result = true when succeed
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| IllegalArgumentException     | Limit of lock name length is 20 bytes.     |

### LockConfigD4UseCase
#### Query lock config
Please refer to [LockConfigD4](###LockConfigD4)

```
query(): Flow<LockConfigD4>
```

Example
```
lockConfigD4UseCase.query()
    .map { lockConfig ->
        // return lock config
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |


#### Turn on/off key press beep
```
setKeyPressBeep(isOn: Boolean): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| isOn     | Boolean     | true<br>false    |

Example
```
lockConfigD4UseCase.setKeyPressBeep(isSoundOn)
    .map { result ->
        // return true when succeed
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Turn on/off vacation mode
```
setVactionMode(isOn: Boolean): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| isOn     | Boolean     | true<br>false    |

Example
```
lockConfigD4UseCase.setVactionMode(isVacationModeOn)
    .map { result ->
        // return true when succeed
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Turn on/off guiding code
```
setGuidingCode(isOn: Boolean): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| isOn     | Boolean     | true<br>false    |

Example
```
lockConfigD4UseCase.setGuidingCode(isGuidingCodeOn)
    .map { result ->
        // return true when succeed
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Turn on/off auto lock
```
setAutoLock(isOn: Boolean, autoLockTime: Int): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| isOn     | Boolean     | true<br>false    |
| autoLockTime     | Int     | Auto lock delay time, 1 for 10s.    |

Example
```
lockConfigD4UseCase.setAutoLock(isAutoLock, autoLockTime)
    .map { result ->
        // return true when succeed
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| IllegalArgumentException     | Auto lock time should be 1 ~ 90.     |

#### Set lock location
```
setLocation(latitude: Double, longitude: Double): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| latitude     | Double     | Latitude of lock location    |
| longitude    | Double     | Longitude of lock location   |

Example
```
lockConfigD4UseCase.setLocation(latitude = latitude, longitude = longitude)
    .map { result ->
        // return true when succeed
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

### LockConfigA0UseCase
#### Query lock config
Please refer to [LockConfigA0](###LockConfigA0)

```
query(): Flow<LockConfigA0>
```

Example
```
lockConfigA0UseCase.query()
    .map { lockConfig ->
        // return lock config
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Set lock location
```
setLocation(latitude: Double, longitude: Double): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| latitude     | Double     | Latitude of lock location    |
| longitude    | Double     | Longitude of lock location   |

Example
```
lockConfigA0UseCase.setLocation(latitude = latitude, longitude = longitude)
    .map { result ->
        // return true when succeed
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Turn on/off guiding code
```
setGuidingCode(isOn: Boolean): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| isOn     | Boolean     | true<br>false    |

Example
```
if (configA0.guidingCode != BleV2Lock.GuidingCode.NOT_SUPPORT.value) {
    val isGuidingCodeOn = configA0.guidingCode == BleV2Lock.GuidingCode.CLOSE.value
        lockConfigA0UseCase.setGuidingCode(isGuidingCodeOn)
        .map { result ->
            // return true when succeed
        }
        .flowOn(Dispatchers.IO)
        .catch { Timer.e(it) }
        .launchIn(viewModelScope)
} else {
    throw LockStatusException.LockFunctionNotSupportException()
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Turn on/off virtual code
```
setVirtualCode(isOn: Boolean): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| isOn     | Boolean     | true<br>false    |

Example
```
if (configA0.virtualCode != BleV2Lock.VirtualCode.NOT_SUPPORT.value) {
    val isVirtualCodeOn = configA0.virtualCode == BleV2Lock.VirtualCode.CLOSE.value
        lockConfigA0UseCase.setVirtualCode(isVirtualCodeOn)
        .map { result ->
            // return true when succeed
        }
        .flowOn(Dispatchers.IO)
        .catch { Timer.e(it) }
        .launchIn(viewModelScope)
} else {
    throw LockStatusException.LockFunctionNotSupportException()
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Turn on/off 2FA
```
setTwoFA(isOn: Boolean): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| isOn     | Boolean     | true<br>false    |

Example
```
if (configA0.twoFA != BleV2Lock.TwoFA.NOT_SUPPORT.value) {
    val isTwoFAOn = configA0.twoFA == BleV2Lock.TwoFA.CLOSE.value
        lockConfigA0UseCase.setTwoFA(isTwoFAOn)
        .map { result ->
            // return true when succeed
        }
        .flowOn(Dispatchers.IO)
        .catch { Timer.e(it) }
        .launchIn(viewModelScope)
} else {
    throw LockStatusException.LockFunctionNotSupportException()
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Turn on/off vacation mode
```
setVacationMode(isOn: Boolean): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| isOn     | Boolean     | true<br>false    |

Example
```
if (configA0.vacationMode != BleV2Lock.VacationMode.NOT_SUPPORT.value) {
    val isVacationModeOn = configA0.vacationMode == BleV2Lock.VacationMode.CLOSE.value
    lockConfigA0UseCase.setVacationMode(isVacationModeOn)
        .map { result ->
            // return true when succeed
        }
        .flowOn(Dispatchers.IO)
        .catch { Timer.e(it) }
        .launchIn(viewModelScope)
} else {
    throw LockStatusException.LockFunctionNotSupportException()
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Turn on/off auto lock
```
setAutoLock(isOn: Boolean, autoLockTime: Int): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| isOn     | Boolean     | true<br>false    |
| autoLockTime     | Int     | Auto lock delay time, between autoLockTimeUpperLimit and autoLockTimeLowerLimit. <br>0xFFFF: Not support |

Example
```
if (configA0.autoLock != BleV2Lock.AutoLock.NOT_SUPPORT.value) {
    val isAutoLock = configA0.autoLock == BleV2Lock.AutoLock.CLOSE.value
    if (autoLockTime < configA0.autoLockTimeUpperLimit || autoLockTime > configA0.autoLockTimeLowerLimit) {
        Timer.d("Set auto lock will fail because autoLockTime is not support value")
    }
    lockConfigA0UseCase.setAutoLock(isAutoLock, autoLockTime)
        .map { result ->
            // return true when succeed
        }
        .flowOn(Dispatchers.IO)
        .catch { Timer.e(it) }
        .launchIn(viewModelScope)
} else {
    throw LockStatusException.LockFunctionNotSupportException()
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| IllegalArgumentException     | Auto lock time illegal argument.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Turn on/off operating sound
```
setOperatingSound(isOn: Boolean): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| isOn     | Boolean     | true<br>false    |

Example
```
if (configA0.operatingSound != BleV2Lock.OperatingSound.NOT_SUPPORT.value) {
    val isOperatingSoundOn = configA0.operatingSound == BleV2Lock.OperatingSound.CLOSE.value
        lockConfigA0UseCase.setOperatingSound(isOperatingSoundOn)
        .map { result ->
            // return true when succeed
        }
        .flowOn(Dispatchers.IO)
        .catch { Timer.e(it) }
        .launchIn(viewModelScope)
} else {
    throw LockStatusException.LockFunctionNotSupportException()
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Turn on/off key press beep
```
setSoundValue(soundType: Int): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| soundType     | Int     | Please refer to [Sound](###Sound)    |


Example
```
if (configA0.soundType != BleV2Lock.SoundType.NOT_SUPPORT.value) {
    lockConfigA0UseCase.setSoundValue(configA0.soundType)
        .map { result ->
            // return true when succeed
        }
        .flowOn(Dispatchers.IO)
        .catch { Timer.e(it) }
        .launchIn(viewModelScope)
} else {
    throw LockStatusException.LockFunctionNotSupportException()
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Turn on/off show fast track mode
```
setShowFastTrackMode(isOn: Boolean): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| isOn     | Boolean     | true<br>false    |

Example
```
if (configA0.showFastTrackMode != BleV2Lock.ShowFastTrackMode.NOT_SUPPORT.value) {
    val isShowFastTrackModeOn = configA0.showFastTrackMode == BleV2Lock.ShowFastTrackMode.CLOSE.value
        lockConfigA0UseCase.setShowFastTrackMode(isShowFastTrackModeOn)
        .map { result ->
            // return true when succeed
        }
        .flowOn(Dispatchers.IO)
        .catch { Timer.e(it) }
        .launchIn(viewModelScope)
} else {
    throw LockStatusException.LockFunctionNotSupportException()
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

### LockUtilityUseCase
#### Factory reset
```
factoryReset(adminCode: String): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| adminCode     | String     | Admin code    |

Example
```
lockUtilityUseCase.factoryReset(adminCode)
    .map { result ->
        // result = true when succeed
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Get firmware version
```
getFirmwareVersion(): Flow<String>
```

Example
```
lockUtilityUseCase.getFirmwareVersion()
    .map { version ->
        // return version code
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |

#### Get lock supported unlock types
```
getLockSupportedUnlockTypes(): Flow<BleV2Lock.SupportedUnlockType>
```

Example
```
lockUtilityUseCase.getLockSupportedUnlockTypes()
    .map { result ->
        // supported unlock type
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

### LockTokenUseCase
#### Query token array
```
queryTokenArray(): Flow<List<Int>>
```

Example
```
lockTokenUseCase.queryTokenArray()
    .map { tokenArray ->
        // return useful token array index
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Query token
```
queryToken(index: Int): Flow<DeviceToken.PermanentToken>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index     | Int     | Query token at index in array    |

Example
```
lockTokenUseCase.queryToken(index)
    .map { deviceToken ->
        if(deviceToken.isPermanent){
            // return permanent token
        } else {
            // return one time token
        }
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Add one time token
```
addOneTimeToken(permission: String, name: String): Flow<AddUserResponse>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| permission | String     | "A": All , "L": Limit    |
| name       | String     | Token name    |

Example
```
lockTokenUseCase.addOneTimeToken(permission, name)
    .map { result ->
        // return add user response
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Edit token
```
editToken(index: Int, permission: String, name: String): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index      | Int        | Edit token at index in array     |
| permission | String     | "A": All , "L": Limit    |
| name       | String     | Token name    |

Example
```
lockTokenUseCase.editToken(index, permission, name)
    .map { result ->
        result = true when succeed
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Delete token
```
deleteToken(index: Int, code: String = ""): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index    | Int     | Delete token at index in array     |
| code     | String  | Only delete permanent token need admin code    |

Example
```
lockTokenUseCase.deleteToken(index, code)
    .map { result ->
        result = true when succeed
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

### LockAccessCodeUseCase
#### Get access code array
```
getAccessCodeArray(): Flow<List<Boolean>>
```

Example
```
lockAccessCodeUseCase.getAccessCodeArray()
    .map { accessCodeArray ->
        // return accessCodeArray
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Query access code
```
queryAccessCode(index: Int): Flow<AccessCode>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index     | Int     | Query access code at index in array     |

Example
```
lockAccessCodeUseCase.queryAccessCode(index)
    .map { accessCode ->
        // return accessCode info
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Add access code
```
addAccessCode(index: Int, isEnabled: Boolean, name: String, code: String, scheduleType: AccessScheduleType): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| --------  | -------- | -------- |
| index        | Int                     | Add index    |
| isEnabled    | Boolean                 | Access code enable    |
| name         | String                  | Access code name    |
| code         | String                  | Access code   |
| scheduleType | AccessScheduleType  | Please refer to [AccessScheduleType](###AccessScheduleType) |

Example
```
lockAccessCodeUseCase.addAccessCode(index, isEnabled, name, code, scheduleType)
    .map { result ->
        result = true when succeed
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Edit access code
```
editAccessCode(index: Int, isEnabled: Boolean, name: String, code: String, scheduleType: AccessScheduleType): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| --------  | -------- | -------- |
| index        | Int                     | Edit index    |
| isEnabled    | Boolean                 | Access code enable    |
| name         | String                  | Access code name    |
| code         | String                  | Access code   |
| scheduleType | AccessScheduleType  | Please refer to [AccessScheduleType](###AccessScheduleType)|

Example
```
lockAccessCodeUseCase.editAccessCode(index, isEnabled, name, code, scheduleType)
    .map { result ->
        result = true when succeed
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Delete access code
```
deleteAccessCode(index: Int): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index    | Int     | Delete access code at index in array    |

Example
```
lockAccessCodeUseCase.deleteAccessCode(index)
    .map { result ->
        result = true when succeed
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

### LockAccessUseCase
#### Get access code array
```
getAccessCodeArray(): Flow<List<Boolean>>
```

Example
```
if(lockSupportedTypes.accessCodeQuantity != BleV2Lock.AccessCodeQuantity.NOT_SUPPORT.value) {
    lockAccessUseCase.getAccessCodeArray()
        .map { accessCodeArray ->
            // return accessCodeArray
        }
        .flowOn(Dispatchers.IO)
        .catch { Timer.e(it) }
        .launchIn(viewModelScope)
} else {
    throw LockStatusException.LockFunctionNotSupportException()
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Query access code
```
queryAccessCode(index: Int): Flow<Access.AccessA6>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index     | Int     | Query access code at index in array     |

Example
```
lockAccessUseCase.queryAccessCode(index)
    .map { accessCode ->
        // return accessCode info
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Add access code
```
addAccessCode(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: String): Flow<Access.AccessA7>
```

Parameter
| Parameter | Type | Description |
| --------  | -------- | -------- |
| index        | Int                     | Add index    |
| isEnabled    | Boolean                 | Access code enable    |
| scheduleType | AccessScheduleType  | Please refer to [AccessScheduleType](###AccessScheduleType) |
| name         | String                  | Access code name    |
| code         | String                  | Access code   |

Example
```
if(lockSupportedTypes.accessCodeQuantity != BleV2Lock.AccessCodeQuantity.NOT_SUPPORT.value) {
    lockAccessUseCase.addAccessCode(index, isEnabled, scheduleType, name, code)
        .map { result ->
            result = Access.AccessA7
        }
        .flowOn(Dispatchers.IO)
        .catch { Timer.e(it) }
        .launchIn(viewModelScope)
} else {
    throw LockStatusException.LockFunctionNotSupportException()
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Edit access code
```
editAccessCode(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: String): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| --------  | -------- | -------- |
| index        | Int                     | Edit index    |
| isEnabled    | Boolean                 | Access code enable    |
| scheduleType | AccessScheduleType  | Please refer to [AccessScheduleType](###AccessScheduleType)|
| name         | String                  | Access code name    |
| code         | String                  | Access code   |

Example
```
if(lockSupportedTypes.accessCodeQuantity != BleV2Lock.AccessCodeQuantity.NOT_SUPPORT.value) {
    lockAccessUseCase.editAccessCode(index, isEnabled, scheduleType, name, code)
        .map { result ->
            result = true when succeed
        }
        .flowOn(Dispatchers.IO)
        .catch { Timer.e(it) }
        .launchIn(viewModelScope)
} else {
    throw LockStatusException.LockFunctionNotSupportException()
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Delete access code
```
deleteAccessCode(index: Int): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index    | Int     | Delete access code at index in array    |

Example
```
if(lockSupportedTypes.accessCodeQuantity != BleV2Lock.AccessCodeQuantity.NOT_SUPPORT.value) {
    lockAccessUseCase.deleteAccessCode(index)
        .map { result ->
            result = true when succeed
        }
        .flowOn(Dispatchers.IO)
        .catch { Timer.e(it) }
        .launchIn(viewModelScope)
} else {
    throw LockStatusException.LockFunctionNotSupportException()
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Get access card array
```
getAccessCardArray(): Flow<List<Boolean>>
```

Example
```
if(lockSupportedTypes.accessCodeQuantity != BleV2Lock.AccessCardQuantity.NOT_SUPPORT.value) {
    lockAccessUseCase.getAccessCardArray()
        .map { accessCardArray ->
            // return accessCardArray
        }
        .flowOn(Dispatchers.IO)
        .catch { Timer.e(it) }
        .launchIn(viewModelScope)
} else {
    throw LockStatusException.LockFunctionNotSupportException()
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Query access card
```
queryAccessCard(index: Int): Flow<Access.AccessA6>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index     | Int     | Query access card at index in array     |

Example
```
lockAccessUseCase.queryAccessCard(index)
    .map { accessCard ->
        // return accessCard info
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Add access card
```
addAccessCard(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: String): Flow<Access.AccessA7>
```

Parameter
| Parameter | Type | Description |
| --------  | -------- | -------- |
| index        | Int                     | Add index    |
| isEnabled    | Boolean                 | Access card enable    |
| scheduleType | AccessScheduleType  | Please refer to [AccessScheduleType](###AccessScheduleType) |
| name         | String                  | Access card name    |
| code         | String                  | Access card   |

Example
```
if(lockSupportedTypes.accessCardQuantity != BleV2Lock.AccessCardQuantity.NOT_SUPPORT.value) {
    lockAccessUseCase.addAccessCard(index, isEnabled, scheduleType, name, code)
        .map { result ->
            result = Access.AccessA7
        }
        .flowOn(Dispatchers.IO)
        .catch { Timer.e(it) }
        .launchIn(viewModelScope)
} else {
    throw LockStatusException.LockFunctionNotSupportException()
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Edit access card
```
editAccessCard(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: String): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| --------  | -------- | -------- |
| index        | Int                     | Edit index    |
| isEnabled    | Boolean                 | Access code enable    |
| scheduleType | AccessScheduleType  | Please refer to [AccessScheduleType](###AccessScheduleType)|
| name         | String                  | Access code name    |
| code         | String                  | Access code   |

Example
```
if(lockSupportedTypes.accessCardQuantity != BleV2Lock.AccessCardQuantity.NOT_SUPPORT.value) {
    lockAccessUseCase.editAccessCard(index, isEnabled, scheduleType, name, code)
        .map { result ->
            result = true when succeed
        }
        .flowOn(Dispatchers.IO)
        .catch { Timer.e(it) }
        .launchIn(viewModelScope)
} else {
    throw LockStatusException.LockFunctionNotSupportException()
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Delete access card
```
deleteAccessCode(index: Int): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index    | Int     | Delete access code at index in array    |

Example
```
if(lockSupportedTypes.accessCardQuantity != BleV2Lock.AccessCardQuantity.NOT_SUPPORT.value) {
    lockAccessUseCase.deleteAccessCard(index)
        .map { result ->
            result = true when succeed
        }
        .flowOn(Dispatchers.IO)
        .catch { Timer.e(it) }
        .launchIn(viewModelScope)
} else {
    throw LockStatusException.LockFunctionNotSupportException()
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Device get access card
```
deviceGetAccessCard(state:Int, index: Int): Flow<Access.AccessA9>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| state    | Int     | 0: Exit <br> 1: Start    |
| index    | Int     | Read access card code index    |

Example
```
if(lockSupportedTypes.accessCardQuantity != BleV2Lock.AccessCardQuantity.NOT_SUPPORT.value) {
    lockAccessUseCase.deviceGetAccessCard(state, index)
        .map { result ->
            result = Access.AccessA9
        }
        .flowOn(Dispatchers.IO)
        .catch { Timer.e(it) }
        .launchIn(viewModelScope)
} else {
    throw LockStatusException.LockFunctionNotSupportException()
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Get fingerprint array
```
getFingerprintArray(): Flow<List<Boolean>>
```

Example
```
if(lockSupportedTypes.fingerprintQuantity != BleV2Lock.FingerprintQuantity.NOT_SUPPORT.value) {
    lockAccessUseCase.getFingerprintArray()
        .map { fingerprintArray ->
            // return fingerprintArray
        }
        .flowOn(Dispatchers.IO)
        .catch { Timer.e(it) }
        .launchIn(viewModelScope)
} else {
    throw LockStatusException.LockFunctionNotSupportException()
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Query fingerprint
```
queryFingerprint(index: Int): Flow<Access.AccessA6>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index     | Int     | Query fingerprint at index in array     |

Example
```
lockAccessUseCase.queryFingerprint(index)
    .map { fingerprint ->
        // return fingerprint info
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Add fingerprint
```
addFingerprint(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: String): Flow<Access.AccessA7>
```

Parameter
| Parameter | Type | Description |
| --------  | -------- | -------- |
| index        | Int                     | Add index    |
| isEnabled    | Boolean                 | Fingerprint enable    |
| scheduleType | AccessScheduleType  | Please refer to [AccessScheduleType](###AccessScheduleType) |
| name         | String                  | Fingerprint name    |
| code         | String                  | Fingerprint   |

Example
```
if(lockSupportedTypes.fingerprintQuantity != BleV2Lock.FingerprintQuantity.NOT_SUPPORT.value) {
    lockAccessUseCase.addFingerprint(index, isEnabled, scheduleType, name, code)
        .map { result ->
            result = Access.AccessA7
        }
        .flowOn(Dispatchers.IO)
        .catch { Timer.e(it) }
        .launchIn(viewModelScope)
} else {
    throw LockStatusException.LockFunctionNotSupportException()
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Edit fingerprint
```
editFingerprint(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: String): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| --------  | -------- | -------- |
| index        | Int                     | Edit index    |
| isEnabled    | Boolean                 | Fingerprint enable    |
| scheduleType | AccessScheduleType  | Please refer to [AccessScheduleType](###AccessScheduleType)|
| name         | String                  | Fingerprint name    |
| code         | String                  | Fingerprint   |

Example
```
if(lockSupportedTypes.fingerprintQuantity != BleV2Lock.FingerprintQuantity.NOT_SUPPORT.value) {
    lockAccessUseCase.editFingerprint(index, isEnabled, scheduleType, name, code)
        .map { result ->
            result = true when succeed
        }
        .flowOn(Dispatchers.IO)
        .catch { Timer.e(it) }
        .launchIn(viewModelScope)
} else {
    throw LockStatusException.LockFunctionNotSupportException()
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Delete fingerprint
```
deleteFingerprint(index: Int): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index    | Int     | Delete fingerprint at index in array    |

Example
```
if(lockSupportedTypes.fingerprintQuantity != BleV2Lock.FingerprintQuantity.NOT_SUPPORT.value) {
    lockAccessUseCase.deleteFingerprint(index)
        .map { result ->
            result = true when succeed
        }
        .flowOn(Dispatchers.IO)
        .catch { Timer.e(it) }
        .launchIn(viewModelScope)
} else {
    throw LockStatusException.LockFunctionNotSupportException()
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Device get fingerprint
```
deviceGetFingerprint(state:Int, index: Int): Flow<Access.AccessA9>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| state    | Int     | 0: Exit <br> 1: Start    |
| index    | Int     | Read fingerprint code index    |

Example
```
if(lockSupportedTypes.fingerprintQuantity != BleV2Lock.FingerprintQuantity.NOT_SUPPORT.value) {
    lockAccessUseCase.deviceGetFingerprint(state, index)
        .map { result ->
            result = Access.AccessA9
        }
        .flowOn(Dispatchers.IO)
        .catch { Timer.e(it) }
        .launchIn(viewModelScope)
} else {
    throw LockStatusException.LockFunctionNotSupportException()
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Get face array
```
getFaceArray(): Flow<List<Boolean>>
```

Example
```
if(lockSupportedTypes.faceQuantity != BleV2Lock.FaceQuantity.NOT_SUPPORT.value) {
    lockAccessUseCase.getFaceArray()
        .map { faceArray ->
            // return faceArray
        }
        .flowOn(Dispatchers.IO)
        .catch { Timer.e(it) }
        .launchIn(viewModelScope)
} else {
    throw LockStatusException.LockFunctionNotSupportException()
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Query face
```
queryFace(index: Int): Flow<Access.AccessA6>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index     | Int     | Query face at index in array     |

Example
```
lockAccessUseCase.queryFace(index)
    .map { face ->
        // return face info
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Add face
```
addFace(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: String): Flow<Access.AccessA7>
```

Parameter
| Parameter | Type | Description |
| --------  | -------- | -------- |
| index        | Int                     | Add index    |
| isEnabled    | Boolean                 | Face enable    |
| scheduleType | AccessScheduleType  | Please refer to [AccessScheduleType](###AccessScheduleType) |
| name         | String                  | Face name    |
| code         | String                  | Face   |

Example
```
if(lockSupportedTypes.faceQuantity != BleV2Lock.FaceQuantity.NOT_SUPPORT.value) {
    lockAccessUseCase.addFace(index, isEnabled, scheduleType, name, code)
        .map { result ->
            result = Access.AccessA7
        }
        .flowOn(Dispatchers.IO)
        .catch { Timer.e(it) }
        .launchIn(viewModelScope)
} else {
    throw LockStatusException.LockFunctionNotSupportException()
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Edit face
```
editFace(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: String): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| --------  | -------- | -------- |
| index        | Int                     | Edit index    |
| isEnabled    | Boolean                 | Face enable    |
| scheduleType | AccessScheduleType  | Please refer to [AccessScheduleType](###AccessScheduleType)|
| name         | String                  | Face name    |
| code         | String                  | Face   |

Example
```
if(lockSupportedTypes.faceQuantity != BleV2Lock.FaceQuantity.NOT_SUPPORT.value) {
    lockAccessUseCase.editFace(index, isEnabled, scheduleType, name, code)
        .map { result ->
            result = true when succeed
        }
        .flowOn(Dispatchers.IO)
        .catch { Timer.e(it) }
        .launchIn(viewModelScope)
} else {
    throw LockStatusException.LockFunctionNotSupportException()
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Delete face
```
deleteFace(index: Int): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index    | Int     | Delete face at index in array    |

Example
```
if(lockSupportedTypes.face != BleV2Lock.Face.NOT_SUPPORT.value) {
    lockAccessUseCase.deleteFace(index)
        .map { result ->
            result = true when succeed
        }
        .flowOn(Dispatchers.IO)
        .catch { Timer.e(it) }
        .launchIn(viewModelScope)
} else {
    throw LockStatusException.LockFunctionNotSupportException()
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Device get face
```
deviceGetFace(state:Int, index: Int): Flow<Access.AccessA9>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| state    | Int     | 0: Exit <br> 1: Start    |
| index    | Int     | Read face code index    |

Example
```
if(lockSupportedTypes.face != BleV2Lock.Face.NOT_SUPPORT.value) {
    lockAccessUseCase.deviceGetFace(state, index)
        .map { result ->
            result = Access.AccessA9
        }
        .flowOn(Dispatchers.IO)
        .catch { Timer.e(it) }
        .launchIn(viewModelScope)
} else {
    throw LockStatusException.LockFunctionNotSupportException()
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

### LockEventLogUseCase
#### Get event quantity
```
getEventQuantity(): Flow<Int>
```

Example
```
lockEventLogUseCase.getEventQuantity()
    .map { result ->
        // return event quantity
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Get event
```
getEvent(index: Int): Flow<EventLog>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index     | Int     | Get event log at index in array |

Example
```
lockEventLogUseCase.getEvent(index)
    .map{ eventLog ->
        // return eventLog
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Delete Event
```
deleteEvent(index: Int): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index    | Int     | Delete event log at index in array |

Example
```
lockEventLogUseCase.deleteEvent(index)
    .map { result ->
        result = true when succeed
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

## Models
### QRCodeContent

| Name | Type | Description |
| -------- | -------- | -------- |
|t|String|Default connection token|
|k|String|Encryption key of data transmission|
|a|String|MAC address of lock|
|m|String|Model name of lock|
|s|String|Optional value, serial number of lock|
|f|String|Optional value, shared from username|
|l|String|Optional avlue, display name of lock|

### LockConnectionInfo

| Name | Type | Description |
| -------- | -------- | -------- |
| macAddress     | String     | BT mac address of lock. You can get it from QR code.     |
| oneTimeToken     | String     | Default connection token of lock. You can get it from QR code.     |
| permanentToken     | String     | Excanged connection token when pairing with lock. You should save it for later use.    |
| keyOne     | String     | Encryption key of data transmission. You can get it from QR code.     |
| keyTwo     | String     | Random-generated encryption key of data transmission.     |
| permission     | String     | Permission of connection token.     |

### DeviceStatusD6
| Name | Type | Description |
| -------- | -------- | -------- |
|config|LockConfigD4|Please refer to [LockConfigD4](###LockConfigD4)|
|lockState|Int|0: Unlocked<br>1: Locked|
|battery|Int|Percentage of battery power|
|batteryState|Int|0: Battery good<br>1: Battery low<br>2: Battery alert|
|timestamp|Long|Time of lock|

### DeviceStatusA2
| Name | Type | Description |
| -------- | -------- | -------- |
|direction|Int|0xA0: Right<br>0xA1: Left<br>0xA2: Unknown<br>0xFF: Not support|
|vacationMode|Int|0: Close<br>1: Open<br>0xFF: Not support|
|deadBolt|Int|0: Not protrude<br>1: protrude<br>0xFF: Not support|
|doorState|Int|0: Open<br>1: Close<br>0xFF: Not support|
|lockState|Int|0: Unlocked<br>1: Locked<br>2: Unknown|
|securityBolt|Int|0: Not protrude<br>1: protrude<br>0xFF: Not support|
|battery|Int|Percentage of battery power|
|batteryState|Int|0: Normal<br>1: Weak current<br>2: Dangerous|

### LockConfigD4
| Name | Type | Value |
| -------- | -------- | -------- |
|direction|LockDirection|Right<br>Left<br>NotDetermined|
|isSoundOn|Boolean|true<br>false|
|isVacationModeOn|Boolean|true<br>false|
|isAutoLock|Boolean|true<br>false|
|autoLockTime|Int|1~90 (1 for 10 seconds)|
|isPreamble|Boolean|true<br>false|
|latitude|Double|Latitude of lock location|
|longitude|Double|Longitude of lock location|

### LockConfigA0
| Name | Type | Value |
| -------- | -------- | -------- |
|latitude|Double|Latitude of lock location|
|longitude|Double|Longitude of lock location|
|direction|Int|0xA0: Right<br>0xA1: Left<br>0xA2: Unknown<br>0xFF: Not support|
|guidingCode|Int|0: Close<br>1: Open<br>0xFF: Not support|
|virtualCode|Int|0: Close<br>1: Open<br>0xFF: Not support|
|twoFA|Int|0: Close<br>1: Open<br>0xFF: Not support|
|vacationMode|Int|0: Close<br>1: Open<br>0xFF: Not support|
|autoLock|Int|0: Close<br>1: Open<br>0xFF: Not support|
|autoLockTime|Int|Auto lock delay time, between autoLockTimeUpperLimit and autoLockTimeLowerLimit. <br>0xFFFF: Not support|
|autoLockTimeUpperLimit|Int|Automatic lock upper limit time. <br>0xFFFF: Not support|
|autoLockTimeLowerLimit|Int|Automatic lock lower limit time. <br>0xFFFF: Not support|
|operatingSound|Int|0: Close<br>1: Open<br>0xFF: Not support|
|soundType|Int|Please refer to [Sound](###Sound)|
|soundValue|Int|Please refer to [Sound](###Sound)|
|showFastTrackMode|Int|0: Close<br>1: Open<br>0xFF: Not support|

### Sound

Lock only supports one of the types

|  Type  | soundType | soundValue                   |
|:------ |:-------:|:------------------------- |
| on/off |  0x01   | 0: Close <br>100: Open          |
| level  |  0x02   | 0: Close <br>50: Low voice <br>100: High voice|
| percentage |  0x03   | 0 ~ 100                   |
| not support |  0xFF   | 0xFF                   |

### PermanentToken
| Name | Type | Value |
| -------- | -------- | -------- |
|isValid|Boolean|true<br>false|
|isPermanent|Boolean|true<br>false|
|token|String|Token|
|isOwner|Boolean|true<br>false|
|name|String|Token name|
|permission|String|A: All , L: Limit |

### OneTimeToken
| Name | Type | Value |
| -------- | -------- | -------- |
|token|String|Token|

### AddUserResponse
| Name | Type | Value |
| -------- | -------- | -------- |
|isSuccessful|Boolean|true<br>false|
|tokenIndexInDevice|Int|Token index |
|token|String|Token|
|content|String|Default empty string|
|lockName|String|Default empty string|

### SupportedUnlockType
| Name | Type | Value |
| -------- | -------- | -------- |
|accessCodeQuantity|Int|Access code quantity<br>0xFFFF: Not support|
|accessCardQuantity|Int|Access card quantity<br>0xFFFF: Not support |
|fingerprintQuantity|Int|Finger print quantity<br>0xFFFF: Not support|
|faceQuantity|Int|Face quantity<br>0xFFFF: Not support|

### AccessCode
| Name | Type | Value |
| -------- | -------- | -------- |
|index|Int|AccessCode index |
|isEnable|Boolean|true<br>false|
|code|String|AccessCode|
|scheduleType|String|Please refer to [AccessScheduleType](###AccessScheduleType)|
|weekDays|Int|Default null|
|from|Int|Default null|
|to|Int|Default null|
|scheduleFrom|Int|Default null|
|scheduleTo|Int|Default null|
|name|String|AccessCode name|

### AccessA6
| Name | Type | Value |
| -------- | -------- | -------- |
|type|Int|0: Access code <br> 1: Access card <br> 2: Fingerprint <br> 3: Face |
|index|Int|Access index |
|isEnable|Boolean|true<br>false|
|code|String|Access Code|
|scheduleType|String|Please refer to [AccessScheduleType](###AccessScheduleType)|
|weekDays|Int|Default null|
|from|Int|Default null|
|to|Int|Default null|
|scheduleFrom|Int|Default null|
|scheduleTo|Int|Default null|
|name|String|Access name|
|nameLen|Int|Access name length|

### AccessA7
| Name | Type | Value |
| -------- | -------- | -------- |
|type|Int|0: Access code <br> 1: Access card <br> 2: Fingerprint <br> 3: Face |
|index|Int|Access index |
|isSuccess|Boolean|true<br>false|

### AccessA9
| Name | Type | Value |
| -------- | -------- | -------- |
|type|Int|1: Access card <br> 2: Fingerprint <br> 3: Face |
|state|Int|0: Exit <br> 1: Start <br> 2: Update|
|index|Int|Read data with a empty access index |
|status|Boolean|true<br>false|
|data|String|Access Card: Card number <br> Fingerprint/Face: Percentage |

### AccessScheduleType
| Name | Description |
| -------- | -------- |
|All|Permanent|
|None|None|
|SingleEntry|Single Entry|
|ValidTimeRange (from: Long, to: Long)|Valid Time Range|
|ScheduleEntry (weekdayBits: Int, from: Int, to: Int)|ScheduleEntry|

### AlertAF
| Name | Type | Description |
| -------- | -------- | -------- |
|alertType|Int|0: Error access code<br>1: Current access code at wrong time<br>2: Current access code but at vacation mode<br>3: Actively press the clear key<br>20: Many error key locked<br>40: Lock break alert|

### EventLog
| Name | Type | Value |
| -------- | -------- | -------- |
|eventTimeStamp|Long|The time when the event log occurred|
|event|Int|0: Auto lock close success<br>1: Auto lock close fail<br>2: App lock close success<br>3: App lock close fail<br>4: Physical button close success<br>5: Physical button close fail<br>6: Close the door manually success<br>7: Close the door manually fail<br>8: App lock open success<br>9: App lock open fail<br>10: Physical button open success<br>11: Physical button open fail<br>12: Open the door manually success<br>13: Open the door manually fail<br>64: Add token<br>65: Edit token<br>66: Delete token<br>80: Add access code<br>81: Edit access code<br>82: Delete access code<br>128: Wrong password<br>129: Connection error<br>130: At wrong time enter correct password<br>131: At vacation mode enter not admin password|
|name|String|Event log name <br>With 64~66 & 80~82 is named operator &#124; operated|
