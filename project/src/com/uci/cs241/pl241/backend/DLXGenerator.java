package com.uci.cs241.pl241.backend;

import java.util.ArrayList;

import com.uci.cs241.pl241.backend.DLXInstruction.InstructionType;
import com.uci.cs241.pl241.ir.PLIRInstruction;
import com.uci.cs241.pl241.ir.PLIRInstruction.OperandType;
import com.uci.cs241.pl241.ir.PLIRInstruction.ResultKind;

public class DLXGenerator
{
	public ArrayList<DLXInstruction> instructions = new ArrayList<DLXInstruction>();

	public void convertFromColoredSSA(ArrayList<PLIRInstruction> ssaInstructions)
	{
		for (PLIRInstruction ssaInst : ssaInstructions)
		{
			DLXInstruction newInst = new DLXInstruction();
			
			// Determine if the instruction contains an immediate value
			boolean isImmediate = false;
			boolean leftConst = false;
			boolean rightConst = false;
			if (ssaInst.kind == ResultKind.CONST)
			{
				isImmediate = true;
			}
			if (ssaInst.op1type == OperandType.CONST)
			{
				isImmediate = true;
				leftConst = true;
			}
			if (ssaInst.op2type == OperandType.CONST)
			{
				isImmediate = true;
				rightConst = true;
			}
			
			// Create the instruction accordingly
			switch (ssaInst.opcode)
			{
				case ADD:
					if (leftConst && rightConst)
					{
						newInst.opcode = InstructionType.ADDI;
					}
					else if (leftConst)
					{
						newInst.opcode = InstructionType.ADDI;
					}
					else if (rightConst)
					{
						newInst.opcode = InstructionType.ADDI;
					}
					else
					{
						newInst.opcode = InstructionType.ADD;
					}
					break;
				case SUB:
					if (leftConst && rightConst)
					{
						newInst.opcode = InstructionType.SUBI;
					}
					else if (leftConst)
					{
						newInst.opcode = InstructionType.SUBI;
					}
					else if (rightConst)
					{
						newInst.opcode = InstructionType.SUBI;
					}
					else
					{
						newInst.opcode = InstructionType.SUB;
					}
					break;
				case MUL:
					break;
				case DIV:
					break;
				case ADDA:
					break;
				case WRITE:
					break;
				case READ:
					break;
				case WLN:
					break;
				case END:
					break;
				case CMP:
					break;
				case BEQ:
					break;
				case BNE:
					break;
				case BLT:
					break;
				case BGE:
					break;
				case BLE:
					break;
				case BGT:
					break;
				case FUNC:
					break;
				case PROC:
					break;
				case LOADPARAM:
					break;
				case LOAD:
					break;
				case STORE:
					break;
				case PHI:
					break;
			}
		}
	}

}
