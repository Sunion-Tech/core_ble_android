package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
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
    private suspend fun getUserArray(): List<Boolean> {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0x90,
            key = statefulConnection.key(),
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "getUserArray")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0x90)
            }
            .take(1)
            .map { notification ->
                bleCmdRepository.resolve(
                    0x90,
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
                Timber.e("getUserArray exception $e")
            }
            .single()
    }

    private suspend fun queryUser(index: Int): User.NinetyOne {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0x91,
            key = statefulConnection.key(),
            data = index.toLittleEndianByteArrayInt16()
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "queryUser index: $index")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0x91)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0x91,
                    statefulConnection.key(),
                    notification
                ) as User.NinetyOne
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e -> Timber.e("queryUser exception $e") }
            .single()
    }

    private suspend fun createUser(
        index: Int,
        name: String,
        uid: String,
        status: Int,
        type: Int,
        credentialRule: Int,
        credentialList: MutableList<BleV3Lock.Credential>? = null,
        weekDayScheduleList: MutableList<BleV3Lock.WeekDaySchedule>? = null,
        yearDayScheduleList: MutableList<BleV3Lock.YearDaySchedule>? = null
    ): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val data = bleCmdRepository.combineUserNinetyTwoCommand(User.NinetyTwoCmd(
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
        val command = bleCmdRepository.createCommand(
            function = 0x92,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "createUser data: $data")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0x92)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0x92,
                    statefulConnection.key(),
                    notification
                ) as User.NinetyTwo
                result.isSuccess
            }
            .flowOn(Dispatchers.IO)
            .catch { e -> Timber.e("createUser exception $e") }
            .single()
    }

    private suspend fun editUser(
        index: Int,
        name: String,
        uid: String,
        status: Int,
        type: Int,
        credentialRule: Int,
        credentialList: MutableList<BleV3Lock.Credential>? = null,
        weekDayScheduleList: MutableList<BleV3Lock.WeekDaySchedule>? = null,
        yearDayScheduleList: MutableList<BleV3Lock.YearDaySchedule>? = null
    ): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val data = bleCmdRepository.combineUserNinetyTwoCommand(User.NinetyTwoCmd(
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
        val command = bleCmdRepository.createCommand(
            function = 0x92,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "editUser data: $data")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0x92)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0x92,
                    statefulConnection.key(),
                    notification
                ) as User.NinetyTwo
                result.isSuccess
            }
            .flowOn(Dispatchers.IO)

            .catch { e -> Timber.e("editUser exception $e") }
            .single()
    }

    private suspend fun deleteUser(index: Int): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0x93,
            key = statefulConnection.key(),
            data = index.toLittleEndianByteArrayInt16()
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "deleteUser index: $index")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0x93)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0x93,
                    statefulConnection.key(),
                    notification
                ) as User.NinetyTwo
                result.isSuccess
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("deleteUser exception $e")
            }
            .single()
    }

    private suspend fun getCredentialArray(): List<Boolean> {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0x94,
            key = statefulConnection.key(),
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "getCredentialArray")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0x94)
            }
            .take(1)
            .map { notification ->
                bleCmdRepository.resolve(
                    0x94,
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
                Timber.e("getCredentialArray exception $e")
            }
            .single()
    }

    private suspend fun getCredential(format: Int, index: Int): User.NinetyFive {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0x95,
            key = statefulConnection.key(),
            data = byteArrayOf(format.toByte()) + index.toLittleEndianByteArrayInt16()
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "getCredential format: $format index: $index")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0x95)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0x95,
                    statefulConnection.key(),
                    notification
                ) as User.NinetyFive
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("getCredential exception $e")
            }
            .single()
    }

    private suspend fun createCredential(
        index: Int,
        credentialDetail: BleV3Lock.CredentialDetail? = null,
    ): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val data = bleCmdRepository.combineUserNinetySixCommand(
            User.NinetySixCmd(
                action = 0x00,
                index = index,
                credentialDetail = credentialDetail
            )
        )
        val command = bleCmdRepository.createCommand(
            function = 0x96,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "createCredential data: $data")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0x96)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0x96,
                    statefulConnection.key(),
                    notification
                ) as User.NinetyTwo
                result.isSuccess
            }
            .flowOn(Dispatchers.IO)
            .catch { e -> Timber.e("createCredential exception $e") }
            .single()
    }

    private suspend fun editCredential(
        index: Int,
        credentialDetail: BleV3Lock.CredentialDetail? = null,
    ): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val data = bleCmdRepository.combineUserNinetySixCommand(
            User.NinetySixCmd(
                action = 0x01,
                index = index,
                credentialDetail = credentialDetail
            )
        )
        val command = bleCmdRepository.createCommand(
            function = 0x96,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "editCredential data: $data")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0x96)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0x96,
                    statefulConnection.key(),
                    notification
                ) as User.NinetyTwo
                result.isSuccess
            }
            .flowOn(Dispatchers.IO)
            .catch { e -> Timber.e("editCredential exception $e") }
            .single()
    }

    private suspend fun deviceGetCredential(type:Int, state:Int, index: Int): User.NinetySeven {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0x97,
            key = statefulConnection.key(),
            data = byteArrayOf(type.toByte(), state.toByte()) + index.toLittleEndianByteArrayInt16()
        )

        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "deviceGetCredential type: $type state: $state index: $index")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0x97)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0x97,
                    statefulConnection.key(),
                    notification
                ) as User.NinetySeven
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("deviceGetCredential exception $e")
            }
            .single()
    }

    suspend fun deviceGetCredentialCard(index: Int): User.NinetySeven = deviceGetCredential(2, 1, index)
    suspend fun deviceGetCredentialFingerprint(index: Int): User.NinetySeven = deviceGetCredential(3, 1, index)
    suspend fun deviceGetCredentialFingerVein(index: Int): User.NinetySeven = deviceGetCredential(4, 1 ,index)
    suspend fun deviceGetCredentialFace(index: Int): User.NinetySeven = deviceGetCredential(5, 1 ,index)

    suspend fun deviceExitCredentialCard(index: Int): User.NinetySeven = deviceGetCredential(2, 0, index)
    suspend fun deviceExitCredentialFingerprint(index: Int): User.NinetySeven = deviceGetCredential(3, 0, index)
    suspend fun deviceExitCredentialFingerVein(index: Int): User.NinetySeven = deviceGetCredential(4, 0 ,index)
    suspend fun deviceExitCredentialFace(index: Int): User.NinetySeven = deviceGetCredential(5, 0 ,index)

    private suspend fun deleteCredential(index: Int): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0x98,
            key = statefulConnection.key(),
            data = index.toLittleEndianByteArrayInt16()
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "deleteCredential index: $index")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0x98)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0x98,
                    statefulConnection.key(),
                    notification
                ) as User.NinetyTwo
                result.isSuccess
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("deleteCredential exception $e")
            }
            .single()
    }

    private suspend fun getHash(index: Int): User.NinetyNine {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0x99,
            key = statefulConnection.key(),
            data = byteArrayOf(index.toByte())
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "getHash index: $index")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0x99)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0x99,
                    statefulConnection.key(),
                    notification
                ) as User.NinetyNine
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("getHash exception $e")
            }
            .single()
    }

    private suspend fun hasUnsyncedData(): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0x9A,
            key = statefulConnection.key(),
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "hasUnsyncedData")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0x9A)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0x9A,
                    statefulConnection.key(),
                    notification
                ) as Boolean
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("hasUnsyncedData exception $e")
            }
            .single()
    }

    private suspend fun getUnsyncedData(): User.NinetyB {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0x9B,
            key = statefulConnection.key(),
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "getUnsyncedData")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0x9B)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0x9B,
                    statefulConnection.key(),
                    notification
                ) as User.NinetyB
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("getUnsyncedData exception $e")
            }
            .single()
    }

    private suspend fun setUnsyncedData(type: Int, time: Long, index: Int): User.NinetyC {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val data = bleCmdRepository.combineUserNinetyCCommand(
            User.NinetyB(
                type = type,
                time = time.toInt(),
                index = index
            )
        )
        val command = bleCmdRepository.createCommand(
            function = 0x9C,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "setUnsyncedData data: $data")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0x9C)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0x9B,
                    statefulConnection.key(),
                    notification
                ) as User.NinetyC
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("setUnsyncedData exception $e")
            }
            .single()
    }

    private suspend fun setAllDataSynced(): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0x9D,
            key = statefulConnection.key(),
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "setAllDataSynced")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0x9D)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0x9D,
                    statefulConnection.key(),
                    notification
                ) as Boolean
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("setAllDataSynced exception $e")
            }
            .single()
    }

}