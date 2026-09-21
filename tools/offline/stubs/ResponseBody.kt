package okhttp3
abstract class ResponseBody:java.io.Closeable {abstract fun charStream():java.io.Reader}
