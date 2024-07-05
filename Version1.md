## Version 1 quick start
### Pairing with lock
To pair with lock, you can get lock connection information by scanning QR code of lock. The content of QR code is encryted with BARCODE_KEY, you can decrypt the contet with the following example code:
```kotlin=
// content: QR code content (base64 encoded string)
// _barcodeKey: decryption key
val qrCodeContent =
    runCatching { lockQRCodeUseCase.parseQRCodeContent(_barcodeKey, content) }.getOrNull()
        ?: runCatching { lockQRCodeUseCase.parseWifiQRCodeContent(_barcodeKey, content) }.getOrNull()

if (qrCodeContent == null) {
    // parsing content failed
}
```
Before connecting to the lock, you can setup connection state observer:
```kotlin=
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
                             // Possible errors:
                             //     BleDisconnectedException
                             //     IllegalTokenException
                             //     DeviceRefusedException
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
```kotlin=
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
```kotlin=
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
```kotlin=
private var _bleDeviceStatusListener: Job? = null
// Setup incoming device status observer
_bleSunionBleNotificationListener?.cancel()
_bleSunionBleNotificationListener = IncomingSunionBleNotificationUseCase()
    .map { sunionBleNotification ->
        when (sunionBleNotification) {
            is DeviceStatus -> {
            }
            is Alert -> {
            }
            is Access -> {            
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
```kotlin=
suspend operator fun invoke(): DeviceStatus.D6
```
Example
```kotlin=
flow { emit(deviceStatusD6UseCase()) }
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
```kotlin=
suspend fun setLockState(desiredState: Int): DeviceStatus.D6
```
Parameter


| Parameter | Type | Description |
| -------- | -------- | -------- |
| desiredState     | Int     | 0: Unlocked ,<BR> 1: Locked     |

Example
```kotlin=
flow { emit(deviceStatusD6UseCase.setLockState(desiredState)) }
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
```kotlin=
suspend fun isAdminCodeExists(): Boolean
```
Example
```kotlin=
flow { emit(adminCodeUseCase.isAdminCodeExists()) }
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
```kotlin=
suspend fun createAdminCode(code: String): Boolean
```
Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| code     | String     | The admin code you want to create.    |

Example
```kotlin=
flow { emit(adminCodeUseCase.createAdminCode(code)) }
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
```kotlin=
suspend fun updateAdminCode(oldCode: String, newCode: String):Boolean
```

Parameters
| Parameter | Type | Description |
| -------- | -------- | -------- |
| oldCode     | String     | The original admin code.    |
| newCode     | String     | The new admin code.    |

Example
```kotlin=
 flow { emit(adminCodeUseCase.updateAdminCode(oldCode, newCode)) }
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
```kotlin=
suspend operator fun invoke(): DeviceStatus
```

Example
```kotlin=
flow { emit(lockDirectionUseCase()) }
    .map { deviceStatus ->
        when (deviceStatus) {
            is DeviceStatus.D6 -> {
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
```kotlin=
suspend fun getTime(): Int
```

Example
```kotlin=
flow { emit(lockTimeUseCase.getTime()) }
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
```kotlin=
suspend fun setTime(timeStamp: Long): Boolean
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| timeStamp     | Long     | Unix time stamp    |

Example
```kotlin=
// Set lock time to now
flow { emit(lockTimeUseCase.setTime(Instant.now().atZone(ZoneId.systemDefault()).toEpochSecond())) }
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
```kotlin=
suspend fun setTimeZone(timezone: String): Boolean
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| timezone     | String     | Time-zone ID, such as Asia/Taipei.    |

Example
```kotlin=
// Set timezone to system default
flow { emit(lockTimeUseCase.setTimeZone(ZoneId.systemDefault().id)) }
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
```kotlin=
suspend fun getName():String
```

Example
```kotlin=
flow { emit(lockNameUseCase.getName()) }
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
```kotlin=
suspend fun setName(name: String): Boolean
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| name     | String     | Lock name    |

Example
```kotlin=
flow { emit(lockNameUseCase.setName(name)) }
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
#### Get lock config
Please refer to [LockConfigD4](###LockConfigD4)

```kotlin=
suspend fun get(): LockConfig.D4
```

Example
```kotlin=
flow { emit(lockConfigD4UseCase.get()) }
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
```kotlin=
suspend fun setKeyPressBeep(isOn: Boolean): Boolean
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| isOn     | Boolean     | true<br>false    |

Example
```kotlin=
flow { emit(lockConfigD4UseCase.setKeyPressBeep(isSoundOn)) }
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
```kotlin=
suspend fun setVacationMode(isOn: Boolean): Boolean
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| isOn     | Boolean     | true<br>false    |

Example
```kotlin=
flow { emit(lockConfigD4UseCase.setVacationMode(isVacationModeOn)) }
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
```kotlin=
suspend fun setGuidingCode(isOn: Boolean):Boolean
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| isOn     | Boolean     | true<br>false    |

Example
```kotlin=
flow { emit(lockConfigD4UseCase.setGuidingCode(isGuidingCodeOn)) }
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
```kotlin=
suspend fun setAutoLock(isOn: Boolean, autoLockTime: Int): Boolean
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| isOn     | Boolean     | true<br>false    |
| autoLockTime     | Int     | Auto lock delay time, 1 for 10s.    |

Example
```kotlin=
flow { emit(lockConfigD4UseCase.setAutoLock(isAutoLock, autoLockTime)) }
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
```kotlin=
suspend fun setLocation(latitude: Double, longitude: Double): Boolean
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| latitude     | Double     | Latitude of lock location    |
| longitude    | Double     | Longitude of lock location   |

Example
```kotlin=
flow { emit(lockConfigD4UseCase.setLocation(latitude = latitude, longitude = longitude)) }
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
```kotlin=
suspend fun factoryReset(adminCode: String): Boolean
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| adminCode     | String     | Admin code    |

Example
```kotlin=
flow { emit(lockUtilityUseCase.factoryReset(adminCode)) }
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
```kotlin=
suspend fun getFirmwareVersion(): String
```

Example
```kotlin=
flow { emit(lockUtilityUseCase.getFirmwareVersion()) }
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
```kotlin=
suspend fun getLockSupportedUnlockTypes(): BleV2Lock.SupportedUnlockType
```

Example
```kotlin=
flow { emit(lockUtilityUseCase.getLockSupportedUnlockTypes()) }
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
```kotlin=
suspend fun getTokenArray(): List<Int>
```

Example
```kotlin=
flow { emit(lockTokenUseCase.getTokenArray()) }
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
```kotlin=
suspend fun getToken(index: Int): DeviceToken.PermanentToken
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index     | Int     | Get token at index in array    |

Example
```kotlin=
flow { emit(lockTokenUseCase.getToken(index)) }
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
```kotlin=
suspend fun addOneTimeToken(permission: String, name: String): AddUserResponse
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| permission | String     | "A": All , "L": Limit    |
| name       | String     | Token name    |

Example
```kotlin=
flow { emit(lockTokenUseCase.addOneTimeToken(permission,name)) }
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
```kotlin=
suspend fun editToken(index: Int, permission: String, name: String): Boolean
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index      | Int        | Edit token at index in array     |
| permission | String     | "A": All , "L": Limit    |
| name       | String     | Token name    |

Example
```kotlin=
flow { emit(lockTokenUseCase.editToken(index, permission, name)) }
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
```kotlin=
suspend fun deleteToken(index: Int, code: String = ""): Boolean
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index    | Int     | Delete token at index in array     |
| code     | String  | Only delete permanent token need admin code    |

Example
```kotlin=
flow { emit(lockTokenUseCase.deleteToken(index, code)) }
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
```kotlin=
suspend fun getAccessCodeArray(): List<Boolean>
```

Example
```kotlin=
flow { emit(lockAccessCodeUseCase.getAccessCodeArray()) }
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
```kotlin=
suspend fun getAccessCode(index: Int): Access.Code
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index     | Int     | Get access code at index in array     |

Example
```kotlin=
flow { emit(lockAccessCodeUseCase.getAccessCode(index)) }
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
```kotlin=
suspend fun addAccessCode(index: Int, isEnabled: Boolean, name: String, code: String, scheduleType: AccessScheduleType): Boolean
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
```kotlin=
flow { emit(lockAccessCodeUseCase.addAccessCode(index, isEnabled, name, code, scheduleType)) }
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
```kotlin=
suspend fun editAccessCode(index: Int, isEnabled: Boolean, name: String, code: String, scheduleType: AccessScheduleType): Boolean {
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
```kotlin=
flow { emit(lockAccessCodeUseCase.editAccessCode(index, isEnabled, name, code, scheduleType)) }
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
```kotlin=
suspend fun deleteAccessCode(index: Int): Boolean
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index    | Int     | Delete access code at index in array    |

Example
```kotlin=
flow { emit(lockAccessCodeUseCase.deleteAccessCode(index)) }
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
```kotlin=
suspend fun getEventQuantity(): Int
```

Example
```kotlin=
flow { emit(lockEventLogUseCase.getEventQuantity()) }
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
```kotlin=
suspend fun getEvent(index: Int): EventLog
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index     | Int     | Get event log at index in array |

Example
```kotlin=
flow { emit(lockEventLogUseCase.getEvent(index)) }
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
```kotlin=
suspend fun deleteEvent(index: Int): Boolean
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index    | Int     | Delete event log at index in array |

Example
```kotlin=
flow { emit(lockEventLogUseCase.deleteEvent(index)) }
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
|config|LockConfig.D4|Please refer to [LockConfigD4](###LockConfigD4)|
|lockState|Int|0: Unlocked<br>1: Locked|
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

### AccessScheduleType
| Name | Description |
| -------- | -------- |
|All|Permanent|
|None|None|
|SingleEntry|Single Entry|
|ValidTimeRange (from: Long, to: Long)|Valid Time Range|
|ScheduleEntry (weekdayBits: Int, from: Int, to: Int)|ScheduleEntry|

### EventLog
| Name | Type | Value |
| -------- | -------- | -------- |
|eventTimeStamp|Long|The time when the event log occurred|
|event|Int|0: Auto lock close success<br>1: Auto lock close fail<br>2: App lock close success<br>3: App lock close fail<br>4: Physical button close success<br>5: Physical button close fail<br>6: Close the door manually success<br>7: Close the door manually fail<br>8: App lock open success<br>9: App lock open fail<br>10: Physical button open success<br>11: Physical button open fail<br>12: Open the door manually success<br>13: Open the door manually fail<br>64: Add token<br>65: Edit token<br>66: Delete token<br>80: Add access code<br>81: Edit access code<br>82: Delete access code<br>128: Wrong password<br>129: Connection error<br>130: At wrong time enter correct password<br>131: At vacation mode enter not admin password|
|name|String|Event log name <br>With 64~66 & 80~91 is named operator &#124; operated|
