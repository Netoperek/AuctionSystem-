package auction.helpers

import java.io._
import scala.collection.mutable.MutableList

object TimeMeasure {
  var data : List[String] = List();

  private def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

  def addResult(result: String) {
    data = data :+ result
  }

  def writeResultsToFile() {
    printToFile(new File(SystemSettings.RESULTS_OUTPUT_FILE)) {
      p =>
        {
          if (p != null) data.foreach(p.println)
        }
    }
  }

}