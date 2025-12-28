package common

import chisel3._
import chisel3.util._

/**
  * ### Request-Return Bundle
  *
  * Primarily used for memories, where the response does not come within the cycle
  * 
  * @param addrBits The number of bits for the address
  * @param dataBits The number of bits for the data
  */
class RequestReturnBundle(val addrBits: Int, val dataBits: Int) extends Bundle {
    val addr = Output(UInt(addrBits.W))
    val data = Input(UInt(dataBits.W))

    def readFrom(addr: UInt): UInt = {
        this.addr := addr
        this.data
    }
}

// Comm Bundles

// Communication bundle between ID and REG stages
class IdRegCommBundle extends Bundle {
    val scoreboardParam0 = new RequestReturnBundle(addrBits = 5, dataBits = 1)
    val scoreboardParam1 = new RequestReturnBundle(addrBits = 5, dataBits = 1)
    val regAccessParam0 = new RequestReturnBundle(addrBits = 5, dataBits = 32)
    val regAccessParam1 = new RequestReturnBundle(addrBits = 5, dataBits = 32)
    val markBusy = Output(Valid(UInt(5.W)))
}

/**
  * ### Decoded Instruction Bundle
  * 
  * Bundle outputted by the InstDecoder after decoding an instruction,
  * Contains all control signals and immediate values needed for execution.
  * 
  * All the register-related fields have been converted to actual data values.
  * 
  * @param aluOp ALU operation to be performed
  * @param memOp Memory operation to be performed
  * @param regWriteDest Destination register address for write-back (write to 0 to discard write-back)
  * @param memWriteData Data to be written to memory (for store instructions)
  */
class DecodedInstructionBundle extends Bundle {
    val aluOp = ALUOpEnum()
    val memOp = MemoryOpEnum()
    val branchOp = ControlOpEnum()
    val memOpWidth = MemoryOpWidthEnum()
    val memReadSigned = Bool()

    // for alu, it is op1 and op2
    // for memory, op1 is base addr, op2 is write data
    // for branch, op1 and op2 are compared, pc is branch target
    val op1 = UInt(32.W)
    val op2 = UInt(32.W)
    val regWriteDest = UInt(5.W)
    val pc = UInt(32.W)
    val imm = UInt(32.W)
}

class memOpBundle extends Bundle {
    val memOp = MemoryOpEnum()
    val memOpWidth = MemoryOpWidthEnum()
    val memReadSigned = Bool()
    val memWriteData = UInt(32.W)
}

class ExOutBundle extends Bundle {
    val resultOrAddr = UInt(32.W) // ALU Result
    val memBundle = new memOpBundle()
    val wdReg = UInt(5.W)
}

object DecodedInstructionBundle {
    def ofNoop(): DecodedInstructionBundle = {
        val bundle = WireDefault(0.U.asTypeOf(new DecodedInstructionBundle))
        bundle
    }
}

class MemWbBundle extends Bundle {
    val data = UInt(32.W)
    val wbReg = UInt(5.W)
}

class IfOutBundle extends Bundle {
    val pc = UInt(32.W)
    val inst = UInt(32.W)
}