package com.uci.cs241.pl241.optimization;

import java.util.ArrayList;
import java.util.HashMap;

import com.uci.cs241.pl241.ir.PLIRBasicBlock;
import com.uci.cs241.pl241.ir.PLIRInstruction.EliminationReason;
import com.uci.cs241.pl241.ir.PLIRInstruction.OperandType;
import com.uci.cs241.pl241.ir.PLIRInstruction.InstructionType;
import com.uci.cs241.pl241.ir.PLIRInstruction;

public class CSE 
{	
	public PLIRBasicBlock performCSE(PLIRBasicBlock root)
	{
		PLIRBasicBlock ret = root;
		
		HashMap<InstructionType, ArrayList<PLIRInstruction>> domList = 
				new HashMap<InstructionType, ArrayList<PLIRInstruction>>();
		
		// Initialize the map
		domList.put(InstructionType.NEG, new ArrayList<PLIRInstruction>());
		domList.put(InstructionType.ADD, new ArrayList<PLIRInstruction>());
		domList.put(InstructionType.SUB, new ArrayList<PLIRInstruction>());
		domList.put(InstructionType.MUL, new ArrayList<PLIRInstruction>());
		domList.put(InstructionType.DIV, new ArrayList<PLIRInstruction>());
		domList.put(InstructionType.CMP, new ArrayList<PLIRInstruction>());
		
		// Record what's already been visited to prevent loops on while loop BBs
		ArrayList<Integer> visited = new ArrayList<Integer>();
		
		// Start depth-first traversal of the tree, recursively
		cseOnBlock(domList, visited, root);
		
		return ret;
	}
	
