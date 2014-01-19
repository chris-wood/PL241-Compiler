package com.uci.cs241.pl241.ir;

import java.util.ArrayList;
import java.util.List;

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
	
	public enum PLIRInstructionOperandType
	{
		CONST, VAR, REG, COND
	};
	
	public PLIRInstruction(int tok)
	{
		// TODO
	}
	
	public PLIRInstruction(PLIRInstructionType opcode, PLIRInstruction left, PLIRInstructionOperandType leftType, PLIRInstruction right, PLIRInstructionOperandType rightType)
	{
		this.opcode = opcode;
		op1 = left;
		op2 = right;
		id = globalSSAIndex++;
	}
	
	public PLIRInstruction(PLIRInstructionType opcode, PLIRInstruction left, PLIRInstructionOperandType leftType, int right)
	{
		this.opcode = opcode;
		op1 = left;
		op1type = leftType;
		op2 = null;
		op2type = PLIRInstructionOperandType.CONST;
		id = globalSSAIndex++;
	}
	
	public PLIRInstruction(PLIRInstructionType opcode, int left, int right)
	{
		this.opcode = opcode;
		op1 = op2 = null;
		op1type = op2type = PLIRInstructionOperandType.CONST;
		i1 = left;
		i2 = right;
		id = globalSSAIndex++;
	}
	
	public static int globalSSAIndex = 0;
	
	public static String InstructionString(PLIRInstructionType type)
	{
		return type.toString().toLowerCase();
	}
	
	// Instruction descriptions
	public int id;
	public PLIRInstructionType opcode;
	public int i1;
	public PLIRInstruction op1;
	public PLIRInstructionOperandType op1type;
	public int i2;
	public PLIRInstruction op2;
	public PLIRInstructionOperandType op2type;
	
	public List<PLIRInstruction> load(PLParseResult result)
	{
		List<PLIRInstruction> instructions = new ArrayList<PLIRInstruction>();
		if (result.kind == PLParseResultKind.VAR)
		{
//			result.regno = allocateReg();
			
			PLIRInstruction left = null;
			PLIRInstruction right = null;
			
			PLIRInstruction inst = new PLIRInstruction(PLIRInstructionType.LOAD, left, PLIRInstructionOperandType.VAR, right, PLIRInstructionOperandType.VAR);
			instructions.add(inst);
			result.kind = PLParseResultKind.REG;
		}
		else if (result.kind == PLParseResultKind.CONST)
		{
			if (result.val == 0)
			{
				result.regno = 0;
			}
			else
			{
//				result.regno = allocateReg();
				
				PLIRInstruction left = null;
				PLIRInstruction right = null;
				
				PLIRInstruction inst = new PLIRInstruction(PLIRInstructionType.ADD, left, PLIRInstructionOperandType.VAR, right, PLIRInstructionOperandType.VAR);
				instructions.add(inst);
			}
		}
		
		return instructions;
	}
}
