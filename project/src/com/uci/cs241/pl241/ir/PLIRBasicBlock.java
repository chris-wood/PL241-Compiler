package com.uci.cs241.pl241.ir;

import java.util.ArrayList;
import java.util.HashMap;

import com.uci.cs241.pl241.ir.PLIRInstruction.PLIRInstructionType;

public class PLIRBasicBlock
{
	public ArrayList<PLIRInstruction> instructions;
	public ArrayList<PLIRInstruction> dominatedInstructions;
	public ArrayList<PLIRBasicBlock> children;
	public ArrayList<PLIRBasicBlock> parents;
	public ArrayList<PLIRBasicBlock> treeVertexSet;
	public ArrayList<PLIRBasicBlock> dominatorSet;
	
	// To be used when inserting phi functions in join nodes
	public HashMap<String, PLIRInstruction> modifiedIdents;
	public HashMap<String, PLIRInstruction> usedIdents;
	
	// These are set if we encounter branches, and must be handled accordingly
	// By default, they are null, so simple checks to see if they're null will help us determine whether we merge block 
	// instructions and where to place phi-instructions
	public PLIRBasicBlock joinNode;
	public PLIRBasicBlock exitNode;
	
	// Return instruction
	public boolean isEntry = false;
	public boolean hasReturn = false;
	public PLIRInstruction returnInst;
	
	// TODO: need to compute this for the dominator tree algorithm!!!
	public int treeSize;
	
	// ID information for the BB
	public int id;
	public static int bbid = 0;
	private boolean fixed = false;
	
	public PLIRBasicBlock()
	{
		this.children = new ArrayList<PLIRBasicBlock>();
		this.parents = new ArrayList<PLIRBasicBlock>();
		this.treeVertexSet = new ArrayList<PLIRBasicBlock>();
		this.dominatorSet = new ArrayList<PLIRBasicBlock>();
		this.instructions = new ArrayList<PLIRInstruction>();
		this.dominatedInstructions = new ArrayList<PLIRInstruction>();
		this.modifiedIdents = new HashMap<String, PLIRInstruction>();
		this.usedIdents = new HashMap<String, PLIRInstruction>();
	}
	
