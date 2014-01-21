package com.uci.cs241.pl241.ir;

import java.util.ArrayList;
import java.util.List;

public class PLIRInstruction
{
	// Instruction descriptions
	public int id;
	public PLIRInstructionType opcode;
	public int i1;
	public PLIRInstruction op1;
	public PLIRInstructionOperandType op1type;
	public int i2;
	public PLIRInstruction op2;
	public PLIRInstructionOperandType op2type;
	
	// Unique identifier for each instruction that's generated
//	public static ArrayList<PLIRInstruction> ssaInstructions = new ArrayList<PLIRInstruction>();
	
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
		CONST, INST, NULL
	};
	
	public PLIRInstruction(PLIRInstructionType opcode)
	{
		this.opcode = opcode;
		op1 = op2 = null;
		op1type = op2type = PLIRInstructionOperandType.NULL;
		id = PLStaticSingleAssignment.addInstruction(this);
	}
	
	public PLIRInstruction(PLIRInstructionType opcode, PLIRInstruction singleOperand)
	{
		this.opcode = opcode;
		op1 = singleOperand;
		op2 = null;
		op1type = PLIRInstructionOperandType.INST; 
		op2type = PLIRInstructionOperandType.NULL;
		id = PLStaticSingleAssignment.addInstruction(this);
	}
	
	public PLIRInstruction(PLIRInstructionType opcode, PLIRInstruction left, PLIRInstruction right)
	{
		this.opcode = opcode;
		op1 = left;
		op2 = right;
		op1type = op2type = PLIRInstructionOperandType.INST;
		id = PLStaticSingleAssignment.addInstruction(this);
	}
	
	public PLIRInstruction(PLIRInstructionType opcode, PLIRInstruction left, PLIRInstructionOperandType leftType, PLIRInstruction right, PLIRInstructionOperandType rightType)
	{
		this.opcode = opcode;
		op1 = left;
		op1type = leftType;
		op2 = right;
		op2type = rightType;
		id = PLStaticSingleAssignment.addInstruction(this);
	}
	
	public PLIRInstruction(PLIRInstructionType opcode, PLIRInstruction left, PLIRInstructionOperandType leftType, int right)
	{
		this.opcode = opcode;
		op1 = left;
		op1type = leftType;
		op2 = null;
		op2type = PLIRInstructionOperandType.CONST;
		id = PLStaticSingleAssignment.addInstruction(this);
	}
	
	public PLIRInstruction(PLIRInstructionType opcode, int left, int right)
	{
		this.opcode = opcode;
		op1 = op2 = null;
		op1type = op2type = PLIRInstructionOperandType.CONST;
		i1 = left;
		i2 = right;
		id = PLStaticSingleAssignment.addInstruction(this);
	}
	
	public static PLIRInstruction createInputInstruction()
	{
		return null;
	}
	
	public static PLIRInstruction createOutputNumInstruction(PLIRInstruction val)
	{
		return null;
	}
	
	public static PLIRInstruction createOutputLineInstruction()
	{
		return null;
	}
	
	@Override
	public String toString()
	{
		String s = "";
		
		switch (opcode)
		{
			case ADD:
				s = "add";
				break;
			case ADDA:
				s = "adda";
				break;
			case WRITE:
				s = "write";
				break;
			case READ:
				s = "read";
				break;
			case WLN:
				s = "wln";
				break;
		}
		
		switch (op1type)
		{
			case INST:
				s = s + " (" + op1.id + ")";
				break;
			case CONST:
				s = s + " #" + i1;
				break;
		}
		
		switch (op2type)
		{
			case INST:
				s = s + " (" + op2.id + ")";
				break;
			case CONST:
				s = s + " #" + i2;
				break;
		}
		
		return s;
	}
	
	
	
	/// OLD BELOW
	
//	public static String InstructionString(PLIRInstructionType type)
//	{
//		return type.toString().toLowerCase();
//	}
//	
//	public List<PLIRInstruction> load(PLParseResult result)
//	{
//		List<PLIRInstruction> instructions = new ArrayList<PLIRInstruction>();
//		if (result.kind == PLParseResultKind.VAR)
//		{
////			result.regno = allocateReg();
//			
//			PLIRInstruction left = null;
//			PLIRInstruction right = null;
//			
//			PLIRInstruction inst = new PLIRInstruction(PLIRInstructionType.LOAD, left, PLIRInstructionOperandType.VAR, right, PLIRInstructionOperandType.VAR);
//			instructions.add(inst);
//			result.kind = PLParseResultKind.REG;
//		}
//		else if (result.kind == PLParseResultKind.CONST)
//		{
//			if (result.val == 0)
//			{
//				result.regno = 0;
//			}
//			else
//			{
////				result.regno = allocateReg();
//				
//				PLIRInstruction left = null;
//				PLIRInstruction right = null;
//				
//				PLIRInstruction inst = new PLIRInstruction(PLIRInstructionType.ADD, left, PLIRInstructionOperandType.VAR, right, PLIRInstructionOperandType.VAR);
//				instructions.add(inst);
//			}
//		}
//		
//		return instructions;
//	}
}
