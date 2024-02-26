package com.sunion.core.ble

import android.annotation.SuppressLint
import com.sunion.core.ble.entity.*
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

    enum class ConfigD6(val byte: Int){
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
    enum class ConfigA2(val byte: Int){
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
//        Timber.d("key:\n${key.toHexPrint()} \ndata:\n" +
//                data.toHexPrint()
//        )
        return try {
            val cipher: Cipher = Cipher.getInstance(CIPHER_MODE)
            val keySpec = SecretKeySpec(key, "AES")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            val encrypted: ByteArray = cipher.doFinal(data)
//            Timber.d("encrypted:\n${encrypted.toHexPrint()}")
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
        return commandSerial.incrementAndGet().toLittleEndianByteArrayInt16()
    }

    fun generateRandomBytes(size: Int): ByteArray = Random.nextBytes(size)

    fun createCommand(
        function: Int,
        key: ByteArray,
        data: ByteArray = byteArrayOf()
    ): ByteArray {
        return when (function) {
            0xA0, 0xA2, 0xA4, 0xB0, 0xCC, 0xCF, 0xD0, 0xD2, 0xD4, 0xD6, 0xD8, 0xE0, 0xE4, 0xEA, 0xEF -> {
                cmd(function, key)
            }
            0xA1 -> {
                cmd(function, key, 28, data)
            }
            0xA3 -> {
                cmd(function, key, 2, data)
            }
            0xA5, 0xB1, 0xC2, 0xD7, 0xE1, 0xE2, 0xE3, 0xE5, 0xEB, 0xEE -> {
                cmd(function, key, 1, data)
            }
            0xA6, 0xAA -> {
                cmd(function, key, 3, data)
            }
            0xA7, 0xA8, 0xC3, 0xC4, 0xC7, 0xC8, 0xCE, 0xD1, 0xD9, 0xE6, 0xE7, 0xE8, 0xEC, 0xED, 0xF0, 0xF1, 0xF2 -> {
                cmd(function, key, data.size, data)
            }
            0xA9, 0xD3 -> {
                cmd(function, key, 4, data)
            }
            0xC0 -> {
                commandSerial.set(0)
                cmd(function, key, 16)
            }
            0xC1 -> {
                cmd(function, key, 8, data)
            }
            0xD5 -> {
                cmd(function, key, 22, data)
            }
            else -> throw IllegalArgumentException("Unknown function")
        }
    }

    private fun cmd(
        function: Int,
        key: ByteArray,
        dataSize :Int? = null,
        data: ByteArray? = null,
        serial: ByteArray = serialIncrementAndGet(),
    ): ByteArray {
        if (function == 0xC0 && serial.size != 2) throw IllegalArgumentException("Invalid serial")
        val sendByte = ByteArray(2)
        sendByte[0] = function.toByte()
        if(function == 0xC0 && dataSize != null){
            sendByte[1] = dataSize.toByte()
            Timber.d("cmd[${function.toHexString()}]: ${sendByte[0].unSignedInt().toHexString()}, $dataSize")
            return encrypt(key, pad(serial + sendByte + generateRandomBytes(dataSize)))
                ?: throw IllegalArgumentException("bytes cannot be null")
        } else if(data != null && dataSize != null){
            sendByte[1] = dataSize.toByte()
            Timber.d("cmd[${function.toHexString()}]: ${sendByte[0].unSignedInt().toHexString()}, $dataSize, ${data.toHexPrint()}")
            return encrypt(key, pad(serial + sendByte + data))
                ?: throw IllegalArgumentException("bytes cannot be null")
        }else {
            Timber.d("cmd[${function.toHexString()}]: ${sendByte.toHexPrint()}")
            return encrypt(key, pad(serial + sendByte))
                ?: throw IllegalArgumentException("bytes cannot be null")
        }
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
        val codeByte = code.accessCodeToHex()
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
        return bytes
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
        return data
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

    fun resolve(function: Int, key: ByteArray, notification: ByteArray, index: Int = 0): Any {
        return decrypt(key, notification)?.let { decrypted ->
            val checkFunction = decrypted.component3().unSignedInt()
            val byteArrayData = decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt())
            val booleanData = decrypted.component5().unSignedInt()
            Timber.d("resolve[${function.toHexString()}]: ${checkFunction.toHexString()}, ${byteArrayData.size}, ${byteArrayData.toHexPrint()}")
            if (checkFunction == function) {
                when (function) {
                    0xA0 -> {
                        // LockConfig.A0
                        resolveA0(byteArrayData)
                    }
                    0xA1, 0xC7, 0xC8, 0xCE, 0xCF, 0xD1, 0xD3, 0xD5, 0xD9, 0xE2, 0xE8, 0xEC, 0xED, 0xEE, 0xEF, 0xF2 -> {
                        // Boolean
                        when (booleanData) {
                            0x01 -> true
                            0x00 -> false
                            else -> throw IllegalArgumentException("Unknown data")
                        }
                    }
                    0xA2 -> {
                        // DeviceStatus.A2
                        resolveA2(byteArrayData)
                    }
                    0xA4 -> {
                        // BleV2Lock.SupportedUnlockType
                        resolveA4(byteArrayData)
                    }
                    0xA5 -> {
                        // Access.A5
                        resolveA5(byteArrayData)
                    }
                    0xA6 -> {
                        // Access.A6
                        resolveA6(byteArrayData)
                    }
                    0xA7, 0xA8, 0xAA -> {
                        // Access.A7
                        resolveA7(byteArrayData)
                    }
                    0xA9 -> {
                        // Access.A9
                        resolveA9(byteArrayData)
                    }
                    0xAF -> {
                        // Alert.AF
                        resolveAF(byteArrayData)
                    }
                    0xB0 -> {
                        // DeviceStatus.B0
                        resolveB0(byteArrayData)
                    }
                    0xC0, 0xC1, 0xD8, 0xE4, 0xEA -> {
                        // ByteArray
                        byteArrayData
                    }
                    0xC2 -> {
                        // Version String
                        resolveC2(byteArrayData)
                    }
                    0xC3 -> {
                        // BleV2Lock.OTAStatus
                        resolveC3(byteArrayData)
                    }
                    0xC4 -> {
                        // HexString
                        byteArrayData.toHexPrint()
                    }
                    0xD0, 0xF0 -> {
                        // String
                        String(byteArrayData)
                    }
                    0xD2, 0xE0 -> {
                        // Int
                        byteArrayData.toInt()
                    }
                    0xD4 -> {
                        // LockConfig.D4
                        resolveD4(byteArrayData)
                    }
                    0xD6 -> {
                        // DeviceStatus.D6
                        resolveD6(byteArrayData)
                    }
                    0xE1 -> {
                        // EventLog
                        resolveE1(byteArrayData)
                    }
                    0xE5 -> {
                        // DeviceToken.PermanentToken
                        resolveE5(byteArrayData)
                    }
                    0xE6 -> {
                        // AddUserResponse
                        resolveE6(byteArrayData)
                    }
                    0xE7 -> {
                        // UpdateTokenResponse
                        resolveE7(byteArrayData)
                    }
                    0xEB -> {
                        // Access.Code
                        resolveEB(byteArrayData, index)
                    }
                    else -> throw IllegalArgumentException("Unknown function byte")
                }
            } else {
                throw IllegalArgumentException("Return function byte is not [$function]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    private fun resolveA0(data: ByteArray): LockConfig.A0 {
        val autoLockTimeInt = data.copyOfRange(ConfigA0.AUTOLOCK_DELAY.byte,ConfigA0.AUTOLOCK_DELAY_LOWER_LIMIT.byte).toInt()
        Timber.d("autoLockTimeInt: $autoLockTimeInt")
        val autoLockTimeLowerLimitInt = data.copyOfRange(ConfigA0.AUTOLOCK_DELAY_LOWER_LIMIT.byte,ConfigA0.AUTOLOCK_DELAY_UPPER_LIMIT.byte).toInt()
        Timber.d("autoLockTimeLowerLimitInt: $autoLockTimeLowerLimitInt")
        val autoLockTimeUpperLimitInt = data.copyOfRange(ConfigA0.AUTOLOCK_DELAY_UPPER_LIMIT.byte,ConfigA0.OPERATING_SOUND.byte).toInt()
        Timber.d("autoLockTimeUpperLimitInt: $autoLockTimeUpperLimitInt")

        val latIntPart = data.copyOfRange(ConfigA0.LATITUDE_INTEGER.byte, ConfigA0.LATITUDE_DECIMAL.byte).toInt()
        Timber.d("latIntPart: $latIntPart")
        val latDecimalPart = data.copyOfRange(ConfigA0.LATITUDE_DECIMAL.byte, ConfigA0.LONGITUDE_INTEGER.byte).toInt()
        val latDecimal = latDecimalPart.toBigDecimal().movePointLeft(9)
        Timber.d("latDecimalPart: $latIntPart, latDecimal: $latDecimal")
        val lat = latIntPart.toBigDecimal().plus(latDecimal)
        Timber.d("lat: $lat, ${lat.toPlainString()}")

        val lngIntPart = data.copyOfRange(ConfigA0.LONGITUDE_INTEGER.byte, ConfigA0.LONGITUDE_DECIMAL.byte).toInt()
        Timber.d("lngIntPart: $lngIntPart")
        val lngDecimalPart = data.copyOfRange(ConfigA0.LONGITUDE_DECIMAL.byte, ConfigA0.LOCK_DIRECTION.byte).toInt()
        val lngDecimal = lngDecimalPart.toBigDecimal().movePointLeft(9)
        Timber.d("lngIntPart: $lngIntPart, lngDecimal: $lngDecimal")
        val lng = lngIntPart.toBigDecimal().plus(lngDecimal)
        Timber.d("lng: $lng, ${lng.toPlainString()}")

        val lockConfigA0 = LockConfig.A0(
            latitude = lat.toDouble(),
            longitude = lng.toDouble(),
            direction = when (data[ConfigA0.LOCK_DIRECTION.byte].unSignedInt()) {
                0xA0 -> BleV2Lock.Direction.RIGHT.value
                0xA1 -> BleV2Lock.Direction.LEFT.value
                0xA2 -> BleV2Lock.Direction.UNKNOWN.value
                else -> BleV2Lock.Direction.NOT_SUPPORT.value
            },
            guidingCode = when (data[ConfigA0.GUIDING_CODE.byte].unSignedInt()) {
                0 -> BleV2Lock.GuidingCode.CLOSE.value
                1 -> BleV2Lock.GuidingCode.OPEN.value
                else -> BleV2Lock.GuidingCode.NOT_SUPPORT.value
            },
            virtualCode = when (data[ConfigA0.VIRTUAL_CODE.byte].unSignedInt()) {
                0 -> BleV2Lock.VirtualCode.CLOSE.value
                1 -> BleV2Lock.VirtualCode.OPEN.value
                else -> BleV2Lock.VirtualCode.NOT_SUPPORT.value
            },
            twoFA = when (data[ConfigA0.TWO_FA.byte].unSignedInt()) {
                0 -> BleV2Lock.TwoFA.CLOSE.value
                1 -> BleV2Lock.TwoFA.OPEN.value
                else -> BleV2Lock.TwoFA.NOT_SUPPORT.value
            },
            vacationMode = when (data[ConfigA0.VACATION_MODE.byte].unSignedInt()) {
                0 -> BleV2Lock.VacationMode.CLOSE.value
                1 -> BleV2Lock.VacationMode.OPEN.value
                else -> BleV2Lock.VacationMode.NOT_SUPPORT.value
            },
            autoLock = when (data[ConfigA0.AUTOLOCK.byte].unSignedInt()) {
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
            operatingSound = when (data[ConfigA0.OPERATING_SOUND.byte].unSignedInt()) {
                0 -> BleV2Lock.OperatingSound.CLOSE.value
                1 -> BleV2Lock.OperatingSound.OPEN.value
                else -> BleV2Lock.OperatingSound.NOT_SUPPORT.value
            },
            soundType = when (data[ConfigA0.SOUND_TYPE.byte].unSignedInt()) {
                0x01 -> BleV2Lock.SoundType.ON_OFF.value
                0x02 -> BleV2Lock.SoundType.LEVEL.value
                0x03 -> BleV2Lock.SoundType.PERCENTAGE.value
                else -> BleV2Lock.SoundType.NOT_SUPPORT.value
            },
            soundValue = when (data[ConfigA0.SOUND_TYPE.byte].unSignedInt()) {
                0x01 -> if (data[ConfigA0.SOUND_VALUE.byte].unSignedInt() == 100) BleV2Lock.SoundValue.OPEN.value else BleV2Lock.SoundValue.CLOSE.value
                0x02 -> when (data[ConfigA0.SOUND_VALUE.byte].unSignedInt()) {
                    100 -> BleV2Lock.SoundValue.HIGH_VOICE.value
                    50 -> BleV2Lock.SoundValue.LOW_VOICE.value
                    else -> BleV2Lock.SoundValue.CLOSE.value
                }
                0x03 -> data[ConfigA0.SOUND_VALUE.byte].unSignedInt()
                else -> BleV2Lock.SoundValue.NOT_SUPPORT.value
            },
            showFastTrackMode = when (data[ConfigA0.SHOW_FAST_TRACK_MODE.byte].unSignedInt()) {
                0 -> BleV2Lock.ShowFastTrackMode.CLOSE.value
                1 -> BleV2Lock.ShowFastTrackMode.OPEN.value
                else -> BleV2Lock.ShowFastTrackMode.NOT_SUPPORT.value
            },
        )
        Timber.d("resolveA0: $lockConfigA0")
        return lockConfigA0
    }

    private fun resolveA2(data: ByteArray): DeviceStatus.A2 {
        val lockSetting = DeviceStatus.A2(
            direction = when (data[ConfigA2.LOCK_DIRECTION.byte].unSignedInt()) {
                0xA0 -> BleV2Lock.Direction.RIGHT.value
                0xA1 -> BleV2Lock.Direction.LEFT.value
                0xA2 -> BleV2Lock.Direction.UNKNOWN.value
                else -> BleV2Lock.Direction.NOT_SUPPORT.value
            },
            vacationMode = when (data[ConfigA2.VACATION_MODE.byte].unSignedInt()) {
                0 -> BleV2Lock.VacationMode.CLOSE.value
                1 -> BleV2Lock.VacationMode.OPEN.value
                else -> BleV2Lock.VacationMode.NOT_SUPPORT.value
            },
            deadBolt = when (data[ConfigA2.DEAD_BOLT.byte].unSignedInt()) {
                0 -> BleV2Lock.DeadBolt.NOT_PROTRUDE.value
                1 -> BleV2Lock.DeadBolt.PROTRUDE.value
                else -> BleV2Lock.DeadBolt.NOT_SUPPORT.value
            },
            doorState = when (data[ConfigA2.DOOR_STATE.byte].unSignedInt()) {
                0 -> BleV2Lock.DoorState.OPEN.value
                1 -> BleV2Lock.DoorState.CLOSE.value
                else -> BleV2Lock.DoorState.NOT_SUPPORT.value
            },
            lockState = when (data[ConfigA2.LOCK_STATE.byte].unSignedInt()) {
                0 -> BleV2Lock.LockState.UNLOCKED.value
                1 -> BleV2Lock.LockState.LOCKED.value
                else -> BleV2Lock.LockState.UNKNOWN.value
            },
            securityBolt = when (data[ConfigA2.SECURITY_BOLT.byte].unSignedInt()) {
                0 -> BleV2Lock.SecurityBolt.NOT_PROTRUDE.value
                1 -> BleV2Lock.SecurityBolt.PROTRUDE.value
                else -> BleV2Lock.SecurityBolt.NOT_SUPPORT.value
            },
            battery = data[ConfigA2.BATTERY.byte].unSignedInt(),
            batteryState = when (data[ConfigA2.LOW_BATTERY.byte].unSignedInt()) {
                0 -> BleV2Lock.BatteryState.NORMAL.value
                1 -> BleV2Lock.BatteryState.WEAK_CURRENT.value
                else -> BleV2Lock.BatteryState.DANGEROUS.value
            }
        )
        Timber.d("resolveA2: $lockSetting")
        return lockSetting
    }

    private fun resolveA4(data: ByteArray): BleV2Lock.SupportedUnlockType {
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
        Timber.d("resolveA4: $response")
        return response
    }

    private fun resolveA5(data: ByteArray): Access.A5 {
        val type = data.component1().unSignedInt()
        val transferComplete = data.component2().unSignedInt()
        val dataByteArray = data.copyOfRange(2, data.size)
        val accessA5 = Access.A5(type, transferComplete, dataByteArray)
        Timber.d("resolveA5: $accessA5")
        return accessA5
    }

    private fun resolveA6(data: ByteArray): Access.A6 {
        val type = data.component1().unSignedInt()
        val index = data.copyOfRange(1, 3).toInt()
        val isEnable = data.component4().unSignedInt() == 0x01
        val scheduleData = data.copyOfRange(4, 16)
        val nameLen = data[16].unSignedInt()
        val name = String(data.copyOfRange(17, 17 + nameLen))
        val accessCode = data.copyOfRange(17 + nameLen, data.size)
        val code = accessCode.accessByteArrayToString()
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
        Timber.d("resolveA6: $accessA6, codeString=$code")
        return accessA6
    }

    private fun resolveA7(data: ByteArray): Access.A7 {
        val type = data.component1().unSignedInt()
        val index = data.copyOfRange(1, 3).toInt()
        val isSuccess = data.component4().unSignedInt() == 0x01
        val accessA7 = Access.A7(type, index, isSuccess)
        Timber.d("resolveA7: $accessA7")
        return accessA7
    }

    private fun resolveA9(data: ByteArray): Access.A9 {
        val type = data.component1().unSignedInt()
        val state = data.component2().unSignedInt()
        val index = data.copyOfRange(2, 4).toInt()
        val status = data.component5().unSignedInt() == 0x01
        val dataInfo = data.copyOfRange(5, data.size)
        val code = dataInfo.accessByteArrayToString()
        val accessA9 = Access.A9(type, state, index, status, dataInfo)
        Timber.d("resolveA9: $accessA9, codeString=$code")
        return accessA9
    }

    private fun resolveAF(data: ByteArray): Alert.AF {
        val alertType = Alert.AF(
            alertType = when (data.toInt()) {
                0 -> BleV2Lock.AlertType.ERROR_ACCESS_CODE.value
                1 -> BleV2Lock.AlertType.CURRENT_ACCESS_CODE_AT_WRONG_TIME.value
                2 -> BleV2Lock.AlertType.CURRENT_ACCESS_CODE_BUT_AT_VACATION_MODE.value
                3 -> BleV2Lock.AlertType.ACTIVELY_PRESS_THE_CLEAR_KEY.value
                20 -> BleV2Lock.AlertType.MANY_ERROR_KEY_LOCKED.value
                40 -> BleV2Lock.AlertType.LOCK_BREAK_ALERT.value
                0xFF -> BleV2Lock.AlertType.NONE.value
                else -> BleV2Lock.AlertType.UNKNOWN_ALERT_TYPE.value
            }
        )
        Timber.d("resolveAF: $alertType")
        return alertType
    }

    private fun resolveB0(data: ByteArray): DeviceStatus.B0 {
        val setWifi  = data.component1()
        val connectWifi  = data.component2()
        val plugStatus  = data.component3()
        val response = DeviceStatus.B0(
            setWifi.toInt(),
            connectWifi.toInt(),
            plugStatus.toInt(),
        )
        Timber.d("resolveB0: $response")
        return response
    }

    private fun resolveC2(data: ByteArray): String {
        val versionName = data.component1().unSignedInt()
        val version = data.copyOfRange(1, 3)
        Timber.d("resolveC2: ${String(version)}")
        return String(version)
    }

    private fun resolveC3(data: ByteArray): BleV2Lock.OTAStatus {
        val target  = data.component1()
        val state  = data.component2()
        val isSuccess  = data.component3()
        val response = BleV2Lock.OTAStatus(
            target.toInt(),
            state.toInt(),
            isSuccess.toInt(),
        )
        Timber.d("resolveC3: $response")
        return response
    }

    private fun resolveD4(data: ByteArray): LockConfig.D4 {
        val latIntPart = data.copyOfRange(ConfigD4.LATITUDE_INTEGER.byte, ConfigD4.LATITUDE_DECIMAL.byte).toInt()
        Timber.d("latIntPart: $latIntPart")
        val latDecimalPart = data.copyOfRange(ConfigD4.LATITUDE_DECIMAL.byte, ConfigD4.LONGITUDE_INTEGER.byte).toInt()
        val latDecimal = latDecimalPart.toBigDecimal().movePointLeft(9)
        Timber.d("latDecimalPart: $latIntPart, latDecimal: $latDecimal")
        val lat = latIntPart.toBigDecimal().plus(latDecimal)
        Timber.d("lat: $lat, ${lat.toPlainString()}")

        val lngIntPart = data.copyOfRange(ConfigD4.LONGITUDE_INTEGER.byte, ConfigD4.LONGITUDE_DECIMAL.byte).toInt()
        Timber.d("lngIntPart: $lngIntPart")
        val lngDecimalPart = data.copyOfRange(ConfigD4.LONGITUDE_DECIMAL.byte, ConfigD4.SIZE.byte).toInt()
        val lngDecimal = lngDecimalPart.toBigDecimal().movePointLeft(9)
        Timber.d("lngIntPart: $lngIntPart, lngDecimal: $lngDecimal")
        val lng = lngIntPart.toBigDecimal().plus(lngDecimal)
        Timber.d("lng: $lng, ${lng.toPlainString()}")

        val lockConfigD4 = LockConfig.D4(
            direction = when (data[ConfigD4.LOCK_DIRECTION.byte].unSignedInt()) {
                0xA0 -> LockDirection.Right
                0xA1 -> LockDirection.Left
                0xA2 -> LockDirection.NotDetermined
                else -> throw LockStatusException.LockDirectionException()
            },
            isSoundOn = data[ConfigD4.KEYPRESS_BEEP.byte].unSignedInt() == 0x01,
            isVacationModeOn = data[ConfigD4.VACATION_MODE.byte].unSignedInt() == 0x01,
            isAutoLock = data[ConfigD4.AUTOLOCK.byte].unSignedInt() == 0x01,
            autoLockTime = data[ConfigD4.AUTOLOCK_DELAY.byte].unSignedInt(),
            isGuidingCodeOn = data[ConfigD4.GUIDING_CODE.byte].unSignedInt() == 0x01,
            latitude = lat.toDouble(),
            longitude = lng.toDouble()
        )
        Timber.d("resolveD4: $lockConfigD4")
        return lockConfigD4
    }

    private fun resolveD6(data: ByteArray): DeviceStatus.D6 {
        val autoLockTime = if (data[ConfigD6.AUTOLOCK_DELAY.byte].unSignedInt() !in 1..90) {
            1
        } else {
            data[ConfigD6.AUTOLOCK_DELAY.byte].unSignedInt()
        }
        val lockSetting = DeviceStatus.D6(
            config = LockConfig.D4(
                direction = when (data[ConfigD6.LOCK_DIRECTION.byte].unSignedInt()) {
                    0xA0 -> LockDirection.Right
                    0xA1 -> LockDirection.Left
                    0xA2 -> LockDirection.NotDetermined
                    else -> throw LockStatusException.LockDirectionException()
                },
                isSoundOn = data[ConfigD6.KEYPRESS_BEEP.byte].unSignedInt() == 0x01,
                isVacationModeOn = data[ConfigD6.VACATION_MODE.byte].unSignedInt() == 0x01,
                isAutoLock = data[ConfigD6.AUTOLOCK.byte].unSignedInt() == 0x01,
                autoLockTime = autoLockTime,
                isGuidingCodeOn = data[ConfigD6.GUIDING_CODE.byte].unSignedInt() == 0x01
            ),
            lockState = when (data[ConfigD6.LOCK_STATE.byte].unSignedInt()) {
                0 -> LockState.UNLOCKED
                1 -> LockState.LOCKED
                else -> LockState.UNKNOWN
            },
            battery = data[ConfigD6.BATTERY.byte].unSignedInt(),
            batteryState = when (data[ConfigD6.LOW_BATTERY.byte].unSignedInt()) {
                0 -> BatteryState.BATTERY_GOOD
                1 -> BatteryState.BATTERY_LOW
                else -> BatteryState.BATTERY_ALERT
            },
            timestamp = data.copyOfRange(ConfigD6.TIMESTAMP.byte, ConfigD6.SIZE.byte).toInt().toLong()
        )
        return lockSetting
    }

    private fun resolveE1(data: ByteArray): EventLog {
        val timestamp = data.copyOfRange(0, 4).toInt().toLong()
        val event = data.component5().unSignedInt()
        val name = data.copyOfRange(5, data.size)
        val log = EventLog(
            eventTimeStamp = timestamp,
            event = event,
            name = String(name)
        )
        Timber.d("resolveE1: $log")
        return log
    }

    private fun resolveE5(data: ByteArray): DeviceToken.PermanentToken {
        val user = DeviceToken.PermanentToken(
            isValid = data.component1().unSignedInt() == 1,
            isPermanent = data.component2().unSignedInt() == 1,
            isOwner = data.component3().unSignedInt() == 1,
            permission = String(data.copyOfRange(3, 4)),
            token = data.copyOfRange(4, 12).toHexString(),
            name = String(data.copyOfRange(12, data.size))
        )
        Timber.d("resolveE5: $user")
        return user
    }

    private fun resolveE6(data: ByteArray): AddUserResponse {
        val isSuccessful = data.component1().unSignedInt() == 0x01
        val tokenIndexInDevice = data.component2().unSignedInt()
        val tokenBytes = data.copyOfRange(2, data.size)
        val token = tokenBytes.toHexString()
        val response = AddUserResponse(
            isSuccessful,
            tokenIndexInDevice,
            token
        )
        Timber.d("resolveE6: $response")
        return response
    }

    private fun resolveE7(data: ByteArray): UpdateTokenResponse {
        val isSuccessful = data.component1().unSignedInt() == 0x01
        val tokenIndexInDevice = data.component2().unSignedInt()
        val isPermanent = data.component3().unSignedInt() == 0x01
        val permission = String(data.copyOfRange(3, 4))
        val tokenBytes = data.copyOfRange(4, 12)
        val name = String(data.copyOfRange(12, data.size))
        val token = tokenBytes.toHexString()
        val response = UpdateTokenResponse(
            isSuccessful,
            tokenIndexInDevice,
            isPermanent,
            permission,
            token,
            name
        )
        Timber.d("resolveE7 $response")
        return response
    }

    private fun resolveEB(data: ByteArray, index: Int): Access.Code {
        val pinCodeLength = data.component2().unSignedInt()
        val pinCode = data.copyOfRange(2, 2 + pinCodeLength)
        val code = pinCode.accessByteArrayToString()
        val scheduleData = data.copyOfRange(2 + pinCodeLength, 2 + pinCodeLength + 12)
        val name = data.copyOfRange(2 + pinCodeLength + 12, data.size)
        val type = String(byteArrayOf(scheduleData.component1()))
        val weekdays = scheduleData.component2().unSignedInt()
        val fromTime = scheduleData.component3().unSignedInt()
        val toTime = scheduleData.component4().unSignedInt()
        val scheduleFrom = scheduleData.copyOfRange(4, 8).toLong()
        val scheduleTo = scheduleData.copyOfRange(8, 12).toLong()
        val userCode = Access.Code(
            index = index,
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
        Timber.d("resolveEB: $userCode")
        return userCode
    }

    fun isValidNotification(key:ByteArray, notification: ByteArray, function:Int): Boolean {
        return decrypt(key, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xEF) {
                throw LockStatusException.AdminCodeNotSetException()
            } else decrypted.component3().unSignedInt() == function
        } ?: false
    }

}