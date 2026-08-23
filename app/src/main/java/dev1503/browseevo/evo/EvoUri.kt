package dev1503.browseevo.evo

/**
 * evo:// schema
 *
 *  evo://index                      -> built-in home page
 *  evo://page/<name>                -> a built-in page (reserved for future use)
 *  evo://interface/<action>/<args>  -> a native interface call
 *  evo://interface/navigate/<value> -> open a URL or search
 */
class EvoUri private constructor(
    val raw: String,
    val authority: String,
    val action: String?,
    val params: String?,
) {

    val isIndex: Boolean get() = authority == AUTHORITY_INDEX
    val isPage: Boolean get() = authority == AUTHORITY_PAGE
    val isInterface: Boolean get() = authority == AUTHORITY_INTERFACE
    val isError: Boolean get() = authority == AUTHORITY_ERROR

    companion object {
        const val SCHEME = "evo"
        const val AUTHORITY_INDEX = "index"
        const val AUTHORITY_PAGE = "page"
        const val AUTHORITY_INTERFACE = "interface"
        const val AUTHORITY_ERROR = "error"
        const val ACTION_NAVIGATE = "navigate"

        fun parse(uri: String): EvoUri? {
            if (!uri.startsWith("$SCHEME://", ignoreCase = true)) return null
            val rest = uri.substring("$SCHEME://".length)
            val authEnd = rest.indexOfFirst { it == '/' || it == '?' || it == '#' }
            val authority = (if (authEnd >= 0) rest.substring(0, authEnd) else rest).lowercase()
            var action: String? = null
            var params: String? = null
            if (authEnd >= 0 && rest[authEnd] == '/') {
                val path = rest.substring(authEnd + 1)
                val qIndex = path.indexOfFirst { it == '?' || it == '#' }
                val pathPart = if (qIndex >= 0) path.substring(0, qIndex) else path
                val slash = pathPart.indexOf('/')
                if (slash >= 0) {
                    action = pathPart.substring(0, slash).lowercase()
                    params = pathPart.substring(slash + 1)
                } else {
                    action = pathPart.lowercase()
                }
            }
            return EvoUri(uri, authority, action, params)
        }

        fun build(authority: String, action: String? = null, params: String? = null): String {
            val sb = StringBuilder("$SCHEME://$authority")
            if (!action.isNullOrEmpty()) {
                sb.append('/').append(action)
                if (!params.isNullOrEmpty()) {
                    sb.append('/').append(params)
                }
            }
            return sb.toString()
        }
    }
}
