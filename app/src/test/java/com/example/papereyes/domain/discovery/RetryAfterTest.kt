package com.example.papereyes.domain.discovery
import com.example.papereyes.data.remote.RetryAfter
import org.junit.Assert.assertEquals
import org.junit.Test
class RetryAfterTest {
 @Test fun deltaSecondsAreOverflowSafe() {
  assertEquals(3000L,RetryAfter.millis("3",0))
  assertEquals(Long.MAX_VALUE,RetryAfter.millis("9999999999999999999999999",0))
  assertEquals(null,RetryAfter.millis("-4",0))
 }
 @Test fun httpDatesAndMalformedHeaders() {
  assertEquals(1000L,RetryAfter.millis("Thu, 01 Jan 1970 00:00:01 GMT",0))
  assertEquals(0L,RetryAfter.millis("Thu, 01 Jan 1970 00:00:01 GMT",2000))
  assertEquals(null,RetryAfter.millis("Thu, 01 Jan 1970 00:00:01 GMT junk",0))
  assertEquals(null,RetryAfter.millis(null,0))
 }
}
