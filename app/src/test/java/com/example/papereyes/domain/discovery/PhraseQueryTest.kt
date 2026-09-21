package com.example.papereyes.domain.discovery
import com.example.papereyes.domain.evidence.TextFingerprint
import java.net.URLEncoder
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
class PhraseQueryTest {
 @Test fun boundedQuotedQueriesCannotInjectSearchOperators() {
  val text="unusual renormalization coefficients describe transverse distributions inside composite systems"
  val query=PhraseQuery.build((1..5).map{TextFingerprint(text,it,1.0)})
  assertEquals(2,Regex(" OR ").findAll(query).count())
  assertTrue(query.startsWith("\""));assertTrue(query.endsWith("\""))
 }
 @Test fun unicodeInputIsBoundedByEncodedLength() {
  val text=(1..12).joinToString(" "){"核子非摂動的分布解析"}
  assertTrue(URLEncoder.encode(PhraseQuery.build((1..5).map{TextFingerprint(text,it,1.0)}),"UTF-8").length<=3000)
 }
}
