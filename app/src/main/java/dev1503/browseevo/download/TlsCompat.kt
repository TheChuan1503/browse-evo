package dev1503.browseevo.download

import android.content.Context
import android.os.Build
import android.util.Base64
import dev1503.browseevo.R
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object TlsCompat {

    private const val PEM_REGEX = "-----BEGIN CERTIFICATE-----([A-Za-z0-9+/=\r\n]+)-----END CERTIFICATE-----"

    @Volatile
    private var cachedSystem: X509TrustManager? = null

    @Volatile
    private var cachedEnhanced: X509TrustManager? = null

    fun defaultTrustManager(): X509TrustManager =
        cachedSystem ?: synchronized(this) {
            cachedSystem ?: systemTrustManager().also { cachedSystem = it }
        }

    fun enhancedTrustManager(context: Context): X509TrustManager {
        cachedEnhanced?.let { return it }
        return synchronized(this) {
            cachedEnhanced ?: run {
                val system = defaultTrustManager()
                val tm = bundledTrustManager(context)
                    ?.let { CompositeTrustManager(listOf(system, it)) }
                    ?: system
                cachedEnhanced = tm
                tm
            }
        }
    }

    fun newSslContext(trustManager: X509TrustManager): SSLContext =
        SSLContext.getInstance("TLS").apply { init(null, arrayOf(trustManager), null) }

    private fun systemTrustManager(): X509TrustManager {
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(null as KeyStore?)
        return factory.trustManagers.filterIsInstance<X509TrustManager>().first()
    }

    private fun bundledTrustManager(context: Context): X509TrustManager? {
        return try {
            val pemText = context.resources.openRawResource(R.raw.cacerts).bufferedReader().use { it.readText() }
            val regex = Regex(PEM_REGEX)
            val factory = CertificateFactory.getInstance("X.509")
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null, null)
            var count = 0
            for (match in regex.findAll(pemText)) {
                val der = Base64.decode(match.groupValues[1], Base64.DEFAULT)
                val cert = factory.generateCertificate(ByteArrayInputStream(der))
                keyStore.setCertificateEntry("bundled_ca_${count++}", cert)
            }
            if (count == 0) return null
            val factory2 = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            factory2.init(keyStore)
            factory2.trustManagers.filterIsInstance<X509TrustManager>().firstOrNull()
        } catch (e: Exception) {
            android.util.Log.w("TlsCompat", "load bundled CAs failed", e)
            null
        }
    }

    private class CompositeTrustManager(
        private val delegates: List<X509TrustManager>
    ) : X509TrustManager {

        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
            delegates.first().checkClientTrusted(chain, authType)
        }

        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
            var lastError: Exception? = null
            for (delegate in delegates) {
                try {
                    delegate.checkServerTrusted(chain, authType)
                    return
                } catch (e: Exception) {
                    lastError = e
                }
            }
            throw lastError ?: CertificateException("No trust anchor found")
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> =
            delegates.flatMap { it.acceptedIssuers.toList() }.toTypedArray()
    }
}
