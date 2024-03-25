package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.accessCodeToHex
import com.sunion.core.ble.entity.BleV3Lock
import com.sunion.core.ble.entity.User
import com.sunion.core.ble.exception.NotConnectedException
import com.sunion.core.ble.toBooleanList
import com.sunion.core.ble.toLittleEndianByteArrayInt16
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import timber.log.Timber
import javax.inject.Inject

class LockUserUseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    private val className = this::class.simpleName ?: "LockUserUseCase"

    suspend fun getUserArray(): List<Boolean> {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::getUserArray.name
        val function = 0x90
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                ) as User.Ninety
            }
            .map { decoded ->
                val list = mutableListOf<Boolean>()
                decoded.data.forEach { it.toBooleanList(list) }
                list.toList()
                list
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
            }
            .single()
    }

    suspend fun queryUser(index: Int): User.NinetyOne {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::queryUser.name
        val function = 0x91
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = index.toLittleEndianByteArrayInt16()
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                ) as User.NinetyOne
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e -> Timber.e("$functionName exception $e") }
            .single()
    }

    suspend fun addUser(
        index: Int,
        name: String,
        uid: Int,
        status: Int,
        type: Int,
        credentialRule: Int,
        credentialList: MutableList<BleV3Lock.Credential>? = null,
        weekDayScheduleList: MutableList<BleV3Lock.WeekDaySchedule>? = null,
        yearDayScheduleList: MutableList<BleV3Lock.YearDaySchedule>? = null
    ): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::addUser.name
        val function = 0x92
        val data = bleCmdRepository.combineUser92Cmd(User.NinetyTwoCmd(
            action = 0x00,
            index = index,
            name = name,
            uid = uid,
            status = status,
            type = type,
            credentialRule = credentialRule,
            credentialList = credentialList,
            weekDayScheduleList = weekDayScheduleList,
            yearDayScheduleList = yearDayScheduleList
        ))
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                ) as User.NinetyTwo
                result.isSuccess
            }
            .flowOn(Dispatchers.IO)
            .catch { e -> Timber.e("$functionName exception $e") }
            .single()
    }

    suspend fun editUser(
        index: Int,
        name: String,
        uid: Int,
        status: Int,
        type: Int,
        credentialRule: Int,
        credentialList: MutableList<BleV3Lock.Credential>? = null,
        weekDayScheduleList: MutableList<BleV3Lock.WeekDaySchedule>? = null,
        yearDayScheduleList: MutableList<BleV3Lock.YearDaySchedule>? = null
    ): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::editUser.name
        val function = 0x92
        val data = bleCmdRepository.combineUser92Cmd(User.NinetyTwoCmd(
            action = 0x01,
            index = index,
            name = name,
            uid = uid,
            status = status,
            type = type,
            credentialRule = credentialRule,
            credentialList = credentialList,
            weekDayScheduleList = weekDayScheduleList,
            yearDayScheduleList = yearDayScheduleList
        ))
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                ) as User.NinetyTwo
                result.isSuccess
            }
            .flowOn(Dispatchers.IO)

            .catch { e -> Timber.e("$functionName exception $e") }
            .single()
    }

    suspend fun deleteUser(index: Int): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::deleteUser.name
        val function = 0x93
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = index.toLittleEndianByteArrayInt16()
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                ) as User.NinetyTwo
                result.isSuccess
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
            }
            .single()
    }

    suspend fun getCredentialArray(): List<Boolean> {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::getCredentialArray.name
        val function = 0x94
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                ) as User.Ninety
            }
            .map { decoded ->
                val list = mutableListOf<Boolean>()
                decoded.data.forEach { it.toBooleanList(list) }
                list.toList()
                list
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
            }
            .single()
    }

    suspend fun getCredentialByUser(index: Int): User.NinetyFive = getCredential(BleV3Lock.CredentialFormat.USER.value, index)
    suspend fun getCredentialByCredential(index: Int): User.NinetyFive = getCredential(BleV3Lock.CredentialFormat.CREDENTIAL.value, index)

    private suspend fun getCredential(format: Int, index: Int): User.NinetyFive {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::getCredential.name
        val function = 0x95
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = byteArrayOf(format.toByte()) + index.toLittleEndianByteArrayInt16()
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                ) as User.NinetyFive
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
            }
            .single()
    }

    suspend fun addCredentialCode(index: Int, status: Int, userIndex:Int, code: ByteArray): Boolean = addCredential(index, BleV3Lock.CredentialDetail(status, BleV3Lock.CredentialType.PIN.value, userIndex, code))
    suspend fun addCredentialCard(index: Int, status: Int, userIndex:Int, code: ByteArray): Boolean = addCredential(index, BleV3Lock.CredentialDetail(status, BleV3Lock.CredentialType.RFID.value, userIndex, code))
    suspend fun addCredentialFingerPrint(index: Int, status: Int, userIndex:Int, code: ByteArray): Boolean = addCredential(index, BleV3Lock.CredentialDetail(status, BleV3Lock.CredentialType.FINGERPRINT.value, userIndex, code))
    suspend fun addCredentialFingerVein(index: Int, status: Int, userIndex:Int, code: ByteArray): Boolean = addCredential(index, BleV3Lock.CredentialDetail(status, BleV3Lock.CredentialType.FINGER_VEIN.value, userIndex, code))
    suspend fun addCredentialFace(index: Int, status: Int, userIndex:Int, code: ByteArray): Boolean = addCredential(index, BleV3Lock.CredentialDetail(status, BleV3Lock.CredentialType.FACE.value, userIndex, code))

    private suspend fun addCredential(
        index: Int,
        credentialDetail: BleV3Lock.CredentialDetail? = null,
    ): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::addCredential.name
        val function = 0x96
        val data = bleCmdRepository.combineUser96Cmd(
            User.NinetySixCmd(
                action = 0x00,
                index = index,
                credentialDetail = credentialDetail
            )
        )
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                ) as User.NinetyTwo
                result.isSuccess
            }
            .flowOn(Dispatchers.IO)
            .catch { e -> Timber.e("$functionName exception $e") }
            .single()
    }

    suspend fun editCredentialCode(index: Int, status: Int, userIndex:Int, code: ByteArray): Boolean = editCredential(index, BleV3Lock.CredentialDetail(status, BleV3Lock.CredentialType.PIN.value, userIndex, code))
    suspend fun editCredentialCard(index: Int, status: Int, userIndex:Int, code: ByteArray): Boolean = editCredential(index, BleV3Lock.CredentialDetail(status, BleV3Lock.CredentialType.RFID.value, userIndex, code))
    suspend fun editCredentialFingerPrint(index: Int, status: Int, userIndex:Int, code: ByteArray): Boolean = editCredential(index, BleV3Lock.CredentialDetail(status, BleV3Lock.CredentialType.FINGERPRINT.value, userIndex, code))
    suspend fun editCredentialFingerVein(index: Int, status: Int, userIndex:Int, code: ByteArray): Boolean = editCredential(index, BleV3Lock.CredentialDetail(status, BleV3Lock.CredentialType.FINGER_VEIN.value, userIndex, code))
    suspend fun editCredentialFace(index: Int, status: Int, userIndex:Int, code: ByteArray): Boolean = editCredential(index, BleV3Lock.CredentialDetail(status, BleV3Lock.CredentialType.FACE.value, userIndex, code))

    private suspend fun editCredential(
        index: Int,
        credentialDetail: BleV3Lock.CredentialDetail? = null,
    ): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::editCredential.name
        val function = 0x96
        val data = bleCmdRepository.combineUser96Cmd(
            User.NinetySixCmd(
                action = 0x01,
                index = index,
                credentialDetail = credentialDetail
            )
        )
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                ) as User.NinetyTwo
                result.isSuccess
            }
            .flowOn(Dispatchers.IO)
            .catch { e -> Timber.e("$functionName exception $e") }
            .single()
    }

    private suspend fun deviceGetCredential(type:Int, state:Int, index: Int): User.NinetySeven {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::deviceGetCredential.name
        val function = 0x97
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = byteArrayOf(type.toByte(), state.toByte()) + index.toLittleEndianByteArrayInt16()
        )

        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                ) as User.NinetySeven
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
            }
            .single()
    }

    suspend fun deviceGetCredentialCard(index: Int): User.NinetySeven = deviceGetCredential(BleV3Lock.CredentialType.RFID.value , 1, index)
    suspend fun deviceGetCredentialFingerprint(index: Int): User.NinetySeven = deviceGetCredential(BleV3Lock.CredentialType.FINGERPRINT.value, 1, index)
    suspend fun deviceGetCredentialFingerVein(index: Int): User.NinetySeven = deviceGetCredential(BleV3Lock.CredentialType.FINGER_VEIN.value, 1 ,index)
    suspend fun deviceGetCredentialFace(index: Int): User.NinetySeven = deviceGetCredential(BleV3Lock.CredentialType.FACE.value, 1 ,index)

    suspend fun deviceExitCredentialCard(index: Int): User.NinetySeven = deviceGetCredential(BleV3Lock.CredentialType.RFID.value, 0, index)
    suspend fun deviceExitCredentialFingerprint(index: Int): User.NinetySeven = deviceGetCredential(BleV3Lock.CredentialType.FINGERPRINT.value, 0, index)
    suspend fun deviceExitCredentialFingerVein(index: Int): User.NinetySeven = deviceGetCredential(BleV3Lock.CredentialType.FINGER_VEIN.value, 0 ,index)
    suspend fun deviceExitCredentialFace(index: Int): User.NinetySeven = deviceGetCredential(BleV3Lock.CredentialType.FACE.value, 0 ,index)

    suspend fun deleteCredential(index: Int): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::deleteCredential.name
        val function = 0x98
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = index.toLittleEndianByteArrayInt16()
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                ) as User.NinetyTwo
                result.isSuccess
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
            }
            .single()
    }

    suspend fun getCredentialHash(): User.NinetyNine = getHash(BleV3Lock.CredentialFormat.CREDENTIAL.value)
    suspend fun getUserHash(): User.NinetyNine = getHash(BleV3Lock.CredentialFormat.USER.value)

    private suspend fun getHash(index: Int): User.NinetyNine {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::getHash.name
        val function = 0x99
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = byteArrayOf(index.toByte())
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                ) as User.NinetyNine
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
            }
            .single()
    }

    suspend fun hasUnsyncedData(): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::hasUnsyncedData.name
        val function = 0x9A
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                ) as Boolean
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
            }
            .single()
    }

    suspend fun getUnsyncedData(): User.NinetyB {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::getUnsyncedData.name
        val function = 0x9B
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                ) as User.NinetyB
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
            }
            .single()
    }

    suspend fun setCredentialUnsyncedData(index: Int): User.NinetyC = setUnsyncedData(BleV3Lock.UnsyncedDataType.CREDENTIAL.value, System.currentTimeMillis(), index)
    suspend fun setUserUnsyncedData(index: Int): User.NinetyC = setUnsyncedData(BleV3Lock.UnsyncedDataType.USER.value, System.currentTimeMillis(), index)
    suspend fun setLogUnsyncedData(index: Int): User.NinetyC = setUnsyncedData(BleV3Lock.UnsyncedDataType.LOG.value, System.currentTimeMillis() ,index)
    suspend fun setTokenUnsyncedData(index: Int): User.NinetyC = setUnsyncedData(BleV3Lock.UnsyncedDataType.TOKEN.value, System.currentTimeMillis() ,index)
    suspend fun setSettingUnsyncedData(index: Int): User.NinetyC = setUnsyncedData(BleV3Lock.UnsyncedDataType.SETTING.value, System.currentTimeMillis() ,index)

    private suspend fun setUnsyncedData(type: Int, time: Long, index: Int): User.NinetyC {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::setUnsyncedData.name
        val function = 0x9C
        val data = bleCmdRepository.combineUser9CCmd(
            User.NinetyB(
                type = type,
                time = time.toInt(),
                index = index
            )
        )
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                ) as User.NinetyC
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
            }
            .single()
    }

    suspend fun setAllDataSynced(): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::setAllDataSynced.name
        val function = 0x9D
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                ) as Boolean
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
            }
            .single()
    }

}