	public PLIRBasicBlock(ArrayList<PLIRBasicBlock> childs, ArrayList<PLIRBasicBlock> parents, PLIRInstruction[] seq)
	{
		this.children = new ArrayList<PLIRBasicBlock>();
		this.parents = new ArrayList<PLIRBasicBlock>();
		this.treeVertexSet = new ArrayList<PLIRBasicBlock>();
		this.dominatorSet = new ArrayList<PLIRBasicBlock>();
		this.instructions = new ArrayList<PLIRInstruction>(seq.length);
		this.modifiedIdents = new HashMap<String, PLIRInstruction>();
		this.dominatedInstructions = new ArrayList<PLIRInstruction>();
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
	
	public static PLIRBasicBlock merge(PLIRBasicBlock result, PLIRBasicBlock nextBlock)
	{
		// merge the blocks intermediate results here
		for (String sym : nextBlock.modifiedIdents.keySet())
		{
			result.addModifiedValue(sym, nextBlock.modifiedIdents.get(sym));
		}
		for (String sym : nextBlock.usedIdents.keySet())
		{
			result.addUsedValue(sym, nextBlock.usedIdents.get(sym));
		}
//		for (PLIRInstruction inst : nextBlock.instructions)
//		{
//			result.instructions.add(inst);
//			result.dominatedInstructions.add(inst);
//		}
		
		// Add to parent
		nextBlock.parents.add(result);
		
		// Merge the contents of the blocks
		if (nextBlock.isEntry)
		{
//			for (PLIRInstruction inst : nextBlock.instructions)
			for (int i = 0; i < nextBlock.instructions.size(); i++)
			{
				result.addInstruction(nextBlock.instructions.get(i));
			}
			if (nextBlock.children.size() > 0)
			{
				for (PLIRBasicBlock block : nextBlock.children)
				{
					result.children.add(block);
				}
				for (PLIRBasicBlock block : nextBlock.dominatorSet)
				{
					result.dominatorSet.add(block);
				}
			}
			
			/// TODO: we should really be adding these to a set of "dominated" instructions... not the instructions of the BB
			if (nextBlock.exitNode != null)
			{
				for (PLIRInstruction inst : nextBlock.exitNode.instructions)
				{
//					result.addInstruction(inst);
//					result.dominatedInstructions.add(inst);
				}
			}
		}
		
		// If the node has an exit, continue on that node (only really changes if statements)
		if (nextBlock.exitNode != null)
		{
			result.fixSpot();
			result.dominatorSet.add(nextBlock.exitNode);
//			result = nextBlock.exitNode;
		}
		
		return result;
	}
	
	public void propogatePhi(String var, PLIRInstruction phi)
	{
		// propoagate through the main instructions in this block's body
		for (PLIRInstruction bInst : instructions)
		{
			System.err.println(bInst.toString());
			boolean replaced = false;
//			if (bInst.op1 != null && bInst.op1.origIdent.equals(var))
			if (bInst.op1 != null && (bInst.op1.equals(phi.op1) || bInst.op1.equals(phi.op2)))
			{
				bInst.replaceLeftOperand(phi);
				replaced = true;
			}
//			if (bInst.op2 != null && bInst.op2.origIdent.equals(var))
			if (bInst.op2 != null && (bInst.op2.equals(phi.op1) || bInst.op2.equals(phi.op2)))
			{
				bInst.replaceRightOperand(phi);
				replaced = true;
			}
			if (replaced && bInst.opcode == PLIRInstructionType.PHI)
			{
				break;
			}
		}
		
		// propagate through dominated instructions
//		for (PLIRInstruction bInst : dominatedInstructions)
//		{
//			System.err.println(bInst.toString());
//			boolean replaced = false;
////			if (bInst.op1 != null && bInst.op1.origIdent.equals(var))
//			if (bInst.op1 != null && (bInst.op1.equals(phi.op1) || bInst.op1.equals(phi.op2)))
//			{
//				bInst.replaceLeftOperand(phi);
//				replaced = true;
//			}
////			if (bInst.op2 != null && bInst.op2.origIdent.equals(var))
//			if (bInst.op2 != null && (bInst.op2.equals(phi.op1) || bInst.op2.equals(phi.op2)))
//			{
//				bInst.replaceRightOperand(phi);
//				replaced = true;
//			}
//			if (replaced && bInst.opcode == PLIRInstructionType.PHI)
//			{
//				break;
//			}
//		}
		
		for (PLIRBasicBlock child : children)
		{
			child.propogatePhi(var, phi);
		}
		
		if (this.exitNode != null)
		{
			exitNode.propogatePhi(var, phi);
		}
	}
	
	public void fixSpot()
	{
		if (!fixed)
		{
			fixed = true;
			id = bbid++;
		}
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
	
	public PLIRInstruction getLastInst()
	{
		return instructions.get(instructions.size() - 1);
	}
	
	public boolean insertInstruction(PLIRInstruction inst, int index)
	{
		if (0 <= index && index <= instructions.size())
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
	
	public ArrayList<String> instSequence()
	{
		ArrayList<PLIRInstruction> orderedInsts = new ArrayList<PLIRInstruction>();
		for (PLIRInstruction inst : instructions)
		{
			if (orderedInsts.size() == 0) orderedInsts.add(inst);
			else
			{
				boolean added = false;
				for (int i = 0; !added && i < orderedInsts.size(); i++)
				{
					if (inst.id < orderedInsts.get(i).id)
					{
						orderedInsts.add(i, inst);
						added = true;
					}
				}
				if (!added) orderedInsts.add(inst);
			}
		}
		
		ArrayList<String> builder = new ArrayList<String>();
		ArrayList<Integer> seen = new ArrayList<Integer>();
		for (PLIRInstruction inst : orderedInsts)
		{
			if (seen.contains(inst.id) == false)
			{
				builder.add(inst.id + " := " + inst.toString());
				seen.add(inst.id);
			}
		}
		return builder;
	}
	
	public String instSequenceString()
	{
		ArrayList<String> insts = instSequence();
		
		StringBuilder builder = new StringBuilder();
		for (String s : insts)
		{
			builder.append(s + "\n");
		}
		
		return builder.toString();
	}
}
