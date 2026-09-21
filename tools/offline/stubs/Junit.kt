// Minimal assertion/annotation compatibility shim, NOT the JUnit runtime.
package org.junit
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Test
object Assert {
 @JvmStatic fun assertEquals(expected: Any?, actual: Any?) { if (expected != actual) throw AssertionError("expected <$expected>, got <$actual>") }
 @JvmStatic fun assertEquals(message: String, expected: Any?, actual: Any?) { if (expected != actual) throw AssertionError("$message: expected <$expected>, got <$actual>") }
 @JvmStatic fun assertEquals(expected: Double, actual: Double, delta: Double) { if (java.lang.Double.compare(expected, actual) != 0 && !(kotlin.math.abs(expected-actual) <= delta)) throw AssertionError("expected <$expected>, got <$actual>") }
 @JvmStatic fun assertNotEquals(expected: Any?, actual: Any?) { if (expected == actual) throw AssertionError("unexpected equality <$actual>") }
 @JvmStatic fun assertTrue(value: Boolean) { if (!value) throw AssertionError("expected true") }
 @JvmStatic fun assertTrue(message: String, value: Boolean) { if (!value) throw AssertionError(message) }
 @JvmStatic fun assertFalse(value: Boolean) { if (value) throw AssertionError("expected false") }
 @JvmStatic fun assertFalse(message: String, value: Boolean) { if (value) throw AssertionError(message) }
 @JvmStatic fun assertNull(value: Any?) { if (value != null) throw AssertionError("expected null, got <$value>") }
 @JvmStatic fun assertNotNull(value: Any?) { if (value == null) throw AssertionError("expected non-null") }
 @JvmStatic fun fail(message: String): Nothing = throw AssertionError(message)
}
