package com.example.papereyes.domain

import java.io.StringReader
import java.lang.reflect.Proxy
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.xmlpull.v1.XmlPullParser

/** JDK XML adapter exercises the production entry state machine, not Android Xml. */
class ArxivFeedParserTest {
 private fun parser(xml:String):XmlPullParser {
  val factory=XMLInputFactory.newFactory()
  factory.setProperty(XMLInputFactory.SUPPORT_DTD,false)
  factory.setProperty("javax.xml.stream.isSupportingExternalEntities",false)
  val reader=factory.createXMLStreamReader(StringReader(xml))
  fun event():Int=when(reader.eventType){
   XMLStreamConstants.START_DOCUMENT->XmlPullParser.START_DOCUMENT
   XMLStreamConstants.END_DOCUMENT->XmlPullParser.END_DOCUMENT
   XMLStreamConstants.START_ELEMENT->XmlPullParser.START_TAG
   XMLStreamConstants.END_ELEMENT->XmlPullParser.END_TAG
   else->XmlPullParser.TEXT
  }
  return Proxy.newProxyInstance(XmlPullParser::class.java.classLoader,arrayOf(XmlPullParser::class.java)){_,method,_->
   when(method.name){
    "getEventType"->event()
    "getName"->if(reader.hasName())reader.localName else null
    "next"->{reader.next();event()}
    "nextText"->reader.elementText
    else->error("Unsupported test adapter method ${method.name}")
   }
  } as XmlPullParser
 }
 @Test fun apiErrorEntryIsNotAPaper(){
  assertNull(ArxivFeedParser.parse(parser("<feed><entry><id>http://arxiv.org/api/errors#bad_id</id><title>Error</title></entry></feed>"),"2609.20448"))
 }
 @Test fun unrelatedEntriesCannotLeakTitleOrAuthors(){
  val xml="""<feed><title>Feed title</title><entry><id>http://arxiv.org/abs/2609.12345v1</id><title>Wrong</title><author><name>Wrong Author</name></author></entry><entry><id>http://arxiv.org/abs/2609.20448v1</id><title>Right paper</title><author><name>Correct Author</name></author><published>2026-09-01</published></entry></feed>"""
  val result=ArxivFeedParser.parse(parser(xml),"2609.20448v1")!!
  assertEquals("Right paper",result.title);assertEquals("Correct Author",result.authors);assertEquals(2026,result.year)
 }
 @Test fun wrongExplicitVersionIsRejected(){
  val xml="<feed><entry><id>http://arxiv.org/abs/2609.20448v2</id><title>Version two</title></entry></feed>"
  assertNull(ArxivFeedParser.parse(parser(xml),"2609.20448v1"))
 }
}
