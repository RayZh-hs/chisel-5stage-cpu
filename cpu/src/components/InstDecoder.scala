package components

import chisel3._
import chisel3.util._
import utility._
import common._

class InstDecoder extends CycleAwareModule {
    val io = IO(new Bundle {
        val inst = Input(UInt(32.W))
        val decodedInst = Output(new DecodedInstructionBundle)
        val regComm = new IdRegCommBundle()
        val invokeStallFrontend = Output(Bool())
        val frontendIsStalled = Input(Bool())
        val flushPC = Output(Valid(UInt(32.W)))
    })

    private val internalDecodedInst = WireDefault(0.U.asTypeOf(new InternalDecodedInstBundle))
    private val regOccupiedRecord0 = RegInit(0.U(5.W))
    private val regOccupiedRecord1 = RegInit(0.U(5.W))
    private val isBusy = WireDefault(false.B)
    private val canForward = WireDefault(false.B)

    private val justStalled = io.frontendIsStalled

    when(justStalled) {
        // wait for both of the registers to be free before continuing
        val recordStall0 = io.regComm.regOccupiedParam0.readFrom(regOccupiedRecord0).asBool
        val recordStall1 = io.regComm.regOccupiedParam1.readFrom(regOccupiedRecord1).asBool
        isBusy := recordStall0 || recordStall1
    } .otherwise {
        isBusy := false.B
    }

    when(isBusy) {
        io.invokeStallFrontend := true.B
        canForward := false.B

        // Send empty response to backend
        io.decodedInst := DecodedInstructionBundle.ofNoop()
    } .otherwise {
        io.invokeStallFrontend := false.B
        
        // Decode instruction
        internalDecodedInst := getDecodedInst(io.inst)

        // Identify hazards
        val (criticalReg1, criticalReg2) = getCriticalRegisters(internalDecodedInst)
        when(!justStalled) {
            val isOccupied0 = io.regComm.regOccupiedParam0.readFrom(criticalReg1).asBool
            val isOccupied1 = io.regComm.regOccupiedParam1.readFrom(criticalReg2).asBool
            regOccupiedRecord0 := Mux(isOccupied0, criticalReg1, 0.U)
            regOccupiedRecord1 := Mux(isOccupied1, criticalReg2, 0.U)
            canForward := !(isOccupied0 || isOccupied1)
        }
    }

    when(canForward) {
        // Forward the decoded instruction to EX
        // Handle jumps now to avoid flushing later stages
        // TODO: Implement jump handling and else passing decoded instruction to the output bundle
    }

    private def getDecodedInst(inst: UInt): InternalDecodedInstBundle = {
        val decoded = WireDefault(0.U.asTypeOf(new InternalDecodedInstBundle))

        decoded.opcode := inst(6, 0)
        decoded.rd := inst(11, 7)
        decoded.funct3 := inst(14, 12)
        decoded.rs1 := inst(19, 15)
        decoded.rs2 := inst(24, 20)
        decoded.funct7 := inst(31, 25)
        
        // Immediate extraction
        val immI = Cat(Fill(20, io.inst(31)), io.inst(31, 20))
        val immS = Cat(Fill(20, io.inst(31)), io.inst(31, 25), io.inst(11, 7))
        val immB = Cat(Fill(19, io.inst(31)), io.inst(31), io.inst(7), io.inst(30, 25), io.inst(11, 8), 0.U(1.W))
        val immU = Cat(io.inst(31, 12), 0.U(12.W))
        val immJ = Cat(Fill(11, io.inst(31)), io.inst(31), io.inst(19, 12), io.inst(20), io.inst(30, 21), 0.U(1.W))

        // Default values
        decoded.instType := common.InstTypeEnum.I_TYPE
        decoded.imm := immI

        // Set inst type and immediate based on opcode
        switch(decoded.opcode) {
            is("b0110011".U) { // R-Type
                decoded.instType := common.InstTypeEnum.R_TYPE
                decoded.imm := 0.U
            }
            is("b0010011".U) { // I-Type (ALU immediate)
                decoded.instType := common.InstTypeEnum.I_TYPE
                decoded.imm := immI
            }
            is("b0000011".U) { // I-Type (Load)
                decoded.instType := common.InstTypeEnum.I_TYPE
                decoded.imm := immI
            }
            is("b0100011".U) { // S-Type (Store)
                decoded.instType := common.InstTypeEnum.S_TYPE
                decoded.imm := immS
            }
            is("b1100011".U) { // B-Type (Branch)
                decoded.instType := common.InstTypeEnum.B_TYPE
                decoded.imm := immB
            }
            is("b0110111".U) { // U-Type (LUI)
                decoded.instType := common.InstTypeEnum.U_TYPE
                decoded.imm := immU
            }
            is("b0010111".U) { // U-Type (AUIPC)
                decoded.instType := common.InstTypeEnum.U_TYPE
                decoded.imm := immU
            }
            is("b1101111".U) { // J-Type (JAL)
                decoded.instType := common.InstTypeEnum.J_TYPE
                decoded.imm := immJ
            }
            is("b1100111".U) { // I-Type (JALR)
                decoded.instType := common.InstTypeEnum.I_TYPE
                decoded.imm := immI
            }
        }

        decoded
    }

    private def getCriticalRegisters(decoded: InternalDecodedInstBundle): (UInt, UInt) = {
        val reg1 = WireDefault(0.U(5.W))
        val reg2 = WireDefault(0.U(5.W))

        switch(decoded.opcode) {
            is("b0110011".U) { // R-Type
                reg1 := decoded.rs1
                reg2 := decoded.rs2
            }
            is("b0010011".U, "b0000011".U, "b1100111".U) { // I-Type
                reg1 := decoded.rs1
                reg2 := 0.U
            }
            is("b0100011".U) { // S-Type
                reg1 := decoded.rs1
                reg2 := decoded.rs2
            }
            is("b1100011".U) { // B-Type
                reg1 := decoded.rs1
                reg2 := decoded.rs2
            }
        }

        (reg1, reg2)
    }

    /**
     * Internal Decoded Instruction Bundle
     *
     * Used within the InstDecoder to represent decoded instructions.
     * The exported version is based on this class but with register values already fetched and ready.
     */
    private class InternalDecodedInstBundle extends Bundle {
        val instType = common.InstTypeEnum()
        val opcode = UInt(7.W)
        val rd = UInt(5.W)
        val funct3 = UInt(3.W)
        val rs1 = UInt(5.W)
        val rs2 = UInt(5.W)
        val funct7 = UInt(7.W)
        val imm = UInt(32.W)
    }
}
