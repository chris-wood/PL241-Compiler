package com.uci.cs241.pl241.backend;

import com.uci.cs241.pl241.ir.PLIRInstruction;

public class DLXInstruction
{
	// Instruction information
	public InstructionType opcode;
	public InstructionFormat format;
	public long encodedForm;
	
	public DLXBasicBlock block;
	
	public String refFunc; 
	public boolean jumpNeedsFix = false;
	
	public int fixupLocation;
	public int pc = -1;
	public PLIRInstruction ssaInst;
	public int offset = 0;
	
	// Register operands
	public int ra;
	public int rb;
	public int rc;
	
	// Possible immediate value
	public int ic; 
	
	@Override
	public String toString()
	{
		return pc + ": " + opcode.toString() + " " + ra + " " + rb + " " + rc;
	}
	
	public enum InstructionFormat
	{
		F1, F2, F3
	}
	
	public enum InstructionType
	{
		// immediate and non-immediate arithmetic instructions
		ADD,
		ADDI,
		SUB,
		SUBI,
		MUL,
		MULI,
		DIV,
		DIVI,
		MOD,
		MODI,
		CMP,
		CMPI,
		OR,
		ORI,
		AND,
		ANDI,
		BIC,
		BICI,
		XOR,
		XORI,
		LSH,
		LSHI,
		ASH,
		ASHI,
		CHK,
		CHKI,
		
		// load/store instructions
		LDW,
		LDX,
		POP,
		STW,
		STX,
		PSH,
		
		// control instructions
		BEQ,
		BNE,
		BLT,
		BGE,
		BLE,
		BGT,
		BSR,
		JSR,
		RET,
		
		// input/output instructions
		RDD,
		WRD,
		WRH,
		WRL
	}
	
	public static boolean isBranch(DLXInstruction inst)
	{
		return (inst.opcode == InstructionType.BEQ || inst.opcode == InstructionType.BNE 
				|| inst.opcode == InstructionType.BLT || inst.opcode == InstructionType.BGE 
				|| inst.opcode == InstructionType.BLE || inst.opcode == InstructionType.BGT);
	}
}
