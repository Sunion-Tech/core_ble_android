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

    suspend fun getUser(index: Int): User.NinetyOne {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::getUser.name
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

}