package com.sunion.core.ble

import android.annotation.SuppressLint
import com.sunion.core.ble.entity.*
import com.sunion.core.ble.exception.LockStatusException
import timber.log.Timber
import java.io.ByteArrayOutputStream
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
    enum class Config82(val byte: Int){
        SIZE(10),
        MAIN_VERSION (0),
        SUB_VERSION (1),
        LOCK_DIRECTION (2),
        VACATION_MODE (3),
        DEAD_BOLT (4),
        DOOR_STATE ( 5),
        LOCK_STATE (6),
        SECURITY_BOLT (7),
        BATTERY (8),
        LOW_BATTERY (9)
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

    enum class Config80(val byte: Int){
        MAIN_VERSION (0),
        SUB_VERSION (1),
        FORMAT_VERSION (2),
        SERVER_VERSION (3),
        LATITUDE_INTEGER (7),
        LATITUDE_DECIMAL (11),
        LONGITUDE_INTEGER (15),
        LONGITUDE_DECIMAL (19),
        LOCK_DIRECTION (23),
        GUIDING_CODE (24),
        VIRTUAL_CODE (25),
        TWO_FA (26),
        VACATION_MODE (27),
        AUTOLOCK (28),
        AUTOLOCK_DELAY ( 29),
        AUTOLOCK_DELAY_LOWER_LIMIT (31),
        AUTOLOCK_DELAY_UPPER_LIMIT (33),
        OPERATING_SOUND (35),
        SOUND_TYPE (36),
        SOUND_VALUE (37),
        SHOW_FAST_TRACK_MODE (38),
        SABBATH_MODE (39),
        PHONETIC_LANGUAGE (40),
        SUPPORT_PHONETIC_LANGUAGE (41),
    }

    enum class Config81(val byte: Int){
        SIZE(30),
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
        SHOW_FAST_TRACK_MODE (27),
        SABBATH_MODE (28),
        PHONETIC_LANGUAGE (29),
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
            0x80, 0x82, 0x85, 0x86, 0x87, 0x8A, 0x90, 0x94, 0x9D, 0xA0, 0xA2, 0xA4, 0xB0, 0xC9, 0xCB, 0xCC, 0xCF, 0xD0, 0xD2, 0xD4, 0xD6, 0xD8, 0xE0, 0xE4, 0xEA, 0xEF -> {
                cmd(function, key)
            }
            0x81, 0x8C, 0x8D, 0x8E, 0x92, 0x96, 0xA7, 0xA8, 0xC3, 0xC4, 0xC7, 0xC8, 0xCE, 0xD1, 0xD9, 0xE6, 0xE7, 0xE8, 0xEC, 0xED, 0xF0, 0xF1, 0xF2, 0xF3, 0xF4-> {
                cmd(function, key, data.size, data)
            }
            0x83, 0x84, 0x91, 0x93, 0x98, 0xA3 -> {
                cmd(function, key, 2, data)
            }
            0x8B, 0x99, 0xA5, 0xB1, 0xC2, 0xD7, 0xE1, 0xE2, 0xE5, 0xEB, 0xEE -> {
                cmd(function, key, 1, data)
            }
            0x95, 0xA6, 0xAA -> {
                cmd(function, key, 3, data)
            }
            0x97, 0xA9, 0xD3 -> {
                cmd(function, key, 4, data)
            }
            0xA1 -> {
                cmd(function, key, 28, data)
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
        val functionName = ::cmd.name
        if (function == 0xC0 && serial.size != 2) throw IllegalArgumentException("Invalid serial")
        val sendByte = ByteArray(2)
        sendByte[0] = function.toByte()
        if(function == 0xC0 && dataSize != null){
            sendByte[1] = dataSize.toByte()
            Timber.d("$functionName[${function.toHexString()}]: ${sendByte[0].unSignedInt().toHexString()}, $dataSize")
            return encrypt(key, pad(serial + sendByte + generateRandomBytes(dataSize)))
                ?: throw IllegalArgumentException("bytes cannot be null")
        } else if(data != null && dataSize != null){
            sendByte[1] = dataSize.toByte()
            Timber.d("$functionName[${function.toHexString()}]: ${sendByte[0].unSignedInt().toHexString()}, $dataSize, ${data.toHexPrint()}")
            return encrypt(key, pad(serial + sendByte + data))
                ?: throw IllegalArgumentException("bytes cannot be null")
        }else {
            Timber.d("$functionName[${function.toHexString()}]: ${sendByte.toHexPrint()}")
            return encrypt(key, pad(serial + sendByte))
                ?: throw IllegalArgumentException("bytes cannot be null")
        }
    }

    fun combineLockConfig81Cmd(lockConfig80: LockConfig.Eighty): ByteArray {
        val settingBytes = ByteArray(Config81.SIZE.byte)

        val latitudeBigDecimal = BigDecimal.valueOf(lockConfig80.latitude)
        val latitudeIntPartBytes = latitudeBigDecimal.toInt().toLittleEndianByteArray()
        for (i in 0..latitudeIntPartBytes.lastIndex) settingBytes[Config81.LATITUDE_INTEGER.byte + i] =
            latitudeIntPartBytes[i]
        val latitudeDecimalInt =
            latitudeBigDecimal.subtract(BigDecimal(latitudeBigDecimal.toInt())).scaleByPowerOfTen(9)
                .toInt()
        val latitudeDecimalPartBytes = latitudeDecimalInt.toLittleEndianByteArray()
        for (i in 0..latitudeDecimalPartBytes.lastIndex) settingBytes[Config81.LATITUDE_DECIMAL.byte + i] =
            latitudeDecimalPartBytes[i]
        Timber.d("latitudeBigDecimal: $latitudeBigDecimal, latitudeIntPart: ${latitudeBigDecimal.toInt()}, latitudeDecimalInt: $latitudeDecimalInt")

        val longitudeBigDecimal = BigDecimal.valueOf(lockConfig80.longitude)
        val longitudeIntPartBytes = longitudeBigDecimal.toInt().toLittleEndianByteArray()
        for (i in 0..longitudeIntPartBytes.lastIndex) settingBytes[Config81.LONGITUDE_INTEGER.byte + i] =
            longitudeIntPartBytes[i]
        val longitudeDecimalInt =
            longitudeBigDecimal.subtract(BigDecimal(longitudeBigDecimal.toInt()))
                .scaleByPowerOfTen(9).toInt()
        val longitudeDecimalPartBytes = longitudeDecimalInt.toLittleEndianByteArray()
        for (i in 0..longitudeDecimalPartBytes.lastIndex) settingBytes[Config81.LONGITUDE_DECIMAL.byte + i] =
            longitudeDecimalPartBytes[i]
        Timber.d("longitudeBigDecimal: $longitudeBigDecimal longitudeBigDecimal: ${longitudeBigDecimal.toInt()}, longitudeDecimalInt: $longitudeDecimalInt")

        Timber.d("LockConfig.80: $lockConfig80")
        settingBytes[Config81.LOCK_DIRECTION.byte] = lockConfig80.direction.toByte()
        settingBytes[Config81.GUIDING_CODE.byte] = lockConfig80.guidingCode.toByte()
        settingBytes[Config81.VIRTUAL_CODE.byte] = lockConfig80.virtualCode.toByte()
        settingBytes[Config81.TWO_FA.byte] = lockConfig80.twoFA.toByte()
        settingBytes[Config81.VACATION_MODE.byte] = lockConfig80.vacationMode.toByte()
        settingBytes[Config81.AUTOLOCK.byte] = lockConfig80.autoLock.toByte()
        val autoLockDelayBytes = lockConfig80.autoLockTime.toLittleEndianByteArrayInt16()
        for (i in 0..autoLockDelayBytes.lastIndex) settingBytes[Config81.AUTOLOCK_DELAY.byte + i] = autoLockDelayBytes[i]
        settingBytes[Config81.OPERATING_SOUND.byte] = lockConfig80.operatingSound.toByte()
        settingBytes[Config81.SOUND_TYPE.byte] = lockConfig80.soundType.toByte()
        settingBytes[Config81.SOUND_VALUE.byte] = lockConfig80.soundValue.toByte()
        settingBytes[Config81.SHOW_FAST_TRACK_MODE.byte] = lockConfig80.showFastTrackMode.toByte()
        settingBytes[Config81.SABBATH_MODE.byte] = lockConfig80.sabbathMode.toByte()
        settingBytes[Config81.PHONETIC_LANGUAGE.byte] = lockConfig80.phoneticLanguage.toByte()
        return settingBytes
    }

    fun combineUser92Cmd(user92Cmd: User.NinetyTwoCmd): ByteArray {
        val outputStream = ByteArrayOutputStream()
        outputStream.write(user92Cmd.action)
        outputStream.write(user92Cmd.userIndex.toLittleEndianByteArrayInt16())
        outputStream.write(user92Cmd.name.toPaddedByteArray(10))
        outputStream.write(user92Cmd.userStatus)
        outputStream.write(user92Cmd.userType)
        outputStream.write(user92Cmd.credentialRule)
        user92Cmd.weekDayScheduleList?.forEach { schedule ->
            outputStream.write(schedule.status)
            outputStream.write(schedule.dayMask)
            outputStream.write(schedule.startHour)
            outputStream.write(schedule.startMinute)
            outputStream.write(schedule.endHour)
            outputStream.write(schedule.endMinute)
        }
        user92Cmd.yearDayScheduleList?.forEach { schedule ->
            Timber.d("ValidTimeRange status: ${schedule.status}, start: ${schedule.start}, end: ${schedule.end}")
            outputStream.write(schedule.status)
            outputStream.write(schedule.start.limitValidTimeRange().toLittleEndianByteArrayInt32())
            outputStream.write(schedule.end.limitValidTimeRange().toLittleEndianByteArrayInt32())
        }
        return outputStream.toByteArray()
    }

    fun combineCredential96Cmd(credential96Cmd: Credential.NinetySixCmd): ByteArray {
        val outputStream = ByteArrayOutputStream()
        outputStream.write(credential96Cmd.action)
        outputStream.write(credential96Cmd.userIndex.toLittleEndianByteArrayInt16())
        if(credential96Cmd.credentialDetail != null){
            outputStream.write(credential96Cmd.credentialDetail.index.toLittleEndianByteArrayInt16())
            outputStream.write(credential96Cmd.credentialDetail.status)
            outputStream.write(credential96Cmd.credentialDetail.type)
            outputStream.write(credential96Cmd.credentialDetail.code.extendedByteArray(8))
        }
        return outputStream.toByteArray()
    }

    fun combineLockConfigA1Cmd(lockConfigA0: LockConfig.A0): ByteArray {
        val settingBytes = ByteArray(ConfigA1.SIZE.byte)

        val latitudeBigDecimal = BigDecimal.valueOf(lockConfigA0.latitude ?: 0.0)
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

        val longitudeBigDecimal = BigDecimal.valueOf(lockConfigA0.longitude ?: 0.0)
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

        Timber.d("LockConfig.A0: $lockConfigA0")
        settingBytes[ConfigA1.LOCK_DIRECTION.byte] = lockConfigA0.direction.toByte()
        settingBytes[ConfigA1.GUIDING_CODE.byte] = lockConfigA0.guidingCode.toByte()
        settingBytes[ConfigA1.VIRTUAL_CODE.byte] = lockConfigA0.virtualCode.toByte()
        settingBytes[ConfigA1.TWO_FA.byte] = lockConfigA0.twoFA.toByte()
        settingBytes[ConfigA1.VACATION_MODE.byte] = lockConfigA0.vacationMode.toByte()
        settingBytes[ConfigA1.AUTOLOCK.byte] = lockConfigA0.autoLock.toByte()
        val autoLockDelayBytes = lockConfigA0.autoLockTime.toLittleEndianByteArrayInt16()
        for (i in 0..autoLockDelayBytes.lastIndex) settingBytes[ConfigA1.AUTOLOCK_DELAY.byte + i] = autoLockDelayBytes[i]
        settingBytes[ConfigA1.OPERATING_SOUND.byte] = lockConfigA0.operatingSound.toByte()
        settingBytes[ConfigA1.SOUND_TYPE.byte] = lockConfigA0.soundType.toByte()
        settingBytes[ConfigA1.SOUND_VALUE.byte] = lockConfigA0.soundValue.toByte()
        settingBytes[ConfigA1.SHOW_FAST_TRACK_MODE.byte] = lockConfigA0.showFastTrackMode.toByte()
        return settingBytes
    }

    fun combineAccessA7Cmd(
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

    fun combineLockConfigD5Cmd(lockConfigD4: LockConfig.D4): ByteArray {
        val settingBytes = ByteArray(ConfigD4.SIZE.byte)
        settingBytes[ConfigD4.LOCK_DIRECTION.byte] = 0xA3.toByte()
        settingBytes[ConfigD4.KEYPRESS_BEEP.byte] = (if (lockConfigD4.isSoundOn) 0x01.toByte() else 0x00.toByte())
        settingBytes[ConfigD4.VACATION_MODE.byte] = if (lockConfigD4.isVacationModeOn) 0x01.toByte() else 0x00.toByte()
        settingBytes[ConfigD4.AUTOLOCK.byte] = if (lockConfigD4.isAutoLock) 0x01.toByte() else 0x00.toByte()
        settingBytes[ConfigD4.AUTOLOCK_DELAY.byte] = lockConfigD4.autoLockTime.toByte()
        settingBytes[ConfigD4.GUIDING_CODE.byte] = (if (lockConfigD4.isGuidingCodeOn) 0x01.toByte() else 0x00.toByte())

        val latitudeBigDecimal = BigDecimal.valueOf(lockConfigD4.latitude ?: 0.0)
        val latitudeIntPartBytes = latitudeBigDecimal.toInt().toLittleEndianByteArray()
        for (i in 0..latitudeIntPartBytes.lastIndex) settingBytes[ConfigD4.LATITUDE_INTEGER.byte + i] = latitudeIntPartBytes[i]
        val latitudeDecimalInt = latitudeBigDecimal.subtract(BigDecimal(latitudeBigDecimal.toInt())).scaleByPowerOfTen(9).toInt()
        val latitudeDecimalPartBytes = latitudeDecimalInt.toLittleEndianByteArray()
        for (i in 0..latitudeDecimalPartBytes.lastIndex) settingBytes[ConfigD4.LATITUDE_DECIMAL.byte + i] = latitudeDecimalPartBytes[i]
        Timber.d("latitudeBigDecimal: $latitudeBigDecimal, latitudeIntPart: ${latitudeBigDecimal.toInt()}, latitudeDecimalInt: $latitudeDecimalInt")

        val longitudeBigDecimal = BigDecimal.valueOf(lockConfigD4.longitude ?: 0.0)
        val longitudeIntPartBytes = longitudeBigDecimal.toInt().toLittleEndianByteArray()
        for (i in 0..longitudeIntPartBytes.lastIndex) settingBytes[ConfigD4.LONGITUDE_INTEGER.byte + i] = longitudeIntPartBytes[i]
        val longitudeDecimalInt = longitudeBigDecimal.subtract(BigDecimal(longitudeBigDecimal.toInt())).scaleByPowerOfTen(9).toInt()
        val longitudeDecimalPartBytes = longitudeDecimalInt.toLittleEndianByteArray()
        for (i in 0..longitudeDecimalPartBytes.lastIndex) settingBytes[ConfigD4.LONGITUDE_DECIMAL.byte + i] = longitudeDecimalPartBytes[i]
        Timber.d("longitudeBigDecimal: $longitudeBigDecimal longitudeBigDecimal: ${longitudeBigDecimal.toInt()}, longitudeDecimalInt: $longitudeDecimalInt")
        return settingBytes
    }

    fun combineAccessCodeCmd(
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

    fun resolve(function: Int, key: ByteArray, notification: ByteArray, index: Int = 0): Any {
        return decrypt(key, notification)?.let { decrypted ->
            val functionName = ::resolve.name
            val checkFunction = decrypted.component3().unSignedInt()
            val byteArrayData = decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt())
            val booleanData = decrypted.component5().unSignedInt()
            Timber.d("$functionName[${function.toHexString()}]: ${checkFunction.toHexString()}, ${byteArrayData.size}, ${byteArrayData.toHexPrint()}")
            if (checkFunction == function) {
                when (function) {
                    0x80 -> {
                        // LockConfig.Eighty
                        resolve80(byteArrayData)
                    }
                    0X81 -> {
                        // LockConfig.EightyOne
                        resolve81(byteArrayData)
                    }
                    0x82 -> {
                        // DeviceStatus.EightyTwo & 0x83
                        resolve82(byteArrayData)
                    }
                    0x84, 0x87, 0x8D, 0X8E, 0x9D, 0xA1, 0xC7, 0xC8, 0xCB, 0xCE, 0xCF, 0xD1, 0xD3, 0xD5, 0xD9, 0xE2, 0xE8, 0xEC, 0xED, 0xEE, 0xEF, 0xF4 -> {
                        // Boolean
                        when (booleanData) {
                            0x01 -> true
                            0x00 -> false
                            else -> throw IllegalArgumentException("Unknown data")
                        }
                    }
                    0x85 -> {
                        // BleV3Lock.UserAbility
                        resolve85(byteArrayData)
                    }
                    0x86 -> {
                        // BleV3Lock.UserCount
                        resolve86(byteArrayData)
                    }
                    0x8A, 0xC0, 0xC1, 0xD8, 0xE4, 0xEA -> {
                        // ByteArray
                        byteArrayData
                    }
                    0x8B -> {
                        // DeviceToken.PermanentToken
                        resolve8B(byteArrayData)
                    }
                    0x90, 0x94 -> {
                        // User.Ninety
                        resolve90(byteArrayData)
                    }
                    0x91 -> {
                        // User.NinetyOne
                        resolve91(byteArrayData)
                    }
                    0x92, 0x93 -> {
                        // User.NinetyTwo
                        resolve92(byteArrayData)
                    }
                    0x95 -> {
                        // User.NinetyFive
                        resolve95(byteArrayData)
                    }
                    0x96, 0x98 -> {
                        // Credential.NinetySix
                        resolve96(byteArrayData)
                    }
                    0x97 -> {
                        // User.NinetySeven
                        resolve97(byteArrayData)
                    }
                    0x99 -> {
                        // User.NinetyNine
                        resolve99(byteArrayData)
                    }
                    0xA0 -> {
                        // LockConfig.A0
                        resolveA0(byteArrayData)
                    }
                    0xA2 -> {
                        // DeviceStatus.A2 & 0xA3
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
                        // DeviceStatus.B0 & 0xB1
                        resolveB0(byteArrayData)
                    }
                    0xC2 -> {
                        // BleV3Lock.LockVersion
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
                    0xC9 -> {
                        // BleV3Lock.AdminPosition
                        resolveC9(byteArrayData)
                    }
                    0xD0, 0xF0, 0xF2 -> {
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
                        // DeviceStatus.D6 & 0xD7
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
                    0xE6, 0x8C -> {
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

    private fun resolve80(data: ByteArray): LockConfig.Eighty {
        val functionName = ::resolve80.name
        val autoLockTimeInt = data.copyOfRange(Config80.AUTOLOCK_DELAY.byte, Config80.AUTOLOCK_DELAY_LOWER_LIMIT.byte).toInt()
        Timber.d("autoLockTimeInt: $autoLockTimeInt")
        val autoLockTimeLowerLimitInt = data.copyOfRange(Config80.AUTOLOCK_DELAY_LOWER_LIMIT.byte, Config80.AUTOLOCK_DELAY_UPPER_LIMIT.byte).toInt()
        Timber.d("autoLockTimeLowerLimitInt: $autoLockTimeLowerLimitInt")
        val autoLockTimeUpperLimitInt = data.copyOfRange(Config80.AUTOLOCK_DELAY_UPPER_LIMIT.byte, Config80.OPERATING_SOUND.byte).toInt()
        Timber.d("autoLockTimeUpperLimitInt: $autoLockTimeUpperLimitInt")

        val latIntPart = data.copyOfRange(Config80.LATITUDE_INTEGER.byte, Config80.LATITUDE_DECIMAL.byte).toInt()
        Timber.d("latIntPart: $latIntPart")
        val latDecimalPart = data.copyOfRange(Config80.LATITUDE_DECIMAL.byte, Config80.LONGITUDE_INTEGER.byte).toInt()
        val latDecimal = latDecimalPart.toBigDecimal().movePointLeft(9)
        Timber.d("latDecimalPart: $latIntPart, latDecimal: $latDecimal")
        val lat = latIntPart.toBigDecimal().plus(latDecimal)
        Timber.d("lat: $lat, ${lat.toPlainString()}")

        val lngIntPart = data.copyOfRange(Config80.LONGITUDE_INTEGER.byte, Config80.LONGITUDE_DECIMAL.byte).toInt()
        Timber.d("lngIntPart: $lngIntPart")
        val lngDecimalPart = data.copyOfRange(Config80.LONGITUDE_DECIMAL.byte, Config80.LOCK_DIRECTION.byte).toInt()
        val lngDecimal = lngDecimalPart.toBigDecimal().movePointLeft(9)
        Timber.d("lngIntPart: $lngIntPart, lngDecimal: $lngDecimal")
        val lng = lngIntPart.toBigDecimal().plus(lngDecimal)
        Timber.d("lng: $lng, ${lng.toPlainString()}")

        var lockConfig80 = LockConfig.Eighty(
            size = data.size,
            mainVersion = data[Config80.MAIN_VERSION.byte].unSignedInt(),
            subVersion = data[Config80.SUB_VERSION.byte].unSignedInt(),
            formatVersion = data[Config80.FORMAT_VERSION.byte].unSignedInt(),
            serverVersion = data[Config80.SERVER_VERSION.byte].unSignedInt(),
            latitude = lat.toDouble(),
            longitude = lng.toDouble(),
            direction = when (data[Config80.LOCK_DIRECTION.byte].unSignedInt()) {
                0xA0 -> BleV3Lock.Direction.RIGHT.value
                0xA1 -> BleV3Lock.Direction.LEFT.value
                0xA2 -> BleV3Lock.Direction.UNKNOWN.value
                else -> BleV3Lock.Direction.NOT_SUPPORT.value
            },
            guidingCode = when (data[Config80.GUIDING_CODE.byte].unSignedInt()) {
                0 -> BleV3Lock.GuidingCode.CLOSE.value
                1 -> BleV3Lock.GuidingCode.OPEN.value
                else -> BleV3Lock.GuidingCode.NOT_SUPPORT.value
            },
            virtualCode = when (data[Config80.VIRTUAL_CODE.byte].unSignedInt()) {
                0 -> BleV3Lock.VirtualCode.CLOSE.value
                1 -> BleV3Lock.VirtualCode.OPEN.value
                else -> BleV3Lock.VirtualCode.NOT_SUPPORT.value
            },
            twoFA = when (data[Config80.TWO_FA.byte].unSignedInt()) {
                0 -> BleV3Lock.TwoFA.CLOSE.value
                1 -> BleV3Lock.TwoFA.OPEN.value
                else -> BleV3Lock.TwoFA.NOT_SUPPORT.value
            },
            vacationMode = when (data[Config80.VACATION_MODE.byte].unSignedInt()) {
                0 -> BleV3Lock.VacationMode.CLOSE.value
                1 -> BleV3Lock.VacationMode.OPEN.value
                else -> BleV3Lock.VacationMode.NOT_SUPPORT.value
            },
            autoLock = when (data[Config80.AUTOLOCK.byte].unSignedInt()) {
                0 -> BleV3Lock.AutoLock.CLOSE.value
                1 -> BleV3Lock.AutoLock.OPEN.value
                else -> BleV3Lock.AutoLock.NOT_SUPPORT.value
            },
            autoLockTime = when (autoLockTimeInt) {
                0xFFFF -> BleV3Lock.AutoLockTime.NOT_SUPPORT.value
                else -> {
                    autoLockTimeInt
                }
            },
            autoLockTimeLowerLimit = when (autoLockTimeLowerLimitInt) {
                0xFFFF -> BleV3Lock.AutoLockTimeLowerLimit.NOT_SUPPORT.value
                else -> {
                    autoLockTimeLowerLimitInt
                }
            },
            autoLockTimeUpperLimit = when (autoLockTimeUpperLimitInt) {
                0xFFFF -> BleV3Lock.AutoLockTimeUpperLimit.NOT_SUPPORT.value
                else -> {
                    autoLockTimeUpperLimitInt
                }
            },
            operatingSound = when (data[Config80.OPERATING_SOUND.byte].unSignedInt()) {
                0 -> BleV3Lock.OperatingSound.CLOSE.value
                1 -> BleV3Lock.OperatingSound.OPEN.value
                else -> BleV3Lock.OperatingSound.NOT_SUPPORT.value
            },
            soundType = when (data[Config80.SOUND_TYPE.byte].unSignedInt()) {
                0x01 -> BleV3Lock.SoundType.ON_OFF.value
                0x02 -> BleV3Lock.SoundType.LEVEL.value
                0x03 -> BleV3Lock.SoundType.PERCENTAGE.value
                else -> BleV3Lock.SoundType.NOT_SUPPORT.value
            },
            soundValue = when (data[Config80.SOUND_TYPE.byte].unSignedInt()) {
                0x01 -> if (data[Config80.SOUND_VALUE.byte].unSignedInt() == 100) BleV3Lock.SoundValue.OPEN.value else BleV3Lock.SoundValue.CLOSE.value
                0x02 -> when (data[Config80.SOUND_VALUE.byte].unSignedInt()) {
                    100 -> BleV3Lock.SoundValue.HIGH_VOICE.value
                    50 -> BleV3Lock.SoundValue.LOW_VOICE.value
                    else -> BleV3Lock.SoundValue.CLOSE.value
                }
                0x03 -> data[Config80.SOUND_VALUE.byte].unSignedInt()
                else -> BleV3Lock.SoundValue.NOT_SUPPORT.value
            },
            showFastTrackMode = when (data[Config80.SHOW_FAST_TRACK_MODE.byte].unSignedInt()) {
                0 -> BleV3Lock.ShowFastTrackMode.CLOSE.value
                1 -> BleV3Lock.ShowFastTrackMode.OPEN.value
                else -> BleV3Lock.ShowFastTrackMode.NOT_SUPPORT.value
            },
            sabbathMode = when (data[Config80.SABBATH_MODE.byte].unSignedInt()) {
                0 -> BleV3Lock.SabbathMode.CLOSE.value
                1 -> BleV3Lock.SabbathMode.OPEN.value
                else -> BleV3Lock.SabbathMode.NOT_SUPPORT.value
            },
            phoneticLanguage = when (data[Config80.PHONETIC_LANGUAGE.byte].unSignedInt()) {
                0 -> BleV3Lock.PhoneticLanguage.ENGLISH.value
                else -> BleV3Lock.PhoneticLanguage.NOT_SUPPORT.value
            },
            supportPhoneticLanguage = when (data[Config80.SUPPORT_PHONETIC_LANGUAGE.byte].unSignedInt()) {
                0 -> BleV3Lock.SupportPhoneticLanguage.NOT_SUPPORT.value
                else -> data[Config80.SUPPORT_PHONETIC_LANGUAGE.byte].unSignedInt()
            },
        )
        Timber.d("$functionName: $lockConfig80")
        return lockConfig80
    }

    private fun resolve81(data: ByteArray): LockConfig.EightyOne {
        val functionName = ::resolve81.name
        val lockConfig81 = LockConfig.EightyOne(
            isSuccess = data.component1().unSignedInt() == 0x01,
            version = data.copyOfRange(1, 5).toInt()
        )
        Timber.d("$functionName: $lockConfig81")
        return lockConfig81
    }

    private fun resolve82(data: ByteArray): DeviceStatus.EightTwo {
        val functionName = ::resolve82.name
        val lockSetting = DeviceStatus.EightTwo(
            mainVersion = data[Config82.MAIN_VERSION.byte].unSignedInt(),
            subVersion = data[Config82.SUB_VERSION.byte].unSignedInt(),
            direction = when (data[Config82.LOCK_DIRECTION.byte].unSignedInt()) {
                0xA0 -> BleV3Lock.Direction.RIGHT.value
                0xA1 -> BleV3Lock.Direction.LEFT.value
                0xA2 -> BleV3Lock.Direction.UNKNOWN.value
                else -> BleV3Lock.Direction.NOT_SUPPORT.value
            },
            vacationMode = when (data[Config82.VACATION_MODE.byte].unSignedInt()) {
                0 -> BleV3Lock.VacationMode.CLOSE.value
                1 -> BleV3Lock.VacationMode.OPEN.value
                else -> BleV3Lock.VacationMode.NOT_SUPPORT.value
            },
            deadBolt = when (data[Config82.DEAD_BOLT.byte].unSignedInt()) {
                0 -> BleV3Lock.DeadBolt.NOT_PROTRUDE.value
                1 -> BleV3Lock.DeadBolt.PROTRUDE.value
                else -> BleV3Lock.DeadBolt.NOT_SUPPORT.value
            },
            doorState = when (data[Config82.DOOR_STATE.byte].unSignedInt()) {
                0 -> BleV3Lock.DoorState.OPEN.value
                1 -> BleV3Lock.DoorState.CLOSE.value
                else -> BleV3Lock.DoorState.NOT_SUPPORT.value
            },
            lockState = when (data[Config82.LOCK_STATE.byte].unSignedInt()) {
                0 -> BleV3Lock.LockState.UNLOCKED.value
                1 -> BleV3Lock.LockState.LOCKED.value
                else -> BleV3Lock.LockState.UNKNOWN.value
            },
            securityBolt = when (data[Config82.SECURITY_BOLT.byte].unSignedInt()) {
                0 -> BleV3Lock.SecurityBolt.NOT_PROTRUDE.value
                1 -> BleV3Lock.SecurityBolt.PROTRUDE.value
                else -> BleV3Lock.SecurityBolt.NOT_SUPPORT.value
            },
            battery = data[Config82.BATTERY.byte].unSignedInt(),
            batteryState = when (data[Config82.LOW_BATTERY.byte].unSignedInt()) {
                0 -> BleV3Lock.BatteryState.NORMAL.value
                1 -> BleV3Lock.BatteryState.WEAK_CURRENT.value
                else -> BleV3Lock.BatteryState.DANGEROUS.value
            }
        )
        Timber.d("$functionName: $lockSetting")
        return lockSetting
    }

    private fun resolve85(data: ByteArray): BleV3Lock.UserAbility {
        val functionName = ::resolve85.name
        val response = BleV3Lock.UserAbility(
            isMatter = data.component1().unSignedInt() == 0x01,
            weekDayScheduleCount = data.component2().unSignedInt(),
            yearDayScheduleCount = data.component3().unSignedInt(),
            codeCredentialCount = data.component4().unSignedInt(),
            cardCredentialCount = data.component5().unSignedInt(),
            fpCredentialCount = data.copyOfRange(5, 6).toInt(),
            faceCredentialCount = data.copyOfRange(6, 7).toInt(),
        )
        Timber.d("$functionName: $response")
        return response
    }

    private fun resolve86(data: ByteArray): BleV3Lock.UserCount {
        val functionName = ::resolve86.name
        val response = BleV3Lock.UserCount(
            matterCount = data.copyOfRange(0, 2).toInt(),
            codeCount = data.copyOfRange(2, 4).toInt(),
            cardCount = data.copyOfRange(4, 6).toInt(),
            fpCount = data.copyOfRange(6, 8).toInt(),
            faceCount = data.copyOfRange(8, 10).toInt(),
        )
        Timber.d("$functionName: $response")
        return response
    }

    private fun resolve8B(data: ByteArray): DeviceToken.PermanentToken {
        val functionName = ::resolve8B.name
        val nameLen = data.copyOfRange(12, 13).toInt()
        val identityLen = data.copyOfRange(13, 14).toInt()
        val bleUser = DeviceToken.PermanentToken(
            isValid = data.component1().unSignedInt() == 1,
            isPermanent = data.component2().unSignedInt() == 1,
            isOwner = data.component3().unSignedInt() == 1,
            permission = String(data.copyOfRange(3, 4)),
            token = data.copyOfRange(4, 12).toHexString(),
            nameLen = nameLen,
            identityLen = identityLen,
            name = String(data.copyOfRange(14, 14 + nameLen)),
            identity = String(data.copyOfRange(14 + nameLen, 14 + nameLen + identityLen))
        )
        Timber.d("$functionName: $bleUser")
        return bleUser
    }

    private fun resolve90(data: ByteArray): User.Ninety {
        val functionName = ::resolve90.name
        val transferComplete = data.component1().unSignedInt()
        val dataByteArray = data.copyOfRange(1, data.size)
        val user90 = User.Ninety(transferComplete, dataByteArray)
        Timber.d("$functionName: $user90")
        return user90
    }

    private fun resolve91(data: ByteArray): User.NinetyOne {
        val functionName = ::resolve91.name
        val index = data.copyOfRange(0, 2).toInt()
        val name = data.copyOfRange(2, 12).nameToString()
        val status = data[12].unSignedInt()
        val type = data[13].unSignedInt()
        val credentialRule = data[14].unSignedInt()
        val weekDayScheduleListCount = data[15].unSignedInt()
        val yearDayScheduleListCount = data[16].unSignedInt()
        val weekDayScheduleListData = data.copyOfRange(17, 17 + weekDayScheduleListCount * 6)
        val yearDayScheduleListData = data.copyOfRange(
            17 + weekDayScheduleListCount * 6,
            17 + weekDayScheduleListCount * 6 + yearDayScheduleListCount * 9
        )
        val weekDayScheduleList:MutableList<BleV3Lock.WeekDaySchedule> = mutableListOf()
        if(weekDayScheduleListCount > 0) {
            for (i in 0 until weekDayScheduleListCount) {
                weekDayScheduleList.add(
                    BleV3Lock.WeekDaySchedule(
                        status = weekDayScheduleListData.copyOfRange(i * 6 + 0, i * 6 + 1).toInt(),
                        dayMask = weekDayScheduleListData.copyOfRange(i * 6 + 1, i * 6 + 2).toInt(),
                        startHour = weekDayScheduleListData.copyOfRange(i * 6 + 2, i * 6 + 3).toInt(),
                        startMinute = weekDayScheduleListData.copyOfRange(i * 6 + 3, i * 6 + 4).toInt(),
                        endHour = weekDayScheduleListData.copyOfRange(i * 6 + 4, i * 6 + 5).toInt(),
                        endMinute = weekDayScheduleListData.copyOfRange(i * 6 + 5, i * 6 + 6).toInt()
                    )
                )
            }
        }
        val yearDayScheduleList:MutableList<BleV3Lock.YearDaySchedule> = mutableListOf()
        if(yearDayScheduleListCount > 0) {
            for (i in 0 until yearDayScheduleListCount) {
                yearDayScheduleList.add(
                    BleV3Lock.YearDaySchedule(
                        status = yearDayScheduleListData.copyOfRange(i * 9 + 0, i * 9 + 1).toInt(),
                        start = yearDayScheduleListData.copyOfRange(i * 9 + 1, i * 9 + 5).toLong(),
                        end = yearDayScheduleListData.copyOfRange(i * 9 + 5, i * 9 + 9).toLong()
                    )
                )
            }
        }
        val user91 = User.NinetyOne(
            userIndex = index,
            name = name,
            userStatus = status,
            userType = type,
            credentialRule = credentialRule,
            weekDayScheduleListCount = weekDayScheduleListCount,
            yearDayScheduleListCount = yearDayScheduleListCount,
            weekDayScheduleList = weekDayScheduleList,
            yearDayScheduleList = yearDayScheduleList,
        )
        Timber.d("$functionName: $user91")
        return user91
    }

    private fun resolve92(data: ByteArray): User.NinetyTwo {
        val functionName = ::resolve92.name
        val user92 = User.NinetyTwo(
            userIndex = data.copyOfRange(0, 2).toInt(),
            isSuccess = data.component3().unSignedInt() == 1,
        )
        Timber.d("$functionName: $user92")
        return user92
    }

    private fun resolve95(data: ByteArray): Credential {
        val functionName = ::resolve95.name
        val format = data.component1().unSignedInt()
        val index = data.copyOfRange(1, 3).toInt()
        val credential95 = if(format == 0) {
            val credentialDetailSize = (data.size - 3) / 12
            if(credentialDetailSize > 0) {
                Credential.NinetyFiveCredential(
                    format = format,
                    index = index,
                    userIndex = data.copyOfRange(3, 5).toInt(),
                    status = data.copyOfRange(5, 6).toInt(),
                    type = data.copyOfRange(6, 7).toInt(),
                    code = if(data.copyOfRange(6, 7).toInt() == BleV3Lock.CredentialType.PIN.value) data.copyOfRange(7, 15).toAsciiString().accessCodeToHex() else data.copyOfRange(7, 15)
                )
            } else {
                Credential.NinetyFiveCredential(
                    format = format,
                    index = index,
                )
            }
        } else {
            val credentialDetailSize = (data.size - 3) / 12
            val credentialDetailList:MutableList<BleV3Lock.CredentialDetail> = mutableListOf()
            if(credentialDetailSize > 0){
                for (i in 0 until credentialDetailSize) {
                    credentialDetailList.add(
                        BleV3Lock.CredentialDetail(
                            index = data.copyOfRange(3 + i * 12, 5 + i * 12).toInt(),
                            status = data.copyOfRange(5 + i * 12, 6 + i * 12).toInt(),
                            type = data.copyOfRange(6 + i * 12, 7 + i * 12).toInt(),
                            code = if(data.copyOfRange(6, 7).toInt() == BleV3Lock.CredentialType.PIN.value) data.copyOfRange(7 + i * 12, 15 + i * 12).toAsciiString().accessCodeToHex() else data.copyOfRange(7 + i * 12, 15 + i * 12)
                        )
                    )
                }
                Credential.NinetyFiveUser(
                    format = format,
                    userIndex = index,
                    credentialDetail = credentialDetailList
                )
            } else {
                Credential.NinetyFiveUser(
                    format = format,
                    userIndex = index
                )
            }
        }
        Timber.d("$functionName: $credential95")
        return credential95
    }

    private fun resolve96(data: ByteArray): Credential.NinetySix {
        val functionName = ::resolve96.name
        val credential96 = Credential.NinetySix(
            index = data.copyOfRange(0, 2).toInt(),
            isSuccess = data.component3().unSignedInt() == 1,
        )
        Timber.d("$functionName: $credential96")
        return credential96
    }

    private fun resolve97(data: ByteArray): Credential.NinetySeven {
        val functionName = ::resolve97.name
        val type = data.component1().unSignedInt()
        val state = data.component2().unSignedInt()
        val index = data.copyOfRange(2, 4).toInt()
        val status = data.component5().unSignedInt()
        val dataInfo = data.copyOfRange(5, data.size)
        val code = dataInfo.accessByteArrayToString()
        val credential97 = Credential.NinetySeven(type, state, index, status, dataInfo)
        Timber.d("$functionName: $credential97 codeString: $code")
        return credential97
    }

    private fun resolve99(data: ByteArray): Data.NinetyNine {
        val functionName = ::resolve99.name
        val target = data.component1().unSignedInt()
        val sha256 = String(data.copyOfRange(1, 33))
        val data99 = Data.NinetyNine(target, sha256)
        Timber.d("$functionName: $data99")
        return data99
    }

    private fun resolveA0(data: ByteArray): LockConfig.A0 {
        val functionName = ::resolveA0.name
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
        Timber.d("$functionName: $lockConfigA0")
        return lockConfigA0
    }

    private fun resolveA2(data: ByteArray): DeviceStatus.A2 {
        val functionName = ::resolveA2.name
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
        Timber.d("$functionName: $lockSetting")
        return lockSetting
    }

    private fun resolveA4(data: ByteArray): BleV2Lock.SupportedUnlockType {
        val functionName = ::resolveA4.name
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
        Timber.d("$functionName: $response")
        return response
    }

    private fun resolveA5(data: ByteArray): Access.A5 {
        val functionName = ::resolveA5.name
        val type = data.component1().unSignedInt()
        val transferComplete = data.component2().unSignedInt()
        val dataByteArray = data.copyOfRange(2, data.size)
        val accessA5 = Access.A5(type, transferComplete, dataByteArray)
        Timber.d("$functionName: $accessA5")
        return accessA5
    }

    private fun resolveA6(data: ByteArray): Access.A6 {
        val functionName = ::resolveA6.name
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
        Timber.d("$functionName: $accessA6 codeString: $code")
        return accessA6
    }

    private fun resolveA7(data: ByteArray): Access.A7 {
        val functionName = ::resolveA7.name
        val type = data.component1().unSignedInt()
        val index = data.copyOfRange(1, 3).toInt()
        val isSuccess = data.component4().unSignedInt() == 0x01
        val accessA7 = Access.A7(type, index, isSuccess)
        Timber.d("$functionName: $accessA7")
        return accessA7
    }

    private fun resolveA9(data: ByteArray): Access.A9 {
        val functionName = ::resolveA9.name
        val type = data.component1().unSignedInt()
        val state = data.component2().unSignedInt()
        val index = data.copyOfRange(2, 4).toInt()
        val status = data.component5().unSignedInt() == 0x01
        val dataInfo = data.copyOfRange(5, data.size)
        val code = dataInfo.accessByteArrayToString()
        val accessA9 = Access.A9(type, state, index, status, dataInfo)
        Timber.d("$functionName: $accessA9 codeString: $code")
        return accessA9
    }

    private fun resolveAF(data: ByteArray): Alert.AF {
        val functionName = ::resolveAF.name
        val alertType = Alert.AF(
            alertType = when (data.toInt()) {
                0 -> BleV2Lock.AlertType.ERROR_ACCESS_CODE.value
                1 -> BleV2Lock.AlertType.CURRENT_ACCESS_CODE_AT_WRONG_TIME.value
                2 -> BleV2Lock.AlertType.CURRENT_ACCESS_CODE_BUT_AT_VACATION_MODE.value
                3 -> BleV2Lock.AlertType.ACTIVELY_PRESS_THE_CLEAR_KEY.value
                20 -> BleV2Lock.AlertType.MANY_ERROR_KEY_LOCKED.value
                40 -> BleV2Lock.AlertType.LOCK_BREAK_ALERT.value
                0xFF -> BleV3Lock.AlertType.NONE.value
                else -> BleV3Lock.AlertType.UNKNOWN_ALERT_TYPE.value
            }
        )
        Timber.d("$functionName: $alertType")
        return alertType
    }

    private fun resolveB0(data: ByteArray): DeviceStatus.B0 {
        val functionName = ::resolveB0.name
        val mainVersion = data.component1().toInt()
        val subVersion = data.component2().toInt()
        val setWifi  = data.component3().toInt()
        val connectWifi  = data.component4().toInt()
        val plugStatus  = data.component5().toInt()
        val response = DeviceStatus.B0(
            mainVersion,
            subVersion,
            setWifi,
            connectWifi,
            plugStatus,
        )
        Timber.d("$functionName: $response")
        return response
    }

    private fun resolveC2(data: ByteArray): BleV3Lock.LockVersion {
        val functionName = ::resolveC2.name
        val lockVersion = BleV3Lock.LockVersion(
            target = data.component1().unSignedInt(),
            mainVersion = data.component2().unSignedInt(),
            subVersion = data.component3().unSignedInt(),
        )
        Timber.d("$functionName: $lockVersion")
        return lockVersion
    }

    private fun resolveC3(data: ByteArray): BleV2Lock.OTAStatus {
        val functionName = ::resolveC3.name
        val target  = data.component1()
        val state  = data.component2()
        val isSuccess  = data.component3()
        val response = BleV2Lock.OTAStatus(
            target.toInt(),
            state.toInt(),
            isSuccess.toInt(),
        )
        Timber.d("$functionName: $response")
        return response
    }

    private fun resolveC9(data: ByteArray): BleV3Lock.AdminPosition {
        val functionName = ::resolveC9.name
        val adminPosition = BleV3Lock.AdminPosition(
            userIndex = data.copyOfRange(0, 2).toInt(),
            credentialIndex = data.copyOfRange(2, 4).toInt(),
        )
        Timber.d("$functionName: $adminPosition")
        return adminPosition
    }

    private fun resolveD4(data: ByteArray): LockConfig.D4 {
        val functionName = ::resolveD4.name
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
        Timber.d("$functionName: $lockConfigD4")
        return lockConfigD4
    }

    private fun resolveD6(data: ByteArray): DeviceStatus.D6 {
        val functionName = ::resolveD6.name
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
        Timber.d("$functionName: $lockSetting")
        return lockSetting
    }

    private fun resolveE1(data: ByteArray): EventLog {
        val functionName = ::resolveE1.name
        val timestamp = data.copyOfRange(0, 4).toInt().toLong()
        val event = data.component5().unSignedInt()
        val name = data.copyOfRange(5, data.size)
        val log = EventLog(
            eventTimeStamp = timestamp,
            event = event,
            name = String(name)
        )
        Timber.d("$functionName: $log")
        return log
    }

    private fun resolveE5(data: ByteArray): DeviceToken.PermanentToken {
        val functionName = ::resolveE5.name
        val user = DeviceToken.PermanentToken(
            isValid = data.component1().unSignedInt() == 1,
            isPermanent = data.component2().unSignedInt() == 1,
            isOwner = data.component3().unSignedInt() == 1,
            permission = String(data.copyOfRange(3, 4)),
            token = data.copyOfRange(4, 12).toHexString(),
            name = String(data.copyOfRange(12, data.size))
        )
        Timber.d("$functionName: $user")
        return user
    }

    private fun resolveE6(data: ByteArray): AddUserResponse {
        val functionName = ::resolveE6.name
        val isSuccessful = data.component1().unSignedInt() == 0x01
        val tokenIndexInDevice = data.component2().unSignedInt()
        val tokenBytes = data.copyOfRange(2, data.size)
        val token = tokenBytes.toHexString()
        val response = AddUserResponse(
            isSuccessful,
            tokenIndexInDevice,
            token
        )
        Timber.d("$functionName: $response")
        return response
    }

    private fun resolveE7(data: ByteArray): UpdateTokenResponse {
        val functionName = ::resolveE7.name
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
        Timber.d("$functionName: $response")
        return response
    }

    private fun resolveEB(data: ByteArray, index: Int): Access.Code {
        val functionName = ::resolveEB.name
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
        Timber.d("$functionName: $userCode")
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