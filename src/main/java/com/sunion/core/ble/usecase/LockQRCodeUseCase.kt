package com.sunion.core.ble.usecase

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.colonMac
import com.sunion.core.ble.entity.QRCodeContent
import com.sunion.core.ble.exception.GenerateBarcodeException
import timber.log.Timber
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockQRCodeUseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
) {
    private val gson = Gson()
    private val barcodeEncoder = BarcodeEncoder()

    fun parseWifiQRCodeContent(barcodeKey: String, content: String): QRCodeContent {
        val data = Base64.decode(content.toByteArray(), Base64.NO_WRAP)
        val decode = decryptV2(data, barcodeKey.toByteArray())
        val decodeString = String(decode!!)
        val qrCodeContent = gson.fromJson(decodeString, QRCodeContent::class.java)!!
        return qrCodeContent.copy(a = qrCodeContent.a.colonMac(), u = qrCodeContent.u?.uppercase(Locale.getDefault()))
    }

    fun parseQRCodeContent(barcodeKey: String, content: String): QRCodeContent {
        val data = Base64.decode(content.toByteArray(), Base64.NO_WRAP)
        val decode = decryptV1(data, barcodeKey.toByteArray())
        val decodeString = String(decode!!)
        val qrCodeContent = gson.fromJson(decodeString, QRCodeContent::class.java)!!
        return qrCodeContent.copy(a = qrCodeContent.a.colonMac(), u = qrCodeContent.u?.uppercase(Locale.getDefault()))
    }

    fun encryptQRCodeContent(barcodeKey: String, qrCodeContent: QRCodeContent): String {
        val content = JsonObject()
        content.addProperty("T", qrCodeContent.t)
        content.addProperty("K", qrCodeContent.k)
        content.addProperty("A", qrCodeContent.a)
        content.addProperty("F", qrCodeContent.f)
        content.addProperty("L", qrCodeContent.l)
        content.addProperty("M", qrCodeContent.m)
        val barcodeContent =  bleCmdRepository.encrypt(
            barcodeKey.toByteArray(),
            bleCmdRepository.pad(content.toString().toByteArray(), true)
        )?.let {
            Base64.encodeToString(it, Base64.NO_WRAP)
        } ?: throw GenerateBarcodeException()
        return barcodeContent
    }

    fun generateQRCode(barcodeContent: String): Bitmap {
        var bitmap: Bitmap? = null
        kotlin.runCatching {
            bitmap = barcodeEncoder.encodeBitmap(
                barcodeContent,
                BarcodeFormat.QR_CODE,
                400,
                400
            )
        }.onFailure { throw it }
        return bitmap!!
    }

    @SuppressLint("GetInstance")
    private fun decryptV2(data: ByteArray, key: ByteArray): ByteArray? {
        val cipher: Cipher = Cipher.getInstance("AES/ECB/PKCS7Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))
        return cipher.doFinal(data)
    }

    @SuppressLint("GetInstance")
    private fun decryptV1(data: ByteArray, key: ByteArray): ByteArray? {
        val cipher: Cipher = Cipher.getInstance("AES/ECB/ZeroBytePadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))
        return cipher.doFinal(data)
    }
}