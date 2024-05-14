package com.sunion.core.ble

import androidx.annotation.VisibleForTesting
import com.sunion.core.ble.exception.ConnectionTokenException
import com.sunion.core.ble.entity.DeviceToken.State.VALID_TOKEN
import com.polidea.rxandroidble2.NotificationSetupMode
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.sunion.core.ble.BleCmdRepository.Companion.NOTIFICATION_CHARACTERISTIC
import com.sunion.core.ble.entity.*
import com.sunion.core.ble.exception.NotConnectedException
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class BleHandShakeUseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository
) : UseCase.ExecuteArgument3<LockConnectionInfo, RxBleDevice, RxBleConnection, Observable<String>> {

    var keyTwoString: String = ""
    var permanentTokenString: String = ""
    var permission = DeviceToken.PERMISSION_NONE

    override fun invoke(
        input1: LockConnectionInfo,
        input2: RxBleDevice,
        input3: RxBleConnection
    ): Observable<String> {
        val keyOne = input1.keyOne.hexToByteArray()
        val connectionToken = if (input1.permanentToken.isNullOrBlank()) input1.oneTimeToken.hexToByteArray() else input1.permanentToken!!.hexToByteArray()
        return bleHandshake(
            lockInfo = input1,
            connection = input3,
            keyOne = keyOne,
            token = connectionToken
        )
    }

    fun bleHandshake(
        lockInfo: LockConnectionInfo,
        connection: RxBleConnection,
        keyOne: ByteArray,
        token: ByteArray
    ): Observable<String> {
        return sendC0(connection, keyOne, token)
            .flatMap { keyTwo ->
                keyTwoString = keyTwo.toHexString()
                sendC1(connection, keyTwo, token)
                    .take(1)
                    .filter { it.first == DeviceToken.ONE_TIME_TOKEN || it.first == VALID_TOKEN }
                    .flatMap { stateAndPermission ->
                        when (stateAndPermission.first) {
                            DeviceToken.ONE_TIME_TOKEN -> {
                                when(lockInfo.model){
                                    "KD01" -> {
                                        waitFor(connection, keyTwo, 0x8B)
                                    }
                                    else -> {
                                        waitFor(connection, keyTwo, 0xE5)
                                    }
                                }

                            }
                            VALID_TOKEN -> {
                                permanentTokenString = token.toHexString()
                                Observable.just(stateAndPermission.second)
                            }
                            else -> {
                                Observable.never()
                            }
                        }
                    }
            }
    }

        private fun waitFor(
            connection: RxBleConnection,
            keyTwo: ByteArray,
            function: Int
        ): Observable<String> {
            return connection
                .setupNotification(
                    NOTIFICATION_CHARACTERISTIC,
                    NotificationSetupMode.DEFAULT
                )
                .flatMap { it }
                .filter { notification -> // [E5] will sent from device
                    bleCmdRepository.decrypt(keyTwo, notification)?.let { bytes ->
                        bytes.component3().unSignedInt() == function
                    } ?: false
                }
                .distinct { notification ->
                    bleCmdRepository.decrypt(keyTwo, notification)?.component3()
                        ?.unSignedInt() ?: function
                }
                .map { notification ->
                    val token = extractToken(bleCmdRepository.resolve(function, keyTwo, notification) as DeviceToken.PermanentToken)
                    keyTwo to token
                }
                .doOnNext { pair ->
                    Timber.d("received [${function.toHexString()}] in exchange one time token")
                    val token = pair.second
                    if (token is DeviceToken.PermanentToken) {
                        permanentTokenString = token.token
                        permission = token.permission
                    }
                }
                .flatMap { pair ->
                    val token = pair.second
                    if (token is DeviceToken.PermanentToken) {
                        Observable.just(token.permission)
                    } else Observable.never()
                }
        }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun sendC0(
        rxConnection: RxBleConnection,
        keyOne: ByteArray,
        token: ByteArray,
        function: Int = 0xC0
    ): Observable<ByteArray> {
        return Observable.zip(
            rxConnection.setupNotification(
                NOTIFICATION_CHARACTERISTIC,
                NotificationSetupMode.DEFAULT
            )
                .flatMap { notification -> notification }
                .filter { notification ->
                    val decrypted = bleCmdRepository.decrypt(
                        keyOne,
                        notification
                    )
                    decrypted?.component3()?.unSignedInt() == function
                },
            rxConnection.writeCharacteristic(
                NOTIFICATION_CHARACTERISTIC,
                bleCmdRepository.createCommand(function, keyOne, token)
            ).toObservable(),
            BiFunction { notification: ByteArray, written: ByteArray ->
                val randomNumberOne = bleCmdRepository.resolve(function, keyOne, written) as ByteArray
//                Timber.d("[${function.toHexString()}] has written: ${written.toHexPrint()} notified: ${notification.toHexPrint()}")
                val randomNumberTwo = bleCmdRepository.resolve(function, keyOne, notification) as ByteArray
//                Timber.d("randomNumberTwo: ${randomNumberTwo.toHexPrint()}")
                val keyTwo = generateKeyTwo(
                    randomNumberOne = randomNumberOne,
                    randomNumberTwo = randomNumberTwo
                )
//                Timber.d("keyTwo: ${keyTwo.toHexPrint()}")
                keyTwo
            }
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun generateKeyTwo(randomNumberOne: ByteArray, randomNumberTwo: ByteArray): ByteArray {
        val keyTwo = ByteArray(16)
        for (i in 0..15) keyTwo[i] =
            ((randomNumberOne[i].unSignedInt()) xor (randomNumberTwo[i].unSignedInt())).toByte()
        return keyTwo
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun sendC1(
        rxConnection: RxBleConnection,
        keyTwo: ByteArray,
        token: ByteArray,
        isLockFromSharing: Boolean = false,
        function: Int = 0xC1
    ): Observable<Pair<Int, String>> {
        return Observable.zip(
            rxConnection.setupNotification(
                NOTIFICATION_CHARACTERISTIC,
                NotificationSetupMode.DEFAULT
            )
                .flatMap { notification -> notification }
                .filter { notification ->
                    bleCmdRepository.decrypt(keyTwo, notification)?.component3()
                        ?.unSignedInt() == function
                },
            rxConnection.writeCharacteristic(
                NOTIFICATION_CHARACTERISTIC,
                bleCmdRepository.createCommand(function, keyTwo, token)
            ).toObservable(),
            BiFunction { notification: ByteArray, written: ByteArray ->
                Timber.d("[${function.toHexString()}] has written: ${written.toHexPrint()}")
                Timber.d("[${function.toHexString()}] has notified: ${notification.toHexPrint()}")
                val tokenStateFromDevice = bleCmdRepository.resolve(function, keyTwo, notification) as ByteArray
                Timber.d("token state from device : ${tokenStateFromDevice.toHexPrint()}")
                val deviceToken = determineTokenState(tokenStateFromDevice, isLockFromSharing)
                Timber.d("token state: ${token.toHexPrint()}")
                val permission = determineTokenPermission(tokenStateFromDevice)
                Timber.d("token permission: $permission")
                deviceToken to permission
            }
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun determineTokenState(data: ByteArray, isLockFromSharing: Boolean): Int {
        return when (data.component1().unSignedInt()) {
            0 -> if (isLockFromSharing) throw ConnectionTokenException.LockFromSharingHasBeenUsedException() else throw ConnectionTokenException.IllegalTokenException()
            1 -> DeviceToken.VALID_TOKEN
            // according to documentation, 2 -> the token has been swapped inside the device,
            // hence the one time token no longer valid to connect.
            2 -> if (isLockFromSharing) throw ConnectionTokenException.LockFromSharingHasBeenUsedException() else throw ConnectionTokenException.DeviceRefusedException()
            3 -> DeviceToken.ONE_TIME_TOKEN
            // 0, and else
            else -> if (isLockFromSharing) throw ConnectionTokenException.LockFromSharingHasBeenUsedException() else throw ConnectionTokenException.IllegalTokenStateException()
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun determineTokenPermission(data: ByteArray): String {
        return String(data.copyOfRange(1, 2))
    }

    // byte array equal to [E5] documentation
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun extractToken(user: DeviceToken.PermanentToken): DeviceToken {
        return if (!user.isValid) {
            throw ConnectionTokenException.IllegalTokenException()
        } else if (user.isPermanent) {
            user
        } else {
            DeviceToken.OneTimeToken(user.token)
        }
    }

}
