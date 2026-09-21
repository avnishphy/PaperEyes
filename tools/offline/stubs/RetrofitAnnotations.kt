// Compile-only annotations, not request/URL serialization.
package retrofit2.http
annotation class GET(val value:String)
annotation class Path(val value:String, val encoded:Boolean=false)
annotation class Query(val value:String, val encoded:Boolean=false)
annotation class Headers(vararg val value:String)
