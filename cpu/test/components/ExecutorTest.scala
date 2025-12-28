package components

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec
import common._

class ExecutorTest extends AnyFlatSpec {
  "Executor" should "execute ADD correctly" in {
    simulate(new Executor) { dut =>
      dut.io.decodedInst.aluOp.poke(ALUOpEnum.ADD)
      dut.io.decodedInst.op1.poke(10.U)
      dut.io.decodedInst.op2.poke(20.U)
      dut.io.decodedInst.regWriteDest.poke(3.U)
      
      dut.clock.step()
      
      dut.io.exOut.resultOrAddr.expect(30.U)
      dut.io.exOut.wdReg.expect(3.U)
    }
  }

  it should "handle branch BEQ correctly (taken)" in {
    simulate(new Executor) { dut =>
      dut.io.decodedInst.branchOp.poke(ControlOpEnum.BEQ)
      dut.io.decodedInst.op1.poke(10.U)
      dut.io.decodedInst.op2.poke(10.U)
      dut.io.decodedInst.pc.poke(100.U)
      dut.io.decodedInst.imm.poke(8.U)
      
      dut.clock.step()
      
      dut.io.jumpTo.valid.expect(true.B)
      dut.io.jumpTo.bits.expect(108.U)
    }
  }

  it should "handle branch BEQ correctly (not taken)" in {
    simulate(new Executor) { dut =>
      dut.io.decodedInst.branchOp.poke(ControlOpEnum.BEQ)
      dut.io.decodedInst.op1.poke(10.U)
      dut.io.decodedInst.op2.poke(20.U)
      dut.io.decodedInst.pc.poke(100.U)
      dut.io.decodedInst.imm.poke(8.U)
      
      dut.clock.step()
      
      dut.io.jumpTo.valid.expect(false.B)
    }
  }
}
