#!/usr/bin/env python3
"""PSI syntax validation for all Kotlin sources; NOT Android/Gradle compilation."""
import os, pathlib, shutil, subprocess, tempfile, sys
root=pathlib.Path(__file__).resolve().parents[1]
kotlinc=shutil.which("kotlinc")
if not kotlinc: print("NOT RUNNABLE: kotlinc unavailable");sys.exit(2)
lib=pathlib.Path(kotlinc).resolve().parent.parent/"lib"
with tempfile.TemporaryDirectory(prefix="papereyes-syntax-") as temp:
 jar=pathlib.Path(temp)/"syntax.jar"
 subprocess.run([kotlinc,str(root/"tools/offline/SyntaxCheck.kt"),"-cp",str(lib/"kotlin-compiler.jar"),"-d",str(jar)],check=True)
 sys.exit(subprocess.run(["java","-cp",str(jar)+os.pathsep+str(lib/"*"),"SyntaxCheckKt",str(root/"app/src")]).returncode)
