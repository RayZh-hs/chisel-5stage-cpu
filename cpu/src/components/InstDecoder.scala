package components

import chisel3._
import chisel3.util._
import utility._
import common._

class InstDecoder extends CycleAwareModule {
    val io = IO(new Bundle {
        val inst = Input(UInt(32.W))
        val pc = Input(UInt(32.W))
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

    io.flushPC.valid := false.B
    io.flushPC.bits := 0.U
    io.regComm.markBusy.valid := false.B
    io.regComm.markBusy.bits := 0.U

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
        }.otherwise {
            // if just stalled and is no longer busy, this means the registers are now free
            regOccupiedRecord0 := 0.U
            regOccupiedRecord1 := 0.U
            canForward := true.B
        }
    }

    when(canForward) {
        // Forward the decoded instruction to EX
        // Handle jumps now to avoid flushing later stages
        
        // Read register values
        val rs1Val = io.regComm.regAccessParam0.readFrom(internalDecodedInst.rs1)
        val rs2Val = io.regComm.regAccessParam1.readFrom(internalDecodedInst.rs2)

        // Default assignments
        io.decodedInst.aluOp := ALUOpEnum.ADD
        io.decodedInst.memOp := MemoryOpEnum.NONE
        io.decodedInst.memOpWidth := MemoryOpWidthEnum.WORD
        io.decodedInst.aluOp1 := rs1Val
        io.decodedInst.aluOp2 := internalDecodedInst.imm
        io.decodedInst.regWriteDest := internalDecodedInst.rd
        io.decodedInst.memWriteData := 0.U

        // Determine signals based on opcode
        switch(internalDecodedInst.opcode) {
            is("b0110011".U) { // R-Type
                io.decodedInst.aluOp := getALUOp(internalDecodedInst.funct3, internalDecodedInst.funct7, true.B)
                io.decodedInst.aluOp2 := rs2Val
            }
            is("b0010011".U) { // I-Type (ALU)
                io.decodedInst.aluOp := getALUOp(internalDecodedInst.funct3, internalDecodedInst.funct7, false.B)
            }
            is("b0000011".U) { // Load
                io.decodedInst.memOp := MemoryOpEnum.READ
                io.decodedInst.memOpWidth := getMemOpWidth(internalDecodedInst.funct3)
            }
            is("b0100011".U) { // Store
                io.decodedInst.memOp := MemoryOpEnum.WRITE
                io.decodedInst.memOpWidth := getMemOpWidth(internalDecodedInst.funct3)
                io.decodedInst.memWriteData := rs2Val
                io.decodedInst.regWriteDest := 0.U
            }
            is("b1100011".U) { // Branch
                io.decodedInst.regWriteDest := 0.U
            }
            is("b0110111".U) { // LUI
                io.decodedInst.aluOp1 := 0.U
            }
            is("b0010111".U) { // AUIPC
                io.decodedInst.aluOp1 := io.pc
            }
            is("b1101111".U) { // JAL
                io.decodedInst.aluOp1 := io.pc
                io.decodedInst.aluOp2 := 4.U
            }
            is("b1100111".U) { // JALR
                io.decodedInst.aluOp1 := io.pc
                io.decodedInst.aluOp2 := 4.U
            }
        }

        // Mark register as busy
        when(io.decodedInst.regWriteDest =/= 0.U) {
            io.regComm.markBusy.valid := true.B
            io.regComm.markBusy.bits := io.decodedInst.regWriteDest
        }

        // Handle Jumps
        val isJal = internalDecodedInst.opcode === "b1101111".U
        val isJalr = internalDecodedInst.opcode === "b1100111".U
        val isBranch = internalDecodedInst.opcode === "b1100011".U

        when(isJal) {
            io.flushPC.valid := true.B
            io.flushPC.bits := io.pc + internalDecodedInst.imm
            io.invokeStallFrontend := true.B
        } .elsewhen(isJalr) {
            io.flushPC.valid := true.B
            io.flushPC.bits := (rs1Val + internalDecodedInst.imm) & ~1.U(32.W)
            io.invokeStallFrontend := true.B
        } .elsewhen(isBranch && checkBranchCondition(internalDecodedInst.funct3, rs1Val, rs2Val)) {
            io.flushPC.valid := true.B
            io.flushPC.bits := io.pc + internalDecodedInst.imm
            io.invokeStallFrontend := true.B
        }
    }

    private def getALUOp(funct3: UInt, funct7: UInt, isRType: Bool): ALUOpEnum.Type = {
        val op = WireDefault(ALUOpEnum.ADD)
        switch(funct3) {
            is("b000".U) { // ADD/SUB
                when(isRType && funct7(5)) { op := ALUOpEnum.SUB }
                .otherwise { op := ALUOpEnum.ADD }
            }
            is("b001".U) { op := ALUOpEnum.SLL }
            is("b010".U) { op := ALUOpEnum.SLT }
            is("b011".U) { op := ALUOpEnum.SLTU }
            is("b100".U) { op := ALUOpEnum.XOR }
            is("b101".U) { // SRL/SRA
                when(funct7(5)) { op := ALUOpEnum.SRA }
                .otherwise { op := ALUOpEnum.SRL }
            }
            is("b110".U) { op := ALUOpEnum.OR }
            is("b111".U) { op := ALUOpEnum.AND }
        }
        op
    }

    private def getMemOpWidth(funct3: UInt): MemoryOpWidthEnum.Type = {
        val width = WireDefault(MemoryOpWidthEnum.WORD)
        switch(funct3) {
            is("b000".U) { width := MemoryOpWidthEnum.BYTE }
            is("b001".U) { width := MemoryOpWidthEnum.HALFWORD }
            is("b010".U) { width := MemoryOpWidthEnum.WORD }
            is("b100".U) { width := MemoryOpWidthEnum.BYTE } // LBU
            is("b101".U) { width := MemoryOpWidthEnum.HALFWORD } // LHU
        }
        width
    }

    private def checkBranchCondition(funct3: UInt, rs1: UInt, rs2: UInt): Bool = {
        val conditionMet = WireDefault(false.B)
        switch(funct3) {
            is("b000".U) { conditionMet := rs1 === rs2 } // BEQ
            is("b001".U) { conditionMet := rs1 =/= rs2 } // BNE
            is("b100".U) { conditionMet := rs1.asSInt < rs2.asSInt } // BLT
            is("b101".U) { conditionMet := rs1.asSInt >= rs2.asSInt } // BGE
            is("b110".U) { conditionMet := rs1 < rs2 } // BLTU
            is("b111".U) { conditionMet := rs1 >= rs2 } // BGEU
        }
        conditionMet
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

        switch(decoded.instType) {
            is(common.InstTypeEnum.R_TYPE, common.InstTypeEnum.S_TYPE, common.InstTypeEnum.B_TYPE) {
                reg1 := decoded.rs1
                reg2 := decoded.rs2
            }
            is(common.InstTypeEnum.I_TYPE) {
                reg1 := decoded.rs1
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
