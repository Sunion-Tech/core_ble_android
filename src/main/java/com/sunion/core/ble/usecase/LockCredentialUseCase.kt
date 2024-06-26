package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.entity.BleV3Lock
import com.sunion.core.ble.entity.Credential
import com.sunion.core.ble.entity.User
import com.sunion.core.ble.exception.NotConnectedException
import com.sunion.core.ble.toAsciiByteArray
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

class LockCredentialUseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    private val className = this::class.simpleName ?: "LockCredentialUseCase"

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

    suspend fun getCredentialByUser(index: Int): Credential.NinetyFiveUser = getCredential(BleV3Lock.CredentialFormat.USER.value, index) as Credential.NinetyFiveUser
    suspend fun getCredentialByCredential(index: Int): Credential.NinetyFiveCredential = getCredential(BleV3Lock.CredentialFormat.CREDENTIAL.value, index) as Credential.NinetyFiveCredential

    private suspend fun getCredential(format: Int, index: Int): Credential {
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
                ) as Credential
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
            }
            .single()
    }

    suspend fun addCredentialCode(index: Int, userIndex:Int, code: String): Boolean = addCredential(userIndex, BleV3Lock.CredentialDetail(index, type = BleV3Lock.CredentialType.PIN.value, code = code.toAsciiByteArray()))
    suspend fun addCredentialCard(index: Int, userIndex:Int, code: ByteArray): Boolean = addCredential(userIndex, BleV3Lock.CredentialDetail(index, type = BleV3Lock.CredentialType.RFID.value, code = code))
    suspend fun addCredentialFingerPrint(index: Int, userIndex:Int): Boolean = addCredential(userIndex, BleV3Lock.CredentialDetail(index, type = BleV3Lock.CredentialType.FINGERPRINT.value, code = index.toLittleEndianByteArrayInt16()))
    suspend fun addCredentialFace(index: Int, userIndex:Int): Boolean = addCredential(userIndex, BleV3Lock.CredentialDetail(index, type = BleV3Lock.CredentialType.FACE.value, code = index.toLittleEndianByteArrayInt16()))

    private suspend fun addCredential(
        userIndex: Int,
        credentialDetail: BleV3Lock.CredentialDetail? = null,
    ): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::addCredential.name
        val function = 0x96
        val data = bleCmdRepository.combineCredential96Cmd(
            Credential.NinetySixCmd(
                action = BleV3Lock.Action.CREATE.value,
                userIndex = userIndex,
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
                ) as Credential.NinetySix
                result.isSuccess
            }
            .flowOn(Dispatchers.IO)
            .catch { e -> Timber.e("$functionName exception $e") }
            .single()
    }

    suspend fun editCredentialCode(index: Int, userIndex:Int, code: String): Boolean = editCredential(userIndex, BleV3Lock.CredentialDetail(index, type = BleV3Lock.CredentialType.PIN.value, code = code.toAsciiByteArray()))
    suspend fun editCredentialCard(index: Int, userIndex:Int, code: ByteArray): Boolean = editCredential(userIndex, BleV3Lock.CredentialDetail(index, type = BleV3Lock.CredentialType.RFID.value, code = code))
    suspend fun editCredentialFingerPrint(index: Int, userIndex:Int): Boolean = editCredential(userIndex, BleV3Lock.CredentialDetail(index, type = BleV3Lock.CredentialType.FINGERPRINT.value, code = index.toLittleEndianByteArrayInt16()))
    suspend fun editCredentialFace(index: Int, userIndex:Int): Boolean = editCredential(userIndex, BleV3Lock.CredentialDetail(index, type = BleV3Lock.CredentialType.FACE.value, code = index.toLittleEndianByteArrayInt16()))

    private suspend fun editCredential(
        userIndex: Int,
        credentialDetail: BleV3Lock.CredentialDetail? = null,
    ): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::editCredential.name
        val function = 0x96
        val data = bleCmdRepository.combineCredential96Cmd(
            Credential.NinetySixCmd(
                action = BleV3Lock.Action.EDIT.value,
                userIndex = userIndex,
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
                ) as Credential.NinetySix
                result.isSuccess
            }
            .flowOn(Dispatchers.IO)
            .catch { e -> Timber.e("$functionName exception $e") }
            .single()
    }

    private suspend fun deviceGetCredential(type:Int, state:Int, index: Int): Credential.NinetySeven {
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
                ) as Credential.NinetySeven
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
            }
            .single()
    }

    suspend fun deviceGetCredentialCard(index: Int): Credential.NinetySeven = deviceGetCredential(BleV3Lock.CredentialType.RFID.value , BleV3Lock.CredentialState.START.value, index)
    suspend fun deviceGetCredentialFingerprint(index: Int): Credential.NinetySeven = deviceGetCredential(BleV3Lock.CredentialType.FINGERPRINT.value,  BleV3Lock.CredentialState.START.value, index)
    suspend fun deviceGetCredentialFace(index: Int): Credential.NinetySeven = deviceGetCredential(BleV3Lock.CredentialType.FACE.value,  BleV3Lock.CredentialState.START.value ,index)

    suspend fun deviceExitCredentialCard(index: Int): Credential.NinetySeven = deviceGetCredential(BleV3Lock.CredentialType.RFID.value,  BleV3Lock.CredentialState.EXIT.value, index)
    suspend fun deviceExitCredentialFingerprint(index: Int): Credential.NinetySeven = deviceGetCredential(BleV3Lock.CredentialType.FINGERPRINT.value,  BleV3Lock.CredentialState.EXIT.value, index)
    suspend fun deviceExitCredentialFace(index: Int): Credential.NinetySeven = deviceGetCredential(BleV3Lock.CredentialType.FACE.value,  BleV3Lock.CredentialState.EXIT.value ,index)

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
                ) as Credential.NinetySix
                result.isSuccess
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
            }
            .single()
    }

}