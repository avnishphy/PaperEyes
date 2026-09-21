package com.example.papereyes.util.concurrency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
class CompletionGateTest {
 @Test fun defersCloseUntilAllCompletionsAndRejectsNewWork() {
  var closes=0;val gate=CompletionGate{closes++}
  assertTrue(gate.acquire());assertTrue(gate.acquire());gate.close();gate.close()
  assertTrue(!gate.acquire());assertEquals(0,closes)
  gate.release();assertEquals(0,closes);gate.release();assertEquals(1,closes);gate.close();assertEquals(1,closes)
 }
 @Test fun concurrentCompletionsCloseExactlyOnce() {
  val count=AtomicInteger();val gate=CompletionGate{count.incrementAndGet()}
  repeat(100){assertTrue(gate.acquire())};gate.close()
  val workers=(1..100).map {Thread{gate.release()}.also{it.start()}}
  workers.forEach{it.join()};assertEquals(1,count.get())
 }
 @Test fun unusedResourceClosesImmediately() {
  var count=0;val gate=CompletionGate{count++};gate.close();assertEquals(1,count)
 }
}
