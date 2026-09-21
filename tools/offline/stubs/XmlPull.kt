package org.xmlpull.v1
interface XmlPullParser {
 val eventType:Int
 val name:String?
 fun next():Int
 fun nextText():String
 fun setFeature(name:String,value:Boolean)
 fun setInput(reader:java.io.Reader)
 companion object {
  const val START_DOCUMENT=0;const val END_DOCUMENT=1;const val START_TAG=2;const val END_TAG=3;const val TEXT=4
  const val FEATURE_PROCESS_NAMESPACES="http://xmlpull.org/v1/doc/features.html#process-namespaces"
 }
}
