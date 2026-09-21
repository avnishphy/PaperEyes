import com.example.papereyes.ocr.DocumentLayoutAnalyzer

/** Synthetic text-only JVM CPU diagnostic; NOT OCR, camera, or network latency. */
object CoreMicrobenchmark {
 @JvmStatic fun main(args:Array<String>) {
  val cases=listOf(
   "arXiv:2609.20448v 1",
   "PHYSICAL REVIEW D 108, 036027 (2023)\nShedding light on shadow generalized parton distributions",
   "A comprehensive theory framework for\nperturbative calculations of δC in\nsuperallowed beta decays",
   "We investigate nonperturbative generalized distributions which reveal distinctive transverse correlations inside composite particles.\nIndependent lattice simulations constrain universal renormalization coefficients across discretization schemes.",
   "References\n[1] A paper doi:10.1000/cited\n[2] Another doi:10.1000/other")
  repeat(200){cases.forEach(DocumentLayoutAnalyzer::fromText)}
  val samples=ArrayList<Double>()
  repeat(1000){cases.forEach{input -> val t=System.nanoTime();DocumentLayoutAnalyzer.fromText(input);samples+=(System.nanoTime()-t)/1000000.0}}
  samples.sort()
  fun q(p:Double)=samples[((samples.size-1)*p).toInt()]
  println("SYNTHETIC TEXT-ONLY JVM CPU: n=${samples.size}, median_ms=${q(.5)}, p90_ms=${q(.9)}, p95_ms=${q(.95)}")
  println("Not evidence of Android OCR/import/live-scan accuracy or latency; no network involved.")
 }
}
