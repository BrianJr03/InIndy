package jr.brian.inindy.data.local

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

@OptIn(ExperimentalForeignApi::class)
actual class TokenStorage {

    actual fun saveToken(token: String) {
        clearToken()
        val bytes = token.encodeToByteArray()
        bytes.usePinned { pinned ->
            val cfData = CFDataCreate(
                null,
                pinned.addressOf(0).reinterpret<UByteVar>(),
                bytes.size.convert()
            )
            val query = createQuery(data = cfData)
            SecItemAdd(query, null)
            CFRelease(query)
            cfData?.let { CFRelease(it) }
        }
    }

    actual fun getToken(): String? = memScoped {
        val query = createQuery(forRead = true)
        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, result.ptr)
        CFRelease(query)
        if (status != errSecSuccess) return@memScoped null
        val resultRef = result.value ?: return@memScoped null
        val cfData: CFDataRef = resultRef.reinterpret()
        val length = CFDataGetLength(cfData).toInt()
        val bytePtr = CFDataGetBytePtr(cfData) ?: run {
            CFRelease(resultRef)
            return@memScoped null
        }
        val byteArray = bytePtr.reinterpret<ByteVar>().readBytes(length)
        CFRelease(resultRef)
        byteArray.decodeToString()
    }

    actual fun clearToken() {
        val query = createQuery()
        SecItemDelete(query)
        CFRelease(query)
    }

    private fun createQuery(
        data: CFDataRef? = null,
        forRead: Boolean = false
    ): CFMutableDictionaryRef? {
        val q = CFDictionaryCreateMutable(null, 0, null, null)
        CFDictionarySetValue(q, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(q, kSecAttrService, CFStringCreateWithCString(null, SERVICE, kCFStringEncodingUTF8))
        CFDictionarySetValue(q, kSecAttrAccount, CFStringCreateWithCString(null, ACCOUNT, kCFStringEncodingUTF8))
        if (data != null) {
            CFDictionarySetValue(q, kSecValueData, data)
        }
        if (forRead) {
            CFDictionarySetValue(q, kSecReturnData, kCFBooleanTrue)
            CFDictionarySetValue(q, kSecMatchLimit, kSecMatchLimitOne)
        }
        return q
    }

    private companion object {
        const val SERVICE = "jr.brian.inindy.auth"
        const val ACCOUNT = "auth_token"
    }
}
