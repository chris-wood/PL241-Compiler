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
	public String label = "";
	
	// TODO: need to compute this for the dominator tree algorithm!!!
	public int treeSize;
	
	// ID information for the BB
	public int id;
	public static int bbid = 0;
	
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
	
	public static PLIRBasicBlock merge(PLIRBasicBlock oldBlock, PLIRBasicBlock newBlock)
	{	
		// Merge the blocks intermediate results here
		for (String sym : newBlock.modifiedIdents.keySet())
		{
			oldBlock.addModifiedValue(sym, newBlock.modifiedIdents.get(sym));
		}
		for (String sym : oldBlock.modifiedIdents.keySet())
		{
			newBlock.addModifiedValue(sym, oldBlock.modifiedIdents.get(sym));
		}
		for (String sym : newBlock.usedIdents.keySet())
		{
			oldBlock.addUsedValue(sym, newBlock.usedIdents.get(sym));
		}
		for (String sym : oldBlock.usedIdents.keySet())
		{
			newBlock.addUsedValue(sym, oldBlock.usedIdents.get(sym));
		}
		
		// Walk to the right spot on the BB tree
		PLIRBasicBlock leftJoin = oldBlock;
		if (leftJoin.joinNode != null)
		{
			ArrayList<Integer> seen = new ArrayList<Integer>();
			while (leftJoin.joinNode != null && seen.contains(leftJoin.id) == false)
			{
				seen.add(leftJoin.id);
				leftJoin = leftJoin.joinNode;
			}
		}
		
		// Remove instructions in nextBlock that appear in result 
		for (PLIRInstruction inst : oldBlock.instructions)
		{
//			System.err.println("Removing redundant instruction: " + inst.toString());
			newBlock.instructions.remove(inst);
		}
		
		// Not symmetric, order matters.
		ArrayList<PLIRInstruction> toRemove = new ArrayList<PLIRInstruction>();
		if (newBlock.joinNode == null || newBlock.isEntry)
		{ 
			for (PLIRInstruction inst : newBlock.instructions)
			{
				leftJoin.instructions.add(inst); // CAW RESULT
				leftJoin.dominatedInstructions.add(inst); // CAW RESULT
				toRemove.add(inst);
			}
		}
		
		// Remove instructions from nextBlock.instructions here since we just added them to the BB above
		for (PLIRInstruction inst : toRemove)
		{
			newBlock.instructions.remove(inst);
		}
		
		// Handle parents
		if (newBlock.isEntry && newBlock.children.size() > 0)
		{
			for (PLIRBasicBlock block : newBlock.children)
			{
				// oldBlock children gets the immediate children
				leftJoin.children.add(block);
				
				// the joinNode of the child points back to the oldBlock
				if (block.joinNode == null)
				{
					block.parents.add(leftJoin);
					if (block.children.contains(newBlock))
					{
						block.children.add(leftJoin);
						block.children.remove(newBlock);
					}
				}
				else
				{
					block.joinNode.parents.add(leftJoin);
					if (block.joinNode.children.contains(newBlock))
					{
						block.joinNode.children.add(leftJoin);
						block.joinNode.children.remove(newBlock);
					}
				}
			}
		}
		
		// Add the dominator sets
		for (PLIRBasicBlock block : newBlock.dominatorSet)
		{
			oldBlock.dominatorSet.add(block);
			leftJoin.dominatorSet.add(block);
		}
		
		oldBlock.fixSpot();
		leftJoin.fixSpot();
		
		PLIRBasicBlock join = newBlock;
		ArrayList<Integer> seen = new ArrayList<Integer>();
		while (join.joinNode != null && seen.contains(join.id) == false)
		{
			seen.add(join.id);
			join = join.joinNode;
			
			for (String sym : oldBlock.modifiedIdents.keySet())
			{
				join.addModifiedValue(sym, oldBlock.modifiedIdents.get(sym));
			}
			for (String sym : oldBlock.usedIdents.keySet())
			{
				join.addUsedValue(sym, oldBlock.usedIdents.get(sym));
			}
		}
		
		// The join of the new tree becomes the join of the old tree (see picture - basically the old tree swallows the new tree)
		if (join.equals(newBlock) == false) leftJoin.joinNode = join;
		
		return oldBlock;
	}
	
	public void propagatePhi(String var, PLIRInstruction phi, ArrayList<PLIRBasicBlock> visited, HashMap<String, PLIRInstruction> scopeMap)
	{
		PLIRInstruction findPhi = phi;
		PLIRInstruction replacePhi = phi;
		
		// Propagate through the main instructions in this block's body
		for (PLIRInstruction bInst : instructions)
		{
//			System.err.println(bInst.toString());
//			System.err.println(bInst.origIdent);
//			if (bInst.opcode == PLIRInstructionType.WRITE)
//			{
//				System.err.println("here");
//			}
			boolean replaced = false;
			
			if (bInst.op1 != null && (bInst.op1.equals(findPhi.op1) || bInst.op1.equals(findPhi.op1)))
			{
//				bInst.replaceLeftOperand(replacePhi);
				bInst.replaceLeftOperand(scopeMap.get(var));
				replaced = true;
			}
			if (bInst.op1 != null && bInst.op1.origIdent.equals(var))
			{
				bInst.replaceLeftOperand(scopeMap.get(var));
				replaced = true;
			}
			if (bInst.op1 != null && bInst.op1.equals(findPhi))
			{
//				bInst.replaceLeftOperand(replacePhi);
				bInst.replaceLeftOperand(scopeMap.get(var));
				replaced = true;
			}
			
			if (bInst.op2 != null && (bInst.op2.equals(findPhi.op1) || bInst.op2.equals(findPhi.op2)))
			{
				bInst.replaceRightOperand(scopeMap.get(var));
				replaced = true;
			}
			if (bInst.op2 != null && bInst.op2.origIdent.equals(var))
			{
				bInst.replaceRightOperand(scopeMap.get(var));
				replaced = true;
			}
			if (bInst.op2 != null && bInst.op2.equals(findPhi))
			{
				bInst.replaceRightOperand(scopeMap.get(var));
				replaced = true;
			}
			
			// If the phi value was used to replace some operand, and this same expression was used to save a result, replace
			// with the newly generated result
			if (replaced && bInst.origIdent.equals(phi.origIdent))
			{
				scopeMap.put(var, bInst);
			}
			
			// Don't propagate past the phi, since it essentially replaces the current value
			if (replaced && bInst.opcode == PLIRInstructionType.PHI)
			{
				findPhi = bInst; 
				scopeMap.put(var, bInst);
			}
		}
	
		// Now propagate down the tree
		for (PLIRBasicBlock child : children)
		{
			if (visited.contains(child) == false)
			{
				visited.add(child);
				child.propagatePhi(var, phi, visited, scopeMap);
			}
		}
		
		
		if (this.joinNode != null)
		{
//			if (visited.contains(this.joinNode) == false)
//			{
				visited.add(this.joinNode);
				joinNode.propagatePhi(var, phi, visited, scopeMap);
//			}
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
