package com.uci.cs241.pl241.optimization;

import java.util.ArrayList;
import java.util.HashMap;

import com.uci.cs241.pl241.frontend.Function;
import com.uci.cs241.pl241.ir.PLIRBasicBlock;
import com.uci.cs241.pl241.ir.PLStaticSingleAssignment;
import com.uci.cs241.pl241.ir.PLIRInstruction.EliminationReason;
import com.uci.cs241.pl241.ir.PLIRInstruction.OperandType;
import com.uci.cs241.pl241.ir.PLIRInstruction.InstructionType;
import com.uci.cs241.pl241.ir.PLIRInstruction;

public class CSE 
{	
	public HashMap<String, Function> functions;
	
	public PLIRBasicBlock performCSE(PLIRBasicBlock root, HashMap<String, Function> functions)
	{
		this.functions = functions;
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
		domList.put(InstructionType.ADDA, new ArrayList<PLIRInstruction>());
		domList.put(InstructionType.LOAD, new ArrayList<PLIRInstruction>());
		domList.put(InstructionType.STORE, new ArrayList<PLIRInstruction>());
		
		// Record what's already been visited to prevent loops on while loop BBs
		ArrayList<Integer> visited = new ArrayList<Integer>();
		
		// Start depth-first traversal of the tree, recursively
		cseOnBlock(domList, visited, root, new ArrayList<PLIRInstruction>());
		
		return ret;
	}
	
