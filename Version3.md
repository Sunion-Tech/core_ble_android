## Version 3 quick start
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

Get device connect info from server (Ble 3.0)
```
curl -X POST "https://apii.ikey-lock.com/v1/production/get" -H "x-api-key: your-api-key" -H "Content-Type: application/json" -d '{"code":"string-from-qrcode"}'
```
| Parameter | Type | Description |
| -------- | -------- | -------- |
|x-api-key| String | api-key
|code| String| string-from-response 

Ble 3.0 lock before connect need to scan device (Ble 3.0)
```kotlin=
var disposable: Disposable? = null
var currentConnectMacAddress: String? = null
fun startBleScan(uuid: String, productionGetResponse: ProductionGetResponse, isReconnect: Boolean = false) {
        disposable?.dispose()
        disposable = bleScanUseCase.scanUuid(uuid = uuid)
            .timeout(30, TimeUnit.SECONDS)
            .take(1)
            .subscribe(
                { scanResult ->
                    // 處理掃描到的 BLE 裝置
                    currentConnectMacAddress = scanResult.bleDevice.macAddress
                },
                { throwable ->
                    // 處理錯誤
                    Timber.e("Scan error: $throwable")
                },
                {
                    // 處理掃描完成事件
                    if(productionGetResponse.address.isNullOrBlank() && currentConnectMacAddress.isNullOrBlank()){
                        Timber.d("Production api or scan ble to get mac address failed.")
                    }
                    if(isReconnect){
                        connect()
                    } else {
                        _lockConnectionInfo = LockConnectionInfo.from(productionGetResponse, currentConnectMacAddress)
                        Timber.d("lockConnectionInfo: $lockConnectionInfo")
                    }
                }
            )
    }
```

Connecting to lock
```kotlin=
statefulConnection.establishConnection(
    macAddress = macAddress,     // get from sacn device
    keyOne = keyOne,             // get from production get resp
    oneTimeToken = oneTimeToken, // get from production get resp
    permanentToken = null,
    model = model, // get from production get resp
    isSilentlyFail = false
)
```

### Connecting to paired lock
To connect to paired lock, you should use the saved LockConnectionInfo to connect to lock.
```kotlin=
statefulConnection.establishConnection(
    macAddress = lockConnectionInfo!!.macAddress!!,
    keyOne = lockConnectionInfo!!.keyOne!!,
    oneTimeToken = lockConnectionInfo!!.oneTimeToken!!,
    permanentToken = lockConnectionInfo!!.permanentToken!!,
    model = lockConnectionInfo!!.model,
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

### DeviceStatus82UseCase
#### Query device status from lock
```kotlin=
suspend operator fun invoke(): DeviceStatus.EightTwo
```
Example
```kotlin=
flow { emit(deviceStatus82UseCase()) }
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
```kotlin=
suspend fun setLockState(state: Int): DeviceStatus.EightTwo
```
Parameter


| Parameter | Type | Description |
| -------- | -------- | -------- |
| state     | Int     | 0: Unlocked ,<BR> 1: Locked     |

