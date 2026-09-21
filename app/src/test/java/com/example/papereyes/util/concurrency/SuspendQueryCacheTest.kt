package com.example.papereyes.util.concurrency
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
class SuspendQueryCacheTest {
 @Test fun simultaneousSameQueryLoadsOnce() = runBlocking {
  var calls=0
  val cache=SuspendQueryCache<String,Int>()
  val values=(1..12).map {async {cache.getOrLoad("same") {delay(10);++calls}}}.awaitAll()
  assertTrue(values.all {it==1});assertEquals(1,calls)
 }
 @Test fun expirationAndLruEvictionWork() = runBlocking {
  var now=0L;var calls=0
  val cache=SuspendQueryCache<String,Int>(capacity=2,clockMillis={now},ttlMillis={100})
  cache.getOrLoad("a"){++calls};cache.getOrLoad("b"){++calls};cache.getOrLoad("a"){++calls}
  cache.getOrLoad("c"){++calls};cache.getOrLoad("b"){++calls};assertEquals(4,calls)
  now=101;cache.getOrLoad("b"){++calls};assertEquals(5,calls)
 }
 @Test fun exceptionAndCancellationDoNotPoisonCache() = runBlocking {
  val cache=SuspendQueryCache<String,Int>()
  try {cache.getOrLoad("key"){throw CancellationException()}} catch (_:CancellationException) {}
  try {cache.getOrLoad("key"){error("offline")}} catch (_:IllegalStateException) {}
  assertEquals(7,cache.getOrLoad("key"){7})
 }
}
