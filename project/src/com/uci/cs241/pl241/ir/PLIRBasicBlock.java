package com.uci.cs241.pl241.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.uci.cs241.pl241.frontend.PLSymbolTable;
import com.uci.cs241.pl241.ir.PLIRInstruction.OperandType;
import com.uci.cs241.pl241.ir.PLIRInstruction.InstructionType;
import com.uci.cs241.pl241.ir.PLIRInstruction.ResultKind;

public class PLIRBasicBlock
{
	public ArrayList<PLIRInstruction> instructions;
	
	public PLIRBasicBlock leftChild;
	public PLIRBasicBlock rightChild;
	public ArrayList<PLIRBasicBlock> parents;
	
	public ArrayList<PLIRBasicBlock> treeVertexSet;
	public ArrayList<PLIRBasicBlock> dominatorSet;
	
	// To be used when inserting phi functions in join nodes
	public HashMap<String, PLIRInstruction> modifiedIdents;
	public HashMap<String, PLIRInstruction> usedIdents;
	
	public String arrayName;
	public String scopeName;
	
	// These are set if we encounter branches, and must be handled accordingly
	// By default, they are null, so simple checks to see if they're null will help us determine whether we merge block 
	// instructions and where to place phi-instructions
	public PLIRBasicBlock joinNode;
	
	// Return instruction
	public boolean isEntry = false;
	public boolean isWhileEntry = false;
	public boolean hasReturn = false;
	public PLIRInstruction returnInst;
	
	// instruction array handling
	public ArrayList<PLIRInstruction> arrayOperands;
	
	// For rendering
	public boolean omit = false;
	public String label = "";
	
	// TODO: need to compute this for the dominator tree algorithm!!!
	public int treeSize;
	
	// ID information for the BB
	public int id;
	public static int bbid = 0;
	
	// for live range calculation
	public int visitNumber = 0;
	public int mark = 0;
	public HashSet<PLIRInstruction> liveAtEnd = new HashSet<PLIRInstruction>();
	public boolean isLoopHeader = false;
	public ArrayList<PLIRBasicBlock> wrappedLoopHeaders = new ArrayList<PLIRBasicBlock>(); 
	
	public PLIRBasicBlock()
	{
		id = bbid++;
		this.parents = new ArrayList<PLIRBasicBlock>();
		this.treeVertexSet = new ArrayList<PLIRBasicBlock>();
		this.dominatorSet = new ArrayList<PLIRBasicBlock>();
		this.instructions = new ArrayList<PLIRInstruction>();
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
		
		// Save whileEntry flag
		oldBlock.isWhileEntry = oldBlock.isWhileEntry || newBlock.isWhileEntry;
		
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
			if (inst.type != OperandType.CONST)
			{
				newBlock.instructions.remove(inst);
			}
		}
		
		// Not symmetric, order matters.
		ArrayList<PLIRInstruction> toRemove = new ArrayList<PLIRInstruction>();
		if (newBlock.joinNode == null || newBlock.isEntry || newBlock.returnInst != null)
		{ 
			for (PLIRInstruction inst : newBlock.instructions)
			{
//				if (inst.opcode != InstructionType.BEQ)
				{
					leftJoin.instructions.add(inst); 
					toRemove.add(inst);
				}
			}
		}
		
		// Remove instructions from nextBlock.instructions here since we just added them to the BB above
		for (PLIRInstruction inst : toRemove)
		{
			newBlock.instructions.remove(inst);
		}
		
		// Handle parents
//		if (newBlock.isEntry && newBlock.children.size() > 0)
//		{
//			for (PLIRBasicBlock block : newBlock.children)
//			{
//				// oldBlock children gets the immediate children
//				leftJoin.children.add(block);
//				
//				// the joinNode of the child points back to the oldBlock
//				if (block.joinNode == null)
//				{
//					block.parents.add(leftJoin);
//					if (block.children.contains(newBlock))
//					{
//						block.children.add(leftJoin);
//						block.children.remove(newBlock);
//					}
//				}
//				else
//				{
//					block.joinNode.parents.add(leftJoin);
//					if (block.joinNode.children.contains(newBlock))
//					{
//						block.joinNode.children.add(leftJoin);
//						block.joinNode.children.remove(newBlock);
//					}
//				}
//			}
//		}
		
