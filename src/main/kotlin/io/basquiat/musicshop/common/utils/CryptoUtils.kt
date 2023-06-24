package io.basquiat.musicshop.common.utils

import org.springframework.security.crypto.bcrypt.BCrypt
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class CryptoUtils {

    companion object {
        /** 임의의 secret key AES에서는 16비트 */
        private const val ALGORITHM = "AES"
        private const val ALGORITHM_MODE = "AES/CBC/PKCS5Padding"
        private const val KEY = "123457689basquiatisgreatartistok"
        private val IV = KEY.substring(0, 16)

        fun encrypt(text: String): String {
            val cipher = Cipher.getInstance(ALGORITHM_MODE)
            cipher.init(Cipher.ENCRYPT_MODE,
                        SecretKeySpec(KEY.toByteArray(), ALGORITHM),
                        IvParameterSpec(IV.toByteArray())
            )
            val encrypted = cipher.doFinal(text.toByteArray(charset(StandardCharsets.UTF_8.name())))
            return Base64.getEncoder().encodeToString(encrypted)
        }

        fun decrypt(cipherText: String): String {
            val cipher = Cipher.getInstance(ALGORITHM_MODE)
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(KEY.toByteArray(), ALGORITHM),
                IvParameterSpec(IV.toByteArray())
            )
            return String(cipher.doFinal(Base64.getDecoder().decode(cipherText)), StandardCharsets.UTF_8)
        }

        fun encryptPassword(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt())

        fun matchPassword(password: String, encrypted: String) = BCrypt.checkpw(password, encrypted)

    }
}
