package com.uci.cs241.pl241.ir;

import java.util.ArrayList;
import java.util.HashMap;

import com.uci.cs241.pl241.ir.PLIRInstruction.PLIRInstructionType;

public class PLIRBasicBlock
{
	public ArrayList<PLIRInstruction> instructions;
	public ArrayList<PLIRInstruction> dominatedInstructions;
	public ArrayList<PLIRInstruction> carriedInstructions;
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
	
	// For rendering
	public boolean omit = false;
	
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
		this.carriedInstructions = new ArrayList<PLIRInstruction>();
		this.modifiedIdents = new HashMap<String, PLIRInstruction>();
		this.usedIdents = new HashMap<String, PLIRInstruction>();
	}
	
	public static PLIRBasicBlock merge(PLIRBasicBlock result, PLIRBasicBlock nextBlock)
	{
		// Remove instructions in nextBlock that appear in result 
		for (PLIRInstruction inst : result.instructions)
		{
			System.err.println("Removing redundant instruction: " + inst.toString());
			nextBlock.instructions.remove(inst);
		}
		
		// Merge the blocks intermediate results here
		for (String sym : nextBlock.modifiedIdents.keySet())
		{
			result.addModifiedValue(sym, nextBlock.modifiedIdents.get(sym));
		}
		for (String sym : result.modifiedIdents.keySet())
		{
			nextBlock.addModifiedValue(sym, result.modifiedIdents.get(sym));
		}
		for (String sym : nextBlock.usedIdents.keySet())
		{
			result.addUsedValue(sym, nextBlock.usedIdents.get(sym));
		}
		for (String sym : result.usedIdents.keySet())
		{
			nextBlock.addUsedValue(sym, result.usedIdents.get(sym));
		}
		
		// Not symmetric, order matters.
		ArrayList<PLIRInstruction> toRemove = new ArrayList<PLIRInstruction>(); 
		for (PLIRInstruction inst : nextBlock.instructions)
		{
			result.instructions.add(inst);
			result.dominatedInstructions.add(inst);
			toRemove.add(inst);
		}
		
		// remove instructions from nextBlock.instructions here since we just added them to the BB above
		for (PLIRInstruction inst : toRemove)
		{
			nextBlock.instructions.remove(inst);
		}
		
		// Add to parent
		nextBlock.parents.add(result);
		
		// If next is an entry, merge with what we have
		if (nextBlock.isEntry)
		{
			// Handle parents
			if (nextBlock.children.size() > 0)
			{
				for (PLIRBasicBlock block : nextBlock.children)
				{
					result.children.add(block);
					block.parents.add(result);
					block.parents.remove(nextBlock);
				}
				for (PLIRBasicBlock block : nextBlock.dominatorSet)
				{
					result.dominatorSet.add(block);
				}
			}
		}
		
		/// TODO: we should really be adding these to a set of "dominated" instructions... not the instructions of the BB
		if (nextBlock.exitNode != null)
		{
			result.exitNode = nextBlock.exitNode;
			result.fixSpot();
			result.dominatorSet.add(nextBlock.exitNode);
		}
		
		if (nextBlock.joinNode != null)
		{	
			result.fixSpot();
			result.joinNode = nextBlock.joinNode;
		}
		
		return result;
	}
	
	public void propogatePhi(String var, PLIRInstruction phi, ArrayList<PLIRBasicBlock> visited)
	{
		// propoagate through the main instructions in this block's body
//		for (PLIRInstruction bInst : instructions)
		for (PLIRInstruction bInst : instructions)
		{
			System.err.println(bInst.toString());
			boolean replaced = false;
			
			if (bInst.op1 != null && (bInst.op1.equals(phi.op1) || bInst.op1.equals(phi.op2)))
			{
				bInst.replaceLeftOperand(phi);
				replaced = true;
			}
			if (bInst.op1 != null && bInst.op1.origIdent.equals(var))
			{
				bInst.replaceLeftOperand(phi);
				replaced = true;
			}
			
			if (bInst.op2 != null && (bInst.op2.equals(phi.op1) || bInst.op2.equals(phi.op2)))
			{
				bInst.replaceRightOperand(phi);
				replaced = true;
			}
			if (bInst.op2 != null && bInst.op2.origIdent.equals(var))
			{
				bInst.replaceRightOperand(phi);
				replaced = true;
			}
			
			// Don't propagate past the phi, since it essentially replaces the current value
			if (replaced && bInst.opcode == PLIRInstructionType.PHI)
			{
				break;
			}
		}
	
		// Now propagate down the tree
		for (PLIRBasicBlock child : children)
		{
			if (visited.contains(child) == false)
			{
				visited.add(child);
				child.propogatePhi(var, phi, visited);
			}
		}
		
		
		if (this.joinNode != null)
		{
			if (visited.contains(this.joinNode) == false)
			{
				visited.add(this.joinNode);
				exitNode.propogatePhi(var, phi, visited);
			}
		}
	}
	
	public void fixSpot()
	{
		if (!fixed)
		{
			fixed = true;
			id = bbid++;
			if (id == 0)
			{
				int x = 0;
			}
		}
	}
	
	public void addUsedValue(String ident, PLIRInstruction inst)
	{
		usedIdents.put(ident, inst);
	}
	
	public void addModifiedValue(String ident, PLIRInstruction inst)
	{
		modifiedIdents.put(ident, inst);
	}
	
	public boolean addInstruction(PLIRInstruction inst)
	{
		if (inst != null && inst.id == 1)
		{
			int x = 0;
		}
		if (inst != null)
		{
			carriedInstructions.add(inst);
			dominatedInstructions.add(inst);
			return instructions.add(inst);
		}
		return false;
	}
	
	public boolean removeInstruction(PLIRInstruction inst)
	{
		return instructions.remove(inst);
	}
	
	public PLIRInstruction getLastInst()
	{
		if (instructions.size() > 0)
		{
			return instructions.get(instructions.size() - 1);
		}
		return null;
	}
	
	public boolean insertInstruction(PLIRInstruction inst, int index)
	{
		if (0 <= index && index <= instructions.size())
		{
			dominatedInstructions.add(inst);
			instructions.add(index, inst);
			return true;
		}
		return false;
	}
	
	public ArrayList<String> instSequence(ArrayList<Integer> globalSeen)
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
			if (seen.contains(inst.id) == false && globalSeen.contains(inst.id) == false)
			{
				globalSeen.add(inst.id);
				builder.add(inst.id + " := " + inst.toString());
				seen.add(inst.id);
			}
		}
		return builder;
	}
	
	public String instSequenceString(ArrayList<Integer> seen)
	{
		ArrayList<String> insts = instSequence(seen);
		
		StringBuilder builder = new StringBuilder();
		for (String s : insts)
		{
			builder.append(s + "\n");
		}
		
		return builder.toString();
	}
}