		if (newBlock.isLoopHeader)
		{
			leftJoin.isLoopHeader = true;
		}
		
		if (newBlock.hasReturn)
		{
			oldBlock.hasReturn = true;
		}
		
		// Propagate enclosed loop headers into the new block
		for (PLIRBasicBlock wlh : oldBlock.wrappedLoopHeaders)
		{
			HashSet<PLIRBasicBlock> seen = new HashSet<PLIRBasicBlock>();
			seen.add(newBlock);
			newBlock.propogateLoopHeader(seen, wlh);
		}
		
		if (newBlock.isEntry)
		{
			// Handle left child
			if (newBlock.leftChild != null)
			{	
				leftJoin.leftChild = newBlock.leftChild;
			}
//			newBlock.leftChild.wrappedLoopHeaders.add(leftJoin);
			
			if (newBlock.leftChild != null && newBlock.leftChild.joinNode == null)
			{
				newBlock.leftChild.parents.add(leftJoin);
				if (newBlock.leftChild.rightChild != null && newBlock.leftChild.rightChild.equals(newBlock))
				{
					newBlock.leftChild.rightChild = leftJoin;
				}
			}
			else if (newBlock.leftChild != null)
			{
				newBlock.leftChild.joinNode.parents.add(leftJoin);
				if (newBlock.leftChild.joinNode.rightChild != null && newBlock.leftChild.joinNode.rightChild.equals(newBlock))
				{
					newBlock.leftChild.joinNode.rightChild = leftJoin;
				}
			}
			
			// Handle right child
			if (newBlock.rightChild != null)
			{
				leftJoin.rightChild = newBlock.rightChild;
			}
//			newBlock.rightChild.wrappedLoopHeaders.add(leftJoin);
			
			if (newBlock.rightChild != null && newBlock.rightChild.joinNode == null)
			{
				newBlock.rightChild.parents.add(leftJoin);
				if (newBlock.rightChild.rightChild != null && newBlock.rightChild.rightChild.equals(newBlock))
				{
					newBlock.rightChild.rightChild = leftJoin;
				}
				// same for right child?!?!
			}
			else if (newBlock.rightChild != null)
			{
				newBlock.rightChild.joinNode.parents.add(leftJoin);
				if (newBlock.rightChild.joinNode.rightChild != null && newBlock.rightChild.joinNode.rightChild.equals(newBlock))
				{
					newBlock.rightChild.joinNode.rightChild = leftJoin;
				}
			}
		}
		
		// Absorb the dominator sets
		for (PLIRBasicBlock block : newBlock.dominatorSet)
		{
			leftJoin.dominatorSet.add(block);
		}
		
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
		if (join.equals(newBlock) == false) 
		{
			leftJoin.joinNode = join;
			oldBlock.joinNode = join;
		}
		
		return oldBlock;
	}
	
	// Propagate the enclosing loop header to all 
	public void propogateLoopHeader(HashSet<PLIRBasicBlock> seen, PLIRBasicBlock header)
	{
		ArrayList<PLIRBasicBlock> stack = new ArrayList<PLIRBasicBlock>();
		stack.add(this);
		while (stack.isEmpty() == false)
		{
			PLIRBasicBlock curr = stack.get(stack.size() - 1);
			stack.remove(stack.size() - 1);
			if (seen.contains(curr) == false)
			{
				seen.add(curr);
				curr.wrappedLoopHeaders.add(header);
				if (curr.leftChild != null)
				{
					stack.add(curr.leftChild);
				}
				if (curr.rightChild != null)
				{
					stack.add(curr.rightChild);
				}
			}
		}
	}
	
