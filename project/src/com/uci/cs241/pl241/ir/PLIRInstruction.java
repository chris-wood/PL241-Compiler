package com.uci.cs241.pl241.ir;

public class PLIRInstruction
{
	public enum PLIRInstructionType 
	{
		NEG,
		ADD,
		SUB,
		MUL,
		DIV,
		CMP,
		
		ADDA,
		LOAD,
		STORE,
		MOVE,
		PHI,
		
		END,
		
		BRA,
		BNE,
		BEQ,
		BLE,
		BLT,
		BGE,
		BGT,
		
		READ,
		WRITE,
		WLN
	};
	
	public PLIRInstruction(PLIRInstructionType opcode, PLIRInstruction left, PLIRInstruction right)
	{
		this.opcode = opcode;
		op1 = left;
		op2 = right;
		id = globalId++;
	}
	
	public static int globalId = 0;
	
	public static String InstructionString(PLIRInstructionType type)
	{
		return type.toString().toLowerCase();
	}
	
	// Instruction descriptions
	private int id;
	private PLIRInstructionType opcode;
	private PLIRInstruction op1;
	private PLIRInstruction op2;
}
