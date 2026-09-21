package com.example.papereyes.domain.discovery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
class AbstractReconstructionTest {
 @Test fun ordersPositionsAndRepeatsWords() {
  assertEquals(listOf("the field and the particle"),AbstractReconstruction.segments(mapOf("particle" to listOf(4),"the" to listOf(3,0),"field" to listOf(1),"and" to listOf(2))))
 }
 @Test fun missingAndConflictingPositionsNeverInventPhrases() {
  assertEquals(listOf("a b","d"),AbstractReconstruction.segments(mapOf("a" to listOf(0),"b" to listOf(1),"d" to listOf(3))))
  assertEquals(listOf("a","d"),AbstractReconstruction.segments(mapOf("a" to listOf(0),"b" to listOf(1),"wrong" to listOf(1),"d" to listOf(2))))
 }
 @Test fun pathologicalAndAbsentInputsAreRejected() {
  assertTrue(AbstractReconstruction.segments(mapOf("x" to listOf(Int.MAX_VALUE))).isEmpty())
  assertTrue(AbstractReconstruction.segments(mapOf("x" to listOf(-1))).isEmpty())
  assertTrue(AbstractReconstruction.segments(null).isEmpty())
 }
}