	private void cseOnBlock(HashMap<InstructionType, ArrayList<PLIRInstruction>> parentDomList, 
			ArrayList<Integer> visited, PLIRBasicBlock block, ArrayList<PLIRInstruction> killedAddresses)
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
			HashMap<PLIRInstruction, PLIRInstruction> killMap = new HashMap<PLIRInstruction, PLIRInstruction>();
			
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
				if (killMap.containsKey(inst.op1))
				{
					inst.op1 = killMap.get(inst.op1);
				}
				if (inst.op2 != null)
				{
					while (inst.op2.refInst != null)
					{
						inst.op2 = inst.op2.refInst;
					}
				}
				if (killMap.containsKey(inst.op2))
				{
					inst.op2 = killMap.get(inst.op2);
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
						    else if (inst.opcode == InstructionType.LOAD && inst.op1.equals(parentInst.op1))
						    {
						    	// Need to check and make sure this load wasn't killed by a store between its spot and the parent
						    	boolean killed = killedAddresses.contains(inst.op1);
						    	boolean funcKilled = false;
						    	for (int i = parentInst.id; i < inst.id && !funcKilled; i++)
						    	{
						    		PLIRInstruction other = PLStaticSingleAssignment.instructions.get(i); 
						    		if (other.opcode == InstructionType.FUNC || other.opcode == InstructionType.PROC)
						    		{
						    			if (functions.get(other.funcName).killedArrays.contains(inst.dummyName))
						    			{
						    				funcKilled = true;
						    			}
						    		}
						    	}
						    	if (!killed && !funcKilled)
						    	{
						    		inst.removeInstruction(EliminationReason.CSE, parentInst);
						    	}
						    	else if (!funcKilled)
						    	{
						    		killMap.put(parentInst, inst); // anything that references the parent must now point to this new load
						    	}
						    	
						    	// If the address was killed before, now it's OK
						    	killedAddresses.remove(inst.op1); // disregard return - we don't care if this succeeds or not
						    }
						    else if (inst.opcode == InstructionType.STORE && inst.op1.equals(parentInst.op1))
						    {
						    	if (inst.op2type == OperandType.CONST && parentInst.op2type == OperandType.CONST && inst.i2 == parentInst.i2)
						    	{
						    		inst.removeInstruction(EliminationReason.CSE, parentInst);	
						    	}
						    	else if (inst.op2 != null && parentInst.op2 != null && inst.op2.equals(parentInst.op2))
						    	{
						    		inst.removeInstruction(EliminationReason.CSE, parentInst);
						    	}
						    }
						    else if (inst.op1type == OperandType.INST && (inst.op2type == OperandType.INST || inst.op2type == OperandType.ADDRESS))
							{
								if (inst.op1.equals(parentInst.op1) && inst.op2.equals(parentInst.op2) && killedAddresses.contains(inst.op1) == false && killedAddresses.contains(inst.op2) == false)
								{
									inst.removeInstruction(EliminationReason.CSE, parentInst);
								}
							}
							else if (inst.op1type == OperandType.CONST && (inst.op2type == OperandType.INST || inst.op2type == OperandType.ADDRESS))
							{
								if (inst.i1 == parentInst.i1 && inst.op2.equals(parentInst.op2) && killedAddresses.contains(inst.op2) == false)
								{
									inst.removeInstruction(EliminationReason.CSE, parentInst);
								}
							}
							else if ((inst.op1type == OperandType.INST || inst.op1type == OperandType.ADDRESS) && inst.op2type == OperandType.CONST)
							{
								if (inst.op1.equals(parentInst.op1) && killedAddresses.contains(inst.op1) == false && inst.i2 == parentInst.i2)
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
					    			if ((inst.op1.equals(parentInst.op1) && inst.op2.equals(parentInst.op2) && killedAddresses.contains(inst.op1) == false && killedAddresses.contains(inst.op2) == false) ||
					    				((inst.op1.equals(parentInst.op2) && inst.op2.equals(parentInst.op1) && killedAddresses.contains(inst.op1) == false && killedAddresses.contains(inst.op2) == false)))
					    			{
					    				inst.removeInstruction(EliminationReason.CSE, parentInst);
					    			}
						    	}
					    		
					    		// case #3: add inst const
					    		//          add inst const
					    		else if ((inst.op1type == OperandType.INST && parentInst.op1type == OperandType.INST) &&
						    			(inst.op2type == OperandType.CONST && parentInst.op2type == OperandType.CONST))	
					    		{
					    			if ((inst.op1.equals(parentInst.op1) && killedAddresses.contains(inst.op1) == false && inst.i2 == parentInst.i2 ))
					    			{
					    				inst.removeInstruction(EliminationReason.CSE, parentInst);
					    			}
					    		}
					    		
					    		// case #4: add inst const
					    		//          add const inst
					    		else if ((inst.op1type == OperandType.INST && parentInst.op1type == OperandType.CONST) &&
						    			(inst.op2type == OperandType.CONST && parentInst.op2type == OperandType.INST))	
						    		{
					    				if ((inst.op1.equals(parentInst.op2) && killedAddresses.contains(inst.op1) == false && inst.i2 == parentInst.i1 ))
						    			{
						    				inst.removeInstruction(EliminationReason.CSE, parentInst);
						    			}
						    		}
					    		
					    		// case #5: add const inst
					    		//          add const inst
					    		else if ((inst.op1type == OperandType.CONST && parentInst.op1type == OperandType.CONST) &&
						    			(inst.op2type == OperandType.INST && parentInst.op2type == OperandType.INST))	
						    		{
					    				if ((inst.op2.equals(parentInst.op2) && killedAddresses.contains(inst.op2) == false && inst.i1 == parentInst.i1))
						    			{
						    				inst.removeInstruction(EliminationReason.CSE, parentInst);
						    			}
						    		}
					    		
					    		// case #6: add const inst
					    		//          add inst const
					    		else if ((inst.op1type == OperandType.CONST && parentInst.op1type == OperandType.INST) &&
						    			(inst.op2type == OperandType.INST && parentInst.op2type == OperandType.CONST))	
						    		{
					    				if ((inst.op2.equals(parentInst.op1) && killedAddresses.contains(inst.op2) == false && inst.i1 == parentInst.i2 ))
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
					    				if ((inst.op1.equals(parentInst.op1) && inst.op2.equals(parentInst.op2) && killedAddresses.contains(inst.op1) == false && killedAddresses.contains(inst.op2) == false) ||
						    				((inst.op1.equals(parentInst.op2) && inst.op2.equals(parentInst.op1) && killedAddresses.contains(inst.op1) == false && killedAddresses.contains(inst.op2) == false)))
						    			{
						    				inst.removeInstruction(EliminationReason.CSE, parentInst);
						    			}
						    		}
					    		
					    		// case #3: mul inst const
					    		//          mul inst const
					    		else if ((inst.op1type == OperandType.INST && parentInst.op1type == OperandType.INST) &&
						    			(inst.op2type == OperandType.CONST && parentInst.op2type == OperandType.CONST))	
						    		{
					    			if ((inst.op1.equals(parentInst.op1) && killedAddresses.contains(inst.op1) == false && inst.i2 == parentInst.i2 ))
					    			{
					    				inst.removeInstruction(EliminationReason.CSE, parentInst);
					    			}
						    		}
					    		
					    		// case #4: mul inst const
					    		//          mul const inst
					    		else if ((inst.op1type == OperandType.INST && parentInst.op1type == OperandType.CONST) &&
						    			(inst.op2type == OperandType.CONST && parentInst.op2type == OperandType.INST))	
						    		{
					    			if ((inst.op1.equals(parentInst.op2) && killedAddresses.contains(inst.op1) == false &&inst.i2 == parentInst.i1 ))
					    			{
					    				inst.removeInstruction(EliminationReason.CSE, parentInst);
					    			}
						    		}
					    		
					    		// case #5: mul const inst
					    		//          mul const inst
					    		else if ((inst.op1type == OperandType.CONST && parentInst.op1type == OperandType.CONST) &&
						    			(inst.op2type == OperandType.INST && parentInst.op2type == OperandType.INST))	
						    		{
					    			if ((inst.op2.equals(parentInst.op2) && killedAddresses.contains(inst.op2) == false && inst.i1 == parentInst.i1))
					    			{
					    				inst.removeInstruction(EliminationReason.CSE, parentInst);
					    			}
						    		}
					    		
					    		// case #6: mul const inst
					    		//          mul inst const
					    		else if ((inst.op1type == OperandType.CONST && parentInst.op1type == OperandType.INST) &&
						    			(inst.op2type == OperandType.INST && parentInst.op2type == OperandType.CONST))	
						    		{
					    			if ((inst.op2.equals(parentInst.op1) && killedAddresses.contains(inst.op2) == false && inst.i1 == parentInst.i2 ))
					    			{
					    				inst.removeInstruction(EliminationReason.CSE, parentInst);
					    			}
						    		}
					    		
					    		break;
					    	default:
					    		break;
					    	}
							
							
							if (inst.opcode == InstructionType.STORE)
							{
								for (int i = 0; i < inst.id; i++)
								{
									PLIRInstruction other = PLStaticSingleAssignment.instructions.get(i);
									if (other.opcode == InstructionType.LOAD && other.op1.equals(inst.op1))
									{
										killedAddresses.add(other);
									}
								}
								killedAddresses.add(inst.op1);
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
				cseOnBlock(domList, visited, child, killedAddresses);
			}
		}
	}
}
