package com.example.papereyes.util.concurrency
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
class RequestGateTest {
 @Test fun firstRequestIsImmediateAndSubsequentCallsArePaced() = runBlocking {
  var now=0L;val waits=mutableListOf<Long>()
  val gate=RequestGate(3000,clockMillis={now},sleep={waits+=it;now+=it})
  gate.execute{1};assertTrue(waits.isEmpty());gate.execute{2}
  assertEquals(listOf(3000L),waits)
 }
 @Test fun longCooldownFailsFastAndIsHonoredAcrossCalls() = runBlocking {
  var now=0L;val gate=RequestGate(1000,clockMillis={now},sleep={now+=it})
  gate.coolDown(60000);var blocked=false
  try {gate.execute{error("must not call provider")}}catch(_:ProviderRateLimitedException){blocked=true}
  assertTrue(blocked);now=60000;assertEquals(7,gate.execute{7})
 }
 @Test fun failureStillPacesNextRequest() = runBlocking {
  var now=0L;val gate=RequestGate(1000,clockMillis={now},sleep={now+=it})
  try {gate.execute{error("offline")}}catch(_:IllegalStateException){}
  gate.execute{1};assertEquals(1000L,now)
 }
}
