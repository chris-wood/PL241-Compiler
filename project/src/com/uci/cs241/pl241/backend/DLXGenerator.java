package com.uci.cs241.pl241.backend;

import java.util.ArrayList;

import com.uci.cs241.pl241.ir.PLIRInstruction;

public class DLXGenerator
{
	public ArrayList<DLXInstruction> instructions = new ArrayList<DLXInstruction>();

	public void convertFromColoredSSA(ArrayList<PLIRInstruction> ssaInstructions)
	{
		for (PLIRInstruction ssaInst : ssaInstructions)
		{
			switch (ssaInst.opcode)
			{
				case ADD:
					break;
				case SUB:
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