//	public boolean terminateAtNextPhi = false;
	public void propagatePhi(int offset, ArrayList<PLIRBasicBlock> visited, PLSymbolTable scope, int branch, HashMap<String, PLIRInstruction> scopeMap, String scopeName)
	{	
//		if (visited.contains(this) == false)
		{	
//			PLIRInstruction findPhi = phi;
//			PLIRInstruction replacePhi = phi;
//			
//			int p1id = phi.op1 != null ? phi.op1.id : -1;
//			int p2id = phi.op2 != null ? phi.op2.id : -1;
			
			// Sort instructions in the BB by their ID
			ArrayList<PLIRInstruction> sortedInstructions = new ArrayList<PLIRInstruction>();
			for (PLIRInstruction inst : instructions)
			{
				sortedInstructions.add(inst);
			}
			if (sortedInstructions.size() > 1)
			{
				boolean sorted = false;
				while (!sorted)
				{
					sorted = true;
					for (int i = 0; i < sortedInstructions.size() - 1; i++)
					{
						for (int j = i + 1; j < sortedInstructions.size(); j++)
						{
							if (sortedInstructions.get(i).id > sortedInstructions.get(j).id)
							{
								PLIRInstruction tmp = sortedInstructions.get(i);
								sortedInstructions.set(i, sortedInstructions.get(j));
								sortedInstructions.set(j, tmp);
								sorted = false;
							}
						}
					}
				}
			}
			
			
			// Propagate through the main instructions in this block's body
			for (int i = 0; i < sortedInstructions.size(); i++)
			{
				PLIRInstruction bInst = sortedInstructions.get(i);
				boolean replaced = false;
				
				// Check all replaced variables
				for (String var : scopeMap.keySet())
				{
					PLIRInstruction replacement = scopeMap.get(var);

					if (bInst.id == 118)
					{
						System.err.println("asd");
					}
					
					// Short circuits
					if (bInst.id != 0 && replacement.id > bInst.id || (bInst.isConstant))
					{
						continue;
					}
					if (bInst.opcode == InstructionType.BEQ) continue;
					
					// Handle comparisons specially since their dependencies are embedded in the instruction
					if (bInst.opcode == InstructionType.CMP)
					{
						if (bInst.op1 != null && bInst.op1.checkForReplace(replacement, var, scopeName))
						{
							bInst.evaluate(-1, offset, replacement, scope, new ArrayList<PLIRInstruction>(), branch);
						}
						
						if (bInst.op2 != null && bInst.op2.checkForReplace(replacement, var, scopeName))
						{
							bInst.evaluate(-1, offset, replacement, scope, new ArrayList<PLIRInstruction>(), branch);
						}
					}
					else
					{	
						// guard against constant overwriting
						boolean couldHaveReplaced = false;
						
						if (!(bInst.opcode == InstructionType.PHI && bInst.op1 != null && bInst.op1.isConstant) 
								&& !(bInst.opcode == InstructionType.PHI && branch == 2))
						{
							if (bInst.op1 != null &&  bInst.op1.ident.get(scopeName) != null && bInst.op1.ident.get(scopeName).equals(var))
							{
		//						if (!(replaced && bInst.opcode == InstructionType.PHI))
								{
									if (bInst.opcode == InstructionType.PHI && bInst.guards.contains(var))
									{
										replaced  = bInst.replaceLeftOperand(replacement);
										couldHaveReplaced = true;
									}
									else
									{
										replaced  = bInst.replaceLeftOperand(replacement);
										couldHaveReplaced = true;
									}
		//							replaced = true;
								}
							}
							else if (bInst.op1 != null && bInst.op1.equals(replacement))
							{
		//						if (!(replaced && bInst.opcode == InstructionType.PHI))
								{
									if (bInst.opcode == InstructionType.PHI && bInst.guards.contains(var))
									{
										replaced  = bInst.replaceLeftOperand(replacement);
										couldHaveReplaced = true;
									}
									else
									{
										replaced  = bInst.replaceLeftOperand(replacement);
										couldHaveReplaced = true;
									}
		//							replaced = true;
								}
							}
							else if (bInst.op1name != null && bInst.op1name.equals(var))
							{
								if (bInst.opcode == InstructionType.PHI && bInst.guards.contains(var))
								{
									replaced  = bInst.replaceLeftOperand(replacement);
									couldHaveReplaced = true;
								}
								else
								{
									replaced  = bInst.replaceLeftOperand(replacement);
									couldHaveReplaced = true;
								}
		//						replaced = true;
							}
						}
						
						if (bInst.opcode != InstructionType.STORE && !(bInst.whilePhi && branch == 1))
						{
							if (bInst.op2 != null && bInst.op2.ident.get(scopeName) != null && bInst.op2.ident.get(scopeName).equals(var) && !replaced)
							{
								replaced = bInst.replaceRightOperand(replacement);
							}
							else if (bInst.op2 != null && bInst.op2.equals(replacement) && !replaced)
							{
								replaced = bInst.replaceRightOperand(replacement);
							}
							else if (bInst.op2name != null && bInst.op2name.equals(var) && !replaced)
							{
								replaced = bInst.replaceRightOperand(replacement);
							}
						}
						else if (bInst.opcode == InstructionType.PHI && branch == 2)
						{
							System.out.println("here");
						}
						
						// Check for replacement...
						if (replaced && bInst.opcode != InstructionType.STORE) // && !couldHaveReplaced)
						{
							ArrayList<PLIRInstruction> visitedInsts = new ArrayList<PLIRInstruction>();
							
							int target = bInst.id - 1;
							if (bInst.id == 0 && bInst.tempPosition == 0)
							{
								for (int j = i + 1; j < sortedInstructions.size(); j++)
								{
									if (sortedInstructions.get(j).id > 0)
									{
										target = sortedInstructions.get(j).id - 1;
										break;
									}
								}
							}
							
							// Re-evaluate this instruction since one of its operands was replaced
							bInst.evaluate(target, offset, replacement, scope, visitedInsts, branch);
							System.out.println("Replaced: " + bInst);
							
							// Check to see if this is a new variable
							String instName = bInst.ident.get(scopeName);
							System.out.println(bInst.ident);
							if (instName != null && instName.equals(var) == false)
							{
								scopeMap.put(instName, bInst);
							}
						}
						else
						{
							if (bInst.op1 != null && bInst.op1.checkForReplace(replacement, var, scopeName))
							{
								bInst.evaluate(-1, offset, replacement, scope, new ArrayList<PLIRInstruction>(), branch);
							}
							
							if (bInst.op2 != null && bInst.op2.checkForReplace(replacement, var, scopeName))
							{
								bInst.evaluate(-1, offset, replacement, scope, new ArrayList<PLIRInstruction>(), branch);
							}
						}
						
						if (replaced && couldHaveReplaced && bInst.opcode == InstructionType.PHI)
						{
							scopeMap.put(var, bInst);
						}
						
						// If the phi value was used to replace some operand, and this same expression was used to save a result, replace
						// with the newly generated result
						else if (replaced && ((bInst.ident.get(scopeName) != null && replacement.ident.get(scopeName) != null 
								&& bInst.ident.get(scopeName).equals(replacement.ident.get(scopeName)))
								|| (bInst.saveName.get(scopeName) != null && replacement.ident.get(scopeName) != null && 
								bInst.saveName.get(scopeName).equals(replacement.ident.get(scopeName)))) 
								&& bInst.kind != ResultKind.CONST)
						{
							scopeMap.put(var, bInst);
						}
						
						// If there is an assignment that matches this PHI variable, use its value...
						if (bInst.id > 0 && bInst.generated && (bInst.ident.get(scopeName) != null && bInst.ident.get(scopeName).equals(var)))
						{
							if (bInst.opcode == InstructionType.LOAD)
							{
								// Need to reload based on the value of the phi...?
								scopeMap.put(var, bInst);
							}
							else
							{
								scopeMap.put(var, bInst);
							}
						}
					}
				}
			}
		
			// Now propagate down the tree
			if (leftChild != null && visited.contains(leftChild) == false)
			{
				leftChild.propagatePhi(offset, visited, scope, 1, scopeMap, scopeName);
			}
			if (rightChild != null && visited.contains(rightChild) == false && rightChild.isWhileEntry == false)
			{
				visited.add(rightChild);
				
				rightChild.propagatePhi(offset, visited, scope, 2, scopeMap, scopeName);
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
		if (inst != null)
		{
			boolean added = instructions.add(inst);			
			return added;
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
