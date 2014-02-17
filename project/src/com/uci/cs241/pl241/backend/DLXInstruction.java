package com.uci.cs241.pl241.backend;

public class DLXInstruction
{
	// Instruction information
	public InstructionType opcode;
	public InstructionFormat format;
	
	// Register operands
	public int ra;
	public int rb;
	public int rc;
	
	// Possible immediate value
	public int ic; 
	
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
		DMPI,
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
}
