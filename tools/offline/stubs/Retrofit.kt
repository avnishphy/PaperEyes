// Type shapes ONLY. Networking paths deliberately fail rather than simulate HTTP.
package retrofit2
class HeaderShape(private val values:Map<String,String> = emptyMap()) {
 operator fun get(name:String):String? = values.entries.firstOrNull{it.key.equals(name,true)}?.value
}
class Response<T>(private val headerValues:HeaderShape = HeaderShape()) {
 fun headers()=headerValues
}
class HttpException:Exception() {
 fun code():Int = error("No HTTP runtime in portable harness")
 fun response():Response<*>? = error("No HTTP runtime in portable harness")
}
class Retrofit {
 class Builder {
  fun baseUrl(url:String)=this
  fun client(client:Any)=this
  fun addConverterFactory(factory:Any)=this
  fun build()=Retrofit()
 }
 fun <T> create(type:Class<T>):T = error("No Retrofit runtime in portable harness")
}
