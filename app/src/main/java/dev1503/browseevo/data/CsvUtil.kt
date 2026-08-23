package dev1503.browseevo.data

object CsvUtil {

    fun escape(value: String, maxFieldLength: Int = Int.MAX_VALUE): String {
        val safeValue = if (value.length > maxFieldLength) value.substring(0, maxFieldLength) else value
        if (safeValue.contains(',') || safeValue.contains('"') || safeValue.contains('\n') || safeValue.contains('\r')) {
            return "\"" + safeValue.replace("\"", "\"\"") + "\""
        }
        return safeValue
    }

    fun parseLine(line: String, maxFieldLength: Int = Int.MAX_VALUE): List<String>? {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        if (current.length >= maxFieldLength) return null
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> {
                    if (current.length >= maxFieldLength) return null
                    current.append(c)
                }
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
