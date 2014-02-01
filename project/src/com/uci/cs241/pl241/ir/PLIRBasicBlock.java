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
		id = bbid++;
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
		
		// If next is an entry, merge with what we have
		if (nextBlock.isEntry)
		{
			PLIRBasicBlock join = result;
			if (nextBlock.joinNode != null)
			{
				ArrayList<Integer> seen = new ArrayList<Integer>();
				while (join.joinNode != null && seen.contains(join.id) == false)
				{
					seen.add(join.id);
					join = join.joinNode;
				}
			}
			
			if (result.id == 34 || nextBlock.id == 34)
			{
				System.out.println("asd");
				int x = 0;
			}
			// Remove instructions in nextBlock that appear in result 
			for (PLIRInstruction inst : result.instructions)
			{
				System.err.println("Removing redundant instruction: " + inst.toString());
				nextBlock.instructions.remove(inst);
			}
			
			// Not symmetric, order matters.
			ArrayList<PLIRInstruction> toRemove = new ArrayList<PLIRInstruction>(); 
			for (PLIRInstruction inst : nextBlock.instructions)
			{
				join.instructions.add(inst); // CAW RESULT
				join.dominatedInstructions.add(inst); // CAW RESULT
				toRemove.add(inst);
			}
			
			// Remove instructions from nextBlock.instructions here since we just added them to the BB above
			for (PLIRInstruction inst : toRemove)
			{
				nextBlock.instructions.remove(inst);
			}
			
			// Handle parents
			if (nextBlock.children.size() > 0)
			{
				for (PLIRBasicBlock block : nextBlock.children)
				{
//					result.children.add(block);
					join.children.add(block);
					block.parents.add(result);
					block.parents.remove(nextBlock);
				}
			}
			for (PLIRBasicBlock block : nextBlock.dominatorSet)
			{
				result.dominatorSet.add(block);
//				join.dominatorSet.add(block);
			}
		}
		else
		{
			// Remove instructions in nextBlock that appear in result 
			for (PLIRInstruction inst : result.instructions)
			{
				System.err.println("Removing redundant instruction: " + inst.toString());
				nextBlock.instructions.remove(inst);
			}
			
			PLIRBasicBlock join = result;
			ArrayList<Integer> seen = new ArrayList<Integer>();
			while (join.joinNode != null && seen.contains(join.id) == false)
			{
				seen.add(join.id);
				join = join.joinNode;
			}
			
			// Not symmetric, order matters.
			ArrayList<PLIRInstruction> toRemove = new ArrayList<PLIRInstruction>(); 
			for (PLIRInstruction inst : nextBlock.instructions)
			{
				join.instructions.add(inst);
				join.dominatedInstructions.add(inst);
				toRemove.add(inst);
			}
			
			// Remove instructions from nextBlock.instructions here since we just added them to the BB above
			for (PLIRInstruction inst : toRemove)
			{
				nextBlock.instructions.remove(inst);
			}
		}
		
		/// TODO: we should really be adding these to a set of "dominated" instructions... not the instructions of the BB
		if (nextBlock.exitNode != null)
		{
			result.exitNode = nextBlock.exitNode;
			result.dominatorSet.add(nextBlock.exitNode);
			result.fixSpot();
		}
		
		if (nextBlock.joinNode != null)
		{
			result.fixSpot();
			
			PLIRBasicBlock join = nextBlock;
			ArrayList<Integer> seen = new ArrayList<Integer>();
			while (join.joinNode != null && seen.contains(join.id) == false)
			{
				seen.add(join.id);
				join = join.joinNode;
				
				for (String sym : result.modifiedIdents.keySet())
				{
					join.addModifiedValue(sym, result.modifiedIdents.get(sym));
				}
				for (String sym : result.usedIdents.keySet())
				{
					join.addUsedValue(sym, result.usedIdents.get(sym));
				}
			}
			
			if (join.instructions.size() > 0)
			{
				result.joinNode = join;
			}
		}
		
		return result;
	}
	
	public void propagatePhi(String var, PLIRInstruction phi, ArrayList<PLIRBasicBlock> visited)
	{
		// Propagate through the main instructions in this block's body
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
				child.propagatePhi(var, phi, visited);
			}
		}
		
		
		if (this.joinNode != null)
		{
			if (visited.contains(this.joinNode) == false)
			{
				visited.add(this.joinNode);
				exitNode.propagatePhi(var, phi, visited);
			}
		}
	}
	
	public void fixSpot()
	{
		// noop
//		if (!fixed)
//		{
//			fixed = true;
////			id = bbid++;
//		}
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
