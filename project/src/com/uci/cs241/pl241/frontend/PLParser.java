package com.uci.cs241.pl241.frontend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.uci.cs241.pl241.ir.PLIRBasicBlock;
import com.uci.cs241.pl241.ir.PLIRInstruction;
import com.uci.cs241.pl241.ir.PLIRInstruction.OperandType;
import com.uci.cs241.pl241.ir.PLIRInstruction.PLIRInstructionType;
import com.uci.cs241.pl241.ir.PLIRInstruction.ResultKind;
import com.uci.cs241.pl241.ir.PLStaticSingleAssignment;

public class PLParser
{
	// NODES
	// SUBCLASSES: assignment, func/proc, array, block, designator, functionblock, load/store, main, param 
	
	// Current symbol/token values used for parsing
	private String sym;
	private int toksym;
	private PLIRBasicBlock root;
	
	// BB depth (to uniquify scopes)
	private int blockDepth = 0;
	
	// Useful things to help teh parser
	private boolean globalVariableParsing;
	
	// Other necessary things
	private PLSymbolTable scope;
	
	public enum IdentType {VAR, ARRAY};
	private HashMap<String, IdentType> identTypeMap = new HashMap<String, IdentType>();
	
	// TODO!!!
	private ArrayList<String> deferredPhiIdents = new ArrayList<String>();
	
	// TODO!!!
	private String funcName = "";
	private ArrayList<String> callStack = new ArrayList<String>();
	private HashMap<String, PLIRBasicBlock> funcBlockMap = new HashMap<String, PLIRBasicBlock>();
	private HashMap<String, PLIRBasicBlock> procBlockMap = new HashMap<String, PLIRBasicBlock>();
	private HashMap<String, Integer> paramMap = new HashMap<String, Integer>();
	private HashMap<String, Boolean> funcFlagMap = new HashMap<String, Boolean>();
	
	public PLParser()
	{
		paramMap.put("InputNum", 0);
		paramMap.put("OutputNewLine", 0);
		paramMap.put("OutputNum", 1);
		
		PLStaticSingleAssignment.init();
	}
	
	public void debug(String msg)
	{
		System.err.println(">>> " + msg);
	}
	
	public void SyntaxError(String msg) throws PLSyntaxErrorException
	{
		throw new PLSyntaxErrorException(msg + ": " + sym);
	}
	
	private void advance(PLScanner in)
	{
		try
		{
			in.next();
			toksym = in.sym;
			sym = in.symstring;
		}
		catch (PLSyntaxErrorException e)
		{
			e.printStackTrace();
		}
	}
	
	// this is what's called - starting with the computation non-terminal
	public PLIRBasicBlock parse(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock result = null;
		advance(in);
		if (toksym == PLToken.mainToken)
		{
			advance(in);
			scope = new PLSymbolTable();
			scope.pushNewScope("main");
			
			// Parse global variables
			globalVariableParsing = true;
			while (toksym == PLToken.varToken || toksym == PLToken.arrToken)
			{
				result = parse_varDecl(in); 
			}
			globalVariableParsing = false;
			
			while (toksym == PLToken.funcToken || toksym == PLToken.procToken)
			{
				// TODO: merge together, but make a separate BB from the ones above, and tie the BB to the procedure/function name
				int funcType = toksym;
				result = this.parse_funcDecl(in);
				
				switch (funcType)
				{
				case PLToken.funcToken:
					funcBlockMap.put(funcName, result); // save this so that others may use it
					funcFlagMap.put(funcName, true);
					break;
				case PLToken.procToken:
					procBlockMap.put(funcName, result); // save this so that others may use it
					funcFlagMap.put(funcName, false);
					break;
				}
				
			}
			
			if (toksym == PLToken.openBraceToken)
			{
				// eat the sequence of statements that make up the computation
				advance(in);
				result = parse_statSequence(in);
				
				// parse the close of the computation
				if (toksym == PLToken.closeBraceToken)
				{
					advance(in);
					if (toksym == PLToken.periodToken)
					{
						PLIRInstruction inst = new PLIRInstruction(PLIRInstructionType.END);
					}
					else
					{
						SyntaxError("'.' missing in computation non-terminal");
					}
				}
				else
				{
					SyntaxError("'}' missing in computation non-terminal");
				}
			}
			else
			{
				SyntaxError("'{' missing in computation non-terminal");
			}
		}
		else
		{
			SyntaxError("Computation does not begin with main keyword");
		}
		
		return root;
	}

