# Sunion BLE communication SDK fro Android
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
dependencies {
    ...
    implementation project(':core_ble_android')
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
### IncomingDeviceStatusUseCase
IncomingDeviceStatusUseCase collects device status notified by lock. You can setup the observer when lock connection is ready:
```
private var _bleDeviceStatusListener: Job? = null
// Setup incoming device status observer
_bleDeviceStatusListener?.cancel()
_bleDeviceStatusListener = incomingDeviceStatusUseCase()
    .map { deviceStatus ->
        when (deviceStatus) {
            is DeviceStatus.DeviceStatusD6 -> {
            }
            else -> { _currentDeviceStatus = DeviceStatus.UNKNOWN }
        }
    }
    .catch { e -> Timber.e("Incoming DeviceStatusD6 exception $e") }
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
| desiredState     | Int     | 0: Locked ,<BR> 1: Unlocked     |

Example
```
deviceStatusD6UseCase.setLockState(1)
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
operator fun invoke(): Flow<DeviceStatus.DeviceStatusD6>
```

Example
```
lockDirectionUseCase()
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
| index     | Int     | Query token array index    |

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
| permission | String     | A: All , L: Limit    |
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
| index      | Int        | Edit token array index    |
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
| index    | Int     | Delete token array index    |
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
| index     | Int     | Query access code array index    |

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
addAccessCode(index: Int, isEnabled: Boolean, name: String, code: String, scheduleType: AccessCodeScheduleType): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| --------  | -------- | -------- |
| index        | Int                     | Add index    |
| isEnabled    | Boolean                 | Access code enable    |
| name         | String                  | Access code name    |
| code         | String                  | Access code   |
| scheduleType | AccessCodeScheduleType  | All: Permanent <br> None: None <br> SingleEntry: Single entry <br> ScheduleEntry: Scheduled entry <br> ValidTimeRange: Valid time range |

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
editAccessCode(index: Int, isEnabled: Boolean, name: String, code: String, scheduleType: AccessCodeScheduleType): Flow<Boolean>
```

Parameter
| Parameter | Type | Description |
| --------  | -------- | -------- |
| index        | Int                     | Add index    |
| isEnabled    | Boolean                 | Access code enable    |
| name         | String                  | Access code name    |
| code         | String                  | Access code   |
| scheduleType | AccessCodeScheduleType  | All: Permanent <br> None: None <br> SingleEntry: Single entry <br> ScheduleEntry: Scheduled entry <br> ValidTimeRange: Valid time range |

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
| index    | Int     | Delete access code array index    |

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
|lockState|Int|0: Locked<br>1: Unlocked|
|battery|Int|Percentage of battery power|
|batteryState|Int|0: Battery good<br>1: Battery low<br>2: Battery alert|
|timestamp|Long|Time of lock|

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

