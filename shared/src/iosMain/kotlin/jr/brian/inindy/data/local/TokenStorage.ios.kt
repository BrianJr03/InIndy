package jr.brian.inindy.data.local

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CFBridgingRelease
import kotlinx.cinterop.CFBridgingRetain
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.dictionary
import platform.Foundation.setObject
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

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class TokenStorage {

    actual fun saveToken(token: String) {
        clearToken()
        val data = (token as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        val query = baseQuery().apply {
            setObject(data, forKey = kSecValueData as Any)
        }
        val cfQuery = CFBridgingRetain(query) as CFDictionaryRef?
        SecItemAdd(cfQuery, null)
        CFBridgingRelease(cfQuery)
    }

    actual fun getToken(): String? = memScoped {
        val query = baseQuery().apply {
            setObject(NSNumber(bool = true), forKey = kSecReturnData as Any)
            setObject(kSecMatchLimitOne as Any, forKey = kSecMatchLimit as Any)
        }
        val cfQuery = CFBridgingRetain(query) as CFDictionaryRef?
        val resultVar = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(cfQuery, resultVar.ptr)
        CFBridgingRelease(cfQuery)
        if (status != errSecSuccess) return@memScoped null
        val nsData = CFBridgingRelease(resultVar.value) as? NSData ?: return@memScoped null
        NSString.create(nsData, NSUTF8StringEncoding) as String?
    }

    actual fun clearToken() {
        val query = baseQuery()
        val cfQuery = CFBridgingRetain(query) as CFDictionaryRef?
        SecItemDelete(cfQuery)
        CFBridgingRelease(cfQuery)
    }

    private fun baseQuery(): NSMutableDictionary = NSMutableDictionary.dictionary().apply {
        setObject(kSecClassGenericPassword as Any, forKey = kSecClass as Any)
        setObject(SERVICE as NSString, forKey = kSecAttrService as Any)
        setObject(ACCOUNT as NSString, forKey = kSecAttrAccount as Any)
    }

    private companion object {
        const val SERVICE = "jr.brian.inindy.auth"
        const val ACCOUNT = "auth_token"
    }
}
