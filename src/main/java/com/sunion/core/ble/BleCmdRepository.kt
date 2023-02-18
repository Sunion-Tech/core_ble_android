package com.sunion.core.ble

import android.annotation.SuppressLint
import android.util.Base64
import androidx.annotation.VisibleForTesting
import com.sunion.core.ble.entity.*
import com.sunion.core.ble.exception.ConnectionTokenException
import com.sunion.core.ble.exception.LockStatusException
import timber.log.Timber
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class BleCmdRepository @Inject constructor(){

    private val commandSerial = AtomicInteger()

    companion object {
        const val CIPHER_MODE = "AES/ECB/NoPadding"
        val NOTIFICATION_CHARACTERISTIC: UUID = UUID.fromString("de915dce-3539-61ea-ade7-d44a2237601f")
    }

    enum class D6Features(val byte: Int){
        SIZE(13),
        LOCK_DIRECTION (0),
        KEYPRESS_BEEP (1),
        VACATION_MODE (2),
        AUTOLOCK (3),
        AUTOLOCK_DELAY ( 4),
        GUIDING_CODE (5),
        LOCK_STATE (6),
        BATTERY (7),
        LOW_BATTERY (8),
        TIMESTAMP (9)
    }
    enum class Config(val byte: Int){
        SIZE(22),
        LOCK_DIRECTION (0),
        KEYPRESS_BEEP (1),
        VACATION_MODE (2),
        AUTOLOCK (3),
        AUTOLOCK_DELAY ( 4),
        GUIDING_CODE (5),
        LATITUDE_INTEGER (6),
        LATITUDE_DECIMAL (10),
        LONGITUDE_INTEGER (14),
        LONGITUDE_DECIMAL (18)
    }

    @SuppressLint("GetInstance")
    fun encrypt(key: ByteArray, data: ByteArray): ByteArray? {
        Timber.d("key:\n${key.toHex()}")
        return try {
            val cipher: Cipher = Cipher.getInstance(CIPHER_MODE)
            val keySpec = SecretKeySpec(key, "AES")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            val encrypted: ByteArray = cipher.doFinal(data)
            Timber.d("encrypted:\n${encrypted.toHex()}")
            encrypted
        } catch (exception: Exception) {
            Timber.d(exception)
            null
        }
    }

    @SuppressLint("GetInstance")
    fun decrypt(key: ByteArray, data: ByteArray): ByteArray? {
//        Timber.d("key:\n${key.toHex()}")
        return try {
            val cipher: Cipher = Cipher.getInstance(CIPHER_MODE)
            val keySpec = SecretKeySpec(key, "AES")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val original: ByteArray = cipher.doFinal(data)
//            Timber.d("decrypted: \n${original.toHex()}")
            original
        } catch (exception: Exception) {
            Timber.d(exception)
            null
        }
    }

    fun pad(data: ByteArray, padZero: Boolean = false): ByteArray {
        if (data.isEmpty()) throw IllegalArgumentException("Invalid command.")
        val padNumber = 16 - (data.size) % 16
        val padBytes = if (padZero) ByteArray(padNumber) else Random.nextBytes(padNumber)
//        println(padBytes.toHex())
        return if (data.size % 16 == 0) {
            data
        } else {
            data + padBytes
        }
    }

    private fun serialIncrementAndGet(): ByteArray {
        val serial = commandSerial.incrementAndGet()
        val array = ByteArray(2)
        val byteBuffer = ByteBuffer.allocate(4)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        byteBuffer.putInt(serial)
        byteBuffer.flip()
        byteBuffer.get(array)
        return array
    }

    // byte array equal to [E5] documentation
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun extractToken(byteArray: ByteArray): DeviceToken {
        return if (byteArray.component1().unSignedInt() == 0) {
            throw ConnectionTokenException.IllegalTokenException()
        } else {
            Timber.d("[E5]: ${byteArray.toHex()}")
            val isValid = byteArray.component1().unSignedInt() == 1
            val isPermanentToken = byteArray.component2().unSignedInt() == 1
            val isOwnerToken = byteArray.component3().unSignedInt() == 1
            val permission = String(byteArray.copyOfRange(3, 4))
            val token = byteArray.copyOfRange(4, 12)
            if (isPermanentToken) {
                val name = String(byteArray.copyOfRange(12, byteArray.size))
                DeviceToken.PermanentToken(
                    isValid,
                    isPermanentToken,
                    Base64.encodeToString(token, Base64.DEFAULT),
                    isOwnerToken,
                    name,
                    permission
                )
            } else {
                DeviceToken.OneTimeToken(Base64.encodeToString(token, Base64.DEFAULT))
            }
        }
    }

    fun createCommand(
        function: Int,
        key: ByteArray,
        data: ByteArray = byteArrayOf()
    ): ByteArray {
//        Timber.d("create command: [${String.format("%2x", function)}]")
        return when (function) {
            0xC0 -> {
                commandSerial.set(0)
                cmd_c0(serialIncrementAndGet(), key)
            }
            0xC1 -> cmd_c1(serialIncrementAndGet(), key, data)
            0xC7 -> cmd_c7(serialIncrementAndGet(), key, data)
            0xC8 -> cmd_c8(serialIncrementAndGet(), key, data)
            0xCC -> cmd_cc(serialIncrementAndGet(), key)
            0xCE -> cmd_ce(serialIncrementAndGet(), key, data)
            0xF0 -> cmd_f0(serialIncrementAndGet(), key, data)
            0xF1 -> cmd_f1(serialIncrementAndGet(), key, data)
            0xD0 -> cmd_d0(serialIncrementAndGet(), key)
            0xD1 -> cmd_d1(serialIncrementAndGet(), key, data)
            0xD2 -> cmd_d2(serialIncrementAndGet(), key)
            0xD3 -> cmd_d3(serialIncrementAndGet(), key, data)
            0xD4 -> cmd_d4(serialIncrementAndGet(), key)
            0xD5 -> cmd_d5(serialIncrementAndGet(), key, data)
            0xD6 -> cmd_d6(serialIncrementAndGet(), key)
            0xD7 -> cmd_d7(serialIncrementAndGet(), key, data)
            0xD8 -> cmd_d8(serialIncrementAndGet(), key)
            0xD9 -> cmd_d9(serialIncrementAndGet(), key, data)
            0xE0 -> cmd_e0(serialIncrementAndGet(), key)
            0xE1 -> cmd_e1(serialIncrementAndGet(), key, data)
            0xE2 -> cmd_e2(serialIncrementAndGet(), key, data)
            0xE4 -> cmd_e4(serialIncrementAndGet(), key)
            0xE5 -> cmd_e5(serialIncrementAndGet(), key, data)
            0xE6 -> cmd_e6(serialIncrementAndGet(), key, data)
            0xE7 -> cmd_e7(serialIncrementAndGet(), key, data)
            0xE8 -> cmd_e8(serialIncrementAndGet(), key, data)
            0xEA -> cmd_ea(serialIncrementAndGet(), key)
            0xEB -> cmd_eb(serialIncrementAndGet(), key, data)
            0xEC -> cmd_ec(serialIncrementAndGet(), key, data)
            0xED -> cmd_ed(serialIncrementAndGet(), key, data)
            0xEE -> cmd_ee(serialIncrementAndGet(), key, data)
            0xEF -> cmd_ef(serialIncrementAndGet(), key)
            else -> throw IllegalArgumentException("Unknown function")
        }
    }

    fun generateRandomBytes(size: Int): ByteArray = Random.nextBytes(size)

    /**
     * ByteArray [C0] data command, length 16 of random number.
     *
     * @return An encrypted byte array.
     * */
    fun cmd_c0(serial: ByteArray, aesKeyOne: ByteArray): ByteArray {
        if (serial.size != 2) throw IllegalArgumentException("Invalid serial")
        val sendByte = ByteArray(2)
        sendByte[0] = 0xC0.toByte() // function
        sendByte[1] = 0x10 // len
        Timber.d("c0: ${(serial + sendByte).toHex()}")
        return encrypt(aesKeyOne, pad(serial + sendByte + generateRandomBytes(0x10)))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }
    /**
     * ByteArray [C1] data command. To retrieve the token state.
     *
     * @return An encoded byte array of [C1] command.
     * */
    fun cmd_c1(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        token: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xC1.toByte() // function
        sendByte[1] = 0x08 // len=8
//        Timber.d("c1: ${(serial + sendByte).toHex()}")
        return encrypt(aesKeyTwo, pad(serial + sendByte + token)) ?: throw IllegalArgumentException(
            "bytes cannot be null"
        )
    }
    fun cmd_c7(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        code: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(3)
        sendByte[0] = 0xC7.toByte() // function
        sendByte[1] = (code.size + 1).toByte() // len
        sendByte[2] = (code.size).toByte() // code size
//        Timber.d("c7: ${(serial + sendByte + code).toHex()}")
        return encrypt(aesKeyTwo, pad(serial + sendByte + code))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [C8] data command. Add admin code
     *
     * @return An encoded byte array of [C8] command.
     * */
    fun cmd_c8(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        code: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xC8.toByte() // function
        sendByte[1] = (code.size).toByte() // len
        Timber.d("c8: ${(serial + sendByte + code).toHex()}")
        return encrypt(aesKeyTwo, pad(serial + sendByte + code))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [CC] data command. To bolt the lock direction.
     *
     * @return An encoded byte array of [CC] command.
     * */
    fun cmd_cc(
        serial: ByteArray,
        aesKeyTwo: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xCC.toByte() // function
        return encrypt(aesKeyTwo, pad(serial + sendByte)) ?: throw IllegalArgumentException(
            "bytes cannot be null"
        )
    }

    /**
     * ByteArray [CE] data command. Add admin code
     *
     * @return An encoded byte array of [CE] command.
     * */
    fun cmd_ce(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        code: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xCE.toByte() // function
        sendByte[1] = (code.size).toByte() // len
//        Timber.d("ce: ${(serial + sendByte + code).toHex()}")
        return encrypt(aesKeyTwo, pad(serial + sendByte + code))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [D0] data command. Get the lock name.
     *
     * @return An encoded byte array of [D0] command.
     * */
    fun cmd_d0(
        serial: ByteArray,
        aesKeyTwo: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xD0.toByte() // function
        return encrypt(aesKeyTwo, pad(serial + sendByte))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [D1] data command. Change the lock name.
     *
     * @return An encoded byte array of [D1] command.
     * */
    fun cmd_d1(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        name: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xD1.toByte() // function
        sendByte[1] = name.size.toByte() // len
        return encrypt(aesKeyTwo, pad(serial + sendByte + name))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [D2] data command. Get the lock time.
     *
     * @return An encoded byte array of [D2] command.
     * */
    fun cmd_d2(
        serial: ByteArray,
        aesKeyTwo: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xD2.toByte() // function
        return encrypt(aesKeyTwo, pad(serial + sendByte))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [D3] data command. Change the lock name.
     *
     * @return An encoded byte array of [D3] command.
     * */
    fun cmd_d3(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        bytes: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xD3.toByte() // function
        sendByte[1] = 0x04.toByte() // len
        return encrypt(aesKeyTwo, pad(serial + sendByte + bytes))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    fun settingBytes(setting: LockConfig.LockConfigD4): ByteArray {
        val settingBytes = ByteArray(Config.SIZE.byte)
        settingBytes[Config.LOCK_DIRECTION.byte] = 0xA3.toByte()
        settingBytes[Config.KEYPRESS_BEEP.byte] = (if (setting.isSoundOn) 0x01.toByte() else 0x00.toByte())
        settingBytes[Config.VACATION_MODE.byte] = if (setting.isVacationModeOn) 0x01.toByte() else 0x00.toByte()
        settingBytes[Config.AUTOLOCK.byte] = if (setting.isAutoLock) 0x01.toByte() else 0x00.toByte()
        settingBytes[Config.AUTOLOCK_DELAY.byte] = setting.autoLockTime.toByte()
        settingBytes[Config.GUIDING_CODE.byte] = (if (setting.isGuidingCodeOn) 0x01.toByte() else 0x00.toByte())

        val latitudeBigDecimal = BigDecimal.valueOf(setting.latitude ?: 0.0)
        val latitudeIntPartBytes = intToLittleEndianBytes(latitudeBigDecimal.toInt())
        for (i in 0..latitudeIntPartBytes.lastIndex) settingBytes[Config.LATITUDE_INTEGER.byte + i] = latitudeIntPartBytes[i]
        val latitudeDecimalInt = latitudeBigDecimal.subtract(BigDecimal(latitudeBigDecimal.toInt())).scaleByPowerOfTen(9).toInt()
        val latitudeDecimalPartBytes = intToLittleEndianBytes(latitudeDecimalInt)
        for (i in 0..latitudeDecimalPartBytes.lastIndex) settingBytes[Config.LATITUDE_DECIMAL.byte + i] = latitudeDecimalPartBytes[i]
        Timber.d("latitudeBigDecimal: $latitudeBigDecimal, latitudeIntPart: ${latitudeBigDecimal.toInt()}, latitudeDecimalInt: $latitudeDecimalInt")

        val longitudeBigDecimal = BigDecimal.valueOf(setting.longitude ?: 0.0)
        val longitudeIntPartBytes = intToLittleEndianBytes(longitudeBigDecimal.toInt())
        for (i in 0..longitudeIntPartBytes.lastIndex) settingBytes[Config.LONGITUDE_INTEGER.byte + i] = longitudeIntPartBytes[i]
        val longitudeDecimalInt = longitudeBigDecimal.subtract(BigDecimal(longitudeBigDecimal.toInt())).scaleByPowerOfTen(9).toInt()
        val longitudeDecimalPartBytes = intToLittleEndianBytes(longitudeDecimalInt)
        for (i in 0..longitudeDecimalPartBytes.lastIndex) settingBytes[Config.LONGITUDE_DECIMAL.byte + i] = longitudeDecimalPartBytes[i]
        Timber.d("longitudeBigDecimal: $longitudeBigDecimal longitudeBigDecimal: ${longitudeBigDecimal.toInt()}, longitudeDecimalInt: $longitudeDecimalInt")

        return settingBytes
    }

    /**
     * ByteArray [D4] data command. Get the lock setting.
     *
     * @return An encoded byte array of [D4] command.
     * */
    fun cmd_d4(
        serial: ByteArray,
        aesKeyTwo: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xD4.toByte() // function
        return encrypt(aesKeyTwo, pad(serial + sendByte))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [D5] data command. Send the lock setting.
     *
     * @return An encoded byte array of [D5] command.
     * */
    fun cmd_d5(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        data: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xD5.toByte() // function
        sendByte[1] = Config.SIZE.byte.toByte() // function
        return encrypt(aesKeyTwo, pad(serial + sendByte + data))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [D6] data command. Toggle the lock state.
     *
     * @return An encoded byte array of [D6] command.
     * */
    fun cmd_d6(
        serial: ByteArray,
        aesKeyTwo: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xD6.toByte() // function
        return encrypt(aesKeyTwo, pad(serial + sendByte))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [D7] data command. Toggle the lock state.
     *
     * @return An encoded byte array of [D7] command.
     * */
    fun cmd_d7(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        state: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(3)
        sendByte[0] = 0xD7.toByte() // function
        sendByte[1] = 0x01.toByte() // len
        sendByte[2] = state.first()
        return encrypt(aesKeyTwo, pad(serial + sendByte))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [D8] data command. Set a certain timezone to the lock.
     *
     * @return An encoded byte array of [D8] command.
     * */
    fun cmd_d8(
        serial: ByteArray,
        aesKeyTwo: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xD8.toByte() // function
        return encrypt(aesKeyTwo, pad(serial + sendByte))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [D9] data command. Set a certain timezone to the lock.
     *
     * @return An encoded byte array of [D9] command.
     * */
    fun cmd_d9(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        bytes: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xD9.toByte() // function
        sendByte[1] = bytes.size.toByte() // len
        return encrypt(aesKeyTwo, pad(serial + sendByte + bytes))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [E0] data command. Get the quantity of log .
     *
     * @return An encoded byte array of [E0] command.
     * */
    fun cmd_e0(
        serial: ByteArray,
        aesKeyTwo: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xE0.toByte() // function
        return encrypt(aesKeyTwo, pad(serial + sendByte))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [E1] data command. Get the index of log.
     *
     * @return An encoded byte array of [E1] command.
     * */
    fun cmd_e1(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        index: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(3)
        sendByte[0] = 0xE1.toByte() // function
        sendByte[1] = 0x01.toByte() // function
        sendByte[2] = index.first() // function
        return encrypt(aesKeyTwo, pad(serial + sendByte))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [E2] data command. Delete the event log at specific index.
     *
     * @param bytes Index of the event log
     * @return An encoded byte array of [E2] command.
     * */
    fun cmd_e2(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        bytes: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xE2.toByte() // function
        sendByte[1] = bytes.size.toByte() // function
        return encrypt(aesKeyTwo, pad(serial + sendByte + bytes))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [E4] data command. Get the token array, in order to know which slot has a token.
     * The quantity of token is limited to 10.
     *
     * @return An encoded byte array of [E4] command.
     * */
    fun cmd_e4(
        serial: ByteArray,
        aesKeyTwo: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xE4.toByte() // function
        return encrypt(aesKeyTwo, pad(serial + sendByte))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [E5] data command. Query the token by index.
     *
     * @return An encoded byte array of [E5] command.
     * */
    fun cmd_e5(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        bytes: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xE5.toByte() // function
        sendByte[1] = bytes.size.toByte() // function
        return encrypt(aesKeyTwo, pad(serial + sendByte + bytes))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [E6] data command. Generate one-time token.
     *
     * @return An encoded byte array of [E6] command.
     * */
    fun cmd_e6(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        bytes: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xE6.toByte() // function
        sendByte[1] = bytes.size.toByte() // function
        Timber.d("[e6] send bytes: ${(sendByte + bytes).toHex()}")
        return encrypt(aesKeyTwo, pad(serial + sendByte + bytes))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [E7] data command. Update token.
     *
     * @return An encoded byte array of [E7] command.
     * */
    fun cmd_e7(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        bytes: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xE7.toByte() // function
        sendByte[1] = bytes.size.toByte() // function
        return encrypt(aesKeyTwo, pad(serial + sendByte + bytes))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [E8] data command. Delete the user token at specific index.
     *
     * @param bytes Index of the user token
     * @return An encoded byte array of [E8] command.
     * */
    fun cmd_e8(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        bytes: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xE8.toByte() // function
        sendByte[1] = bytes.size.toByte() // function
        return encrypt(aesKeyTwo, pad(serial + sendByte + bytes))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [EA] data command. Query the pin code array.
     *
     * @return An encoded byte array of [EA] command.
     * */
    fun cmd_ea(
        serial: ByteArray,
        aesKeyTwo: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xEA.toByte() // function
        return encrypt(aesKeyTwo, pad(serial + sendByte))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [EB] data command. Query the pin code array.
     *
     * @return An encoded byte array of [EB] command.
     * */
    fun cmd_eb(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        bytes: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(3)
        sendByte[0] = 0xEB.toByte() // function
        sendByte[1] = 0x01.toByte() // function
        sendByte[2] = bytes.component1() // function
        return encrypt(aesKeyTwo, pad(serial + sendByte))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    fun combineUserCodeCommand(
        index: Int,
        isEnabled: Boolean,
        name: String,
        code: String,
        scheduleType: AccessCodeScheduleType
    ): ByteArray {
        val isEnabledByte = byteArrayOf(if (isEnabled) 0x01.toByte() else 0x00.toByte())
        val nameByte = name.toByteArray()
        val codeByte = stringCodeToHex(code)
        val scheduleByte = ByteArray(12)
        scheduleByte[0] = scheduleType.getByteOfType()

        when (scheduleType) {
            is AccessCodeScheduleType.ValidTimeRange -> {
                Timber.d("ValidTimeRange from: ${scheduleType.from.toInt()}, to: ${scheduleType.to.toInt()}")

                val fromTimeByteArray = intToLittleEndianBytes(scheduleType.from.toInt())
                for (i in 0..fromTimeByteArray.lastIndex) scheduleByte[i + 4] = fromTimeByteArray[i]

                val toTimeByteArray = intToLittleEndianBytes(scheduleType.to.toInt())
                for (i in 0..toTimeByteArray.lastIndex) scheduleByte[i + 8] = toTimeByteArray[i]
            }
            is AccessCodeScheduleType.ScheduleEntry -> {
                Timber.d("ScheduleEntry weekdays: ${scheduleType.weekdayBits}, ${Integer.toBinaryString(scheduleType.weekdayBits)}, from: ${scheduleType.from}, to: ${scheduleType.to}")
                val weekBuffer = ByteBuffer.allocate(1)
                weekBuffer.order(ByteOrder.LITTLE_ENDIAN)
                weekBuffer.put(scheduleType.weekdayBits.toByte())
                weekBuffer.flip()
                scheduleByte[1] = weekBuffer.get()

                val fromBuffer = ByteBuffer.allocate(1)
                fromBuffer.order(ByteOrder.LITTLE_ENDIAN)
                fromBuffer.put(scheduleType.from.toByte())
                fromBuffer.flip()
                scheduleByte[2] = fromBuffer.get()

                val toBuffer = ByteBuffer.allocate(1)
                toBuffer.order(ByteOrder.LITTLE_ENDIAN)
                toBuffer.put(scheduleType.to.toByte())
                toBuffer.flip()
                scheduleByte[3] = toBuffer.get()
            }
            else -> {}
        }
        val bytes = byteArrayOf(index.toByte()) + isEnabledByte + codeByte.size.toByte() + codeByte + scheduleByte + nameByte
        Timber.d("Schedule: ${bytes.toHex()}")
        return bytes
    }

    /**
     * ByteArray [EC] data command. Add user code
     *
     * @return An encoded byte array of [EC] command.
     * */
    fun cmd_ec(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        code: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xEC.toByte() // function
        sendByte[1] = (code.size).toByte() // len
        Timber.d("ec: ${(serial + sendByte + code).toHex()}")
        return encrypt(aesKeyTwo, pad(serial + sendByte + code))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [ED] data command. Modify user code
     *
     * @return An encoded byte array of [ED] command.
     * */
    fun cmd_ed(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        code: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xED.toByte() // function
        sendByte[1] = (code.size).toByte() // len
        Timber.d("ed: ${(serial + sendByte + code).toHex()}")
        return encrypt(aesKeyTwo, pad(serial + sendByte + code))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [EE] data command. Delete the pin code array.
     *
     * @return An encoded byte array of [EE] command.
     * */
    fun cmd_ee(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        bytes: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(3)
        sendByte[0] = 0xEE.toByte() // function
        sendByte[1] = 0x01.toByte() // function
        sendByte[2] = bytes.component1() // function
        return encrypt(aesKeyTwo, pad(serial + sendByte))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [EF] data command. Query the lock has admin code or not.
     *
     * @return An encoded byte array of [EF] command.
     * */
    fun cmd_ef(
        serial: ByteArray,
        aesKeyTwo: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xEF.toByte() // function
        return encrypt(aesKeyTwo, pad(serial + sendByte))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    fun cmd_f0(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        code: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xF0.toByte() // function
        sendByte[1] = (code.size).toByte() // len
        Timber.d("f0: ${(serial + sendByte + code).toHex()}")
        return encrypt(aesKeyTwo, pad(serial + sendByte + code))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [F1] data command. Toggle the lock state with user identity id.
     *
     * @return An encoded byte array of [F1] command.
     * */
    fun cmd_f1(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        data: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xF1.toByte() // function
        sendByte[1] = (data.size).toByte() // len
        return encrypt(aesKeyTwo, pad(serial + sendByte + data))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * Resolve [F0] The result of deleting a user code.
     *
     * @param notification Data return from device.
     * @return ByteArray represent token.
     *
     * */
    fun resolveF0(aesKeyTwo: ByteArray, notification: ByteArray): String {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xF0) {
                String(decrypted.copyOfRange(2, decrypted.size - 1))
            } else {
                throw IllegalArgumentException("Return function byte is not [F0]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    fun resolveC0(keyOne: ByteArray, notification: ByteArray): ByteArray {
        return decrypt(keyOne, notification)?.let { decrypted ->
//            Timber.d("[C0] decrypted: ${decrypted.toHex()}")
            if (decrypted.component3().unSignedInt() == 0xC0) {
                return decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt())
            } else {
                throw IllegalArgumentException("Return function byte is not [C0]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }
    fun resolveC1(aesKeyTwo: ByteArray, notification: ByteArray): ByteArray {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
//            Timber.d("[C1] decrypted: ${decrypted.toHex()}")
            if (decrypted.component3().unSignedInt() == 0xC1) {
                return decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt())
            } else {
                throw IllegalArgumentException("Return function byte is not [C1]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }
    fun resolveC7(aesKeyTwo: ByteArray, notification: ByteArray): Boolean {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xC7) {
                when {
                    decrypted.component5().unSignedInt() == 0x01 -> true
                    decrypted.component5().unSignedInt() == 0x00 -> false
                    else -> throw IllegalArgumentException("Unknown data")
                }
            } else {
                throw IllegalArgumentException("Return function byte is not [C7]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }
    /**
     * Resolve [C8] token generated from device.
     *
     * @param notification Data return from device.
     * @return ByteArray represent token.
     *
     * */
    fun resolveC8(aesKeyTwo: ByteArray, notification: ByteArray): Boolean {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xC8) {
                when {
                    decrypted.component5().unSignedInt() == 0x01 -> true
                    decrypted.component5().unSignedInt() == 0x00 -> false
                    else -> throw IllegalArgumentException("Unknown data")
                }
            } else {
                throw IllegalArgumentException("Return function byte is not [C8]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [CE] token generated from device.
     *
     * @param notification Data return from device.
     * @return ByteArray represent token.
     *
     * */
    fun resolveCE(aesKeyTwo: ByteArray, notification: ByteArray): Boolean {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xCE) {
                when {
                    decrypted.component5().unSignedInt() == 0x01 -> true
                    decrypted.component5().unSignedInt() == 0x00 -> false
                    else -> throw IllegalArgumentException("Unknown data")
                }
            } else {
                throw IllegalArgumentException("Return function byte is not [C8]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [D0] Indicate whether the lock name is.
     *
     * @param notification Data return from device.
     * @return ByteArray represent the lock name.
     *
     * */
    fun resolveD0(aesKeyTwo: ByteArray, notification: ByteArray): String {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xD0) {
                return String(
                    decrypted.copyOfRange(
                        4,
                        4 + decrypted.component4().unSignedInt()
                    )
                )
            } else {
                throw IllegalArgumentException("Return function byte is not [D0]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [D1] Indicate whether name has been set or not.
     *
     * @param notification Data return from device.
     * @return ByteArray represent the lock name has been set.
     *
     * */
    fun resolveD1(aesKeyTwo: ByteArray, notification: ByteArray): Boolean {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xD1) {
                when {
                    decrypted.component4().unSignedInt() == 0x01 -> true
                    decrypted.component4().unSignedInt() == 0x00 -> false
                    else -> throw IllegalArgumentException("Unknown data")
                }
            } else {
                throw IllegalArgumentException("Return function byte is not [D1]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [D2] Resolve the time.
     *
     * @param notification Data return from device.
     * @return ByteArray represent token.
     *
     * */
    fun resolveD2(aesKeyTwo: ByteArray, notification: ByteArray): Int {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xD2) {
                val data = decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt())
                return Integer.reverseBytes(ByteBuffer.wrap(data).int)
            } else {
                throw IllegalArgumentException("Return function byte is not [D2]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [D3] The result of setting time.
     *
     * @param notification Data return from device.
     * @return ByteArray represent token.
     *
     * */
    fun resolveD3(aesKeyTwo: ByteArray, notification: ByteArray): Boolean {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xD3) {
                when {
                    decrypted.component5().unSignedInt() == 0x01 -> true
                    decrypted.component5().unSignedInt() == 0x00 -> false
                    else -> throw IllegalArgumentException("Unknown data")
                }
            } else {
                throw IllegalArgumentException("Return function byte is not [D3]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [D4] lock setting.
     *
     * @param notification Data return from device.
     * @return ByteArray represent lock status.
     *
     * */
    fun resolveD4(aesKeyTwo: ByteArray, notification: ByteArray): LockConfig.LockConfigD4 {
        return aesKeyTwo.let { keyTwo ->
            decrypt(keyTwo, notification)?.let { decrypted ->
                if (decrypted.component3().unSignedInt() == 0xD4) {
                    decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt()).let { bytes ->
                        Timber.d("[D4] ${bytes.toHex()}")

                        val latIntPart = Integer.reverseBytes(ByteBuffer.wrap(bytes.copyOfRange(Config.LATITUDE_INTEGER.byte, Config.LATITUDE_DECIMAL.byte)).int)
                        Timber.d("latIntPart: $latIntPart")
                        val latDecimalPart = Integer.reverseBytes(ByteBuffer.wrap(bytes.copyOfRange(Config.LATITUDE_DECIMAL.byte, Config.LONGITUDE_INTEGER.byte)).int)
                        val latDecimal = latDecimalPart.toBigDecimal().movePointLeft(9)
                        Timber.d("latDecimalPart: $latIntPart, latDecimal: $latDecimal")
                        val lat = latIntPart.toBigDecimal().plus(latDecimal)
                        Timber.d("lat: $lat, ${lat.toPlainString()}")

                        val lngIntPart = Integer.reverseBytes(ByteBuffer.wrap(bytes.copyOfRange(Config.LONGITUDE_INTEGER.byte, Config.LONGITUDE_DECIMAL.byte)).int)
                        Timber.d("lngIntPart: $lngIntPart")
                        val lngDecimalPart = Integer.reverseBytes(ByteBuffer.wrap(bytes.copyOfRange(Config.LONGITUDE_DECIMAL.byte, Config.SIZE.byte)).int)
                        val lngDecimal = lngDecimalPart.toBigDecimal().movePointLeft(9)
                        Timber.d("lngIntPart: $lngIntPart, lngDecimal: $lngDecimal")
                        val lng = lngIntPart.toBigDecimal().plus(lngDecimal)
                        Timber.d("lng: $lng, ${lng.toPlainString()}")

                        val lockConfigD4 = LockConfig.LockConfigD4(
                            direction = when (bytes[Config.LOCK_DIRECTION.byte].unSignedInt()) {
                                0xA0 -> LockDirection.Right
                                0xA1 -> LockDirection.Left
                                0xA2 -> LockDirection.NotDetermined
                                else -> throw LockStatusException.LockDirectionException()
                            },
                            isSoundOn = bytes[Config.KEYPRESS_BEEP.byte].unSignedInt() == 0x01,
                            isVacationModeOn = bytes[Config.VACATION_MODE.byte].unSignedInt() == 0x01,
                            isAutoLock = bytes[Config.AUTOLOCK.byte].unSignedInt() == 0x01,
                            autoLockTime = bytes[Config.AUTOLOCK_DELAY.byte].unSignedInt(),
                            isGuidingCodeOn = bytes[Config.GUIDING_CODE.byte].unSignedInt() == 0x01,
                            latitude = lat.toDouble(),
                            longitude = lng.toDouble()
                        )
                        Timber.d("[D4] lockConfig: $lockConfigD4")
                        return lockConfigD4
                    }
                } else {
                    throw IllegalArgumentException("Return function byte is not [D4]")
                }
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [D5] The result of lock setting.
     *
     * @param notification Data return from device.
     * @return ByteArray represent token.
     *
     * */
    fun resolveD5(aesKeyTwo: ByteArray, notification: ByteArray): Boolean {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xD5) {
                when {
                    decrypted.component5().unSignedInt() == 0x01 -> true
                    decrypted.component5().unSignedInt() == 0x00 -> false
                    else -> throw IllegalArgumentException("Unknown data")
                }
            } else {
                throw IllegalArgumentException("Return function byte is not [D5]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [D6] lock status.
     *
     * @param notification Data return from device.
     * @return ByteArray represent lock status.
     *
     * */
    fun resolveD6(aesKeyTwo: ByteArray, notification: ByteArray): DeviceStatus.DeviceStatusD6 {
        return aesKeyTwo.let { keyTwo ->
            decrypt(keyTwo, notification)?.let { decrypted ->
                if (decrypted.component3().unSignedInt() == 0xD6) {
                    decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt()).let { bytes ->
                        val autoLockTime = if (bytes[D6Features.AUTOLOCK_DELAY.byte].unSignedInt() !in 1..90) {
                            1
                        } else {
                            bytes[D6Features.AUTOLOCK_DELAY.byte].unSignedInt()
                        }
                        val lockSetting = DeviceStatus.DeviceStatusD6(
                            config = LockConfig.LockConfigD4(
                                direction = when (bytes[D6Features.LOCK_DIRECTION.byte].unSignedInt()) {
                                    0xA0 -> LockDirection.Right
                                    0xA1 -> LockDirection.Left
                                    0xA2 -> LockDirection.NotDetermined
                                    else -> throw LockStatusException.LockDirectionException()
                                },
                                isSoundOn = bytes[D6Features.KEYPRESS_BEEP.byte].unSignedInt() == 0x01,
                                isVacationModeOn = bytes[D6Features.VACATION_MODE.byte].unSignedInt() == 0x01,
                                isAutoLock = bytes[D6Features.AUTOLOCK.byte].unSignedInt() == 0x01,
                                autoLockTime = autoLockTime,
                                isGuidingCodeOn = bytes[D6Features.GUIDING_CODE.byte].unSignedInt() == 0x01
                            ),
                            lockState = when (bytes[D6Features.LOCK_STATE.byte].unSignedInt()) {
                                0 -> LockState.LOCKED
                                1 -> LockState.UNLOCKED
                                else -> LockState.UNKNOWN
                            },
                            battery = bytes[D6Features.BATTERY.byte].unSignedInt(),
                            batteryState = when (bytes[D6Features.LOW_BATTERY.byte].unSignedInt()) {
                                0 -> BatteryState.BATTERY_GOOD
                                1 -> BatteryState.BATTERY_LOW
                                else -> BatteryState.BATTERY_ALERT
                            },
                            timestamp = Integer.reverseBytes(ByteBuffer.wrap(bytes.copyOfRange(D6Features.TIMESTAMP.byte, D6Features.SIZE.byte)).int).toLong()
                        )
                        return lockSetting
                    }
                } else {
                    throw IllegalArgumentException("Return function byte is not [D6]")
                }
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [D9] The result of lock setting.
     *
     * @param notification Data return from device.
     * @return ByteArray represent token.
     *
     * */
    fun resolveD9(aesKeyTwo: ByteArray, notification: ByteArray): Boolean {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xD9) {
                when {
                    decrypted.component5().unSignedInt() == 0x01 -> true
                    decrypted.component5().unSignedInt() == 0x00 -> false
                    else -> throw IllegalArgumentException("Unknown data")
                }
            } else {
                throw IllegalArgumentException("Return function byte is not [D9]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [E0] The quantity of log.
     *
     * @param notification Data return from device.
     * @return ByteArray represent token.
     *
     * */
    fun resolveE0(aesKeyTwo: ByteArray, notification: ByteArray): Int {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xE0) {
                val quantity =
                    decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt()).first()
                        .unSignedInt()
                Timber.d("quantity: $quantity")
                return quantity
            } else {
                throw IllegalArgumentException("Return function byte is not [E0]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [E1] Get the user code at specific index.
     *
     * @param notification Data return from device.
     * @return ByteArray represent a user code setting.
     *
     * */
    fun resolveE1(aesKeyTwo: ByteArray, notification: ByteArray): EventLog {
        return aesKeyTwo.let { keyTwo ->
            decrypt(keyTwo, notification)?.let { decrypted ->
                if (decrypted.component3().unSignedInt() == 0xE1) {
                    val dataLength = decrypted.component4().unSignedInt()
                    val data = decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt())
                    Timber.d("[E1] dataLength: $dataLength, ${data.toHex()}")

                    val timestamp =
                        Integer.reverseBytes(ByteBuffer.wrap(data.copyOfRange(0, 4)).int).toLong()
                    val event = data.component5().unSignedInt()
                    val name = data.copyOfRange(5, data.size)
                    val log = EventLog(
                        eventTimeStamp = timestamp,
                        event = event,
                        name = String(name)
                    )
                    Timber.d("[E1] read log from device: $log")
                    log
                } else {
                    throw IllegalArgumentException("Return function byte is not [E1]")
                }
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [E2] The result of deleting a log.
     *
     * @param notification Data return from device.
     * @return ByteArray represent token.
     *
     * */
    fun resolveE2(aesKeyTwo: ByteArray, notification: ByteArray): Boolean {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xE2) {
                when {
                    decrypted.component5().unSignedInt() == 0x01 -> true
                    decrypted.component5().unSignedInt() == 0x00 -> false
                    else -> throw IllegalArgumentException("Unknown data")
                }
            } else {
                throw IllegalArgumentException("Return function byte is not [E2]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [E4] Get the token array, in order to know which slot has a token.
     *
     * @param notification Data return from device.
     * @return ByteArray represent whether the lock has admin code or not.
     *
     * */
    fun resolveE4(aesKeyTwo: ByteArray, notification: ByteArray): ByteArray {
        return aesKeyTwo.let { keyTwo ->
            decrypt(keyTwo, notification)?.let { decrypted ->
                if (decrypted.component3().unSignedInt() == 0xE4) {
                    decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt())
                } else {
                    throw IllegalArgumentException("Return function byte is not [E4]")
                }
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [E5] token generated from device.
     *
     * @param notification Data return from device.
     * @return ByteArray represent token.
     *
     * */
    fun resolveE5(notification: ByteArray): ByteArray {
        return if (notification.component3().unSignedInt() == 0xE5) {
            notification.copyOfRange(4, 4 + notification.component4().unSignedInt())
        } else {
            throw IllegalArgumentException("Return function byte is not [E5]")
        }
    }


    /**
     * Same with [resolveE5], but return a [User] data class for presentation purpose,
     * without any sensitive information.
     *
     * @param notification Data return from device.
     * @return ByteArray represent token.
     *
     * */
    fun resolveUser(aesKeyTwo: ByteArray, notification: ByteArray): DeviceToken.PermanentToken {
        return aesKeyTwo.let { keyTwo ->
            decrypt(keyTwo, notification)?.let { decrypted ->
                if (decrypted.component3().unSignedInt() == 0xE5) {
                    val data = decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt())
                    val user = DeviceToken.PermanentToken(
                        isValid = data.component1().unSignedInt() == 1,
                        isPermanent = data.component2().unSignedInt() == 1,
                        isOwner = data.component3().unSignedInt() == 1,
                        name = String(data.copyOfRange(12, data.size)),
                        permission = String(data.copyOfRange(3, 4)),
                        token = Base64.encodeToString(data.copyOfRange(4, 12), Base64.DEFAULT)
                    )
                    Timber.d("user: $user")
                    user
                } else {
                    throw IllegalArgumentException("Return function byte is not [E5]")
                }
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [E6] One-time token command result from device.
     *
     * @param notification Data return from device.
     * @return ByteArray represent token.
     *
     * */
    fun resolveE6(aesKeyTwo: ByteArray, notification: ByteArray): AddUserResponse {
        return aesKeyTwo.let { keyTwo ->
            decrypt(keyTwo, notification)?.let { decrypted ->
                if (decrypted.component3().unSignedInt() == 0xE6) {
                    val data = decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt())
                    Timber.d("[E6] ${data.toHex()}")
                    val isSuccessful = data.component1().unSignedInt() == 0x01
                    val tokenIndexInDevice = data.component2().unSignedInt()
                    val tokenBytes = data.copyOfRange(2, data.size)
                    Timber.d("token bytes: ${tokenBytes.toHex()}")
                    val token = Base64.encodeToString(tokenBytes, Base64.DEFAULT)
                    val response = AddUserResponse(
                        isSuccessful,
                        tokenIndexInDevice,
                        token
                    )
                    Timber.d("add user response: $response, token: $token")
                    return response
                } else {
                    throw IllegalArgumentException("Return function byte is not [E6]")
                }
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [E7] One-time token command result from device.
     *
     * @param notification Data return from device.
     * @return ByteArray represent token.
     *
     * */
    fun resolveE7(aesKeyTwo: ByteArray, notification: ByteArray): UpdateTokenResponse {
        return aesKeyTwo.let { keyTwo ->
            decrypt(keyTwo, notification)?.let { decrypted ->
                if (decrypted.component3().unSignedInt() == 0xE7) {
                    val data = decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt())

                    Timber.d("")
                    val isSuccessful = data.component1().unSignedInt() == 0x01
                    val tokenIndexInDevice = data.component2().unSignedInt()
                    val isPermanent = data.component3().unSignedInt() == 0x01
                    val permission = String(data.copyOfRange(3, 4))
                    val tokenBytes = data.copyOfRange(4, 12)
                    val name = String(data.copyOfRange(12, data.size))
                    Timber.d("token bytes: ${tokenBytes.toHex()}")
                    val token = Base64.encodeToString(tokenBytes, Base64.DEFAULT)
                    val response = UpdateTokenResponse(
                        isSuccessful,
                        tokenIndexInDevice,
                        isPermanent,
                        permission,
                        token,
                        name
                    )
                    Timber.d("update token response: $response, token: $token")
                    return response
                } else {
                    throw IllegalArgumentException("Return function byte is not [E7]")
                }
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [E8] The result of deleting a user.
     *
     * @param notification Data return from device.
     * @return ByteArray represent token.
     *
     * */
    fun resolveE8(aesKeyTwo: ByteArray, notification: ByteArray): Boolean {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xE8) {
                when {
                    decrypted.component5().unSignedInt() == 0x01 -> true
                    decrypted.component5().unSignedInt() == 0x00 -> false
                    else -> throw IllegalArgumentException("Unknown data")
                }
            } else {
                throw IllegalArgumentException("Return function byte is not [E8]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [EA] Get the pin code array.
     * The bit in each byte indicates that whether the position contains a pin code or not,
     * and in the following order [7, 6, 5, 4, 3, 2, 1, 0]、[15, 14, 13, 12, 11...], etc.
     *
     * @param notification Data return from device.
     * @return ByteArray represent whether the lock has admin code or not.
     *
     * */
    fun resolveEa(aesKeyTwo: ByteArray, notification: ByteArray): ByteArray {
        return aesKeyTwo.let { keyTwo ->
            decrypt(keyTwo, notification)?.let { decrypted ->
                if (decrypted.component3().unSignedInt() == 0xEA) {
                    decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt())
                } else {
                    throw IllegalArgumentException("Return function byte is not [EA]")
                }
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [EB] Get the user code at specific index.
     *
     * @param notification Data return from device.
     * @return ByteArray represent a user code setting.
     *
     * */
    fun resolveEb(aesKeyTwo: ByteArray, notification: ByteArray): AccessCode {
        return aesKeyTwo.let { keyTwo ->
            decrypt(keyTwo, notification)?.let { decrypted ->
                if (decrypted.component3().unSignedInt() == 0xEB) {
                    val dataLength = decrypted.component4().unSignedInt()
                    val data = decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt())
                    Timber.d("[EB] dataLength: $dataLength, ${data.toHex()}")
                    val pinCodeLength = data.component2().unSignedInt()
                    val pinCode = data.copyOfRange(2, 2 + pinCodeLength)
                    val code = pinCode.map { it.unSignedInt().toString() }
                        .joinToString(separator = "") { it }
                    val scheduleData = data.copyOfRange(2 + pinCodeLength, 2 + pinCodeLength + 12)
                    val name = data.copyOfRange(2 + pinCodeLength + 12, dataLength)

                    val type = String(byteArrayOf(scheduleData.component1()))
                    val weekdays = scheduleData.component2().unSignedInt()
                    val fromTime = scheduleData.component3().unSignedInt()
                    val toTime = scheduleData.component4().unSignedInt()
                    val scheduleFrom =
                        Integer.reverseBytes(ByteBuffer.wrap(scheduleData.copyOfRange(4, 8)).int)
                    val scheduleTo =
                        Integer.reverseBytes(ByteBuffer.wrap(scheduleData.copyOfRange(8, 12)).int)
                    val userCode = AccessCode(
                        index = 999,
                        isEnable = data.component1().unSignedInt() == 0x01,
                        code = code,
                        scheduleType = type,
                        weekDays = weekdays,
                        from = fromTime,
                        to = toTime,
                        scheduleFrom = scheduleFrom,
                        scheduleTo = scheduleTo,
                        name = String(name)
                    )
                    Timber.d("[EB] read userCode from device: $userCode")
                    userCode
                } else {
                    throw IllegalArgumentException("Return function byte is not [EB]")
                }
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [EC] The result of adding a user code.
     *
     * @param notification Data return from device.
     * @return ByteArray represent token.
     *
     * */
    fun resolveEc(aesKeyTwo: ByteArray, notification: ByteArray): Boolean {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xEC) {
                when {
                    decrypted.component5().unSignedInt() == 0x01 -> true
                    decrypted.component5().unSignedInt() == 0x00 -> false
                    else -> throw IllegalArgumentException("Unknown data")
                }
            } else {
                throw IllegalArgumentException("Return function byte is not [EC]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [ED] The result of modifying a user code.
     *
     * @param notification Data return from device.
     * @return ByteArray represent token.
     *
     * */
    fun resolveEd(aesKeyTwo: ByteArray, notification: ByteArray): Boolean {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xED) {
                when {
                    decrypted.component5().unSignedInt() == 0x01 -> true
                    decrypted.component5().unSignedInt() == 0x00 -> false
                    else -> throw IllegalArgumentException("Unknown data")
                }
            } else {
                throw IllegalArgumentException("Return function byte is not [ED]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [EE] The result of deleting a user code.
     *
     * @param notification Data return from device.
     * @return ByteArray represent token.
     *
     * */
    fun resolveEe(aesKeyTwo: ByteArray, notification: ByteArray): Boolean {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xEE) {
                when {
                    decrypted.component5().unSignedInt() == 0x01 -> true
                    decrypted.component5().unSignedInt() == 0x00 -> false
                    else -> throw IllegalArgumentException("Unknown data")
                }
            } else {
                throw IllegalArgumentException("Return function byte is not [EE]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [EF] Indicate whether the lock has admin code or not.
     *
     * @param notification Data return from device.
     * @return ByteArray represent whether the lock has admin code or not.
     *
     * */
    fun resolveEf(aesKeyTwo: ByteArray, notification: ByteArray): Boolean {
        return aesKeyTwo.let { keyTwo ->
            decrypt(keyTwo, notification)?.let { decrypted ->
                Timber.d("<-- de:${decrypted.toHex()} hasAdminCodeBeenSetByBle")
                if (decrypted.component3().unSignedInt() == 0xEF) {
                    when {
                        decrypted.component5().unSignedInt() == 0x01 -> true
                        decrypted.component5().unSignedInt() == 0x00 -> false
                        else -> throw IllegalArgumentException("Unknown data")
                    }
                } else {
                    throw IllegalArgumentException("Return function byte is not [EF]")
                }
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    fun stringCodeToHex(code: String): ByteArray {
        return code.takeIf { it.isNotBlank() }
            ?.filter { it.isDigit() }
            ?.map { Character.getNumericValue(it).toByte() }
            ?.toByteArray()
            ?: throw IllegalArgumentException("Invalid user code string")
    }

    fun generateKeyTwoThen(
        randomNumberOne: ByteArray,
        randomNumberTwo: ByteArray,
        function: (ByteArray) -> Unit
    ) {
        val keyTwo = ByteArray(16)
        for (i in 0..15) keyTwo[i] =
            ((randomNumberOne[i].unSignedInt()) xor (randomNumberTwo[i].unSignedInt())).toByte()
        function.invoke(keyTwo)
    }

    fun generateKeyTwo(randomNumberOne: ByteArray, randomNumberTwo: ByteArray): ByteArray {
        val keyTwo = ByteArray(16)
        for (i in 0..15) keyTwo[i] =
            ((randomNumberOne[i].unSignedInt()) xor (randomNumberTwo[i].unSignedInt())).toByte()
        return keyTwo
    }

    fun determineTokenPermission(data: ByteArray): String {
        return String(data.copyOfRange(1, 2))
    }


    fun determineTokenState(data: ByteArray, isLockFromSharing: Boolean): Int {
        return when (data.component1().unSignedInt()) {
            0 -> if (isLockFromSharing) throw ConnectionTokenException.LockFromSharingHasBeenUsedException() else throw ConnectionTokenException.IllegalTokenException()
            1 -> DeviceToken.VALID_TOKEN
            // according to documentation, 2 -> the token has been swapped inside the device,
            // hence the one time token no longer valid to connect.
            2 -> if (isLockFromSharing) throw ConnectionTokenException.LockFromSharingHasBeenUsedException() else throw ConnectionTokenException.DeviceRefusedException()
            3 -> DeviceToken.ONE_TIME_TOKEN
//             0, and else
            else -> if (isLockFromSharing) throw ConnectionTokenException.LockFromSharingHasBeenUsedException() else throw ConnectionTokenException.IllegalTokenStateException()
        }
    }

    fun intToLittleEndianBytes(int: Int): ByteArray {
        val bytes = ByteArray(4)
        val byteBuffer = ByteBuffer.allocate(4)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        byteBuffer.putInt(int)
        byteBuffer.flip()
        byteBuffer.get(bytes)
        return bytes
    }

}

fun Byte.unSignedInt() = this.toInt() and 0xFF