	// non-terminal
	private PLIRBasicBlock parse_ident(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		String symName = sym;
		PLIRBasicBlock block = null;
		
		if (identTypeMap.containsKey(sym))
		{
			switch (identTypeMap.get(sym))
			{
			case ARRAY:
				PLIRInstruction instArray = new PLIRInstruction();
				
				// Memorize the ident we saw here
				if (instArray != null)
				{
//					System.err.println("inst " + sym + " from scope: " + instArray);
					instArray.wasIdent = true;
					instArray.origIdent = sym;
				}
				
				// Eat the symbol, create the block with the single instruction, add the ident to the list
				// of used identifiers, and return
				advance(in);
				
				block = new PLIRBasicBlock();
				block.instructions.add(instArray);
				block.addUsedValue(symName, instArray);
				
				return block;
			case VAR:
				
				PLIRInstruction inst = scope.getCurrentValue(sym);
				
				// Memorize the ident we saw here
				if (inst != null)
				{
//					System.err.println("inst " + sym + " from scope: " + inst);
					inst.wasIdent = true;
					inst.origIdent = sym;
				}
				
				// Eat the symbol, create the block with the single instruction, add the ident to the list
				// of used identifiers, and return
				advance(in);
				
				block = new PLIRBasicBlock();
				block.instructions.add(inst);
				block.addUsedValue(symName, inst);
				
				return block;
			}
		}
		else if (globalVariableParsing)
		{
			// Initialize the variable to 0
			PLIRInstruction inst = new PLIRInstruction();
			inst.opcode = PLIRInstructionType.ADD;
			inst.i1 = 0;
			inst.i2 = 0;
			inst.kind = ResultKind.CONST;
			
			// Eat the symbol, create the block with the single instruction, add the ident to the list
			// of used identifiers, and return
			advance(in);
			
			block = new PLIRBasicBlock();
			block.instructions.add(inst);
			block.addUsedValue(symName, inst);
			
			return block;
		}
		else
		{
			debug("Previously unencountered identifier: " + sym);
		}
		
		PLIRInstruction inst = scope.getCurrentValue(sym);
		
		// Memorize the ident we saw here
		if (inst != null)
		{
			inst.wasIdent = true;
			inst.origIdent = sym;
		}
		
		// Eat the symbol, create the block with the single instruction, add the ident to the list
		// of used identifiers, and return
		advance(in);
		
		block = new PLIRBasicBlock();
		block.instructions.add(inst);
		block.addUsedValue(symName, inst);
		
		return block;
	}

