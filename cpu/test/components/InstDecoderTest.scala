package components

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec
import common._

class InstDecoderTest extends AnyFlatSpec {
  "InstDecoder" should "decode ADDI instruction correctly" in {
    simulate(new InstDecoder) { dut =>
      // ADDI x1, x0, 10  => 000000001010 00000 000 00001 0010011
      val inst = "h00a00093".U(32.W)
      
      dut.io.ifInput.valid.poke(true.B)
      dut.io.ifInput.bits.inst.poke(inst)
      dut.io.ifInput.bits.pc.poke(0.U)
      
      // Mock register file response (not busy)
      dut.io.regComm.scoreboardParam0.data.poke(0.U)
      dut.io.regComm.scoreboardParam1.data.poke(0.U)
      dut.io.regComm.regAccessParam0.data.poke(0.U) // x0 is 0
      dut.io.regComm.regAccessParam1.data.poke(0.U)
      
      dut.clock.step()
      
      dut.io.decodedInst.aluOp.expect(ALUOpEnum.ADD)
      dut.io.decodedInst.op1.expect(0.U)
      dut.io.decodedInst.op2.expect(10.U)
      dut.io.decodedInst.regWriteDest.expect(1.U)
      dut.io.decodedInst.imm.expect(10.U)
    }
  }

  it should "decode ADD instruction correctly" in {
    simulate(new InstDecoder) { dut =>
      // ADD x3, x1, x2 => 0000000 00010 00001 000 00011 0110011
      val inst = "h002081b3".U(32.W)
      
      dut.io.ifInput.valid.poke(true.B)
      dut.io.ifInput.bits.inst.poke(inst)
      dut.io.ifInput.bits.pc.poke(4.U)
      
      // Mock register file response
      dut.io.regComm.scoreboardParam0.data.poke(0.U)
      dut.io.regComm.scoreboardParam1.data.poke(0.U)
      dut.io.regComm.regAccessParam0.data.poke(10.U) // x1 = 10
      dut.io.regComm.regAccessParam1.data.poke(20.U) // x2 = 20
      
      dut.clock.step()
      
      dut.io.decodedInst.aluOp.expect(ALUOpEnum.ADD)
      dut.io.decodedInst.op1.expect(10.U)
      dut.io.decodedInst.op2.expect(20.U)
      dut.io.decodedInst.regWriteDest.expect(3.U)
    }
  }
}
