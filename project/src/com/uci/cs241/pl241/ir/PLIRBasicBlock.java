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
		
		// Add the dominator sets
		for (PLIRBasicBlock block : newBlock.dominatorSet)
		{
//			oldBlock.dominatorSet.add(block);
			leftJoin.dominatorSet.add(block);
		}
		
//		oldBlock.fixSpot();
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
	
	public boolean terminateAtNextPhi = false;
	public void propagatePhi(String var, int offset, PLIRInstruction phi, ArrayList<PLIRBasicBlock> visited, PLSymbolTable scope, int branch)
	{
		HashMap<String, PLIRInstruction> scopeMap = new HashMap<String, PLIRInstruction>();
		
//		if (visited.contains(this) == false)
		{
//			visited.add(this);
			
			PLIRInstruction findPhi = phi;
			PLIRInstruction replacePhi = phi;
			
			int p1id = phi.op1 != null ? phi.op1.id : -1;
			int p2id = phi.op2 != null ? phi.op2.id : -1;
			
			scopeMap.put(var, phi);
			
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
//			for (PLIRInstruction bInst : sortedInstructions)
			for (int i = 0; i < sortedInstructions.size(); i++)
			{
				PLIRInstruction bInst = sortedInstructions.get(i);
				boolean replaced = false;
				
				if (i == 16)
				{
					System.err.println("asd");
				}
				
				if (bInst.id != 0 && phi.id > bInst.id || (bInst.isConstant))
				{
					continue;
				}
				
				if (bInst.opcode == InstructionType.BEQ) continue;
				
				if (bInst.opcode == InstructionType.PHI || bInst.opcode == InstructionType.CMP)
				{
					System.err.println("asd");
//					return;
				}
				
				if (bInst.opcode == InstructionType.PHI)
				{
//					continue;
					if ((bInst.id == p1id || bInst.id == p2id) && phi.id > bInst.id)
					{
						continue;
					}
//					if (var.equals(bInst.ident.get(scope.getCurrentScope())) == false)
				}
				
//				if (phi.origIdent.equals("j"))
//				{
//					System.out.println("here");
//				}
				
				if (bInst.opcode == InstructionType.CMP)
				{
					PLIRInstruction cmpInst = bInst;
					System.err.println("here");
					if (cmpInst.op1 != null)
					{
						boolean innerReplaced = false;
						for (int ii = 0; ii < cmpInst.op1.dependents.size(); ii++)
						{
							PLIRInstruction dependent = cmpInst.op1.dependents.get(ii);
//							if (dependent.origIdent.equals(var))
							if (dependent.ident.get(scope.getCurrentScope()) != null && dependent.ident.get(scope.getCurrentScope()).equals(var))
							{
								// OK, we need to unfold cmpInst.op1, but how?...
								innerReplaced = true;
							}
						}
						if (innerReplaced)
						{
							cmpInst.evaluate(cmpInst.id - 1, offset, scopeMap.get(var), scope, new ArrayList<PLIRInstruction>(), branch);
						}
					}
					
					if (cmpInst.op2 != null)
					{
						boolean innerReplaced = false;
						for (PLIRInstruction dependent : cmpInst.op2.dependents)
						{
//							if (dependent.origIdent.equals(var))
							if (dependent.ident.get(scope.getCurrentScope()) != null && dependent.ident.get(scope.getCurrentScope()).equals(var))
							{
								innerReplaced = true;
							}
						}
						if (innerReplaced)
						{
							cmpInst.evaluate(cmpInst.id - 1, offset, scopeMap.get(var), scope, new ArrayList<PLIRInstruction>(), branch);
						}
					}
				}
				else
				{	
					// guard against constant overwriting
					boolean couldHaveReplaced = false;
					
					if (!(bInst.opcode == InstructionType.PHI && bInst.op1 != null && bInst.op1.isConstant) 
							&& !(bInst.opcode == InstructionType.PHI && branch == 2))
					{
						
						PLIRInstruction dependent = bInst.op1;
//						while (dependent.op1 != null)
//						{
//							if (!(dependent.opcode == InstructionType.PHI && dependent.op1 != null && dependent.op1.isConstant) 
//									&& !(dependent.opcode == InstructionType.PHI && branch == 2))
//							{
//								if (dependent.op1 != null &&  dependent.op1.ident.get(scope.getCurrentScope()) != null && dependent.op1.ident.get(scope.getCurrentScope()).equals(var))
//								{
//									replaced = dependent.replaceLeftOperand(phi);
//								}
//							}
//						}
//						while (dependent.op2 != null)
//						{
//							if (!(dependent.opcode == InstructionType.PHI && dependent.op2 != null && dependent.op2.isConstant) 
//									&& !(dependent.opcode == InstructionType.PHI && branch == 2))
//							{
//								if (bInst.op1 != null &&  bInst.op1.ident.get(scope.getCurrentScope()) != null && bInst.op1.ident.get(scope.getCurrentScope()).equals(var))
//								{
//									
//								}
//							}
//						}
						
						System.out.println(scope.getCurrentScope());
						if (bInst.op1 != null &&  bInst.op1.ident.get(scope.getCurrentScope()) != null && bInst.op1.ident.get(scope.getCurrentScope()).equals(var))
						{
	//						if (!(replaced && bInst.opcode == InstructionType.PHI))
							{
								if (bInst.opcode == InstructionType.PHI && bInst.guards.contains(var))
								{
									replaced  = bInst.replaceLeftOperand(phi);
									couldHaveReplaced = true;
								}
								else
								{
									replaced  = bInst.replaceLeftOperand(scopeMap.get(var));
									couldHaveReplaced = true;
								}
	//							replaced = true;
							}
						}
						else if (bInst.op1 != null && bInst.op1.equals(findPhi))
						{
	//						if (!(replaced && bInst.opcode == InstructionType.PHI))
							{
								if (bInst.opcode == InstructionType.PHI && bInst.guards.contains(var))
								{
									replaced  = bInst.replaceLeftOperand(phi);
									couldHaveReplaced = true;
								}
								else
								{
									replaced  = bInst.replaceLeftOperand(scopeMap.get(var));
									couldHaveReplaced = true;
								}
	//							replaced = true;
							}
						}
						else if (bInst.op1name != null && bInst.op1name.equals(var))
						{
							if (bInst.opcode == InstructionType.PHI && bInst.guards.contains(var))
							{
								replaced  = bInst.replaceLeftOperand(phi);
								couldHaveReplaced = true;
							}
							else
							{
								replaced  = bInst.replaceLeftOperand(scopeMap.get(var));
								couldHaveReplaced = true;
							}
	//						replaced = true;
						}
					}
					
	//				if (bInst.opcode != InstructionType.PHI && bInst.opcode != InstructionType.STORE)
					if (bInst.opcode != InstructionType.STORE && !(bInst.whilePhi && branch == 1))
					{
	//					if (bInst.op2 != null && bInst.op2.origIdent.equals(var) && !replaced)
						if (bInst.op2 != null && bInst.op2.ident.get(scope.getCurrentScope()) != null && bInst.op2.ident.get(scope.getCurrentScope()).equals(var) && !replaced)
						{
	//						if (!(replaced && bInst.opcode == InstructionType.PHI))
							{
								replaced = bInst.replaceRightOperand(scopeMap.get(var));
	//							replaced = true;
							}
						}
						else if (bInst.op2 != null && bInst.op2.equals(findPhi) && !replaced)
						{
	//						if (!(replaced && bInst.opcode == InstructionType.PHI))
							{
								replaced = bInst.replaceRightOperand(scopeMap.get(var));
	//							replaced = true;
							}
						}
						else if (bInst.op2name != null && bInst.op2name.equals(var) && !replaced)
						{
	//						if (!(replaced && bInst.opcode == InstructionType.PHI))
							{
								replaced = bInst.replaceRightOperand(scopeMap.get(var));
	//							replaced = true;
							}
						}
					}
					else if (bInst.opcode == InstructionType.PHI && branch == 2)
					{
						System.out.println("here");
					}
					
					// ALWAYS re-evaluate! Make our lives so much easier...
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
						PLIRInstruction replacement = scopeMap.get(var);
						bInst.evaluate(target, offset, replacement, scope, visitedInsts, branch);
					}
					
					// Don't propagate past the phi, since it essentially replaces the current value
	//				if (replaced && bInst.opcode == InstructionType.PHI)
	//				{
	//					findPhi = bInst; 
	//					scopeMap.put(var, bInst);
	//				}
					
					if (replaced && couldHaveReplaced && bInst.opcode == InstructionType.PHI)
					{
						findPhi = bInst; 
						scopeMap.put(var, bInst);
					}
					
					// If the phi value was used to replace some operand, and this same expression was used to save a result, replace
					// with the newly generated result
	//				else if (replaced && (bInst.origIdent.equals(phi.origIdent) || bInst.saveName.equals(phi.origIdent)) && bInst.kind != ResultKind.CONST)
					else if (replaced && ((bInst.ident.get(scope.getCurrentScope()) != null && phi.ident.get(scope.getCurrentScope()) != null 
							&& bInst.ident.get(scope.getCurrentScope()).equals(phi.ident.get(scope.getCurrentScope())))
							|| (bInst.saveName.get(scope.getCurrentScope()) != null && phi.ident.get(scope.getCurrentScope()) != null && 
							bInst.saveName.get(scope.getCurrentScope()).equals(phi.ident.get(scope.getCurrentScope())))) 
							&& bInst.kind != ResultKind.CONST)
					{
	//					System.out.println("now replacing " + phi.origIdent + " with : " + bInst.toString());
						scopeMap.put(var, bInst);
					}
					
	//				// If there is an assignment that matches this PHI variable, use its value...
	//				if (bInst.id > 0 && bInst.generated && (bInst.origIdent != null && bInst.origIdent.equals(var)))
					if (bInst.id > 0 && bInst.generated && (bInst.ident.get(scope.getCurrentScope()) != null && bInst.ident.get(scope.getCurrentScope()).equals(var)))
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
				
				// If for some reason the phi value was prematurely re-assigned, perhaps because of a READ, then don't propogate further
//				if (bInst.origIdent.equals(phi.origIdent))
//				{
//					terminateAtNextPhi = true;
//					return; // in this case, we short circuit since the value has been reasssigned without making use of the phi
//				}
			}
		
			// Now propagate down the tree
			if (leftChild != null && visited.contains(leftChild) == false)
			{
//				visited.add(leftChild);
				
				leftChild.propagatePhi(var, offset, scopeMap.get(var), visited, scope, 1);
			}
			if (rightChild != null && visited.contains(rightChild) == false && rightChild.isWhileEntry == false)
			{
				visited.add(rightChild);
				
				rightChild.propagatePhi(var, offset, scopeMap.get(var), visited, scope, 2);
			}
			
//			for (PLIRBasicBlock child : children)
//			{
//				if (visited.contains(child) == false)
//				{
//					visited.add(child);
//					child.propagatePhi(var, scopeMap.get(var), visited, scopeMap);
//				}
//			}
			
			
//			if (this.joinNode != null)
//			{
//				if (visited.contains(this.joinNode) == false)
//				{
//					visited.add(this.joinNode);
//					joinNode.propagatePhi(var, scopeMap.get(var), visited, scopeMap);
//				}
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
