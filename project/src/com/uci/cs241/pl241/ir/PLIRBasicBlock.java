package com.uci.cs241.pl241.ir;

import java.util.ArrayList;

public class PLIRBasicBlock
{
	private ArrayList<PLIRInstruction> instructions;
	
	public PLIRBasicBlock(PLIRInstruction[] seq)
	{
		instructions = new ArrayList(seq.length);
		for (int i = 0; i < seq.length; i++)
		{
			instructions.add(seq[i]);;
		}
	}
	
	public boolean addInstruction(PLIRInstruction inst)
	{
		instructions.add(inst);
		
		return true;
	}
	
	public boolean removeInstruction(PLIRInstruction inst)
	{
		instructions.remove(inst);
		
		return true;
	}
	
	public boolean insertInstruction(PLIRInstruction inst, int index)
	{
		if (0 < index && index < instructions.size())
		{
			instructions.add(index, inst);
			return true;
		}
		return false;
	}
}
