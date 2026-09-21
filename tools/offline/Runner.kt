import org.junit.Test
import java.lang.reflect.InvocationTargetException
fun main(args: Array<String>) {
 var passed = 0; var failed = 0
 for (name in args) {
  val klass = Class.forName(name)
  for (method in klass.declaredMethods.sortedBy { it.name }) {
   if (method.getAnnotation(Test::class.java) == null) continue
   try { method.invoke(klass.getDeclaredConstructor().newInstance()); passed++; println("PASS $name.${method.name}") }
   catch (e: InvocationTargetException) { failed++; println("FAIL $name.${method.name}: ${e.targetException}"); e.targetException.printStackTrace() }
  }
 }
 println("PORTABLE ASSERTIONS: $passed passed, $failed failed (not Android, Gradle, or JUnit runtime)")
 check(failed == 0) { "$failed portable tests failed" }
}
