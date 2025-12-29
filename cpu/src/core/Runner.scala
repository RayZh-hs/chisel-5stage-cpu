package core

import chisel3._
import chisel3.simulator.EphemeralSimulator._

object Runner {
  final case class RunResult(exitCode: Option[BigInt], cycles: Int)

  def run(hexPath: String, maxCycles: Int = 1000, verbose: Boolean = false): RunResult = {
    val absHexPath = new java.io.File(hexPath).getAbsolutePath
    var result: RunResult = RunResult(None, 0)

    simulate(new RV32ICore(absHexPath, verbose)) { dut =>
      dut.reset.poke(true.B)
      dut.clock.step(5)
      dut.reset.poke(false.B)

      var cycles = 0
      var exitCode: Option[BigInt] = None
      while (exitCode.isEmpty && cycles < maxCycles) {
        dut.clock.step()
        cycles += 1
        if (dut.io.exitCode.valid.peek().litToBoolean) {
          exitCode = Some(dut.io.exitCode.bits.peek().litValue)
        }
      }
      result = RunResult(exitCode, cycles)
    }

    result
  }

  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      println("Usage: Runner <hex_file> [max_cycles] [verbose]")
      sys.exit(1)
    }
    val hexPath = args(0)
    val maxCycles = if (args.length > 1) args(1).toInt else 1000
    val verbose = if (args.length > 2) args(2).toBoolean else false

    val res = run(hexPath, maxCycles, verbose)
    res.exitCode match {
      case Some(code) =>
        println(s"Program exited with code: $code")
      case None =>
        println(s"Simulation timed out after ${res.cycles} cycles")
        sys.exit(1)
    }
  }
}
