package com.uci.cs241.pl241.ir;

import java.util.ArrayList;

public class PLIRBasicBlock
{
	public ArrayList<PLIRInstruction> instructions;
	public ArrayList<PLIRBasicBlock> children;
	public ArrayList<PLIRBasicBlock> parents;
	public ArrayList<PLIRBasicBlock> treeVertexSet;
	public int treeSize;
	
	public PLIRBasicBlock(ArrayList<PLIRBasicBlock> childs, ArrayList<PLIRBasicBlock> parents, PLIRInstruction[] seq)
	{
		this.children = new ArrayList<PLIRBasicBlock>();
		this.parents = new ArrayList<PLIRBasicBlock>();
		this.treeVertexSet = new ArrayList<PLIRBasicBlock>();
		this.instructions = new ArrayList<PLIRInstruction>(seq.length);
		
		for (PLIRBasicBlock block : parents)
		{
			this.parents.add(block);
		}	
		for (PLIRBasicBlock block : childs)
		{
			this.children.add(block);
		}
		for (int i = 0; i < seq.length; i++)
		{
			this.instructions.add(seq[i]);;
		}
	}
	
	public boolean addInstruction(PLIRInstruction inst)
	{
		return instructions.add(inst);
	}
	
	public boolean removeInstruction(PLIRInstruction inst)
	{
		return instructions.remove(inst);
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
