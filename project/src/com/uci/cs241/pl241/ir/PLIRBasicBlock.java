package com.uci.cs241.pl241.ir;

import java.util.ArrayList;
import java.util.HashMap;

public class PLIRBasicBlock
{
	public ArrayList<PLIRInstruction> instructions;
	public ArrayList<PLIRBasicBlock> children;
	public ArrayList<PLIRBasicBlock> parents;
	public ArrayList<PLIRBasicBlock> treeVertexSet;
	
	// To be used when inserting phi functions in join nodes
	public HashMap<String, PLIRInstruction> modifiedIdents;
	public HashMap<String, PLIRInstruction> usedIdents;
	
	// These are set if we encounter branches, and must be handled accordingly
	// By default, they are null, so simple checks to see if they're null will help us determine whether we merge block 
	// instructions and where to place phi-instructions
	public PLIRBasicBlock joinNode;
	public PLIRBasicBlock exitNode;
	
	// Return instruction
	public boolean hasReturn = false;
	public PLIRInstruction returnInst;
	
	// TODO: need to compute this for the dominator tree algorithm!!!
	public int treeSize;
	
	public int id;
	public static int bbid = 0;
	
	public PLIRBasicBlock()
	{
		this.children = new ArrayList<PLIRBasicBlock>();
		this.parents = new ArrayList<PLIRBasicBlock>();
		this.treeVertexSet = new ArrayList<PLIRBasicBlock>();
		this.instructions = new ArrayList<PLIRInstruction>();
		this.modifiedIdents = new HashMap<String, PLIRInstruction>();
		this.usedIdents = new HashMap<String, PLIRInstruction>();
	}
	
	public PLIRBasicBlock(ArrayList<PLIRBasicBlock> childs, ArrayList<PLIRBasicBlock> parents, PLIRInstruction[] seq)
	{
		this.children = new ArrayList<PLIRBasicBlock>();
		this.parents = new ArrayList<PLIRBasicBlock>();
		this.treeVertexSet = new ArrayList<PLIRBasicBlock>();
		this.instructions = new ArrayList<PLIRInstruction>(seq.length);
		this.modifiedIdents = new HashMap<String, PLIRInstruction>();
		this.usedIdents = new HashMap<String, PLIRInstruction>();
		
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
		
		this.id = bbid++;
	}
	
	public void addUsedValue(String ident, PLIRInstruction inst)
	{
		usedIdents.put(ident, inst);
//		System.err.println(if i)
		if (ident.equals("a"))
		{
			System.err.println(inst);
//			System.err.println("inst: " + inst.toString());
		}
	}
	
	public void addModifiedValue(String ident, PLIRInstruction inst)
	{
		modifiedIdents.put(ident, inst);
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
		if (0 <= index && index < instructions.size())
		{
			instructions.add(index, inst);
			return true;
		}
		return false;
	}
	
	public String toVcgNodeString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("node: \n");
		builder.append("{\n");	
		builder.append("title: \" " + id + "\"  \n");
		
		// Build instruction sequence string
		StringBuilder instBuilder = new StringBuilder();
		for (PLIRInstruction inst : instructions)
		{
			instBuilder.append(inst.toString() + ";");
		}
		builder.append("label: \" " + instBuilder.toString() + " \"  \n");
		
		builder.append("}\n");
		return builder.toString();
	}
}
