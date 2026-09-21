import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtPsiFactory
import java.io.File

fun main(args: Array<String>) {
 val disposable=Disposer.newDisposable()
 try {
  val environment=KotlinCoreEnvironment.createForProduction(disposable,CompilerConfiguration(),EnvironmentConfigFiles.JVM_CONFIG_FILES)
  val factory=KtPsiFactory(environment.project,false)
  var errors=0;var files=0
  File(args.single()).walkTopDown().filter{it.isFile && (it.extension=="kt" || it.extension=="kts")}.forEach { file ->
   files++
   val parsed=if(file.extension=="kts") factory.createFile(file.name,file.readText()) else factory.createFile(file.name,file.readText())
   PsiTreeUtil.findChildrenOfType(parsed,PsiErrorElement::class.java).forEach{error ->
    println("SYNTAX ERROR ${file.path}:${error.textOffset}: ${error.errorDescription}");errors++
   }
  }
  println("KOTLIN SYNTAX: $files files parsed, $errors errors (not Android symbol/API checking)")
  check(errors==0)
 } finally {Disposer.dispose(disposable)}
}
