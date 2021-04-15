// Copyright (C) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License. See LICENSE in project root for information.

package com.microsoft.ml.spark.codegen

import java.io.File

import com.microsoft.ml.spark.codegen.Config._
import com.microsoft.ml.spark.core.env.FileUtilities._
import com.microsoft.ml.spark.core.test.base.TestBase
import com.microsoft.ml.spark.core.test.fuzzing.PyTestFuzzing
import com.microsoft.ml.spark.core.utils.JarLoadingUtils.instantiateServices
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils._

object CodeGenUtils {
  def clean(dir: File): Unit = if (dir.exists()) FileUtils.forceDelete(dir)

  def toDir(f: File): File = new File(f, File.separator)
}

object OldCodeGen {
  import CodeGenUtils._

  def main(args: Array[String]): Unit = {
    clean(PackageDir)
    clean(RSrcDir)
    SparklyRWrapperGenerator(Version)
    RPackageDir.mkdirs()
    zipFolder(RSrcDir, new File(RPackageDir, s"mmlspark-$Version.zip"))
  }

}

object CodeGen {
  import CodeGenUtils._

  def generatePythonClasses(): Unit = {
    instantiateServices[Wrappable].foreach { w =>
      w.makePyFile()
    }
  }

  private def makeInitFiles(packageFolder: String = ""): Unit = {
    val dir = new File(new File(PySrcDir, "mmlspark"), packageFolder)
    val packageString = if (packageFolder != "") packageFolder.replace("/", ".") else ""
    val importStrings =
      dir.listFiles.filter(_.isFile).sorted
        .map(_.getName)
        .filter(name => name.endsWith(".py") && !name.startsWith("_") && !name.startsWith("test"))
        .map(name => s"from mmlspark$packageString.${getBaseName(name)} import *\n").mkString("")
    writeFile(new File(dir, "__init__.py"), packageHelp(importStrings))
    dir.listFiles().filter(_.isDirectory).foreach(f =>
      makeInitFiles(packageFolder + "/" + f.getName)
    )
  }

  def main(args: Array[String]): Unit = {
    clean(PackageDir)
    clean(PySrcDir)
    generatePythonClasses()
    TestBase.stopSparkSession()
    FileUtils.copyDirectoryToDirectory(toDir(PySrcOverrideDir), toDir(PySrcDir))
    makeInitFiles()
    OldCodeGen.main(args)
  }
}

object TestGen {
  import CodeGenUtils._

  def generatePythonTests(): Unit = {
    instantiateServices[PyTestFuzzing[_]].foreach { ltc =>
      try {
        ltc.makePyTestFile()
      } catch {
        case _: NotImplementedError =>
          println(s"ERROR: Could not generate test for ${ltc.testClassName} because of Complex Parameters")
      }
    }
  }

  private def makeInitFiles(packageFolder: String = ""): Unit = {
    val dir = new File(new File(PyTestDir, "mmlsparktest"), packageFolder)
    writeFile(new File(dir, "__init__.py"), "")
    dir.listFiles().filter(_.isDirectory).foreach(f =>
      makeInitFiles(packageFolder + "/" + f.getName)
    )
  }

  def main(args: Array[String]): Unit = {
    clean(TestDataDir)
    clean(PyTestDir)
    generatePythonTests()
    TestBase.stopSparkSession()
    FileUtils.copyDirectoryToDirectory(toDir(PyTestOverrideDir), toDir(PyTestDir))
    makeInitFiles()
  }
}