	private void cseOnBlock(HashMap<InstructionType, ArrayList<PLIRInstruction>> parentDomList, 
			ArrayList<Integer> visited, PLIRBasicBlock block)
	{
		// Avoid cyclic loops from while loops
		if (visited.contains(block.id))
		{
			return;
		}
		else
		{
			HashMap<InstructionType, ArrayList<PLIRInstruction>> domList = 
					new HashMap<InstructionType, ArrayList<PLIRInstruction>>();
			
			// Re-populate the map
			for (InstructionType key : parentDomList.keySet())
			{
				domList.put(key, new ArrayList<PLIRInstruction>());
				for (PLIRInstruction inst : parentDomList.get(key))
				{
					domList.get(key).add(inst);
				}
			}
			
			// Sort the instructions based on their ID... just in case they somehow got out of sync
			boolean sorted = false;
			while (!sorted)
			{
				sorted = true;
				for (int i = 0; i < block.instructions.size() - 1; i++)
				{
					if (block.instructions.get(i).id > block.instructions.get(i + 1).id)
					{
						PLIRInstruction inst = block.instructions.get(i);
						PLIRInstruction next = block.instructions.get(i + 1);
						block.instructions.set(i, next);
						block.instructions.set(i + 1, inst);
						sorted = false;
					}
				}
			}
			
			// Check for subexpression elimination on the parent
			for (PLIRInstruction inst : block.instructions)
			{
				if (inst.op1 != null)
				{
					while (inst.op1.refInst != null)
					{
						inst.op1 = inst.op1.refInst;
					}
				}
				if (inst.op2 != null)
				{
					while (inst.op2.refInst != null)
					{
						inst.op2 = inst.op2.refInst;
					}
				}
				
				if (domList.containsKey(inst.opcode))
				{
					for (PLIRInstruction parentInst : domList.get(inst.opcode))
					{
						if (inst.equals(parentInst) && inst.id > parentInst.id) // really shouldn't have to do this, but just in case
						{
							inst.removeInstruction(EliminationReason.CSE, parentInst);
						}
						else if (inst.opcode == parentInst.opcode && inst.id > parentInst.id) // special case to handle commutative operations
						{
						    if (inst.opcode == InstructionType.ADD && parentInst.opcode == InstructionType.ADD 
						    		&& inst.op1type == OperandType.FP && parentInst.op1type == OperandType.FP 
						    		&& inst.op2address.equals(parentInst.op2address)) // special type of comparison
						    {
						    	inst.removeInstruction(EliminationReason.CSE, parentInst);
						    }
						    else if (inst.op1type == OperandType.INST && inst.op2type == OperandType.INST)
							{
								if (inst.op1.equals(parentInst.op1) && inst.op2.equals(parentInst.op2))
								{
									inst.removeInstruction(EliminationReason.CSE, parentInst);
								}
							}
							else if (inst.op1type == OperandType.CONST && inst.op2type == OperandType.INST)
							{
								if (inst.i1 == parentInst.i1 && inst.op2.equals(parentInst.op2))
								{
									inst.removeInstruction(EliminationReason.CSE, parentInst);
								}
							}
							else if (inst.op1type == OperandType.INST && inst.op2type == OperandType.CONST)
							{
								if (inst.op1.equals(parentInst.op1) && inst.i2 == parentInst.i2)
								{
									inst.removeInstruction(EliminationReason.CSE, parentInst);
								}
							}
							else if (inst.op1type == OperandType.CONST && inst.op2type == OperandType.CONST)
							{
								if (inst.i1 == parentInst.i1 && inst.i2 == parentInst.i2)
								{
									inst.removeInstruction(EliminationReason.CSE, parentInst);
								}
							}
							switch (inst.opcode)
					    	{
					    	case ADD:
					    		// case #1: add const const
					    		//          add const const
					    		if ((inst.op1type == OperandType.CONST && parentInst.op1type == OperandType.CONST) &&
					    			(inst.op2type == OperandType.CONST && parentInst.op2type == OperandType.CONST))	
					    		{
					    			if ((inst.i1 == parentInst.i1 && inst.i2 == parentInst.i2))
					    			{
					    				inst.removeInstruction(EliminationReason.CSE, parentInst);
					    			}
					    		}
					    		
					    		// case #2: add inst inst
					    		//          add inst inst
					    		else if ((inst.op1type == OperandType.INST && parentInst.op1type == OperandType.INST) &&
						    			(inst.op2type == OperandType.INST && parentInst.op2type == OperandType.INST))	
						    	{
					    			if ((inst.op1.equals(parentInst.op1) && inst.op2.equals(parentInst.op2)) ||
					    				((inst.op1.equals(parentInst.op2) && inst.op2.equals(parentInst.op1))))
					    			{
					    				inst.removeInstruction(EliminationReason.CSE, parentInst);
					    			}
						    	}
					    		
					    		// case #3: add inst const
					    		//          add inst const
					    		else if ((inst.op1type == OperandType.INST && parentInst.op1type == OperandType.INST) &&
						    			(inst.op2type == OperandType.CONST && parentInst.op2type == OperandType.CONST))	
					    		{
					    			if ((inst.op1.equals(parentInst.op1) && inst.i2 == parentInst.i2 ))
					    			{
					    				inst.removeInstruction(EliminationReason.CSE, parentInst);
					    			}
					    		}
					    		
					    		// case #4: add inst const
					    		//          add const inst
					    		else if ((inst.op1type == OperandType.INST && parentInst.op1type == OperandType.CONST) &&
						    			(inst.op2type == OperandType.CONST && parentInst.op2type == OperandType.INST))	
						    		{
					    				if ((inst.op1.equals(parentInst.op2) && inst.i2 == parentInst.i1 ))
						    			{
						    				inst.removeInstruction(EliminationReason.CSE, parentInst);
						    			}
						    		}
					    		
					    		// case #5: add const inst
					    		//          add const inst
					    		else if ((inst.op1type == OperandType.CONST && parentInst.op1type == OperandType.CONST) &&
						    			(inst.op2type == OperandType.INST && parentInst.op2type == OperandType.INST))	
						    		{
					    				if ((inst.op2.equals(parentInst.op2) && inst.i1 == parentInst.i1))
						    			{
						    				inst.removeInstruction(EliminationReason.CSE, parentInst);
						    			}
						    		}
					    		
					    		// case #6: add const inst
					    		//          add inst const
					    		else if ((inst.op1type == OperandType.CONST && parentInst.op1type == OperandType.INST) &&
						    			(inst.op2type == OperandType.INST && parentInst.op2type == OperandType.CONST))	
						    		{
					    				if ((inst.op2.equals(parentInst.op1) && inst.i1 == parentInst.i2 ))
						    			{
						    				inst.removeInstruction(EliminationReason.CSE, parentInst);
						    			}
						    		}
					    		
					    		break;
					    	case MUL:
					    		// case #1: mul const const
					    		//          mul const const
					    		if ((inst.op1type == OperandType.CONST && parentInst.op1type == OperandType.CONST) &&
					    			(inst.op2type == OperandType.CONST && parentInst.op2type == OperandType.CONST))	
					    		{
					    			if ((inst.i1 == parentInst.i1 && inst.i2 == parentInst.i2))
					    			{
					    				inst.removeInstruction(EliminationReason.CSE, parentInst);
					    			}
					    		}
					    		
					    		// case #2: mul inst inst
					    		//          mul inst inst
					    		else if ((inst.op1type == OperandType.INST && parentInst.op1type == OperandType.INST) &&
						    			(inst.op2type == OperandType.INST && parentInst.op2type == OperandType.INST))	
						    		{
					    				if ((inst.op1.equals(parentInst.op1) && inst.op2.equals(parentInst.op2)) ||
						    				((inst.op1.equals(parentInst.op2) && inst.op2.equals(parentInst.op1))))
						    			{
						    				inst.removeInstruction(EliminationReason.CSE, parentInst);
						    			}
						    		}
					    		
					    		// case #3: mul inst const
					    		//          mul inst const
					    		else if ((inst.op1type == OperandType.INST && parentInst.op1type == OperandType.INST) &&
						    			(inst.op2type == OperandType.CONST && parentInst.op2type == OperandType.CONST))	
						    		{
					    			if ((inst.op1.equals(parentInst.op1) && inst.i2 == parentInst.i2 ))
					    			{
					    				inst.removeInstruction(EliminationReason.CSE, parentInst);
					    			}
						    		}
					    		
					    		// case #4: mul inst const
					    		//          mul const inst
					    		else if ((inst.op1type == OperandType.INST && parentInst.op1type == OperandType.CONST) &&
						    			(inst.op2type == OperandType.CONST && parentInst.op2type == OperandType.INST))	
						    		{
					    			if ((inst.op1.equals(parentInst.op2) && inst.i2 == parentInst.i1 ))
					    			{
					    				inst.removeInstruction(EliminationReason.CSE, parentInst);
					    			}
						    		}
					    		
					    		// case #5: mul const inst
					    		//          mul const inst
					    		else if ((inst.op1type == OperandType.CONST && parentInst.op1type == OperandType.CONST) &&
						    			(inst.op2type == OperandType.INST && parentInst.op2type == OperandType.INST))	
						    		{
					    			if ((inst.op2.equals(parentInst.op2) && inst.i1 == parentInst.i1))
					    			{
					    				inst.removeInstruction(EliminationReason.CSE, parentInst);
					    			}
						    		}
					    		
					    		// case #6: mul const inst
					    		//          mul inst const
					    		else if ((inst.op1type == OperandType.CONST && parentInst.op1type == OperandType.INST) &&
						    			(inst.op2type == OperandType.INST && parentInst.op2type == OperandType.CONST))	
						    		{
					    			if ((inst.op2.equals(parentInst.op1) && inst.i1 == parentInst.i2 ))
					    			{
					    				inst.removeInstruction(EliminationReason.CSE, parentInst);
					    			}
						    		}
					    		
					    		break;
					    	default:
					    		break;
					    	}
						}
					}
					domList.get(inst.opcode).add(0, inst);
				}
			}
			
			// Mark as visited
			visited.add(block.id);
			
			// DFS on the children now
			for (PLIRBasicBlock child : block.dominatorSet)
			{
				cseOnBlock(domList, visited, child);
			}
		}
	}
}