Example
```kotlin=
flow { emit(deviceStatus82UseCase.setLockState(state)) }
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
```kotlin=
suspend fun setSecurityBolt(state: Int): DeviceStatus.EightTwo
```
Parameter


| Parameter | Type | Description |
| -------- | -------- | -------- |
| state     | Int     | 0: NotProtrue ,<BR> 1: Protrude     |

Example
```kotlin=
flow { emit(deviceStatus82UseCase.setSecurityBolt(state)) }
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
            is DeviceStatus.EightTwo -> {
                if(deviceStatus.direction.isNotSupport()) {
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

### LockConfig80UseCase
#### Get lock config
Please refer to [LockConfig80](###LockConfig80)

```kotlin=
suspend fun get(): LockConfig.Eighty
```

Example
```kotlin=
flow { emit(lockConfig80UseCase.get()) }
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
flow { emit(lockConfig80UseCase.setLocation(latitude = latitude, longitude = longitude)) }
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
suspend fun setGuidingCode(isOn: Boolean): Boolean
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| isOn     | Boolean     | true<br>false    |

Example
```kotlin=
flow { emit(lockConfig80UseCase.query()) }
    .catch { Timer.e(it) }
    .map { lockConfig ->
        val isGuidingCodeOn = lockConfig.guidingCode == BleV3Lock.GuidingCode.CLOSE.value
        val result = lockConfig80UseCase.setGuidingCode(isGuidingCodeOn)
        result
    }
    .catch { Timer.e(it) }
    .flowOn(Dispatchers.IO)
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Turn on/off virtual code
```kotlin=
suspend fun setVirtualCode(isOn: Boolean): Boolean
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| isOn     | Boolean     | true<br>false    |

Example
```kotlin=
flow { emit(lockConfig80UseCase.query()) }
    .catch { Timer.e(it) }
    .map { lockConfig ->
        val isVirtualCodeOn = lockConfig.virtualCode == BleV3Lock.VirtualCode.CLOSE.value
        val result = lockConfig80UseCase.setVirtualCode(isVirtualCodeOn)
        result
    }
    .catch { Timer.e(it) }
    .flowOn(Dispatchers.IO)
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Turn on/off 2FA
```kotlin=
suspend fun setTwoFA(isOn: Boolean): Boolean
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| isOn     | Boolean     | true<br>false    |

Example
```kotlin=
flow { emit(lockConfig80UseCase.query()) }
    .catch { Timer.e(it) }
    .map { lockConfig ->
        val isTwoFAOn = lockConfig.twoFA == BleV3Lock.TwoFA.CLOSE.value
        val result = lockConfig80UseCase.setTwoFA(isTwoFAOn)
        result
    }
    .catch { Timer.e(it) }
    .flowOn(Dispatchers.IO)
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

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
flow { emit(lockConfig80UseCase.query()) }
    .catch { Timer.e(it) }
    .map { lockConfig ->
        val isVacationModeOn = lockConfig.vacationMode == BleV3Lock.VacationMode.CLOSE.value
        val result = lockConfig80UseCase.setVacationMode(isVacationModeOn)
        result
    }
    .catch { Timer.e(it) }
    .flowOn(Dispatchers.IO)
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Turn on/off auto lock
```kotlin=
suspend fun setAutoLock(isOn: Boolean, autoLockTime: Int): Boolean
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| isOn     | Boolean     | true<br>false    |
| autoLockTime     | Int     | Auto lock delay time, between autoLockTimeUpperLimit and autoLockTimeLowerLimit. <br>0xFFFF: Not support |

Example
```kotlin=
flow { emit(lockConfig80UseCase.query()) }
    .catch { Timer.e(it) }
    .map { lockConfig ->
        val isAutoLock = lockConfig.autoLock == BleV3Lock.AutoLock.CLOSE.value
        val result = lockConfig80UseCase.setAutoLock(isAutoLock, autoLockTime)
        result
    }
    .catch { Timer.e(it) }
    .flowOn(Dispatchers.IO)
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| IllegalArgumentException     | Auto lock time illegal argument.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Turn on/off operating sound
```kotlin=
suspend fun setOperatingSound(isOn: Boolean): Boolean
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| isOn     | Boolean     | true<br>false    |

Example
```kotlin=
flow { emit(lockConfig80UseCase.query()) }
    .catch { Timer.e(it) }
    .map { lockConfig ->
        val isOperatingSoundOn = lockConfig.operatingSound == BleV3Lock.OperatingSound.CLOSE.value
        val result = lockConfig80UseCase.setOperatingSound(isOperatingSoundOn)
        result
    }
    .catch { Timer.e(it) }
    .flowOn(Dispatchers.IO)
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Turn on/off key press beep
```kotlin=
suspend fun setSoundValue(soundValue :Int): Boolean
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| soundType     | Int     | Please refer to [Sound](###Sound)    |


Example
```kotlin=
flow { emit(lockConfig80UseCase.query()) }
    .catch { Timer.e(it) }
    .map { lockConfig ->
        val value = when (lockConfig.soundType) {
            0x01 -> if(lockConfig.soundValue == 100) 0 else 100
            0x02 -> if(lockConfig.soundValue == 100) 50 else if(lockConfig.soundValue == 50) 0 else 100
            else -> soundValue
        }
        val result = lockConfig80UseCase.setSoundValue(value)
        result
    }
    .catch { Timer.e(it) }
    .flowOn(Dispatchers.IO)
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Turn on/off show fast track mode
```kotlin=
suspend fun setShowFastTrackMode(isOn: Boolean): Boolean
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| isOn     | Boolean     | true<br>false    |

Example
```kotlin=
flow { emit(lockConfig80UseCase.query()) }
    .catch { Timer.e(it) }
    .map { lockConfig ->
        val isShowFastTrackModeOn = lockConfig.showFastTrackMode == BleV3Lock.ShowFastTrackMode.CLOSE.value
        val result = lockConfig80UseCase.setShowFastTrackMode(isShowFastTrackModeOn)
        result
    }
    .catch { Timer.e(it) }
    .flowOn(Dispatchers.IO)
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Turn on/off sabbath mode
```kotlin=
suspend fun setSabbathMode(isOn: Boolean): Boolean
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| isOn     | Boolean     | true<br>false    |

Example
```kotlin=
flow { emit(lockConfig80UseCase.query()) }
    .catch { Timer.e(it) }
    .map { lockConfig ->
        val isSabbathMode = lockConfig.sabbathMode == BleV3Lock.SabbathMode.CLOSE.value
        val result = lockConfig80UseCase.setSabbathMode(isSabbathMode)
        result
    }
    .catch { Timer.e(it) }
    .flowOn(Dispatchers.IO)
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Turn on/off phonetic language
```kotlin=
suspend fun setPhoneticLanguage(language: Language): Boolean
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| language     | Int     | language support value    |

Example
```kotlin=
flow { emit(lockConfig80UseCase.query()) }
    .catch { Timer.e(it) }
    .map { lockConfig ->
        val result = lockConfig80UseCase.setPhoneticLanguage(language)
        result
    }
    .catch { Timer.e(it) }
    .flowOn(Dispatchers.IO)
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

### PlugConfigUseCase
#### Get plug config
Please refer to [DeviceStatusB0](###DeviceStatusB0)

```kotlin=
suspend fun getDeviceStatus(): DeviceStatus.B0
```

Example
```kotlin=
flow { emit(plugConfigUseCase()) }
    .map { plugConfig ->
        // return plug config
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

#### Factory reset(for plug)
```kotlin=
suspend fun factoryReset(): Boolean
```
Example
```kotlin=
flow { emit(lockUtilityUseCase.factoryReset()) }
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

### LockBleUserUseCase
#### GetBleUserArray
```kotlin=
suspend fun getBleUserArray(): List<Int>
```

Example
```kotlin=
flow { emit(lockBleUserUseCase.getBleUserArray()) }
    .map { bleUserArray ->
        // return useful bleUser array index
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

#### Get BleUser
```kotlin=
suspend fun getBleUser(index: Int): DeviceToken.PermanentToken
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index     | Int     | Get ble user at index in array    |

Example
```kotlin=
flow { emit(lockBleUserUseCase.getBleUser(index)) }
    .map { bleUser ->
        if(bleUser.isPermanent){
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

#### Add one time bleUser
```kotlin=
suspend fun addOneTimeBleUser(permission: String, name: String, identity:String): AddUserResponse
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| permission | String     | "A": All , "L": Limit    |
| name       | String     | Token name    |
| identity   | String     | Identity value    |

Example
```kotlin=
flow { emit(lockBleUserUseCase.getBleUser(permission,name,identity)) }
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

#### Edit bleUser
```kotlin=
suspend fun editBleUser(index: Int, permission: String, name: String, identity:String): Boolean
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index      | Int        | Edit token at index in array     |
| permission | String     | "A": All , "L": Limit    |
| name       | String     | Token name    |
| identity   | String     | Identity value    |

Example
```kotlin=
flow { emit(lockBleUserUseCase.editBleUser(index, permission, name, identity)) }
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

#### Delete bleUser
```kotlin=
suspend fun deleteBleUser(index: Int, code: String = ""): Boolean
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index    | Int     | Delete token at index in array     |
| code     | String  | Only delete permanent token need admin code    |

Example
```kotlin=
flow { emit(lockBleUserUseCase.deleteBleUser(index, code)) }
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

### LockUserUseCase
#### Get user ability
```kotlin=
suspend fun getUserAbility(): BleV3Lock.UserAbility
```

Example
```kotlin=
flow { emit(lockUserUseCase.getUserAbility()) }
    .map { userAbility ->
        // return userAbility
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Get user count
```kotlin=
suspend fun getUserCount(): BleV3Lock.UserCount
```

Example
```kotlin=
flow { emit(lockUserUseCase.getUserCount()) }
    .map { userCount ->
        // return userCount
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

#### Is matter device
```kotlin=
suspend fun isMatterDevice(): Boolean
```

Example
```kotlin=
flow { emit(lockUserUseCase.isMatterDevice()) }
    .map { result ->
        // return isMatterDevice
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Get user array
```kotlin=
suspend fun getUserArray(): List<Boolean>
```

Example
```kotlin=
flow { emit(lockUserUseCase.getUserArray()) }
    .map { result ->
        // return userArray
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Get user
```kotlin=
suspend fun getUser(index: Int): User
```
| Parameter | Type | Description |
| --------  | -------- | -------- |
| index        | Int                     | User index    |

Example
```kotlin=
flow { emit(lockUserUseCase.getUser(index)) }
    .map { result ->
        // return user
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Add user
```kotlin=
suspend fun addUser(
        userIndex: Int,
        name: String,
        userStatus: Int,
        userType: Int,
        credentialRule: Int,
        weekDayScheduleList: MutableList<BleV3Lock.WeekDaySchedule>? = null,
        yearDayScheduleList: MutableList<BleV3Lock.YearDaySchedule>? = null
    ): Boolean
```
| Parameter | Type | Description |
| --------  | -------- | -------- |
| userIndex | Int | User index |
| name | String | User name |
| userStatus | UserStatus | [UserStatus/CredentialStatus](###UserStatus/CredentialStatus) |
| userType | Int | [UserType](###UserType) |
| credentialRule | [CredentialRule](###CredentialRule) |
| weekDayScheduleList | MutableList<BleV3Lock.WeekDaySchedule>  | WeekDaySchedule |
| yearDayScheduleList | MutableList<BleV3Lock.YearDaySchedule>  | YearDaySchedule |

Example
```kotlin=
val name = "User ${lastUserIndex + 1}"
val index = lastUserIndex + 1
val uid = lastUserIndex + 1
val userStatus = BleV3Lock.UserStatus.OCCUPIED_ENABLED.value
val userType = BleV3Lock.UserType.UNRESTRICTED.value
val credentialRule = BleV3Lock.CredentialRule.SINGLE.value
val weekDaySchedule = mutableListOf<BleV3Lock.WeekDaySchedule>()
val yearDaySchedule = mutableListOf<BleV3Lock.YearDaySchedule>()
for (i in 0 until userAbility!!.weekDayScheduleCount){
    weekDaySchedule.add(BleV3Lock.WeekDaySchedule(BleV3Lock.ScheduleStatus.AVAILABLE.value, enumValues<BleV3Lock.DaysMaskMap>()[i].value, 8, 0, 18, 0))
}
for (i in 0 until userAbility!!.yearDayScheduleCount){
    yearDaySchedule.add(BleV3Lock.YearDaySchedule(BleV3Lock.ScheduleStatus.AVAILABLE.value, timestamp, timestamp))
}

flow { emit(lockUserUseCase.addUser(index, name, userStatus, userType, credentialRule, weekDaySchedule, yearDaySchedule)) }
    .map { result ->
        if(result){
            // success
        }
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |


#### Edit user
```kotlin=
suspend fun editUser(
        userIndex: Int,
        name: String,
        userStatus: Int,
        userType: Int,
        credentialRule: Int,
        weekDayScheduleList: MutableList<BleV3Lock.WeekDaySchedule>? = null,
        yearDayScheduleList: MutableList<BleV3Lock.YearDaySchedule>? = null
    ): Boolean
```
| Parameter | Type | Description |
| --------  | -------- | -------- |
| userIndex | Int | User index |
| name | String | User name |
| userStatus | UserStatus | [UserStatus/CredentialStatus](###UserStatus/CredentialStatus) |
| userType | Int | [UserType](###UserType) |
| credentialRule | [CredentialRule](###CredentialRule) |
| weekDayScheduleList | MutableList<BleV3Lock.WeekDaySchedule>  | WeekDaySchedule |
| yearDayScheduleList | MutableList<BleV3Lock.YearDaySchedule>  | YearDaySchedule |

Example
```kotlin=
flow { emit(lockUserUseCase.editUser(index, name, userStatus, userType, credentialRule, weekDaySchedule, yearDaySchedule)) }
    .map { result ->
        if(result){
            // success
        }
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Delete user
```kotlin=
suspend fun deleteUser(index: Int): Boolean
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index    | Int     | Delete user at index in array    |

Example
```kotlin=
flow { emit(lockUserUseCase.deleteUser(index)) }
    .map { result ->
        result = true when succeed
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

### LockCredentialUseCase
#### Get credential array
```kotlin=
suspend fun getCredentialArray(): List<Boolean>
```

Example
```kotlin=
flow { emit(lockCredentialUseCase.getCredentialArray()) }
    .map { credentialArray ->
        // return credentialArray
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Get credential by user
```kotlin=
suspend fun getCredentialByUser(index: Int): Credential.NinetyFiveUser = getCredential(BleV3Lock.CredentialFormat.USER.value, index) as Credential.NinetyFiveUser
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index    | Int    | Get credential at user index in array     |

Example
```kotlin=
flow { emit(lockCredentialUseCase.getCredentialByUser(index)) }
    .map { credential ->
        // return credential info
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
```

#### Get credential by credential
```kotlin=
suspend fun getCredentialByCredential(index: Int): Credential.NinetyFiveCredential = getCredential(BleV3Lock.CredentialFormat.CREDENTIAL.value, index) as Credential.NinetyFiveCredential
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index    | Int    | Get credential at credential index in array     |

Example
```kotlin=
flow { emit(lockCredentialUseCase.getCredentialByCredential(index)) }
    .map { credential ->
        // return credential info
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

#### Add credential code
```kotlin=
suspend fun addCredentialCode(index: Int, userIndex:Int, code: String): Boolean = addCredential(userIndex, BleV3Lock.CredentialDetail(index, type = BleV3Lock.CredentialType.PIN.value, code = code.toAsciiByteArray()))
```

Parameter
| Parameter | Type | Description |
| --------  | -------- | -------- |
| index        | Int | credential index    |
| userIndex    | Int | user index    |
| code | String  | code |

Example
```kotlin=
flow { emit(lockCredentialUseCase.addCredentialCode(index, userIndex, code)) }
    .map { result ->
        if(result){
            // success
        }
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Add credential card
```kotlin=
suspend fun addCredentialCard(index: Int, userIndex:Int, code: ByteArray): Boolean = addCredential(userIndex, BleV3Lock.CredentialDetail(index, type = BleV3Lock.CredentialType.RFID.value, code = code))
```

Parameter
| Parameter | Type | Description |
| --------  | -------- | -------- |
| index        | Int | credential index    |
| userIndex    | Int | user index    |
| code | ByteArray  | card code |

Example
```kotlin=
flow { emit(lockCredentialUseCase.addCredentialCard(index, userIndex, code)) }
    .map { result ->
        if(result){
            // success
        }
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Add credential fingerprint
```kotlin=
suspend fun addCredentialFingerPrint(index: Int, userIndex:Int): Boolean = addCredential(userIndex, BleV3Lock.CredentialDetail(index, type = BleV3Lock.CredentialType.FINGERPRINT.value, code = index.toLittleEndianByteArrayInt16()))
```

Parameter
| Parameter | Type | Description |
| --------  | -------- | -------- |
| index        | Int | credential index    |
| userIndex    | Int | user index    |

Example
```kotlin=
flow { emit(lockCredentialUseCase.addCredentialFingerPrint(index, userIndex)) }
    .map { result ->
        if(result){
            // success
        }
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Add credential face
```kotlin=
suspend fun addCredentialFace(index: Int, userIndex:Int): Boolean = addCredential(userIndex, BleV3Lock.CredentialDetail(index, type = BleV3Lock.CredentialType.FACE.value, code = index.toLittleEndianByteArrayInt16()))
```

Parameter
| Parameter | Type | Description |
| --------  | -------- | -------- |
| index        | Int | credential index    |
| userIndex    | Int | user index    |

Example
```kotlin=
flow { emit(lockCredentialUseCase.addCredentialFace(index, userIndex)) }
    .map { result ->
        if(result){
            // success
        }
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Edit credential code
```kotlin=
suspend fun editCredentialCode(index: Int, userIndex:Int, code: String): Boolean = addCredential(userIndex, BleV3Lock.CredentialDetail(index, type = BleV3Lock.CredentialType.PIN.value, code = code.toAsciiByteArray()))
```

Parameter
| Parameter | Type | Description |
| --------  | -------- | -------- |
| index        | Int | credential index    |
| userIndex    | Int | user index    |
| code | String  | code |

Example
```kotlin=
flow { emit(lockCredentialUseCase.editCredentialCode(index, userIndex, code)) }
    .map { result ->
        if(result){
            // success
        }
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Edit credential card
```kotlin=
suspend fun editCredentialCard(index: Int, userIndex:Int, code: ByteArray): Boolean = addCredential(userIndex, BleV3Lock.CredentialDetail(index, type = BleV3Lock.CredentialType.RFID.value, code = code))
```

Parameter
| Parameter | Type | Description |
| --------  | -------- | -------- |
| index        | Int | credential index    |
| userIndex    | Int | user index    |
| code | ByteArray  | card code |

Example
```kotlin=
flow { emit(lockCredentialUseCase.editCredentialCard(index, userIndex, code)) }
    .map { result ->
        if(result){
            // success
        }
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Edit credential fingerprint
```kotlin=
suspend fun editCredentialFingerPrint(index: Int, userIndex:Int): Boolean = addCredential(userIndex, BleV3Lock.CredentialDetail(index, type = BleV3Lock.CredentialType.FINGERPRINT.value, code = index.toLittleEndianByteArrayInt16()))
```

Parameter
| Parameter | Type | Description |
| --------  | -------- | -------- |
| index        | Int | credential index    |
| userIndex    | Int | user index    |

Example
```kotlin=
flow { emit(lockCredentialUseCase.editCredentialFingerPrint(index, userIndex)) }
    .map { result ->
        if(result){
            // success
        }
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Edit credential face
```kotlin=
suspend fun editCredentialFace(index: Int, userIndex:Int): Boolean = addCredential(userIndex, BleV3Lock.CredentialDetail(index, type = BleV3Lock.CredentialType.FINGERPRINT.value, code = index.toLittleEndianByteArrayInt16()))
```

Parameter
| Parameter | Type | Description |
| --------  | -------- | -------- |
| index        | Int | credential index    |
| userIndex    | Int | user index    |

Example
```kotlin=
flow { emit(lockCredentialUseCase.editCredentialFace(index, userIndex)) }
    .map { result ->
        if(result){
            // success
        }
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Delete credential
```kotlin=
suspend fun deleteCredential(index: Int): Boolean
```

Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index    | Int     | Delete credential at index in array    |

Example
```kotlin=
flow { emit(lockCredentialUseCase.deleteCredential(index)) }
    .map { result ->
        result = true when succeed
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
| LockFunctionNotSupportException | Lock function not support.|

#### Device get credential card
```kotlin=
suspend fun deviceGetCredentialCard(index: Int): Credential.NinetySeven = deviceGetCredential(BleV3Lock.CredentialType.RFID.value , BleV3Lock.CredentialState.START.value, index)
```
Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index    | Int     | create:0xFFFF, edit:credential index |

Example
```kotlin=
flow { emit(lockCredentialUseCase.deviceGetCredentialCard(index)) }
    .map { result ->
        // return Credential.NinetySeven
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Device get credential fingerprint
```kotlin=
suspend fun deviceGetCredentialFingerprint(index: Int): Credential.NinetySeven = deviceGetCredential(BleV3Lock.CredentialType.FINGERPRINT.value , BleV3Lock.CredentialState.START.value, index)
```
Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index    | Int     | create:0xFFFF, edit:credential index |

Example
```kotlin=
flow { emit(lockCredentialUseCase.deviceGetCredentialFingerprint(index)) }
    .map { result ->
        // return Credential.NinetySeven
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Device get credential face
```kotlin=
suspend fun deviceGetCredentialFace(index: Int): Credential.NinetySeven = deviceGetCredential(BleV3Lock.CredentialType.FACE.value , BleV3Lock.CredentialState.START.value, index)
```
Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index    | Int     | create:0xFFFF, edit:credential index |

Example
```kotlin=
flow { emit(lockCredentialUseCase.deviceGetCredentialFace(index)) }
    .map { result ->
        // return Credential.NinetySeven
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Device exit credential card
```kotlin=
suspend fun deviceExitCredentialCard(index: Int): Credential.NinetySeven = deviceGetCredential(BleV3Lock.CredentialType.RFID.value , BleV3Lock.CredentialState.EXIT.value, index)
```
Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index    | Int     | credential index |

Example
```kotlin=
flow { emit(lockCredentialUseCase.deviceExitCredentialCard(index)) }
    .map { result ->
        // return Credential.NinetySeven
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Device exit credential fingerprint
```kotlin=
suspend fun deviceExitCredentialFingerprint(index: Int): Credential.NinetySeven = deviceGetCredential(BleV3Lock.CredentialType.FINGERPRINT.value , BleV3Lock.CredentialState.EXIT.value, index)
```
Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index    | Int     | credential index |

Example
```kotlin=
flow { emit(lockCredentialUseCase.deviceExitCredentialFingerprint(index)) }
    .map { result ->
        // return Credential.NinetySeven
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### Device exit credential face
```kotlin=
suspend fun deviceExitCredentialFace(index: Int): Credential.NinetySeven = deviceGetCredential(BleV3Lock.CredentialType.FACE.value , BleV3Lock.CredentialState.EXIT.value, index)
```
Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index    | Int     | credential index |

Example
```kotlin=
flow { emit(lockCredentialUseCase.deviceExitCredentialFace(index)) }
    .map { result ->
        // return Credential.NinetySeven
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

```kotlin=
suspend fun deviceGetCredentialCard(index: Int): Credential.NinetySeven = deviceGetCredential(BleV3Lock.CredentialType.RFID.value , BleV3Lock.CredentialState.START.value, index)
```
Parameter
| Parameter | Type | Description |
| -------- | -------- | -------- |
| index    | Int     | create:0xFFFF, edit:credential index |

Example
```kotlin=
flow { emit(lockCredentialUseCase.deviceGetCredentialCard(index)) }
    .map { result ->
        // return Credential.NinetySeven
    }
    .flowOn(Dispatchers.IO)
    .catch { Timer.e(it) }
    .launchIn(viewModelScope)
}
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

### LockWifiUseCase
#### ScanWifi
```kotlin=
fun collectWifiList3(): Flow<WifiList>
suspend fun scanWifi3()
```

Example
```kotlin=
collectWifiListJob?.cancel()
collectWifiListJob = lockWifiUseCase.collectWifiList3()
    .catch { Timer.e(it) }
    .onEach { wifi ->
        when (wifi) {
            WifiList.End -> {
                collectWifiListJob?.cancel()
                if(scanWifiJob != null){
                    scanWifiJob?.cancel()
                    scanWifiJob = null
                }
            }
            is WifiList.Wifi -> {
                Timer.d("result:\nssid: ${wifi.ssid} needPassword: ${wifi.needPassword}")
            }
        }
    }
    .flowOn(Dispatchers.IO)
    .launchIn(viewModelScope)

if (scanWifiJob != null) return
scanWifiJob = flow { emit(lockWifiUseCase.scanWifi3()) }
.flowOn(Dispatchers.IO)
.catch { Timer.e(it) }
.launchIn(viewModelScope)

```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

#### ConnectToWifi
```kotlin=
suspend fun connectToWifi3(ssid: String, password: String): Boolean
```

Example
```kotlin=
if (isCollectingConnectToWifiState) return
connectToWifiJob?.cancel()
connectToWifiJob = lockWifiUseCase
    .collectConnectToWifiState3()
    .flowOn(Dispatchers.IO)
    .onEach { wifiConnectState ->
        val progressMessage =
            when (wifiConnectState) {
                WifiConnectState.ConnectWifiSuccess -> "Wifi connected, connecting to cloud service..."
                WifiConnectState.ConnectWifiFail -> "Connect to Wi-Fi failed."
                WifiConnectState.Failed -> "Unknown error."
            }

        if (wifiConnectState == WifiConnectState.ConnectWifiFail) {
            Timber.e("CWifiFail")
        }
    }
    .catch {Timber.e(it)}
    .launchIn(viewModelScope)

flow { emit(lockWifiUseCase.connectToWifi3(ssid, password)) }
    .flowOn(Dispatchers.IO)
    .catch {Timber.e(it)}
    .launchIn(viewModelScope)
```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |

### LockOTAUseCase
#### OTA update
```kotlin=
suspend fun setOTAStart(target:Int, fileSize:Int): BleV2Lock.OTAStatus
suspend fun transferOTAData(offset:Int, data: ByteArray): String
suspend fun setOTAFinish(target:Int, fileSize:Int, iv:String, signature:String): BleV2Lock.OTAStatus
```
| Parameter | Type | Description |
| -------- | -------- | -------- |
| target    | Int     | wireless,mcu |
| fileSize    | Int     | file size |
| data    | ByteArray     | ota data |
| iv    | String     | initial vactor, Only 'finish' needs to be specified |
| signature    | String     | file identify, Only 'finish' needs to be specified |

Example
```kotlin=
val chunkSize = 128
val buffer = ByteArray(chunkSize)
viewModelScope.launch(Dispatchers.IO) {
    try {
        var bytesRead: Int
        lockOTAUseCase.setOTAStart(target, fileSize)

        inputStream.use { inputStream ->
            var blockNumber = 0
            bytesRead = inputStream.read(buffer)

            while (bytesRead != -1) {

                val result = lockOTAUseCase.transferOTAData(
                    blockNumber * chunkSize,
                    buffer.take(bytesRead).toByteArray()
                )
                Timber.d("end: $result")

                blockNumber++
                bytesRead = inputStream.read(buffer)
            }
            val result = lockOTAUseCase.setOTAFinish(target, fileSize, ivString, signature)
            Timber.d("end: $result")
        }

        inputStream.close()
    } catch (e: Exception) {
        e.printStackTrace()
        lockOTAUseCase.setOTACancel(target)
    }
}

```

Exception
| Exception | Description |
| -------- | -------- |
| NotConnectedException     | Mobile APP is not connected with lock.     |
| AdminCodeNotSetException     | Admin code has not been set.     |
#### OTA cancel
```kotlin=
suspend fun setOTACancel(target:Int): BleV2Lock.OTAStatus
```
| Parameter | Type | Description |
| -------- | -------- | -------- |
| target    | Int     | wireless,mcu |

Example
```kotlin=
flow { emit(lockOTAUseCase.setOTACancel(target)) }
    .map { result ->
        if(result){
            // success
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

## Models
### QRCodeContent

| Name | Type | Description |
| ---- | ---- | ----------- |
|  U   |String| Device uuid 16-length |

### LockConnectionInfo

| Name | Type | Description |
| -------- | -------- | -------- |
| macAddress     | String     | BT mac address of lock. You can get it from QR code.     |
| oneTimeToken     | String     | Default connection token of lock. You can get it from QR code.     |
| permanentToken     | String     | Excanged connection token when pairing with lock. You should save it for later use.    |
| keyOne     | String     | Encryption key of data transmission. You can get it from QR code.     |
| keyTwo     | String     | Random-generated encryption key of data transmission.     |
| permission     | String     | Permission of connection token.     |

### DeviceStatus82
| Name | Type | Description |
| -------- | -------- | -------- |
|mainVesion|Int|main vesion (cmd 3.0.0 is 3)|
|subVesion|Int|sub vesion (cmd 3.1.5 is 15)|
|direction|Int|0xA0: Right<br>0xA1: Left<br>0xA2: Unknown<br>0xFF: Not support|
|vacationMode|Int|0: Close<br>1: Open<br>0xFF: Not support|
|deadBolt|Int|0: Not protrude<br>1: protrude<br>0xFF: Not support|
|doorState|Int|0: Open<br>1: Close<br>0xFF: Not support|
|lockState|Int|0: Unlocked<br>1: Locked<br>2: Unknown|
|securityBolt|Int|0: Not protrude<br>1: protrude<br>0xFF: Not support|
|battery|Int|Percentage of battery power|
|batteryState|Int|0: Normal<br>1: Weak current<br>2: Dangerous|

### LockConfig80
| Name | Type | Value |
| -------- | -------- | -------- |
|mainVesion|Int|main vesion (cmd 3.0.0 is 3)|
|subVesion|Int|sub vesion (cmd 3.1.5 is 15)|
|formatVesion|Int|format vesion default:1|
|serverVesion|Double|server vesion|
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
|sabbathMode|Int|0: Close<br>1: Open<br>0xFF: Not support|
|phoneticLanguage|Int|0: ENGLISH<br>1: SPANISH<br>0: ENGLISH<br>2: FRENCH<br>3: CHINESE<br>0xFF: Not support|
|supportPhoneticLanguage|Int|8 bit value for 8 language 0~0xFF, one bit value<br>1: Support<br>0: Not support|

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

### UserType
| Name | Description | Value |
| -------- | -------- | -------- |
|UNRESTRICTED|Permanent| 0 |
|YEAR_DAY_SCHEDULE (from: Long, to: Long)|Valid Time Range| 1 |
|WEEK_DAY_SCHEDULE (weekdayBits: Int, from: Int, to: Int)|ScheduleEntry| 2 |
|DISPOSABLE|Single Entry| 6 |
|UNKNOWN|None| 10 |

### UserStatus/CredentialStatus
| Name | Value |
| -------- | -------- |
|AVAILABLE| 0x00 |
|OCCUPIED_ENABLED| 0x01 |
|OCCUPIED_DISABLED| 0x03 |
|UNKNOWN| 2 |

### CredentialType
| Name | Value |
| -------- | -------- |
|PIN| 1 |
|RFID| 2 |
|FINGERPRINT| 3 |
|FACE| 5 |

### CredentialRule
| Name | Value |
| -------- | -------- |
|SINGLE| 0x00 |
|DUAL| 0x01 |
|TRI| 0x02 |
|UNKNOWN| 3 |

### DaysMaskMap
| Name | Value |
| -------- | -------- |
|SUNDAY| 0x01 |
|MONDAY| 0x02 |
|TUESDAY| 0x04 |
|WEDNESDAY| 0x08 |
|THURSDAY| 0x10 |
|FRIDAY| 0x20 |
|SATURDAY| 0x40 |

### AlertAF
| Name | Type | Description |
| -------- | -------- | -------- |
|alertType|Int|0: Error access code<br>1: Current access code at wrong time<br>2: Current access code but at vacation mode<br>3: Actively press the clear key<br>20: Many error key locked<br>40: Lock break alert|

### EventLog
| Name | Type | Value |
| -------- | -------- | -------- |
|eventTimeStamp|Long|The time when the event log occurred|
|event|Int|0: Auto lock close success<br>1: Auto lock close fail<br>2: App lock close success<br>3: App lock close fail<br>4: Physical button close success<br>5: Physical button close fail<br>6: Close the door manually success<br>7: Close the door manually fail<br>8: App lock open success<br>9: App lock open fail<br>10: Physical button open success<br>11: Physical button open fail<br>12: Open the door manually success<br>13: Open the door manually fail<br>14: Card open the door success<br>15: Card open the door fail<br>16: Fingerprint open the door success<br>17: Fingerprint open the door fail<br>18: Face open the door success<br>19: Face open the door fail<br>20: TwoFA open the door success<br>21: TwoFA open the door fail<br>22: Matter open door success<br>23: Matter open door failure<br>24: Matter close door success<br>25: Matter close door failure<br>64: Add token<br>65: Edit token<br>66: Delete token<br>80: Add access code<br>81: Edit access code<br>82: Delete access code<br>83: Add access card<br>84: Edit access card<br>85: Delete access card<br>86: Add fingerprint<br>87: Edit fingerprint<br>88: Delete fingerprint<br>89: Add face<br>90: Edit face<br>91: Delete face<br>128: Wrong password<br>129: Connection error<br>130: At wrong time enter correct password<br>131: At vacation mode enter not admin password<br>132: Wrong access card<br>133: Wrong fingerprint<br>134: Wrong face<br>135: TwoFA error|
|name|String|Event log name <br>With 64~66 & 80~91 is named operator &#124; operated|