	// non-terminal
	private PLIRBasicBlock parse_number(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{	
		// Result: add 0 tokenValue
		// this just puts an immediate value in a temporary variable (no moves!)
		PLIRInstruction li = new PLIRInstruction(PLIRInstructionType.ADD, 0, Integer.parseInt(sym)); 
		advance(in);
		PLIRBasicBlock block = new PLIRBasicBlock();
		block.instructions.add(li);
		return block;
	}

	private PLIRBasicBlock parse_designator(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock result = parse_ident(in);
		while (toksym == PLToken.openBracketToken)
		{
			advance(in);
			
			// TODO: merge
			
			result = parse_expression(in);
			if (toksym != PLToken.closeBracketToken)
			{
				SyntaxError("']' missing from designator non-terminal.");
			}
			advance(in);
		}
		
		return result;
	}

	private PLIRBasicBlock parse_factor(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock factor = null;
		if (toksym == PLToken.openParenToken)
		{
			advance(in);
			
			// Parse the inner expression and make sure the parens match afterwards
			factor = parse_expression(in);
			if (toksym != PLToken.closeParenToken)
			{
				SyntaxError("')' missing from factor non-terminal");
			}
			
			// They do, so eat the token and advance
			advance(in);
		}
		else if (toksym == PLToken.callToken)
		{
			// If a factor is a function call, we need to use the return value (there must be one!)
			factor = parse_funcCall(in);
			PLIRInstruction funcInst = factor.getLastInst();
			
			
			
//			if (factor.hasReturn == false)
//			{
//				SyntaxError("Function that was invoked had no return value!");
//			}
			
//			if (callStack.get(callStack.size() - 1).equals("InputNum"))
//			{
//				// pass, this is a special case
//			}
//			else
//			{
//				
//			}
			
			// Remove the function from the callstack (we've returned from the call)
			callStack.remove(callStack.size() - 1); // remove
		}
		else if (toksym == PLToken.number)
		{
			factor = parse_number(in);
		}
		else if (toksym == PLToken.ident)
		{
			factor = parse_designator(in);
		}
		else
		{
			SyntaxError("Invalid case in parse_factor");
		}
		
		return factor;
	}

	private PLIRBasicBlock parse_term(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock factor = parse_factor(in);
		if (toksym == PLToken.timesToken || toksym == PLToken.divToken)
		{
			int operator = toksym;
			PLIRBasicBlock termNode = new PLIRBasicBlock();
			advance(in);
			
			// Now parse the right term and build the resulting node 
			PLIRBasicBlock rightNode = parse_term(in);
			
			// Form the expression instruction
			PLIRInstruction leftValue = factor.instructions.get(factor.instructions.size() - 1);
			PLIRInstruction rightValue = rightNode.instructions.get(rightNode.instructions.size() - 1);
			PLIRInstructionType opcode = operator == PLToken.timesToken ? PLIRInstructionType.MUL : PLIRInstructionType.DIV;
			
			PLIRInstruction termInst = new PLIRInstruction(opcode, leftValue, rightValue);
			termInst.forceGenerate();
			termNode.addInstruction(termInst);
			
//			exprNode.setLeft(term);
//			exprNode.setRight(rightNode);
			return termNode;
		}
		else
		{
			return factor;
		}
	}

	private PLIRBasicBlock parse_expression(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock term = parse_term(in);
		if (toksym == PLToken.plusToken || toksym == PLToken.minusToken)
		{
			int operator = toksym;
			PLIRBasicBlock exprNode = new PLIRBasicBlock();
			advance(in);
			
			// Now parse the right term and build the resulting node 
			PLIRBasicBlock rightNode = parse_expression(in);
			
			// Form the expression instruction
			PLIRInstruction leftValue = term.instructions.get(term.instructions.size() - 1);
			PLIRInstruction rightValue = rightNode.instructions.get(rightNode.instructions.size() - 1);
			PLIRInstructionType opcode = operator == PLToken.plusToken ? PLIRInstructionType.ADD : PLIRInstructionType.SUB;
			
			PLIRInstruction exprInst = new PLIRInstruction(opcode, leftValue, rightValue);
			exprInst.forceGenerate();
			debug("forcing generation of: " + exprInst);
			exprNode.addInstruction(exprInst);
			
			return exprNode;
		}
		else
		{
//			System.err.println(term.instructions);
//			for (PLIRInstruction inst : term.instructions)
//			{
//				inst.forceGenerate();
//			}
			return term;
		}
	}

	private PLIRBasicBlock parse_relation(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock left = parse_expression(in);
		if (PLToken.isRelationalToken(toksym) == false)
		{
			SyntaxError("Invalid relational character");
		}
		
		// Save the relational code
		int condcode = toksym;
		
		// Eat the relational token
		advance(in);
		
		PLIRBasicBlock right = parse_expression(in);
		
		// Build the comparison instruction with the memorized condition
		PLIRInstruction leftInst = left.instructions.get(left.instructions.size() - 1);
		PLIRInstruction rightInst = right.instructions.get(right.instructions.size() - 1);
		PLIRInstruction inst = PLIRInstruction.create_cmp(leftInst, rightInst);
		inst.condcode = condcode;
		inst.fixupLocation = 0;
		
		// Create the relation block containing the instruction
		PLIRBasicBlock relation = new PLIRBasicBlock();
		relation.addInstruction(leftInst);
		relation.addInstruction(rightInst);
		relation.addInstruction(inst);
		
		// 
		
		// Save whatever values are used in these expressions
		// TODO: need a merge BBs method
		debug(left.usedIdents.size() + " !!!");
		for (String sym : left.usedIdents.keySet())
		{
			debug("adding " + sym + " with " + left.usedIdents.get(sym).toString());
			relation.addUsedValue(sym, left.usedIdents.get(sym));
		}
		for (String sym : right.usedIdents.keySet())
		{
			relation.addUsedValue(sym, right.usedIdents.get(sym));
		}
		
		return relation;
	}

	private PLIRBasicBlock parse_assignment(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock result = null;
		
		if (toksym != PLToken.letToken)
		{
			throw new PLSyntaxErrorException("assignment");
		}
		else
		{
			advance(in);
			String varName = sym;
			
			// Check to make sure these variables are in scope before being used
			if (scope.isVarInScope(varName))
			{
				result = parse_designator(in);
				if (toksym == PLToken.becomesToken)
				{
					advance(in);
					if (sym.equals("a"))
					{
						int x = 0;
					}
					result = parse_expression(in);
					
					// The last instruction added to the BB is the one that holds the value for this assignment
					PLIRInstruction inst = result.instructions.get(result.instructions.size() - 1);
					
					// If we aren't deferring generation because of a potential PHI usage, just replace with the current value in scope
					if (deferredPhiIdents.contains(varName) == false)
					{
						scope.updateSymbol(varName, inst); // (SSA ID) := expr
					}
					else // else, force the current instruction to be generated so it can be used in a phi later
					{
						inst.kind = ResultKind.VAR;
//						inst.generated = false;
						inst.forceGenerate();
					}
					result.addModifiedValue(varName, inst);
				}
				else
				{
					SyntaxError("'<-' character missing in assignment statement");
				}
			}
			else
			{
				SyntaxError("Variable " + varName + " not in current scope: " + scope.getCurrentScope());
			}
		}
		
		return result;
	}

	private PLIRBasicBlock parse_funcCall(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock result = null;
		
		if (toksym != PLToken.callToken)
		{
			SyntaxError("Invalid start to funcCall non-terminal");
		}
		else
		{
			advance(in);
			
			// Save function identifier in case it's a predefined function with a special instruction
			String funcName = sym;
			result = parse_ident(in);
			callStack.add(funcName); // add to call stack
			
			if (funcName.equals("one"))
			{
				int x = 0;
			}
			
			if (toksym == PLToken.openParenToken)
			{
				advance(in);
				
				ArrayList<PLIRInstruction> operands = new ArrayList<PLIRInstruction>(); 
				
				if (toksym != PLToken.semiToken && toksym != PLToken.elseToken
						&& toksym != PLToken.fiToken && toksym != PLToken.odToken 
						&& toksym != PLToken.closeBraceToken && toksym != PLToken.closeParenToken)
				{
					
					result = parse_expression(in);
					operands.add(result.getLastInst());
					
					// Special case for single parameter
					if (toksym == PLToken.commaToken && funcName.equals("OutputNum"))
					{
						SyntaxError("Function OutputNum only takes a single parameter");
					}
					else if (toksym != PLToken.commaToken && funcName.equals("OutputNum"))
					{
						PLIRInstruction inst = new PLIRInstruction(PLIRInstructionType.WRITE, result.getLastInst());
						result = new PLIRBasicBlock();
						result.addInstruction(inst);
					}
					else
					{
						// Read in the rest of the instructions
						while (toksym == PLToken.commaToken)
						{
							advance(in);
							
							// TODO: we should really merge the BBs here
							
							result = parse_expression(in);
							operands.add(result.getLastInst());
						}
						
						if (paramMap.get(funcName) != operands.size())
						{
							SyntaxError("Function: " + funcName + " invoked with the wrong number of arguments. Expected " + paramMap.get(funcName) + ", got " + operands.size());
						}
						
						PLIRInstruction callInst = PLIRInstruction.create_call(funcName, true, operands);
						result = new PLIRBasicBlock();
						result.hasReturn = funcFlagMap.get(funcName); // special case... this is a machine instruction, not a user-defined function
						result.addInstruction(callInst);
					}
				}
				else if (toksym == PLToken.closeParenToken && funcName.equals("InputNum"))
				{
					PLIRInstruction inst = new PLIRInstruction(PLIRInstructionType.READ);
					result = new PLIRBasicBlock();
					result.hasReturn = true; // special case... this is a machine instruction, not a user-defined function
					result.addInstruction(inst);
				}
				else if (toksym == PLToken.closeParenToken && funcName.equals("OutputNewLine"))
				{
					PLIRInstruction inst = new PLIRInstruction(PLIRInstructionType.WLN);
					result = new PLIRBasicBlock();
					result.addInstruction(inst);
				}
				
				// Eat the last token and proceed
				advance(in);
			}
			else if (paramMap.get(funcName) == 0)
			{
//				advance(in);
				ArrayList<PLIRInstruction> emptyList = new ArrayList<PLIRInstruction>();
				PLIRInstruction callInst = PLIRInstruction.create_call(funcName, funcFlagMap.get(funcName), emptyList);
				result = new PLIRBasicBlock();
				result.hasReturn = funcFlagMap.get(funcName); // special case... this is a machine instruction, not a user-defined function
				result.addInstruction(callInst);
			}
			else
			{
				SyntaxError("Function: " + funcName + " invoked with the wrong number of arguments. Expected " + paramMap.get(funcName) + ", got 0");
			}
		}
		
		return result;
	}

	private PLIRBasicBlock parse_statement(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock result = null;
		
		if (toksym == PLToken.letToken)
		{
			result = parse_assignment(in);
		}
		else if (toksym == PLToken.callToken)
		{
			result = parse_funcCall(in);
			callStack.remove(callStack.size() - 1);
		}
		else if (toksym == PLToken.ifToken)
		{
			PLIRInstruction follow = new PLIRInstruction();
			follow.fixupLocation = 0;
			
			advance(in);
			
			scope.pushNewScope("if" + (blockDepth++));
			
			// Parse the condition relation
			PLIRBasicBlock entry = parse_relation(in);
			PLIRInstruction x = entry.instructions.get(entry.instructions.size() - 1);
			PLIRInstruction branch = CondNegBraFwd(x);
			entry.addInstruction(branch);
			
			// Check for follow through branch
			if (toksym != PLToken.thenToken)
			{
				SyntaxError("Missing then clause");
			}
			advance(in);
			
			// Parse the follow-through and add it as the first child of the entry block
			PLIRBasicBlock thenBlock = parse_statSequence(in);
			entry.children.add(thenBlock);
			
			// Create the artificial join node and make it a child of the follow-through branch,
			// and also the exit/join block of the entry block
			PLIRBasicBlock joinNode = new PLIRBasicBlock();
			thenBlock.children.add(joinNode);
			entry.exitNode = entry.joinNode = joinNode;
			thenBlock.fixSpot();
			
			// Check for an else branch
			int offset = 0;
			PLIRBasicBlock elseBlock = null;
			ArrayList<String> sharedModifiers = new ArrayList<String>();
			if (toksym == PLToken.elseToken)
			{
				PLIRInstruction uncond = UnCondBraFwd(follow);
				advance(in);
				
				// Parse the else block
				elseBlock = parse_statSequence(in);
				thenBlock.addInstruction(uncond);
				entry.children.add(elseBlock);
				elseBlock.children.add(joinNode);
				elseBlock.fixSpot();
				
				// Check for necessary phis to be inserted in the join block
				// We need phis for variables that were modified in both branches so we fall through with the right value
				sharedModifiers = new ArrayList<String>();
				for (String i1 : thenBlock.modifiedIdents.keySet())
				{
					for (String i2 : elseBlock.modifiedIdents.keySet())
					{
						if (i1.equals(i2) && sharedModifiers.contains(i1) == false)
						{
							debug("adding: " + i1);
							sharedModifiers.add(i1);
						}
					}
				}
				debug("(if statement) Inserting " + sharedModifiers.size() + " phis");
				ArrayList<PLIRInstruction> phisToAdd = new ArrayList<PLIRInstruction>(); 
				for (int i = 0; i < sharedModifiers.size(); i++)
				{
					String var = sharedModifiers.get(i);
					offset++;
					PLIRInstruction thenInst = thenBlock.modifiedIdents.get(var);
					debug(thenInst.toString());
					PLIRInstruction elseInst = elseBlock.modifiedIdents.get(var);
					debug(elseInst.toString());
					PLIRInstruction phi = PLIRInstruction.create_phi(thenInst, elseInst, PLStaticSingleAssignment.globalSSAIndex);
					joinNode.insertInstruction(phi, 0);
					phisToAdd.add(phi);
					
					// The current value in scope needs to be updated now with the result of the phi
//					scope.updateSymbol(var, phi);
					entry.modifiedIdents.put(var, phi);
					joinNode.modifiedIdents.put(var, phi);
				}
				
				// Jump back from the if statement scope so we can update the variables with the appropriate phi result
				scope.popScope();
				blockDepth--;
				for (int i = 0; i < phisToAdd.size(); i++)
				{
					scope.updateSymbol(sharedModifiers.get(i), phisToAdd.get(i));
					
					// Add to this block's list of modified identifiers
					// Rationale: since we added a phi, the value potentially changes (is modified), 
					// 	so the latest value in the current scope needs to be modified
					entry.modifiedIdents.put(sharedModifiers.get(i), phisToAdd.get(i));
				}
				
				// Check for modifications in the else block (but not in the if)
				ArrayList<String> modifiers = new ArrayList<String>();
				for (String modded : elseBlock.modifiedIdents.keySet())
				{
					if (sharedModifiers.contains(modded) == false)
					{
						modifiers.add(modded);
					}
				}
				debug("(if statement not checking then) Inserting " + modifiers.size() + " phis");
				for (String var : modifiers)
				{
					// Check to make sure this thing was actually in scope!
					if (scope.getCurrentValue(var) == null)
					{
						SyntaxError("Uninitialized identifier in path: " + var);
					}
					
					offset++;
					PLIRInstruction leftInst = elseBlock.modifiedIdents.get(var);
					debug(leftInst.toString());
					PLIRInstruction followInst = scope.getCurrentValue(var);
					PLIRInstruction phi = PLIRInstruction.create_phi(leftInst, followInst, PLStaticSingleAssignment.globalSSAIndex);
					joinNode.insertInstruction(phi, 0);
					
					// The current value in scope needs to be updated now with the result of the phi
					scope.updateSymbol(var, phi);
					
					// Add to this block's list of modified identifiers
					// Rationale: since we added a phi, the value potentially changes (is modified), 
					// 	so the latest value in the current scope needs to be modified
					entry.modifiedIdents.put(var, phi);
					joinNode.modifiedIdents.put(var, phi);
				}
			}
			else
			{
				scope.popScope();
				blockDepth--;
			}
			
			// Check for necessary phis to be inserted in the join block
			// We need phis for variables that were modified in both branches so we fall through with the right value
			ArrayList<String> modifiers = new ArrayList<String>();
			for (String modded : thenBlock.modifiedIdents.keySet())
			{
				if (sharedModifiers.contains(modded) == false)
				{
					modifiers.add(modded);
				}
			}
			debug("(if statement withoutb else) Inserting " + modifiers.size() + " phis");
			for (String var : modifiers)
			{
				// Check to make sure this thing was actually in scope!
				if (scope.getCurrentValue(var) == null)
				{
					SyntaxError("Uninitialized identifier in path: " + var);
				}
				
				offset++;
				PLIRInstruction leftInst = thenBlock.modifiedIdents.get(var);
				PLIRInstruction followInst = scope.getCurrentValue(var);
				PLIRInstruction phi = PLIRInstruction.create_phi(leftInst, followInst, PLStaticSingleAssignment.globalSSAIndex);
				joinNode.insertInstruction(phi, 0);
				
				// The current value in scope needs to be updated now with the result of the phi
				scope.updateSymbol(var, phi);
				
				// Add to this block's list of modified identifiers
				// Rationale: since we added a phi, the value potentially changes (is modified), 
				// 	so the latest value in the current scope needs to be modified
				entry.modifiedIdents.put(var, phi);
				joinNode.modifiedIdents.put(var, phi);
			}
			
			PLStaticSingleAssignment.displayInstructions();
			
			// After the phis have been inserted at the appropriate positions, fixup the entry instructions
			Fixup(x.fixupLocation, -offset - 1);
			
			// Fix the join BB index
			joinNode.fixSpot();
			
			// Configure the dominator tree connections
			entry.dominatorSet.add(thenBlock);
			if (elseBlock != null)
			{
				entry.dominatorSet.add(elseBlock);
			}
			entry.dominatorSet.add(joinNode);
			
			// Fixup the follow-through branch
			Fixup(follow.fixupLocation, -offset);
			
			// Check for fi token and then eat it
			if (toksym != PLToken.fiToken)
			{
				SyntaxError("Missing 'fi' close to if statement");
			}
			advance(in);
			
			// Save the resulting basic block
			entry.isEntry = true;
			result = entry;
		}
		else if (toksym == PLToken.whileToken)
		{	
			// Eat the while token and then save the current PC
			advance(in);
			int loopLocation = PLStaticSingleAssignment.globalSSAIndex;
			
			scope.pushNewScope("while" + (blockDepth++));
			
			// Parse the condition (relation) for the loop
			PLIRBasicBlock entry = parse_relation(in);
			PLIRInstruction x = entry.instructions.get(entry.instructions.size() - 1);
			CondNegBraFwd(x);
			PLIRInstruction bgeInst = PLStaticSingleAssignment.instructions.get(PLStaticSingleAssignment.globalSSAIndex - 1);
			
			// Determine which identifiers are used in the entry/join node so we can defer generation (if PHIs are needed)
			deferredPhiIdents.clear();
			for (String i2 : entry.usedIdents.keySet())
			{
				deferredPhiIdents.add(i2);
			}
			
			// Check for the do token and then eat it
			if (toksym != PLToken.doToken)
			{
				SyntaxError("Missing 'do' in while statement");
			}
			advance(in);
			
			// Build the BB of the statement sequence
			PLIRBasicBlock body = parse_statSequence(in);
			entry.children.add(body);
			PLIRInstruction cmpInst = entry.instructions.get(entry.instructions.size() - 1);
			bgeInst.op1 = cmpInst;
			
			// Create the exit block and hook it in along with the join node
			PLIRBasicBlock exit = new PLIRBasicBlock();
			body.children.add(exit);
			entry.children.add(exit);
			entry.joinNode = entry; // the entry is itself the join node for a while loop (see paper)
			entry.exitNode = exit; // exit is the fall through branch, second child (see paper)
			
			scope.popScope();
			blockDepth--;
			
			////////////////////////////////////////////
			// We've passed through the body and know what variables are updated, now we need to insert phis
			// Phis are inserted when variables in the relation are modified in the loop body
			// Left phi value is entry instruction, right phi value is instruction computed in loop body
			
			int offset = 0;
			ArrayList<String> modded = new ArrayList<String>();
			ArrayList<String> filteredModifiers = new ArrayList<String>();
			for (String i1 : body.modifiedIdents.keySet())
			{
				modded.add(i1);
			}
			debug("body modified size = " + body.modifiedIdents.size());
//			debug("entry used size = " + entry.usedIdents.size());
//			for (String i2 : entry.usedIdents.keySet())
//			{
//				for (String i1 : sharedModifiers)
//				{
//					debug("comparing " + i1 + " and " + i2);
//					if (i1.equals(i2) && filteredModifiers.contains(i2) == false)
//					{
//						filteredModifiers.add(i2);
//					}
//				}
//			}
			debug("(while loop) Inserting " + modded.size() + " phis");
			for (String var : modded)
			{
				offset++;
				PLIRInstruction bodyInst = body.modifiedIdents.get(var);
				debug(bodyInst.toString());
				PLIRInstruction preInst = scope.getCurrentValue(var);
				
				// Inject the phi at the appropriate spot in the join node...
				PLIRInstruction phi = PLIRInstruction.create_phi(preInst, bodyInst, loopLocation);
				entry.joinNode.insertInstruction(phi, 0); 
				
				// The current value in scope needs to be updated now with the result of the phi
				scope.updateSymbol(var, phi);
				
				// Add to this block's list of modified identifiers
				// Rationale: since we added a phi, the value potentially changes (is modified), 
				// 	so the latest value in the current scope needs to be modified
				entry.modifiedIdents.put(var, phi);
				
				// Now loop through the entry and fix instructions as needed
				// Those fixed are replaced with the result of this phi if they have used or modified the sym...
				// TODO: this can possibly be recursive...? 
				// no, phis at lower layer will replace themselves automatically...
				debug("patching up phis: " + cmpInst.toString());
				debug(phi.toString());
				debug("checking left operand");
				debug(cmpInst.op1.toString());
				if (cmpInst.op1 != null && cmpInst.op1.origIdent.equals(var))
				{
					cmpInst.replaceLeftOperand(phi);
				}
				debug("checking right operand");
				debug(cmpInst.op2.toString());
				if (cmpInst.op2 != null && cmpInst.op2.origIdent.equals(var))
				{
					cmpInst.replaceRightOperand(phi);
				}
				
				debug("propagating phi throughout the body via recursive descent of the BB graph");
				body.propogatePhi(var, phi);
				
				/*
				
				// Now loop through the body and fix instructions as needed
				// TODO: this can possibly be recursive...?
				// no, phis at lower layer will replace themselves automatically...
				debug("past patch");
				for (PLIRInstruction bInst : body.instructions)
				{
					debug("trying: " + bInst.toString());
					if (bInst.op1 != null && bInst.op1.origIdent.equals(var))
					{
						bInst.replaceLeftOperand(phi);
					}
					if (bInst.op2 != null && bInst.op2.origIdent.equals(var))
					{
						bInst.replaceRightOperand(phi);
					}
				}
				
				*/
			}
			
			////////////////////////////////////////////
			
			// Insert the unconditional branch at (location - pc)
			PLIRInstruction.create_BEQ(loopLocation - PLStaticSingleAssignment.globalSSAIndex);
			
			// Fixup the conditional branch at the appropriate location
			bgeInst.fixupLocation = x.fixupLocation;
			Fixup(bgeInst.fixupLocation, 0); 
			
			// Configure the dominator tree connections
			entry.dominatorSet.add(body);
			entry.dominatorSet.add(exit);
			
			// Check for the closing od token and then eat it
			if (toksym != PLToken.odToken)
			{
				SyntaxError("Missing 'od' in while statement");
			}
			advance(in);
			
			// the result of parsing this basic block is the entry node, which doubles as the join and exit node
			entry.isEntry = true;
			result = entry;
		}
		else if (toksym == PLToken.returnToken)
		{
			advance(in);
			if (toksym != PLToken.semiToken && toksym != PLToken.elseToken
					&& toksym != PLToken.fiToken && toksym != PLToken.odToken && toksym != PLToken.closeBraceToken)
			{
				result = parse_expression(in);
				
				// Since the return statement was followed by an expression, force the expression to be generated...
				result.instructions.get(result.instructions.size() - 1).overrideGenerate = true;
				result.instructions.get(result.instructions.size() - 1).forceGenerate();
				result.returnInst = result.getLastInst();
				debug("Forcing generation of return statement");
			}
		}
		else 
		{
			SyntaxError("Invalid start of a statement");
		}
		
		return result;
	}

	private PLIRBasicBlock parse_statSequence(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		boolean isReturn = toksym == PLToken.returnToken;
		PLIRBasicBlock result = parse_statement(in);
		
		if (root == null)
		{
			root = result;
		}
		
		if (isReturn)
		{
			// TODO: move into BB.setResult()
			result.returnInst = result.getLastInst();
			result.hasReturn = true;
		}
		
		while (toksym == PLToken.semiToken)
		{
			advance(in);
			isReturn = toksym == PLToken.returnToken;
			PLIRBasicBlock nextBlock = parse_statement(in);
			
			if (isReturn)
			{
				// TODO: move into BB.setResult()
				result.returnInst = result.getLastInst();
				result.hasReturn = true;
			}
			
			// Merge the block results here
			result = PLIRBasicBlock.merge(result, nextBlock);
		}
		
		// Fix the spot of the basic block (since we're leaving a statement sequence) and call it a day
		result.fixSpot();
		
		return result;
	}

	private PLIRBasicBlock parse_typeDecl(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock result = null;
		
		if (toksym == PLToken.varToken)
		{
			advance(in);
			
			IdentType type = IdentType.VAR;
			identTypeMap.put(sym, type);
			scope.addVarToScope(sym);
		}
		else if (toksym == PLToken.arrToken)
		{
			advance(in);
			IdentType type = IdentType.ARRAY;
			
			if (toksym == PLToken.openBracketToken)
			{
				advance(in);
				result = parse_number(in);
				if (toksym == PLToken.closeBracketToken)
				{
					advance(in);
					while (toksym == PLToken.openBracketToken)
					{
						advance(in);
						result = parse_number(in);
						if (toksym == PLToken.closeBracketToken)
						{
							SyntaxError("']' missing from typeDecl non-terminal");
						}
						advance(in);
					}
					
					// Symbol name here, finally...
					debug("adding array: " + sym);
					identTypeMap.put(sym, type);
					scope.addVarToScope(sym);
				}
				else
				{
					SyntaxError("']' missing from typeDecl non-terminal");
				}
			}
			else
			{
				SyntaxError("'[' missing from typeDecl non-terminal");
			}
		}
		else
		{
			SyntaxError("Invalid start to varDecl non-terminal");
		}
		
		return null;
	}

	private PLIRBasicBlock parse_varDecl(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{	
		IdentType type = IdentType.VAR;
		if (toksym == PLToken.varToken)
		{
			type = IdentType.VAR;
		}
		else if (toksym == PLToken.arrToken)
		{
			type = IdentType.ARRAY;
		}
		
		PLIRBasicBlock result = parse_typeDecl(in);
		result = parse_ident(in);
		
		while (toksym == PLToken.commaToken)
		{
			advance(in);
			scope.addVarToScope(sym);
			identTypeMap.put(sym, type);
			result = parse_ident(in); 
		}
		if (toksym != PLToken.semiToken)
		{
			SyntaxError("';' missing from varDecl");
		}
		advance(in);
		
		return result;
	}

	private PLIRBasicBlock parse_funcDecl(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock result = null;
		if (toksym == PLToken.funcToken || toksym == PLToken.procToken)
		{
			int callType = toksym;
			
			advance(in);
			scope.addVarToScope(sym);
			scope.pushNewScope(sym);
			funcName = sym; // save for recovery later on
			result = parse_ident(in);
			
			if (toksym != PLToken.semiToken)
			{
				result = parse_formalParam(in);
			}
			else
			{
				paramMap.put(funcName, 0); // no formal parameters specified
			}
			
			// Eat the semicolon and then parse the body
			advance(in);  
			result = parse_funcBody(in);
			
			// Eat the semicolon terminating the body
			if (toksym != PLToken.semiToken)
			{
				SyntaxError("';' missing from funcDecl non-terminal");
			}
			advance(in);
			
			// Leave the scope of this new function
			String leavingScope = scope.popScope();
			blockDepth--;
			debug("Leaving scope: " + leavingScope);
		}
		else
		{
			SyntaxError("Invalid start to funcDecl non-terminal");
		}
		
		return result;
	}

	private PLIRBasicBlock parse_formalParam(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock result = null;
		
		int numParams = 0;
		if (toksym == PLToken.openParenToken)
		{
			advance(in);
			
			if (toksym == PLToken.closeParenToken)
			{
				// pass, no params
				advance(in);
			}
			else
			{
				result = parse_ident(in);
				numParams++;
				while (toksym == PLToken.commaToken)
				{
					advance(in);
					result = parse_ident(in);
					numParams++;
				}
				
				if (toksym != PLToken.closeParenToken)
				{
					SyntaxError("')' missing from formalParam");
				}
				
				advance(in);
			}
		}
		
		paramMap.put(funcName, numParams);
		
		return result;
	}

	private PLIRBasicBlock parse_funcBody(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock result = null;
		
		if (toksym == PLToken.varToken || toksym == PLToken.arrToken)
		{
			result = parse_varDecl(in);
		}
		
		if (toksym != PLToken.openBraceToken)
		{
			SyntaxError("'{' missing from funcBody non-terminal.");
		}
		
		// eat the open brace '{'
		advance(in);
		
		// Must be a statSequence here, if there exists one!
		if (toksym != PLToken.closeBraceToken)
		{
			result = parse_statSequence(in);
			if (toksym != PLToken.closeBraceToken)
			{
				SyntaxError("'}' missing from statSequence non-terminal in funcBody");
			}
			advance(in);
		}
		
		return result;
	}
	
	// per spec
	private PLIRInstruction CondNegBraFwd(PLIRInstruction x)
	{
		x.fixupLocation = PLStaticSingleAssignment.globalSSAIndex;
		PLIRInstruction inst = PLIRInstruction.create_branch(x, x.condcode);
		return inst;
	}
	
	// per spec
	private PLIRInstruction UnCondBraFwd(PLIRInstruction x)
	{
		PLIRInstruction inst = PLIRInstruction.create_BEQ(x.fixupLocation);
		x.fixupLocation = PLStaticSingleAssignment.globalSSAIndex - 1;
		return inst;
	}

	// set the address to which we jump...
	// buffer[loc] = (pc - loc) + offset;
	// MODIFIED FROM SPEC => offset needed to be introduced to account for phi injection
	private void Fixup(int loc, int offset)
	{
		debug("Fixing: " + loc + ", " + (PLStaticSingleAssignment.globalSSAIndex - loc + offset) + ", " + offset);
		PLStaticSingleAssignment.instructions.get(loc).i2 = (PLStaticSingleAssignment.globalSSAIndex - loc + offset);
	}
}
