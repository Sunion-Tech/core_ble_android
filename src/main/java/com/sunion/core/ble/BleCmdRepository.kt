package com.sunion.core.ble

import android.annotation.SuppressLint
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
    enum class A2Features(val byte: Int){
        SIZE(8),
        LOCK_DIRECTION (0),
        VACATION_MODE (1),
        DEAD_BOLT (2),
        DOOR_STATE ( 3),
        LOCK_STATE (4),
        SECURITY_BOLT (5),
        BATTERY (6),
        LOW_BATTERY (7)
    }
    enum class ConfigD4(val byte: Int){
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
    enum class ConfigA0(val byte: Int){
        SIZE(32),
        LATITUDE_INTEGER (0),
        LATITUDE_DECIMAL (4),
        LONGITUDE_INTEGER (8),
        LONGITUDE_DECIMAL (12),
        LOCK_DIRECTION (16),
        GUIDING_CODE (17),
        VIRTUAL_CODE (18),
        TWO_FA (19),
        VACATION_MODE (20),
        AUTOLOCK (21),
        AUTOLOCK_DELAY ( 22),
        AUTOLOCK_DELAY_LOWER_LIMIT (24),
        AUTOLOCK_DELAY_UPPER_LIMIT (26),
        OPERATING_SOUND (28),
        SOUND_TYPE (29),
        SOUND_VALUE (30),
        SHOW_FAST_TRACK_MODE (31)
    }
    enum class ConfigA1(val byte: Int){
        SIZE(28),
        LATITUDE_INTEGER (0),
        LATITUDE_DECIMAL (4),
        LONGITUDE_INTEGER (8),
        LONGITUDE_DECIMAL (12),
        LOCK_DIRECTION (16),
        GUIDING_CODE (17),
        VIRTUAL_CODE (18),
        TWO_FA (19),
        VACATION_MODE (20),
        AUTOLOCK (21),
        AUTOLOCK_DELAY ( 22),
        OPERATING_SOUND (24),
        SOUND_TYPE (25),
        SOUND_VALUE (26),
        SHOW_FAST_TRACK_MODE (27)
    }

    @SuppressLint("GetInstance")
    fun encrypt(key: ByteArray, data: ByteArray): ByteArray? {
        Timber.d("key:\n${key.toHexPrint()} \ndata:\n" +
                data.toHexPrint()
        )
        return try {
            val cipher: Cipher = Cipher.getInstance(CIPHER_MODE)
            val keySpec = SecretKeySpec(key, "AES")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            val encrypted: ByteArray = cipher.doFinal(data)
            Timber.d("encrypted:\n${encrypted.toHexPrint()}")
            encrypted
        } catch (exception: Exception) {
            Timber.d(exception)
            null
        }
    }

    @SuppressLint("GetInstance")
    fun decrypt(key: ByteArray, data: ByteArray): ByteArray? {
//        Timber.d("key:\n${key.toHexPrint()}")
        return try {
            val cipher: Cipher = Cipher.getInstance(CIPHER_MODE)
            val keySpec = SecretKeySpec(key, "AES")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val original: ByteArray = cipher.doFinal(data)
//            Timber.d("decrypted: \n${original.toHexPrint()}")
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
//        println(padBytes.toHexPrint())
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
            Timber.d("[E5]: ${byteArray.toHexPrint()}")
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
                    isOwnerToken,
                    permission,
                    token.toHexString(),
                    name,
                )
            } else {
                DeviceToken.OneTimeToken(token.toHexString())
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
            0xA0 -> cmd_a0(serialIncrementAndGet(), key)
            0xA1 -> cmd_a1(serialIncrementAndGet(), key, data)
            0xA2 -> cmd_a2(serialIncrementAndGet(), key)
            0xA3 -> cmd_a3(serialIncrementAndGet(), key, data)
            0xA4 -> cmd_a4(serialIncrementAndGet(), key)
            0xA5 -> cmd_a5(serialIncrementAndGet(), key, data)
            0xA6 -> cmd_a6(serialIncrementAndGet(), key, data)
            0xA7 -> cmd_a7(serialIncrementAndGet(), key, data)
            0xA8 -> cmd_a8(serialIncrementAndGet(), key, data)
            0xA9 -> cmd_a9(serialIncrementAndGet(), key, data)
            0xAA -> cmd_aa(serialIncrementAndGet(), key, data)
            0xB0 -> cmd_b0(serialIncrementAndGet(), key)
            0xB1 -> cmd_b1(serialIncrementAndGet(), key, data)
            0xC1 -> cmd_c1(serialIncrementAndGet(), key, data)
            0xC3 -> cmd_c3(serialIncrementAndGet(), key, data)
            0xC4 -> cmd_c4(serialIncrementAndGet(), key, data)
            0xC7 -> cmd_c7(serialIncrementAndGet(), key, data)
            0xC8 -> cmd_c8(serialIncrementAndGet(), key, data)
            0xCC -> cmd_cc(serialIncrementAndGet(), key)
            0xCE -> cmd_ce(serialIncrementAndGet(), key, data)
            0xCF -> cmd_cf(serialIncrementAndGet(), key)
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
            0xF0 -> cmd_f0(serialIncrementAndGet(), key, data)
            0xF1 -> cmd_f1(serialIncrementAndGet(), key, data)
            else -> throw IllegalArgumentException("Unknown function")
        }
    }

    fun generateRandomBytes(size: Int): ByteArray = Random.nextBytes(size)

    /**
     * ByteArray [B0] data command. Get the plug status.
     *
     * @return An encoded byte array of [B0] command.
     * */
    fun cmd_b0(
        serial: ByteArray,
        aesKeyTwo: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xB0.toByte() // function
        return encrypt(aesKeyTwo, pad(serial + sendByte))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [B1] data command. Set the plug status.
     *
     * @return An encoded byte array of [B1] command.
     * */
    fun cmd_b1(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        name: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xB1.toByte() // function
        sendByte[1] = name.size.toByte() // len
        return encrypt(aesKeyTwo, pad(serial + sendByte + name))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

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
        Timber.d("c0: ${(serial + sendByte).toHexPrint()}")
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
//        Timber.d("c1: ${(serial + sendByte).toHexPrint()}")
        return encrypt(aesKeyTwo, pad(serial + sendByte + token)) ?: throw IllegalArgumentException(
            "bytes cannot be null"
        )
    }

    fun cmd_c3(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        code: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xC3.toByte() // function
        sendByte[1] = (code.size).toByte() // len
        Timber.d("c3: ${(serial + sendByte + code).toHexPrint()}")
        return encrypt(aesKeyTwo, pad(serial + sendByte + code))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    fun cmd_c4(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        code: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xC4.toByte() // function
        sendByte[1] = code.size.toByte() // len
        return encrypt(aesKeyTwo, pad(serial + sendByte + code))
            ?: throw IllegalArgumentException("bytes cannot be null")
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
//        Timber.d("c7: ${(serial + sendByte + code).toHexPrint()}")
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
        Timber.d("c8: ${(serial + sendByte + code).toHexPrint()}")
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
//        Timber.d("ce: ${(serial + sendByte + code).toHexPrint()}")
        return encrypt(aesKeyTwo, pad(serial + sendByte + code))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [CF] data command. Factory reset
     *
     * @return An encoded byte array of [CF] command.
     * */
    fun cmd_cf(
        serial: ByteArray,
        aesKeyTwo: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xCF.toByte() // function
        return encrypt(aesKeyTwo, pad(serial + sendByte)) ?: throw IllegalArgumentException(
            "bytes cannot be null"
        )
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

    fun settingBytesD4(setting: LockConfig.D4): ByteArray {
        val settingBytes = ByteArray(ConfigD4.SIZE.byte)
        settingBytes[ConfigD4.LOCK_DIRECTION.byte] = 0xA3.toByte()
        settingBytes[ConfigD4.KEYPRESS_BEEP.byte] = (if (setting.isSoundOn) 0x01.toByte() else 0x00.toByte())
        settingBytes[ConfigD4.VACATION_MODE.byte] = if (setting.isVacationModeOn) 0x01.toByte() else 0x00.toByte()
        settingBytes[ConfigD4.AUTOLOCK.byte] = if (setting.isAutoLock) 0x01.toByte() else 0x00.toByte()
        settingBytes[ConfigD4.AUTOLOCK_DELAY.byte] = setting.autoLockTime.toByte()
        settingBytes[ConfigD4.GUIDING_CODE.byte] = (if (setting.isGuidingCodeOn) 0x01.toByte() else 0x00.toByte())

        val latitudeBigDecimal = BigDecimal.valueOf(setting.latitude ?: 0.0)
        val latitudeIntPartBytes = latitudeBigDecimal.toInt().toLittleEndianByteArray()
        for (i in 0..latitudeIntPartBytes.lastIndex) settingBytes[ConfigD4.LATITUDE_INTEGER.byte + i] = latitudeIntPartBytes[i]
        val latitudeDecimalInt = latitudeBigDecimal.subtract(BigDecimal(latitudeBigDecimal.toInt())).scaleByPowerOfTen(9).toInt()
        val latitudeDecimalPartBytes = latitudeDecimalInt.toLittleEndianByteArray()
        for (i in 0..latitudeDecimalPartBytes.lastIndex) settingBytes[ConfigD4.LATITUDE_DECIMAL.byte + i] = latitudeDecimalPartBytes[i]
        Timber.d("latitudeBigDecimal: $latitudeBigDecimal, latitudeIntPart: ${latitudeBigDecimal.toInt()}, latitudeDecimalInt: $latitudeDecimalInt")

        val longitudeBigDecimal = BigDecimal.valueOf(setting.longitude ?: 0.0)
        val longitudeIntPartBytes = longitudeBigDecimal.toInt().toLittleEndianByteArray()
        for (i in 0..longitudeIntPartBytes.lastIndex) settingBytes[ConfigD4.LONGITUDE_INTEGER.byte + i] = longitudeIntPartBytes[i]
        val longitudeDecimalInt = longitudeBigDecimal.subtract(BigDecimal(longitudeBigDecimal.toInt())).scaleByPowerOfTen(9).toInt()
        val longitudeDecimalPartBytes = longitudeDecimalInt.toLittleEndianByteArray()
        for (i in 0..longitudeDecimalPartBytes.lastIndex) settingBytes[ConfigD4.LONGITUDE_DECIMAL.byte + i] = longitudeDecimalPartBytes[i]
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
        sendByte[1] = ConfigD4.SIZE.byte.toByte() // function
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
        Timber.d("[e6] send bytes: ${(sendByte + bytes).toHexPrint()}")
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
        scheduleType: AccessScheduleType
    ): ByteArray {
        val isEnabledByte = byteArrayOf(if (isEnabled) 0x01.toByte() else 0x00.toByte())
        val nameByte = name.toByteArray()
        val codeByte = stringCodeToHex(code)
        val scheduleByte = ByteArray(12)
        scheduleByte[0] = scheduleType.getByteOfType()

        when (scheduleType) {
            is AccessScheduleType.ValidTimeRange -> {
                Timber.d("ValidTimeRange from: ${scheduleType.from}, to: ${scheduleType.to}")

                val fromTimeByteArray = scheduleType.from.limitValidTimeRange().toLittleEndianByteArrayInt32()
                for (i in 0..fromTimeByteArray.lastIndex) scheduleByte[i + 4] = fromTimeByteArray[i]

                val toTimeByteArray = scheduleType.to.limitValidTimeRange().toLittleEndianByteArrayInt32()
                for (i in 0..toTimeByteArray.lastIndex) scheduleByte[i + 8] = toTimeByteArray[i]
            }
            is AccessScheduleType.ScheduleEntry -> {
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
        Timber.d("Schedule: ${bytes.toHexPrint()}")
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
        Timber.d("ec: ${(serial + sendByte + code).toHexPrint()}")
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
        Timber.d("ed: ${(serial + sendByte + code).toHexPrint()}")
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
        Timber.d("f0: ${(serial + sendByte + code).toHexPrint()}")
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
     * ByteArray [A0] data command. Toggle the lock state.
     *
     * @return An encoded byte array of [A0] command.
     * */
    fun cmd_a0(
        serial: ByteArray,
        aesKeyTwo: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xA0.toByte() // function
        return encrypt(aesKeyTwo, pad(serial + sendByte))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [A1] data command. Send the lock setting.
     *
     * @return An encoded byte array of [A1] command.
     * */
    fun cmd_a1(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        data: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xA1.toByte() // function
        sendByte[1] = ConfigA1.SIZE.byte.toByte() // function
        return encrypt(aesKeyTwo, pad(serial + sendByte + data))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [A2] data command. Toggle the lock state.
     *
     * @return An encoded byte array of [A2] command.
     * */
    fun cmd_a2(
        serial: ByteArray,
        aesKeyTwo: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xA2.toByte() // function
        return encrypt(aesKeyTwo, pad(serial + sendByte))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [A3] data command. Toggle the lock state.
     *
     * @return An encoded byte array of [A3] command.
     * */
    fun cmd_a3(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        state: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(4)
        sendByte[0] = 0xA3.toByte() // function
        sendByte[1] = 0x02.toByte() // len
        sendByte[2] = state.component1()
        sendByte[3] = state.component2()
        Timber.d(sendByte.toHexPrint())
        return encrypt(aesKeyTwo, pad(serial + sendByte))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [A4] data command. Get lock supported unlock types.
     *
     * @return An encoded byte array of [A4] command.
     * */
    fun cmd_a4(
        serial: ByteArray,
        aesKeyTwo: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xA4.toByte() // function
        return encrypt(aesKeyTwo, pad(serial + sendByte))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [A5] data command. Query access array.
     *
     * @return An encoded byte array of [A5] command.
     * */
    fun cmd_a5(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        data: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xA5.toByte() // function
        sendByte[1] = 0x01.toByte() // len
        return encrypt(aesKeyTwo, pad(serial + sendByte + data))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [A6] data command. Query access.
     *
     * @return An encoded byte array of [A6] command.
     * */
    fun cmd_a6(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        data: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xA6.toByte() // function
        sendByte[1] = 0x03.toByte() // len
        return encrypt(aesKeyTwo, pad(serial + sendByte + data))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    fun combineAccessA7Command(
        accessA7Cmd: Access.A7Cmd
    ): ByteArray {
        val typeByte = byteArrayOf(accessA7Cmd.type.toByte())
        val indexByte = accessA7Cmd.index.toLittleEndianByteArrayInt16()
        val isEnabledByte = byteArrayOf(if (accessA7Cmd.isEnable) 0x01.toByte() else 0x00.toByte())
        val scheduleByte = ByteArray(12)
        scheduleByte[0] = accessA7Cmd.scheduleType.getByteOfType()
        val nameLenByte = accessA7Cmd.nameLen.toByte()
        val nameByte = accessA7Cmd.name.toByteArray()
        val codeByte = accessA7Cmd.code
        Timber.d("combineAccessA7Command codeByte: ${codeByte.toHexPrint()}")

        when (accessA7Cmd.scheduleType) {
            is AccessScheduleType.ValidTimeRange -> {
                Timber.d("ValidTimeRange from: ${accessA7Cmd.scheduleType.from}, to: ${accessA7Cmd.scheduleType.to}")

                val fromTimeByteArray = accessA7Cmd.scheduleType.from.limitValidTimeRange().toLittleEndianByteArrayInt32()
                for (i in 0..fromTimeByteArray.lastIndex) scheduleByte[i + 4] = fromTimeByteArray[i]

                val toTimeByteArray = accessA7Cmd.scheduleType.to.limitValidTimeRange().toLittleEndianByteArrayInt32()
                for (i in 0..toTimeByteArray.lastIndex) scheduleByte[i + 8] = toTimeByteArray[i]
            }
            is AccessScheduleType.ScheduleEntry -> {
                Timber.d("ScheduleEntry weekdays: ${accessA7Cmd.scheduleType.weekdayBits}, ${Integer.toBinaryString(accessA7Cmd.scheduleType.weekdayBits)}, from: ${accessA7Cmd.scheduleType.from}, to: ${accessA7Cmd.scheduleType.to}")
                val weekBuffer = ByteBuffer.allocate(1)
                weekBuffer.order(ByteOrder.LITTLE_ENDIAN)
                weekBuffer.put(accessA7Cmd.scheduleType.weekdayBits.toByte())
                weekBuffer.flip()
                scheduleByte[1] = weekBuffer.get()

                val fromBuffer = ByteBuffer.allocate(1)
                fromBuffer.order(ByteOrder.LITTLE_ENDIAN)
                fromBuffer.put(accessA7Cmd.scheduleType.from.toByte())
                fromBuffer.flip()
                scheduleByte[2] = fromBuffer.get()

                val toBuffer = ByteBuffer.allocate(1)
                toBuffer.order(ByteOrder.LITTLE_ENDIAN)
                toBuffer.put(accessA7Cmd.scheduleType.to.toByte())
                toBuffer.flip()
                scheduleByte[3] = toBuffer.get()
            }
            else -> {}
        }
        val data = typeByte + indexByte + isEnabledByte + scheduleByte + nameLenByte + nameByte + codeByte
        Timber.d("Schedule: ${data.toHexPrint()}")
        return data
    }

    /**
     * ByteArray [A7] data command. Add access.
     *
     * @return An encoded byte array of [A7] command.
     * */
    fun cmd_a7(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        data: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xA7.toByte() // function
        sendByte[1] = (data.size).toByte() // len
        Timber.d("a7: ${(serial + sendByte + data).toHexPrint()}")
        return encrypt(aesKeyTwo, pad(serial + sendByte + data))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [A8] data command. Modify access.
     *
     * @return An encoded byte array of [A8] command.
     * */
    fun cmd_a8(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        data: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xA8.toByte() // function
        sendByte[1] = (data.size).toByte() // len
        Timber.d("a8: ${(serial + sendByte + data).toHexPrint()}")
        return encrypt(aesKeyTwo, pad(serial + sendByte + data))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [A9] data command. Device get access.
     *
     * @return An encoded byte array of [A9] command.
     * */
    fun cmd_a9(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        data: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xA9.toByte() // function
        sendByte[1] = 0x04.toByte() // len
        return encrypt(aesKeyTwo, pad(serial + sendByte + data))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [AA] data command. Delete access.
     *
     * @return An encoded byte array of [AA] command.
     * */
    fun cmd_aa(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        data: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xAA.toByte() // function
        sendByte[1] = 0x03.toByte() // len
        return encrypt(aesKeyTwo, pad(serial + sendByte + data))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * Resolve [B0] Get plug status.
     *
     * @param notification Data return from device.
     * @return ByteArray represent the plug status.
     *
     * */
    fun resolveB0(aesKeyTwo: ByteArray, notification: ByteArray): DeviceStatus.B0 {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xB0) {
                val data = decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt())
                Timber.d("[B0] ${data.toHexPrint()}")
                val setWifi  = data.component1()
                val connectWifi  = data.component2()
                val plugStatus  = data.component3()
                val response = DeviceStatus.B0(
                    setWifi.toInt(),
                    connectWifi.toInt(),
                    plugStatus.toInt(),
                )
                Timber.d("BleV2Lock.PlugStatus response: $response")
                return response
            } else {
                throw IllegalArgumentException("Return function byte is not [B0]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    fun resolveC0(keyOne: ByteArray, notification: ByteArray): ByteArray {
        return decrypt(keyOne, notification)?.let { decrypted ->
//            Timber.d("[C0] decrypted: ${decrypted.toHexPrint()}")
            if (decrypted.component3().unSignedInt() == 0xC0) {
                return decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt())
            } else {
                throw IllegalArgumentException("Return function byte is not [C0]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }
    fun resolveC1(aesKeyTwo: ByteArray, notification: ByteArray): ByteArray {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
//            Timber.d("[C1] decrypted: ${decrypted.toHexPrint()}")
            if (decrypted.component3().unSignedInt() == 0xC1) {
                return decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt())
            } else {
                throw IllegalArgumentException("Return function byte is not [C1]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }
    /**
     * Resolve [C3] Set OTA status.
     *
     * @param notification Data return from device.
     * @return ByteArray represent the OTA status.
     *
     * */
    fun resolveC3(aesKeyTwo: ByteArray, notification: ByteArray): BleV2Lock.OTAStatus {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xC3) {
                val data = decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt())
                Timber.d("[C3] ${data.toHexPrint()}")
                val target  = data.component1()
                val state  = data.component2()
                val isSuccess  = data.component3()
                val response = BleV2Lock.OTAStatus(
                    target.toInt(),
                    state.toInt(),
                    isSuccess.toInt(),
                )
                Timber.d("BleV2Lock.OTAStatus response: $response")
                return response
            } else {
                throw IllegalArgumentException("Return function byte is not [C3]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [C4] Transfer OTA data.
     *
     * @param notification Data return from device.
     * @return ByteArray represent data first byte move to last byte.
     *
     * */
    fun resolveC4(aesKeyTwo: ByteArray, notification: ByteArray): String {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xC4) {
                return decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt()).toHexPrint()
            } else {
                throw IllegalArgumentException("Return function byte is not [C4]")
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
     * Resolve [CF] token generated from device.
     *
     * @param notification Data return from device.
     * @return ByteArray represent token.
     *
     * */
    fun resolveCF(aesKeyTwo: ByteArray, notification: ByteArray): Boolean {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xCF) {
                when {
                    decrypted.component5().unSignedInt() == 0x01 -> true
                    decrypted.component5().unSignedInt() == 0x00 -> false
                    else -> throw IllegalArgumentException("Unknown data")
                }
            } else {
                throw IllegalArgumentException("Return function byte is not [CF]")
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
                return data.toInt()
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
    fun resolveD4(aesKeyTwo: ByteArray, notification: ByteArray): LockConfig.D4 {
        return aesKeyTwo.let { keyTwo ->
            decrypt(keyTwo, notification)?.let { decrypted ->
                if (decrypted.component3().unSignedInt() == 0xD4) {
                    decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt()).let { bytes ->
                        Timber.d("[D4] ${bytes.toHexPrint()}")

                        val latIntPart = bytes.copyOfRange(ConfigD4.LATITUDE_INTEGER.byte, ConfigD4.LATITUDE_DECIMAL.byte).toInt()
                        Timber.d("latIntPart: $latIntPart")
                        val latDecimalPart = bytes.copyOfRange(ConfigD4.LATITUDE_DECIMAL.byte, ConfigD4.LONGITUDE_INTEGER.byte).toInt()
                        val latDecimal = latDecimalPart.toBigDecimal().movePointLeft(9)
                        Timber.d("latDecimalPart: $latIntPart, latDecimal: $latDecimal")
                        val lat = latIntPart.toBigDecimal().plus(latDecimal)
                        Timber.d("lat: $lat, ${lat.toPlainString()}")

                        val lngIntPart = bytes.copyOfRange(ConfigD4.LONGITUDE_INTEGER.byte, ConfigD4.LONGITUDE_DECIMAL.byte).toInt()
                        Timber.d("lngIntPart: $lngIntPart")
                        val lngDecimalPart = bytes.copyOfRange(ConfigD4.LONGITUDE_DECIMAL.byte, ConfigD4.SIZE.byte).toInt()
                        val lngDecimal = lngDecimalPart.toBigDecimal().movePointLeft(9)
                        Timber.d("lngIntPart: $lngIntPart, lngDecimal: $lngDecimal")
                        val lng = lngIntPart.toBigDecimal().plus(lngDecimal)
                        Timber.d("lng: $lng, ${lng.toPlainString()}")

                        val lockConfigD4 = LockConfig.D4(
                            direction = when (bytes[ConfigD4.LOCK_DIRECTION.byte].unSignedInt()) {
                                0xA0 -> LockDirection.Right
                                0xA1 -> LockDirection.Left
                                0xA2 -> LockDirection.NotDetermined
                                else -> throw LockStatusException.LockDirectionException()
                            },
                            isSoundOn = bytes[ConfigD4.KEYPRESS_BEEP.byte].unSignedInt() == 0x01,
                            isVacationModeOn = bytes[ConfigD4.VACATION_MODE.byte].unSignedInt() == 0x01,
                            isAutoLock = bytes[ConfigD4.AUTOLOCK.byte].unSignedInt() == 0x01,
                            autoLockTime = bytes[ConfigD4.AUTOLOCK_DELAY.byte].unSignedInt(),
                            isGuidingCodeOn = bytes[ConfigD4.GUIDING_CODE.byte].unSignedInt() == 0x01,
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
    fun resolveD6(aesKeyTwo: ByteArray, notification: ByteArray): DeviceStatus.D6 {
        return aesKeyTwo.let { keyTwo ->
            decrypt(keyTwo, notification)?.let { decrypted ->
                if (decrypted.component3().unSignedInt() == 0xD6) {
                    decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt()).let { bytes ->
                        val autoLockTime = if (bytes[D6Features.AUTOLOCK_DELAY.byte].unSignedInt() !in 1..90) {
                            1
                        } else {
                            bytes[D6Features.AUTOLOCK_DELAY.byte].unSignedInt()
                        }
                        val lockSetting = DeviceStatus.D6(
                            config = LockConfig.D4(
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
                                0 -> LockState.UNLOCKED
                                1 -> LockState.LOCKED
                                else -> LockState.UNKNOWN
                            },
                            battery = bytes[D6Features.BATTERY.byte].unSignedInt(),
                            batteryState = when (bytes[D6Features.LOW_BATTERY.byte].unSignedInt()) {
                                0 -> BatteryState.BATTERY_GOOD
                                1 -> BatteryState.BATTERY_LOW
                                else -> BatteryState.BATTERY_ALERT
                            },
                            timestamp = bytes.copyOfRange(D6Features.TIMESTAMP.byte, D6Features.SIZE.byte).toInt().toLong()
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
                    Timber.d("[E1] dataLength: $dataLength, ${data.toHexPrint()}")

                    val timestamp = data.copyOfRange(0, 4).toInt().toLong()
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
                        permission = String(data.copyOfRange(3, 4)),
                        token = data.copyOfRange(4, 12).toHexString(),
                        name = String(data.copyOfRange(12, data.size))
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
                    Timber.d("[E6] ${data.toHexPrint()}")
                    val isSuccessful = data.component1().unSignedInt() == 0x01
                    val tokenIndexInDevice = data.component2().unSignedInt()
                    val tokenBytes = data.copyOfRange(2, data.size)
                    Timber.d("token bytes: ${tokenBytes.toHexPrint()}")
                    val token = tokenBytes.toHexString()
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
                    Timber.d("token bytes: ${tokenBytes.toHexPrint()}")
                    val token = tokenBytes.toHexString()
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
    fun resolveEb(aesKeyTwo: ByteArray, notification: ByteArray): Access.Code {
        return aesKeyTwo.let { keyTwo ->
            decrypt(keyTwo, notification)?.let { decrypted ->
                if (decrypted.component3().unSignedInt() == 0xEB) {
                    val dataLength = decrypted.component4().unSignedInt()
                    val data = decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt())
                    Timber.d("[EB] dataLength: $dataLength, ${data.toHexPrint()}")
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
                    val scheduleFrom = scheduleData.copyOfRange(4, 8).toLong()
                    val scheduleTo = scheduleData.copyOfRange(8, 12).toLong()
                    val userCode = Access.Code(
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
                Timber.d("<-- de:${decrypted.toHexPrint()} hasAdminCodeBeenSetByBle")
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

    /**
     * Resolve [A0] lock setting.
     *
     * @param notification Data return from device.
     * @return ByteArray represent lock status.
     *
     * */
    fun resolveA0(aesKeyTwo: ByteArray, notification: ByteArray): LockConfig.A0 {
        return aesKeyTwo.let { keyTwo ->
            decrypt(keyTwo, notification)?.let { decrypted ->
                if (decrypted.component3().unSignedInt() == 0xA0) {
                    decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt()).let { bytes ->
                        Timber.d("[A0] ${bytes.toHexPrint()}")

                        val autoLockTimeInt = bytes.copyOfRange(ConfigA0.AUTOLOCK_DELAY.byte,ConfigA0.AUTOLOCK_DELAY_LOWER_LIMIT.byte).toInt()
                        Timber.d("autoLockTimeInt: $autoLockTimeInt")
                        val autoLockTimeLowerLimitInt = bytes.copyOfRange(ConfigA0.AUTOLOCK_DELAY_LOWER_LIMIT.byte,ConfigA0.AUTOLOCK_DELAY_UPPER_LIMIT.byte).toInt()
                        Timber.d("autoLockTimeLowerLimitInt: $autoLockTimeLowerLimitInt")
                        val autoLockTimeUpperLimitInt = bytes.copyOfRange(ConfigA0.AUTOLOCK_DELAY_UPPER_LIMIT.byte,ConfigA0.OPERATING_SOUND.byte).toInt()
                        Timber.d("autoLockTimeUpperLimitInt: $autoLockTimeUpperLimitInt")

                        val latIntPart = bytes.copyOfRange(ConfigA0.LATITUDE_INTEGER.byte, ConfigA0.LATITUDE_DECIMAL.byte).toInt()
                        Timber.d("latIntPart: $latIntPart")
                        val latDecimalPart = bytes.copyOfRange(ConfigA0.LATITUDE_DECIMAL.byte, ConfigA0.LONGITUDE_INTEGER.byte).toInt()
                        val latDecimal = latDecimalPart.toBigDecimal().movePointLeft(9)
                        Timber.d("latDecimalPart: $latIntPart, latDecimal: $latDecimal")
                        val lat = latIntPart.toBigDecimal().plus(latDecimal)
                        Timber.d("lat: $lat, ${lat.toPlainString()}")

                        val lngIntPart = bytes.copyOfRange(ConfigA0.LONGITUDE_INTEGER.byte, ConfigA0.LONGITUDE_DECIMAL.byte).toInt()
                        Timber.d("lngIntPart: $lngIntPart")
                        val lngDecimalPart = bytes.copyOfRange(ConfigA0.LONGITUDE_DECIMAL.byte, ConfigA0.LOCK_DIRECTION.byte).toInt()
                        val lngDecimal = lngDecimalPart.toBigDecimal().movePointLeft(9)
                        Timber.d("lngIntPart: $lngIntPart, lngDecimal: $lngDecimal")
                        val lng = lngIntPart.toBigDecimal().plus(lngDecimal)
                        Timber.d("lng: $lng, ${lng.toPlainString()}")

                        val lockConfigA0 = LockConfig.A0(
                            latitude = lat.toDouble(),
                            longitude = lng.toDouble(),
                            direction = when (bytes[ConfigA0.LOCK_DIRECTION.byte].unSignedInt()) {
                                0xA0 -> BleV2Lock.Direction.RIGHT.value
                                0xA1 -> BleV2Lock.Direction.LEFT.value
                                0xA2 -> BleV2Lock.Direction.UNKNOWN.value
                                else -> BleV2Lock.Direction.NOT_SUPPORT.value
                            },
                            guidingCode = when (bytes[ConfigA0.GUIDING_CODE.byte].unSignedInt()) {
                                0 -> BleV2Lock.GuidingCode.CLOSE.value
                                1 -> BleV2Lock.GuidingCode.OPEN.value
                                else -> BleV2Lock.GuidingCode.NOT_SUPPORT.value
                            },
                            virtualCode = when (bytes[ConfigA0.VIRTUAL_CODE.byte].unSignedInt()) {
                                0 -> BleV2Lock.VirtualCode.CLOSE.value
                                1 -> BleV2Lock.VirtualCode.OPEN.value
                                else -> BleV2Lock.VirtualCode.NOT_SUPPORT.value
                            },
                            twoFA = when (bytes[ConfigA0.TWO_FA.byte].unSignedInt()) {
                                0 -> BleV2Lock.TwoFA.CLOSE.value
                                1 -> BleV2Lock.TwoFA.OPEN.value
                                else -> BleV2Lock.TwoFA.NOT_SUPPORT.value
                            },
                            vacationMode = when (bytes[ConfigA0.VACATION_MODE.byte].unSignedInt()) {
                                0 -> BleV2Lock.VacationMode.CLOSE.value
                                1 -> BleV2Lock.VacationMode.OPEN.value
                                else -> BleV2Lock.VacationMode.NOT_SUPPORT.value
                            },
                            autoLock = when (bytes[ConfigA0.AUTOLOCK.byte].unSignedInt()) {
                                0 -> BleV2Lock.AutoLock.CLOSE.value
                                1 -> BleV2Lock.AutoLock.OPEN.value
                                else -> BleV2Lock.AutoLock.NOT_SUPPORT.value
                            },
                            autoLockTime = when (autoLockTimeInt) {
                                0xFFFF -> BleV2Lock.AutoLockTime.NOT_SUPPORT.value
                                else -> {
                                    autoLockTimeInt
                                }
                            },
                            autoLockTimeLowerLimit = when (autoLockTimeLowerLimitInt) {
                                0xFFFF -> BleV2Lock.AutoLockTimeLowerLimit.NOT_SUPPORT.value
                                else -> {
                                    autoLockTimeLowerLimitInt
                                }
                            },
                            autoLockTimeUpperLimit = when (autoLockTimeUpperLimitInt) {
                                0xFFFF -> BleV2Lock.AutoLockTimeUpperLimit.NOT_SUPPORT.value
                                else -> {
                                    autoLockTimeUpperLimitInt
                                }
                            },
                            operatingSound = when (bytes[ConfigA0.OPERATING_SOUND.byte].unSignedInt()) {
                                0 -> BleV2Lock.OperatingSound.CLOSE.value
                                1 -> BleV2Lock.OperatingSound.OPEN.value
                                else -> BleV2Lock.OperatingSound.NOT_SUPPORT.value
                            },
                            soundType = when (bytes[ConfigA0.SOUND_TYPE.byte].unSignedInt()) {
                                0x01 -> BleV2Lock.SoundType.ON_OFF.value
                                0x02 -> BleV2Lock.SoundType.LEVEL.value
                                0x03 -> BleV2Lock.SoundType.PERCENTAGE.value
                                else -> BleV2Lock.SoundType.NOT_SUPPORT.value
                            },
                            soundValue = when (bytes[ConfigA0.SOUND_TYPE.byte].unSignedInt()) {
                                0x01 -> if (bytes[ConfigA0.SOUND_VALUE.byte].unSignedInt() == 100) BleV2Lock.SoundValue.OPEN.value else BleV2Lock.SoundValue.CLOSE.value
                                0x02 -> when (bytes[ConfigA0.SOUND_VALUE.byte].unSignedInt()) {
                                    100 -> BleV2Lock.SoundValue.HIGH_VOICE.value
                                    50 -> BleV2Lock.SoundValue.LOW_VOICE.value
                                    else -> BleV2Lock.SoundValue.CLOSE.value
                                }
                                0x03 -> bytes[ConfigA0.SOUND_VALUE.byte].unSignedInt()
                                else -> BleV2Lock.SoundValue.NOT_SUPPORT.value
                            },
                            showFastTrackMode = when (bytes[ConfigA0.SHOW_FAST_TRACK_MODE.byte].unSignedInt()) {
                                0 -> BleV2Lock.ShowFastTrackMode.CLOSE.value
                                1 -> BleV2Lock.ShowFastTrackMode.OPEN.value
                                else -> BleV2Lock.ShowFastTrackMode.NOT_SUPPORT.value
                            },
                        )
                        Timber.d("[A0] lockConfig: $lockConfigA0")
                        return lockConfigA0
                    }
                } else {
                    throw IllegalArgumentException("Return function byte is not [A0]")
                }
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    fun settingBytesA1(setting: LockConfig.A0): ByteArray {
        val settingBytes = ByteArray(ConfigA1.SIZE.byte)

        val latitudeBigDecimal = BigDecimal.valueOf(setting.latitude ?: 0.0)
        val latitudeIntPartBytes = latitudeBigDecimal.toInt().toLittleEndianByteArray()
        for (i in 0..latitudeIntPartBytes.lastIndex) settingBytes[ConfigA1.LATITUDE_INTEGER.byte + i] =
            latitudeIntPartBytes[i]
        val latitudeDecimalInt =
            latitudeBigDecimal.subtract(BigDecimal(latitudeBigDecimal.toInt())).scaleByPowerOfTen(9)
                .toInt()
        val latitudeDecimalPartBytes = latitudeDecimalInt.toLittleEndianByteArray()
        for (i in 0..latitudeDecimalPartBytes.lastIndex) settingBytes[ConfigA1.LATITUDE_DECIMAL.byte + i] =
            latitudeDecimalPartBytes[i]
        Timber.d("latitudeBigDecimal: $latitudeBigDecimal, latitudeIntPart: ${latitudeBigDecimal.toInt()}, latitudeDecimalInt: $latitudeDecimalInt")

        val longitudeBigDecimal = BigDecimal.valueOf(setting.longitude ?: 0.0)
        val longitudeIntPartBytes = longitudeBigDecimal.toInt().toLittleEndianByteArray()
        for (i in 0..longitudeIntPartBytes.lastIndex) settingBytes[ConfigA1.LONGITUDE_INTEGER.byte + i] =
            longitudeIntPartBytes[i]
        val longitudeDecimalInt =
            longitudeBigDecimal.subtract(BigDecimal(longitudeBigDecimal.toInt()))
                .scaleByPowerOfTen(9).toInt()
        val longitudeDecimalPartBytes = longitudeDecimalInt.toLittleEndianByteArray()
        for (i in 0..longitudeDecimalPartBytes.lastIndex) settingBytes[ConfigA1.LONGITUDE_DECIMAL.byte + i] =
            longitudeDecimalPartBytes[i]
        Timber.d("longitudeBigDecimal: $longitudeBigDecimal longitudeBigDecimal: ${longitudeBigDecimal.toInt()}, longitudeDecimalInt: $longitudeDecimalInt")

        Timber.d("LockConfig.A0: $setting")
        settingBytes[ConfigA1.LOCK_DIRECTION.byte] = setting.direction.toByte()
        settingBytes[ConfigA1.GUIDING_CODE.byte] = setting.guidingCode.toByte()
        settingBytes[ConfigA1.VIRTUAL_CODE.byte] = setting.virtualCode.toByte()
        settingBytes[ConfigA1.TWO_FA.byte] = setting.twoFA.toByte()
        settingBytes[ConfigA1.VACATION_MODE.byte] = setting.vacationMode.toByte()
        settingBytes[ConfigA1.AUTOLOCK.byte] = setting.autoLock.toByte()
        val autoLockDelayBytes = setting.autoLockTime.toLittleEndianByteArrayInt16()
        for (i in 0..autoLockDelayBytes.lastIndex) settingBytes[ConfigA1.AUTOLOCK_DELAY.byte + i] = autoLockDelayBytes[i]
        settingBytes[ConfigA1.OPERATING_SOUND.byte] = setting.operatingSound.toByte()
        settingBytes[ConfigA1.SOUND_TYPE.byte] = setting.soundType.toByte()
        settingBytes[ConfigA1.SOUND_VALUE.byte] = setting.soundValue.toByte()
        settingBytes[ConfigA1.SHOW_FAST_TRACK_MODE.byte] = setting.showFastTrackMode.toByte()
        Timber.d("settingBytesA1: ${settingBytes.toHexPrint()}")

        return settingBytes
    }

    /**
     * Resolve [A1] The result of lock setting.
     *
     * @param notification Data return from device.
     * @return ByteArray represent token.
     *
     * */
    fun resolveA1(aesKeyTwo: ByteArray, notification: ByteArray): Boolean {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xA1) {
                when {
                    decrypted.component5().unSignedInt() == 0x01 -> true
                    decrypted.component5().unSignedInt() == 0x00 -> false
                    else -> throw IllegalArgumentException("Unknown data")
                }
            } else {
                throw IllegalArgumentException("Return function byte is not [A1]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [A2] lock status.
     *
     * @param notification Data return from device.
     * @return ByteArray represent lock status.
     *
     * */
    fun resolveA2(aesKeyTwo: ByteArray, notification: ByteArray): DeviceStatus.A2 {
        return aesKeyTwo.let { keyTwo ->
            decrypt(keyTwo, notification)?.let { decrypted ->
                if (decrypted.component3().unSignedInt() == 0xA2) {
                    decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt()).let { bytes ->
                        val lockSetting = DeviceStatus.A2(
                            direction = when (bytes[A2Features.LOCK_DIRECTION.byte].unSignedInt()) {
                                0xA0 -> BleV2Lock.Direction.RIGHT.value
                                0xA1 -> BleV2Lock.Direction.LEFT.value
                                0xA2 -> BleV2Lock.Direction.UNKNOWN.value
                                else -> BleV2Lock.Direction.NOT_SUPPORT.value
                            },
                            vacationMode = when (bytes[A2Features.VACATION_MODE.byte].unSignedInt()) {
                                0 -> BleV2Lock.VacationMode.CLOSE.value
                                1 -> BleV2Lock.VacationMode.OPEN.value
                                else -> BleV2Lock.VacationMode.NOT_SUPPORT.value
                            },
                            deadBolt = when (bytes[A2Features.DEAD_BOLT.byte].unSignedInt()) {
                                0 -> BleV2Lock.DeadBolt.NOT_PROTRUDE.value
                                1 -> BleV2Lock.DeadBolt.PROTRUDE.value
                                else -> BleV2Lock.DeadBolt.NOT_SUPPORT.value
                            },
                            doorState = when (bytes[A2Features.DOOR_STATE.byte].unSignedInt()) {
                                0 -> BleV2Lock.DoorState.OPEN.value
                                1 -> BleV2Lock.DoorState.CLOSE.value
                                else -> BleV2Lock.DoorState.NOT_SUPPORT.value
                            },
                            lockState = when (bytes[A2Features.LOCK_STATE.byte].unSignedInt()) {
                                0 -> BleV2Lock.LockState.UNLOCKED.value
                                1 -> BleV2Lock.LockState.LOCKED.value
                                else -> BleV2Lock.LockState.UNKNOWN.value
                            },
                            securityBolt = when (bytes[A2Features.SECURITY_BOLT.byte].unSignedInt()) {
                                0 -> BleV2Lock.SecurityBolt.NOT_PROTRUDE.value
                                1 -> BleV2Lock.SecurityBolt.PROTRUDE.value
                                else -> BleV2Lock.SecurityBolt.NOT_SUPPORT.value
                            },
                            battery = bytes[A2Features.BATTERY.byte].unSignedInt(),
                            batteryState = when (bytes[A2Features.LOW_BATTERY.byte].unSignedInt()) {
                                0 -> BleV2Lock.BatteryState.NORMAL.value
                                1 -> BleV2Lock.BatteryState.WEAK_CURRENT.value
                                else -> BleV2Lock.BatteryState.DANGEROUS.value
                            }
                        )
                        Timber.d("resolveA2: $lockSetting")
                        return lockSetting
                    }
                } else {
                    throw IllegalArgumentException("Return function byte is not [A2]")
                }
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [A4] get lock supported unlock types.
     *
     * @param notification Data return from device.
     * @return ByteArray represent supported unlock types.
     *
     * */
    fun resolveA4(aesKeyTwo: ByteArray, notification: ByteArray): BleV2Lock.SupportedUnlockType {
        return aesKeyTwo.let { keyTwo ->
            decrypt(keyTwo, notification)?.let { decrypted ->
                if (decrypted.component3().unSignedInt() == 0xA4) {
                    val data = decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt())
                    Timber.d("[A4] ${data.toHexPrint()}")
                    val accessCodeQuantity  = data.copyOfRange(0, 2)
                    val accessCardQuantity  = data.copyOfRange(2, 4)
                    val fingerprintQuantity  = data.copyOfRange(4, 6)
                    val faceQuantity  = data.copyOfRange(6, 8)
                    val response = BleV2Lock.SupportedUnlockType(
                        accessCodeQuantity.toInt(),
                        accessCardQuantity.toInt(),
                        fingerprintQuantity.toInt(),
                        faceQuantity.toInt()
                    )
                    Timber.d("BleV2Lock.SupportedUnlockType response: $response")
                    return response
                } else {
                    throw IllegalArgumentException("Return function byte is not [A4]")
                }
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [A5] Get the access array.
     * The bit in each byte indicates that whether the position contains a pin code or not,
     * and in the following order [7, 6, 5, 4, 3, 2, 1, 0]、[15, 14, 13, 12, 11...], etc.
     *
     * @param notification Data return from device.
     * @return ByteArray represent whether the lock has admin code or not.
     *
     * */
    fun resolveA5(aesKeyTwo: ByteArray, notification: ByteArray): Access.A5 {
        return aesKeyTwo.let { keyTwo ->
            decrypt(keyTwo, notification)?.let { decrypted ->
                if (decrypted.component3().unSignedInt() == 0xA5) {
                    val dataLen = decrypted.component4().unSignedInt()
                    val data = decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt())
                    Timber.d("[A5] dataLen: $dataLen, data: ${data.toHexPrint()}")
                    val type = data.component1().unSignedInt()
                    val transferComplete = data.component2().unSignedInt()
                    val dataByteArray = data.copyOfRange(2, dataLen)
                    val accessA5 = Access.A5(type, transferComplete, dataByteArray)
                    Timber.d("[A5] read access from device: $accessA5")
                    accessA5
                } else {
                    throw IllegalArgumentException("Return function byte is not [A5]")
                }
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [A6] Get the access at specific index.
     *
     * @param notification Data return from device.
     * @return ByteArray represent a accessA6.
     *
     * */
    fun resolveA6(aesKeyTwo: ByteArray, notification: ByteArray): Access.A6 {
        return aesKeyTwo.let { keyTwo ->
            decrypt(keyTwo, notification)?.let { decrypted ->
                if (decrypted.component3().unSignedInt() == 0xA6) {
                    val dataLen = decrypted.component4().unSignedInt()
                    val data = decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt())
                    Timber.d("[A6] dataLen: $dataLen data: ${data.toHexPrint()}")
                    val type = data.component1().unSignedInt()
                    val index = data.copyOfRange(1, 3).toInt()
                    val isEnable = data.component4().unSignedInt() == 0x01
                    val scheduleData = data.copyOfRange(4, 16)
                    val nameLen = data[16].unSignedInt()
                    val name = String(data.copyOfRange(17, 17 + nameLen))
                    val accessCode = data.copyOfRange(17 + nameLen, dataLen)
                    val code = accessCode.map { it.unSignedInt().toString() }
                        .joinToString(separator = "") { it }
                    val scheduleType = String(byteArrayOf(scheduleData.component1()))
                    val weekdays = scheduleData.component2().unSignedInt()
                    val fromTime = scheduleData.component3().unSignedInt()
                    val toTime = scheduleData.component4().unSignedInt()
                    val scheduleFrom = scheduleData.copyOfRange(4, 8).toLong()
                    val scheduleTo = scheduleData.copyOfRange(8, 12).toLong()
                    val accessA6 = Access.A6(
                        type = type,
                        index = index,
                        isEnable = isEnable,
                        scheduleType = scheduleType,
                        weekDays = weekdays,
                        from = fromTime,
                        to = toTime,
                        scheduleFrom = scheduleFrom,
                        scheduleTo = scheduleTo,
                        nameLen = nameLen,
                        name = name,
                        code = accessCode,
                    )
                    Timber.d("[A6] read access from device: $accessA6")
                    accessA6
                } else {
                    throw IllegalArgumentException("Return function byte is not [A6]")
                }
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [A7] The result of adding a access.
     *
     * @param notification Data return from device.
     * @return ByteArray represent accessA7.
     *
     * */
    fun resolveA7(aesKeyTwo: ByteArray, notification: ByteArray): Access.A7 {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xA7) {
                decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt()).let { bytes ->
                    Timber.d("[A7] ${bytes.toHexPrint()}")
                    val type = bytes.component1().unSignedInt()
                    val index = bytes.copyOfRange(1, 3).toInt()
                    val isSuccess = bytes.component4().unSignedInt() == 0x01
                    val accessA7 = Access.A7(type, index, isSuccess)
                    Timber.d("[A7] read access from device: $accessA7")
                    accessA7
                }
            } else {
                throw IllegalArgumentException("Return function byte is not [A7]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [A8] The result of modifying a access.
     *
     * @param notification Data return from device.
     * @return ByteArray represent accessA7.
     *
     * */
    fun resolveA8(aesKeyTwo: ByteArray, notification: ByteArray): Access.A7 {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xA8) {
                decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt()).let { bytes ->
                    Timber.d("[A8] ${bytes.toHexPrint()}")
                    val type = bytes.component1().unSignedInt()
                    val index = bytes.copyOfRange(1, 3).toInt()
                    val isSuccess = bytes.component4().unSignedInt() == 0x01
                    val accessA7 = Access.A7(type, index, isSuccess)
                    Timber.d("[A8] read access from device: $accessA7")
                    accessA7
                }
            } else {
                throw IllegalArgumentException("Return function byte is not [A8]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [A9] The result of device get access.
     *
     * @param notification Data return from device.
     * @return ByteArray represent accessA9.
     *
     * */
    fun resolveA9(aesKeyTwo: ByteArray, notification: ByteArray): Access.A9 {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xA9) {
                val dataLen = decrypted.component4().unSignedInt()
                val data = decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt())
                Timber.d("[A9] dataLen: $dataLen data: ${data.toHexPrint()}")
                val type = data.component1().unSignedInt()
                val state = data.component2().unSignedInt()
                val index = data.copyOfRange(2, 4).toInt()
                val status = data.component5().unSignedInt() == 0x01
                Timber.d("[A9] read access dataInfo: ${data.copyOfRange(5, dataLen).toHexPrint()}")
                val dataInfo = data.copyOfRange(5, dataLen)
                val accessA9 = Access.A9(type, state, index, status, dataInfo)
                Timber.d("[A9] read access from device: $accessA9")
                accessA9
            } else {
                throw IllegalArgumentException("Return function byte is not [A9]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [AA] The result of deleting a access.
     *
     * @param notification Data return from device.
     * @return ByteArray represent AccessA7.
     *
     * */
    fun resolveAA(aesKeyTwo: ByteArray, notification: ByteArray): Access.A7 {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xAA) {
                decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt()).let { bytes ->
                    Timber.d("[AA] ${bytes.toHexPrint()}")
                    val type = bytes.component1().unSignedInt()
                    val index = bytes.copyOfRange(1, 3).toInt()
                    val isSuccess = bytes.component4().unSignedInt() == 0x01
                    val accessA7 = Access.A7(type, index, isSuccess)
                    Timber.d("[AA] read access from device: $accessA7")
                    accessA7
                }
            } else {
                throw IllegalArgumentException("Return function byte is not [AA]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [AF] alert notification.
     *
     * @param notification Data return from device.
     * @return ByteArray represent alert notification.
     *
     * */
    fun resolveAF(aesKeyTwo: ByteArray, notification: ByteArray): Alert.AF {
        return aesKeyTwo.let { keyTwo ->
            decrypt(keyTwo, notification)?.let { decrypted ->
                if (decrypted.component3().unSignedInt() == 0xAF) {
                    decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt()).let { byteArray ->
                        val alertType = Alert.AF(
                            alertType = when (byteArray.toInt()) {
                                0 -> BleV2Lock.AlertType.ERROR_ACCESS_CODE.value
                                1 -> BleV2Lock.AlertType.CURRENT_ACCESS_CODE_AT_WRONG_TIME.value
                                2 -> BleV2Lock.AlertType.CURRENT_ACCESS_CODE_BUT_AT_VACATION_MODE.value
                                3 -> BleV2Lock.AlertType.ACTIVELY_PRESS_THE_CLEAR_KEY.value
                                20 -> BleV2Lock.AlertType.MANY_ERROR_KEY_LOCKED.value
                                40 -> BleV2Lock.AlertType.LOCK_BREAK_ALERT.value
                                else -> BleV2Lock.AlertType.UNKNOWN_ALERT_TYPE.value
                            }
                        )
                        Timber.d("resolveAF: $alertType")
                        return alertType
                    }
                } else {
                    throw IllegalArgumentException("Return function byte is not [AF]")
                }
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    /**
     * Resolve [F0] The result of wifi setting string.
     *
     * @param notification Data return from device.
     * @return String represent wifi setting string.
     *
     * */
    fun resolveF0(aesKeyTwo: ByteArray, notification: ByteArray): String {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xF0) {
                String(decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt()))
            } else {
                throw IllegalArgumentException("Return function byte is not [F0]")
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

    fun isValidNotification(key:ByteArray, notification: ByteArray, function:Int): Boolean {
        return decrypt(key, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xEF) {
                throw LockStatusException.AdminCodeNotSetException()
            } else decrypted.component3().unSignedInt() == function
        } ?: false
    }

}