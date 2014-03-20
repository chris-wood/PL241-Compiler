package com.uci.cs241.pl241.frontend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.lang.model.type.TypeKind;

import com.uci.cs241.pl241.backend.DLXBasicBlock;
import com.uci.cs241.pl241.ir.PLIRBasicBlock;
import com.uci.cs241.pl241.ir.PLIRInstruction;
import com.uci.cs241.pl241.ir.PLIRInstruction.OperandType;
import com.uci.cs241.pl241.ir.PLIRInstruction.InstructionType;
import com.uci.cs241.pl241.ir.PLIRInstruction.ResultKind;
import com.uci.cs241.pl241.ir.PLStaticSingleAssignment;

public class PLParser
{
	// Current symbol/token values used for parsing
	public String sym;
	public int toksym;
	public PLIRBasicBlock root;
	
	// BB depth (to uniquify scopes)
	public int blockDepth = 0;
	
	// Control flags 
	public boolean globalVariableParsing = false;
	public boolean globalFunctionParsing = false; 
	public boolean parseVariableDeclaration = false;
	public boolean isFunction = false;
	public boolean parsingFunctionBody = false;
	
	// For array and variable parsing
	public ArrayList<PLIRInstruction> params;
	public ArrayList<PLIRInstruction> variables;
	
	// The scope symbol table
	public PLSymbolTable scope;
	
	// Identifier/array map
	public enum IdentType {VAR, ARRAY, FUNC};
	public HashMap<String, IdentType> identTypeMap = new HashMap<String, IdentType>();
	public HashMap<String, ArrayList<Integer>> arrayDimensionMap = new HashMap<String, ArrayList<Integer>>();
	
	public HashMap<String, PLIRInstruction> globalVariables = new HashMap<String, PLIRInstruction>();
	private ArrayList<String> deferredPhiIdents = new ArrayList<String>();
	
	public HashMap<PLIRInstruction, String> addressKillMap = new HashMap<PLIRInstruction, String>();
	
	public String funcName = "";
	public ArrayList<String> callStack = new ArrayList<String>();
	public HashMap<String, PLIRBasicBlock> funcBlockMap = new HashMap<String, PLIRBasicBlock>();
	public HashMap<String, PLIRBasicBlock> procBlockMap = new HashMap<String, PLIRBasicBlock>();
	public HashMap<String, Integer> paramMap = new HashMap<String, Integer>();
	public HashMap<String, Boolean> funcFlagMap = new HashMap<String, Boolean>();
	
	// def-use chain data structure
	public HashMap<PLIRInstruction, HashSet<PLIRInstruction>> duChain = new HashMap<PLIRInstruction, HashSet<PLIRInstruction>>(); 
	
	public PLParser()
	{
		paramMap.put("InputNum", 0);
		paramMap.put("OutputNewLine", 0);
		paramMap.put("OutputNum", 1);
		
		identTypeMap.put("InputNum", IdentType.FUNC);
		identTypeMap.put("OutputNewLine", IdentType.FUNC);
		identTypeMap.put("OutputNum", IdentType.FUNC);
		
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
	
	public PLIRBasicBlock findJoin(PLIRBasicBlock left, PLIRBasicBlock right)
	{
		PLIRBasicBlock join = null;
		if (left != null && right != null)
		{
			// DFS on left to build visited set
			ArrayList<PLIRBasicBlock> leftStack = new ArrayList<PLIRBasicBlock>();
			HashMap<Integer, PLIRBasicBlock> joinSeen = new HashMap<Integer, PLIRBasicBlock>();
			leftStack.add(left);
			while (leftStack.isEmpty() == false)
			{
				PLIRBasicBlock curr = leftStack.get(leftStack.size() - 1);
				leftStack.remove(leftStack.size() - 1);
				if (joinSeen.containsKey(curr.id) == false)
				{
					joinSeen.put(curr.id, curr);
					if (curr.rightChild != null)
					{
						leftStack.add(curr.rightChild);
					}
					if (curr.leftChild != null)
					{
						leftStack.add(curr.leftChild);
					}
				}
			}

			// Find common join point
			ArrayList<PLIRBasicBlock> rightStack = new ArrayList<PLIRBasicBlock>();
			rightStack.add(right);
			while (rightStack.isEmpty() == false)
			{
				PLIRBasicBlock curr = rightStack.get(rightStack.size() - 1);
				rightStack.remove(rightStack.size() - 1);
				if (joinSeen.containsKey(curr.id))
				{
					join = joinSeen.get(curr.id);
					return join;
				}
				else
				{
					if (curr.rightChild != null)
					{
						rightStack.add(curr.rightChild);
					}
					if (curr.leftChild != null)
					{
						rightStack.add(curr.leftChild);
					}
				}
			}
		}

		return join;
	}
	
	// this is what's called - starting with the computation non-terminal
	public ArrayList<PLIRBasicBlock> parse(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException, ParserException
	{
		ArrayList<PLIRBasicBlock> blocks = new ArrayList<PLIRBasicBlock>();
		
		PLIRBasicBlock result = new PLIRBasicBlock();
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
				result = PLIRBasicBlock.merge(result, parse_varDecl(in)); 
			}
			globalVariableParsing = false;
			
			// Parse global functions and procedures
			globalFunctionParsing = true;
			while (toksym == PLToken.funcToken || toksym == PLToken.procToken)
			{
				int funcType = toksym;
				PLIRBasicBlock funcEntry = parse_funcDecl(in); // a separate BB for each function/procedure
				funcEntry.label = funcName;
				blocks.add(funcEntry);
				
				switch (funcType)
				{
				case PLToken.funcToken:
					funcBlockMap.put(funcName, funcEntry); // save this so that others may use it
					break;
				case PLToken.procToken:
					procBlockMap.put(funcName, funcEntry); // save this so that others may use it
					break;
				}
				
				PLIRBasicBlock endBlock = funcEntry;
				while (endBlock.joinNode != null)
				{
					endBlock = endBlock.joinNode;
				}
				
				// Insert global variable save functions at the end of the function, if necessary
				Function func = scope.functions.get(funcName);
				for (PLIRInstruction inst : func.vars)
				{
					funcEntry.insertInstruction(inst, 0);
				}
			}
			globalFunctionParsing = false;
			
			// Parse the main computation
			if (toksym == PLToken.openBraceToken)
			{
				// eat the sequence of statements that make up the computation
				advance(in);
				result = parse_statSequence(in);
				while (result.joinNode != null)
				{
					result = result.joinNode;
				}
				
				// Parse the close of the computation
				if (toksym == PLToken.closeBraceToken)
				{
					advance(in);
					if (toksym == PLToken.periodToken)
					{
						PLIRInstruction inst = new PLIRInstruction(scope, InstructionType.END);
						result.addInstruction(inst);
						PLStaticSingleAssignment.globalSSAIndex++; // skip over dummy END instruction
						
						if (root.parents.size() > 0)
						{
							PLIRBasicBlock parent = result.parents.get(0);
							while (parent != null && parent.parents.size() > 0)
							{
								parent = parent.parents.get(0);
							}
							root = parent;
						}			
						
						blocks.add(root);
						
						// Insert globals into the root so the code generator knows what to allocate space for
						for (PLIRInstruction glob : globalVariables.values())
						{
							root.insertInstruction(glob, 0);
						}
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
				SyntaxError("'{' missing in computation non-terminal (there is no main body!)");
			}
		}
		else
		{
			SyntaxError("Computation does not begin with main keyword");
		}
		
		return blocks;
	}

	// non-terminal
	private PLIRBasicBlock parse_ident(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		String symName = sym;
		PLIRBasicBlock block = null;
		
		if (parsingFunctionBody)
		{	
			PLIRInstruction inst = null;
			
			// Ensure this parameter is specified in the body of the function, else syntax error
			Function func = scope.functions.get(funcName);
			
			if (parseVariableDeclaration)
			{
				// Initialize the variable to 0
				inst = new PLIRInstruction(scope);
				inst.opcode = InstructionType.ADD;
				inst.i1 = 0;
				inst.op1type = OperandType.CONST;
				inst.i2 = 0;
				inst.op2type = OperandType.CONST;
				inst.kind = ResultKind.VAR;
				inst.type = OperandType.CONST; 
				inst.overrideGenerate = true;
				inst.forceGenerate(scope);
				inst.ident.put(scope.getCurrentScope(), symName);
				
				func.addParameter(inst);
				
				// Eat the symbol, create the block with the single instruction, add the ident to the list
				// of used identifiers, and return
				advance(in);
				
				block = new PLIRBasicBlock();
				block.addInstruction(inst);
				block.addUsedValue(symName, inst);
				
				// Add the sheet to scope
				scope.addVarToScope(symName);
				scope.updateSymbol(symName, inst);
				duChain.put(inst, new HashSet<PLIRInstruction>());
				return block;
			}
			else if (func.isVarInScope(sym) || func.isLocalVariable(symName))
			{
				inst = scope.getCurrentValue(symName);
			}
			else if (scope.isGlobalVariable(sym))
			{
				inst = scope.getCurrentValue(sym);
				func.addVarToScope(sym);
				
				// Eat the symbol, create the block with the single instruction, add the ident to the list
				// of used identifiers, and return
				advance(in);
				
				block = new PLIRBasicBlock();
				block.addInstruction(inst);
				block.addUsedValue(symName, inst);
				
				// Add the sheet to scope
				scope.addVarToScope(symName);
				scope.updateSymbol(symName, inst);
				
				// The original global variable is used
				if (duChain.containsKey(inst) == false)
				{
					duChain.put(inst, new HashSet<PLIRInstruction>());
				}
				return block;
			}
			else
			{
				inst = func.getOperandByName(sym);
			}
			
			advance(in);
			
			block = new PLIRBasicBlock();
			block.addInstruction(inst);
			block.addUsedValue(symName, inst);
			
			return block;
		}
		
		if (globalVariableParsing) // initialize each variable to the constant 0
		{
			// Initialize the variable to 0
			PLIRInstruction inst = new PLIRInstruction(scope);
			inst.opcode = InstructionType.GLOBAL;
			inst.i1 = 0;
			inst.op1type = OperandType.CONST;
			inst.kind = ResultKind.VAR;
			inst.type = OperandType.ADDRESS;
			inst.overrideGenerate = true;
			inst.forceGenerate(scope);
			
			inst.ident.put(scope.getCurrentScope(), symName);
			inst.saveName.put(scope.getCurrentScope(), symName);
			
			// Eat the symbol, create the block with the single instruction, add the ident to the list
			// of used identifiers, and return
			advance(in);
			
			block = new PLIRBasicBlock();
			block.addInstruction(inst);
			block.addUsedValue(symName, inst);
			
			// Add the sheet to scope
			scope.addVarToScope(symName);
			scope.updateSymbol(symName, inst);
			scope.addGlobalVariable(inst);
			duChain.put(inst, new HashSet<PLIRInstruction>());
			globalVariables.put(symName, inst);
			
			return block;
		}
		else if (identTypeMap.containsKey(sym))
		{
			switch (identTypeMap.get(sym))
			{
			case ARRAY:
				PLIRInstruction instArray = scope.getCurrentValue(sym);
				
				// Memorize the ident we saw here
				if (instArray != null)
				{
					instArray.wasIdent = true;
					instArray.ident.put(scope.getCurrentScope(), sym);
				}
				
				// Eat the symbol, create the block with the single instruction, add the ident to the list
				// of used identifiers, and return
				advance(in);
				
				block = new PLIRBasicBlock();
				block.addInstruction(instArray);
				block.addUsedValue(symName, instArray);
				
				return block;
			case VAR:
				
				PLIRInstruction inst = scope.getCurrentValue(sym);
				
				// Memorize the ident we saw here
				if (inst != null)
				{
					inst.wasIdent = true;
					inst.ident.put(scope.getCurrentScope(), sym);
				}
				
				// Eat the symbol, create the block with the single instruction, add the ident to the list
				// of used identifiers, and return
				advance(in);
				
				block = new PLIRBasicBlock();
				block.addInstruction(inst);
				block.addUsedValue(symName, inst);
				
				return block;
			}
		}
		
		PLIRInstruction inst = scope.getCurrentValue(sym);
		
		// Eat the symbol, create the block with the single instruction, add the ident to the list
		// of used identifiers, and return
		advance(in);
		
		// Create & return the new basic block
		block = new PLIRBasicBlock();
		block.addInstruction(inst);
		block.addUsedValue(symName, inst);
		
		return block;
	}

	// this just puts an immediate value in a temporary variable (no moves!)
	private PLIRBasicBlock parse_number(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{	
		PLIRInstruction li = new PLIRInstruction(scope, InstructionType.ADD, 0, Integer.parseInt(sym));
		li.type = OperandType.CONST;
		li.isConstant = true;
		li.tempPosition = PLStaticSingleAssignment.globalSSAIndex;
		advance(in);
		PLIRBasicBlock block = new PLIRBasicBlock();
		block.addInstruction(li);
		return block;
	}

	private PLIRBasicBlock parse_designator(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		String name = sym;
		
		PLIRBasicBlock result = parse_ident(in);
		
		boolean isArray = false;
		while (toksym == PLToken.openBracketToken)
		{
			isArray = true;
			if (result.arrayOperands == null)
			{
				result.arrayOperands = new ArrayList<PLIRInstruction>();
			}
			advance(in);
			
			result = PLIRBasicBlock.merge(result, parse_expression(in));
			result.arrayOperands.add(result.getLastInst());
			if (toksym != PLToken.closeBracketToken)
			{
				SyntaxError("']' missing from designator non-terminal.");
			}
			advance(in);
		}
		
		if (isArray)
		{
			result.arrayName = name;
		}
		
		if (isArray && arrayDimensionMap.get(name).size() != result.arrayOperands.size())
		{
			SyntaxError("Invalid array indexing. Array " + name + " has " + arrayDimensionMap.get(name).size() 
					+ " dimensions but was indexed with " + result.arrayOperands.size());
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
			
			String funcName = callStack.get(callStack.size() - 1);
			if (callStack.get(callStack.size() - 1).equals("InputNum"))
			{
				// pass, this is a special case
			}
			else if (funcFlagMap.get(funcName) == false)
			{
				SyntaxError("Function that was invoked had no return value!");
			}
			
			// Handle replacement of global variables 
			if (funcName.equals("OutputNum") == false && funcName.equals("OutputNewLine") == false && funcName.equals("InputNum") == false)
			{
				Function func = scope.functions.get(funcName);
				for (PLIRInstruction glob : func.modifiedGlobals.keySet())
				{
					if (glob != null)
					{
						String name = glob.ident.get("main");
						if (scope.getCurrentValue(name).kind == ResultKind.CONST)
						{
							func.constantsToSave.put(name, scope.getCurrentValue(name).tempVal);
						}
						glob.type = OperandType.INST;
						glob.kind = ResultKind.VAR;
						glob.overrideGenerate = true;
						glob.forceGenerate(scope);
						scope.updateSymbol(name, glob);
					}
				}
				
				for (PLIRInstruction glob : func.usedGlobals.keySet())
				{
					if (glob != null)
					{
						String name = glob.ident.get("main");
						if (scope.getCurrentValue(name).kind == ResultKind.CONST)
						{
							func.constantsToSave.put(name, scope.getCurrentValue(name).tempVal);
						}
						glob.type = OperandType.INST;
						glob.kind = ResultKind.VAR;
						glob.overrideGenerate = true;
						glob.forceGenerate(scope);
						scope.updateSymbol(name, glob);
					}
				}
			}
			
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
			PLIRInstruction leftInst = factor.getLastInst();
			if (leftInst.isArray || factor.arrayName != null)
			{	
				ArrayList<PLIRInstruction> offsetInstructions = null;
				if (factor.instructions.get(0).opcode == InstructionType.STORE)
				{
					factor.instructions.remove(0);
				}
				offsetInstructions = arrayOffsetCalculation(factor);
				for (PLIRInstruction inst : offsetInstructions)
				{
					termNode.addInstruction(inst);
				}
				
				PLIRInstruction offset = offsetInstructions.get(offsetInstructions.size() - 1);
				PLIRInstruction load = new PLIRInstruction(scope);
				load.loadInstructions = offsetInstructions;
				load.opcode = InstructionType.LOAD;
				load.op1type = OperandType.ADDRESS;
				load.op1 = offset;
				load.type = OperandType.INST;
				load.forceGenerate(scope);
				load.isArray = true;
				load.dummyName = leftInst.ident.get(scope.getCurrentScope());
				leftInst = load;
				termNode.addInstruction(load);
			}
			else
			{
				for (PLIRInstruction li : factor.instructions)
				{
					if (li.op1 != null)
					{
						leftInst.dependents.add(li.op1);
					}
					if (li.op2 != null)
					{
						leftInst.dependents.add(li.op2);
					}
					leftInst.dependents.add(li);
				}
			}
			
			PLIRInstruction rightInst = rightNode.getLastInst();
			if (rightInst.isArray)
			{
				ArrayList<PLIRInstruction> offsetInstructions = null;
				if (rightNode.instructions.get(0).opcode == InstructionType.STORE)
				{
					rightNode.instructions.remove(0);
				}
				offsetInstructions = arrayOffsetCalculation(rightNode);
				for (PLIRInstruction inst : offsetInstructions)
				{
					termNode.addInstruction(inst);
				}
				
				PLIRInstruction offset = offsetInstructions.get(offsetInstructions.size() - 1);
				PLIRInstruction load = new PLIRInstruction(scope);
				load.loadInstructions = offsetInstructions;
				load.opcode = InstructionType.LOAD;
				load.op1type = OperandType.ADDRESS;
				load.op1 = offset;
				load.type = OperandType.INST;
				load.forceGenerate(scope);
				load.isArray = true;
				load.dummyName = rightInst.ident.get(scope.getCurrentScope());
				rightInst = load;
				termNode.addInstruction(load);
			}
			else
			{
				for (PLIRInstruction li : rightNode.instructions)
				{
					if (li.op1 != null)
					{
						rightInst.dependents.add(li.op1);
					}
					if (li.op2 != null)
					{
						rightInst.dependents.add(li.op2);
					}
					rightInst.dependents.add(li);
				}
			}
			
			InstructionType opcode = operator == PLToken.timesToken ? InstructionType.MUL : InstructionType.DIV;
			
			PLIRInstruction termInst = new PLIRInstruction(scope, opcode, leftInst, rightInst);
			if (termInst.kind == ResultKind.CONST)
			{
				termInst.type = OperandType.CONST;
			}
			else
			{
				termInst.type = OperandType.INST;
			}
			
			for (PLIRInstruction inst : factor.instructions)
			{
				termNode.addInstruction(inst);
			}
			for (PLIRInstruction inst : rightNode.instructions)
			{
				termNode.addInstruction(inst);
			}
			termNode.addInstruction(termInst);
			
			// Save whatever values are used in these expressions
			for (String sym : factor.usedIdents.keySet())
			{
				termNode.addUsedValue(sym, factor.usedIdents.get(sym));
			}
			for (String sym : rightNode.usedIdents.keySet())
			{
				termNode.addUsedValue(sym, rightNode.usedIdents.get(sym));
			}
			
			// Update DU chain
			if (duChain.containsKey(leftInst))
			{
				duChain.get(leftInst).add(termInst);
				leftInst.uses++;
			}
			if (duChain.containsKey(rightInst))
			{
				duChain.get(rightInst).add(termInst);
				rightInst.uses++;
			}
			
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
			PLIRInstruction leftInst = term.getLastInst();
			if (leftInst.isArray || term.arrayName != null)
			{
				ArrayList<PLIRInstruction> offsetInstructions = null;
				if (term.instructions.get(0).opcode == InstructionType.STORE)
				{
					term.instructions.remove(0);
				}
				offsetInstructions = arrayOffsetCalculation(term);
				for (PLIRInstruction inst : offsetInstructions)
				{
					exprNode.addInstruction(inst);
				}
				
				PLIRInstruction offset = offsetInstructions.get(offsetInstructions.size() - 1);
				PLIRInstruction load = new PLIRInstruction(scope);
				load.loadInstructions = offsetInstructions;
				load.opcode = InstructionType.LOAD;
				load.op1type = OperandType.ADDRESS;
				load.op1 = offset;
				load.type = OperandType.INST;
				load.forceGenerate(scope);
				load.isArray = true;
				load.dummyName = term.arrayName;
				leftInst = load;
				exprNode.addInstruction(load);
			}
			else
			{
				for (PLIRInstruction li : term.instructions)
				{
					if (li.op1 != null)
					{
						leftInst.dependents.add(li.op1);
					}
					if (li.op2 != null)
					{
						leftInst.dependents.add(li.op2);
					}
					leftInst.dependents.add(li);
				}
			}
			
			PLIRInstruction rightInst = rightNode.getLastInst();
			if (rightInst.isArray || rightNode.arrayName != null)
			{	
				ArrayList<PLIRInstruction> offsetInstructions = null;
				if (rightNode.instructions.get(0).opcode == InstructionType.STORE)
				{
					rightNode.instructions.remove(0);
				}
				offsetInstructions = arrayOffsetCalculation(rightNode);
				for (PLIRInstruction inst : offsetInstructions)
				{
					exprNode.addInstruction(inst);
				}
				
				PLIRInstruction offset = offsetInstructions.get(offsetInstructions.size() - 1);
				PLIRInstruction load = new PLIRInstruction(scope);
				load.loadInstructions = offsetInstructions;
				load.opcode = InstructionType.LOAD;
				load.op1type = OperandType.ADDRESS;
				load.op1 = offset;
				load.type = OperandType.INST;
				load.forceGenerate(scope);
				load.isArray = true;
				load.dummyName = rightNode.arrayName;
				rightInst = load;
				exprNode.addInstruction(load);
			}
			else
			{
				for (PLIRInstruction li : rightNode.instructions)
				{
					if (li.op1 != null)
					{
						rightInst.dependents.add(li.op1);
					}
					if (li.op2 != null)
					{
						rightInst.dependents.add(li.op2);
					}
					rightInst.dependents.add(li);
				}
			}
			
			InstructionType opcode = operator == PLToken.plusToken ? InstructionType.ADD : InstructionType.SUB;
			PLIRInstruction exprInst = new PLIRInstruction(scope, opcode, leftInst, rightInst);
			if (exprInst.kind == ResultKind.CONST)
			{
				exprInst.type = OperandType.CONST;
			}
			else
			{
				exprInst.type = OperandType.INST;
				exprInst.forceGenerate(scope);
			}
			
			for (PLIRInstruction inst : term.instructions)
			{
				exprNode.addInstruction(inst);
			}
			for (PLIRInstruction inst : rightNode.instructions)
			{
				exprNode.addInstruction(inst);
			}
			exprNode.addInstruction(exprInst);
			
			// Save whatever values are used in these expressions
			for (String sym : term.usedIdents.keySet())
			{
				exprNode.addUsedValue(sym, term.usedIdents.get(sym));
			}
			for (String sym : rightNode.usedIdents.keySet())
			{
				exprNode.addUsedValue(sym, rightNode.usedIdents.get(sym));
			}
			
			// Update DU chain
			if (duChain.containsKey(leftInst))
			{
				duChain.get(leftInst).add(exprInst);
				leftInst.uses++;
			}
			if (duChain.containsKey(rightInst))
			{
				duChain.get(rightInst).add(exprInst);
				rightInst.uses++;
			}
			
			return exprNode;
		}
		else
		{
			return term;
		}
	}

	private PLIRBasicBlock parse_relation(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock left = parse_expression(in);
		
		// Check for an appropriate relational token separating the two expressions
		if (PLToken.isRelationalToken(toksym) == false)
		{
			SyntaxError("Invalid relational character");
		}
		
		// Save the relational code
		int condcode = toksym;
		
		// Eat the relational token
		advance(in);
		
		// The BB to store the result
		PLIRBasicBlock relation = new PLIRBasicBlock();
		
		if (left.instructions.get(0).opcode == InstructionType.STORE)
		{
			left.instructions.remove(0);
		}
		
		PLIRInstruction leftInst = left.getLastInst();
		for (PLIRInstruction li : left.instructions)
		{
			if (li.op1 != null)
			{
				leftInst.dependents.add(li.op1);
			}
			if (li.op2 != null)
			{
				leftInst.dependents.add(li.op2);
			}
			leftInst.dependents.add(li);
		}
		
		// Build the comparison instruction with the memorized condition
		if (leftInst == null || (leftInst != null && leftInst.isArray && 
				leftInst.opcode != InstructionType.LOAD) || left.arrayName != null)
		{
			ArrayList<PLIRInstruction> offsetInstructions = null;
			if (leftInst.opcode == InstructionType.LOAD)
			{
				offsetInstructions = new ArrayList<PLIRInstruction>();
				for (PLIRInstruction inst : leftInst.loadInstructions)
				{
					relation.addInstruction(inst);
					leftInst.dependents.add(inst);
					offsetInstructions.add(inst);
				}
			}
			else
			{
				offsetInstructions = arrayOffsetCalculation(left);
				for (PLIRInstruction inst : offsetInstructions)
				{
					relation.addInstruction(inst);
					leftInst.dependents.add(inst);
				}
			}
			
			PLIRInstruction offset = offsetInstructions.get(offsetInstructions.size() - 1);
			PLIRInstruction load = new PLIRInstruction(scope);
			load.opcode = InstructionType.LOAD;
			load.op1type = OperandType.ADDRESS;
			load.op1 = offset;
			load.type = OperandType.INST;
			load.forceGenerate(scope);
			load.isArray = true;
			load.ident.put(scope.getCurrentScope(), leftInst.ident.get(scope.getCurrentScope()));
			load.dummyName = left.arrayName;
			leftInst = load;
			relation.addInstruction(load);
		}
		
		relation = PLIRBasicBlock.merge(relation, left);
		
		// Parse the right-hand part of the relation expression
		PLIRBasicBlock right = parse_expression(in);
		
		if (right.instructions.get(0).opcode == InstructionType.STORE)
		{
			right.instructions.remove(0);
		}
		
		PLIRInstruction rightInst = right.getLastInst();
		for (PLIRInstruction ri : right.instructions)
		{
			if (ri.op1 != null)
			{
				rightInst.dependents.add(ri.op1);
			}
			if (ri.op2 != null)
			{
				rightInst.dependents.add(ri.op2);
			}
			rightInst.dependents.add(ri);
		}
		
		// Add the result of all relations
		if (rightInst == null || (rightInst != null && rightInst.isArray && rightInst.opcode != InstructionType.LOAD) || right.arrayName != null)
		{	
			ArrayList<PLIRInstruction> offsetInstructions = null;
			if (rightInst.opcode == InstructionType.LOAD)
			{
				offsetInstructions = new ArrayList<PLIRInstruction>();
				for (PLIRInstruction inst : rightInst.loadInstructions)
				{
					relation.addInstruction(inst);
					rightInst.dependents.add(inst);
					offsetInstructions.add(inst);
				}
			}
			else
			{
				offsetInstructions = arrayOffsetCalculation(right);
				for (PLIRInstruction inst : offsetInstructions)
				{
					relation.addInstruction(inst);
					rightInst.dependents.add(inst);
				}
			}
			
			PLIRInstruction offset = offsetInstructions.get(offsetInstructions.size() - 1);
			PLIRInstruction load = new PLIRInstruction(scope);
			load.opcode = InstructionType.LOAD;
			load.op1type = OperandType.ADDRESS;
			load.op1 = offset;
			load.type = OperandType.INST;
			load.forceGenerate(scope);
			load.isArray = true;
			load.ident.put(scope.getCurrentScope(), rightInst.ident.get(scope.getCurrentScope()));
			load.dummyName = right.arrayName;
			rightInst = load;
			relation.addInstruction(load);
		}
		
		relation = PLIRBasicBlock.merge(relation, right);
		
		// Create the comparison instruction that joins the two together
		PLIRInstruction inst = PLIRInstruction.create_cmp(scope, leftInst, rightInst);
		inst.condcode = condcode;
		inst.fixupLocation = 0;
		
		// Create the relation block containing the instruction
		relation.addInstruction(inst);
		
		// Save whatever values are used in these expressions
		for (String sym : left.usedIdents.keySet())
		{
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
				PLIRBasicBlock desigBlock = parse_designator(in);
				PLIRInstruction desigInst = desigBlock.getLastInst();
				
				// Check to see if this assignment needs to be saved at the end of the function
				boolean markToSave = false;
				if (parsingFunctionBody && globalVariables.containsKey(desigBlock.getLastInst().ident.get(scope.getCurrentScope())))
				{
					markToSave = true;
				}
				
				if (globalVariables.containsKey(desigBlock.getLastInst().ident.get(scope.getCurrentScope())))
				{
					desigInst.isGlobalVariable = true;
				}
				
				if (toksym == PLToken.becomesToken)
				{
					advance(in);
					result = parse_expression(in);
					PLIRInstruction storeInst = result.getLastInst();
					
					// Check if the result is an array, in which case we need to load it from memory
					if (storeInst == null || result.arrayName != null)
					{
						ArrayList<PLIRInstruction> offsetInstructions = null;
						if (result.instructions.get(0).opcode == InstructionType.STORE)
						{
							result.instructions.remove(0);
						}
						offsetInstructions = arrayOffsetCalculation(result);
						for (PLIRInstruction inst : offsetInstructions)
						{
							result.addInstruction(inst);
						}

						PLIRInstruction offset = offsetInstructions.get(offsetInstructions.size() - 1);
						PLIRInstruction load = new PLIRInstruction(scope);
						offsetInstructions.add(0, result.instructions.get(0));
						load.loadInstructions = offsetInstructions;
						load.opcode = InstructionType.LOAD;
						load.ident.put(scope.getCurrentScope(), varName);
						load.op1type = OperandType.ADDRESS;
						load.op1 = offset;
						load.type = OperandType.INST;
						load.forceGenerate(scope);
						load.isArray = true;
						load.dummyName = storeInst.ident.get(scope.getCurrentScope());
						result.addInstruction(load);
						storeInst = load;
						storeInst.isGlobalVariable = this.globalVariables.containsKey(desigBlock.arrayName);
						storeInst.ident.put(scope.getCurrentScope(), varName);
						storeInst.saveName.put(scope.getCurrentScope(), varName);
					}
					else
					{
						storeInst.saveName.put(scope.getCurrentScope(), varName);
						storeInst.isGlobalVariable = desigInst.isGlobalVariable;
						if (desigBlock.arrayName != null)
						{
							storeInst.ident.put(scope.getCurrentScope(), desigBlock.arrayName);
							storeInst.isGlobalVariable = false; // code to store in arrays will be in SSA form, we don't generate it on the fly like we do for regular global variables
						}
						if (storeInst.isGlobalVariable  && storeInst.kind == ResultKind.CONST)
						{
							storeInst.ident.put(scope.getCurrentScope(), varName);
						}
					}
					
					result = PLIRBasicBlock.merge(desigBlock, result);
					
					if (desigBlock.arrayOperands == null)
					{		
						storeInst.tempPosition = PLStaticSingleAssignment.globalSSAIndex;
						if (deferredPhiIdents.contains(varName)) 
						{
							storeInst.kind = ResultKind.VAR;
							storeInst.overrideGenerate = true;
							storeInst.forceGenerate(scope);
						}
						else if (globalFunctionParsing && globalVariables.containsKey(varName))
						{
							storeInst.kind = ResultKind.VAR;
							storeInst.overrideGenerate = true;
							storeInst.forceGenerate(scope);
						}
						
						scope.updateSymbol(varName, storeInst); // (SSA ID) := expr
						duChain.put(storeInst, new HashSet<PLIRInstruction>());
						
						if (storeInst.isGlobalVariable || storeInst.opcode == InstructionType.GLOBAL)
						{
							String currScope = scope.getCurrentScope();
							Function func = scope.functions.get(funcName);
							
							if (func != null && storeInst.ident.get(currScope) != null)
							{
								func.addModifiedGlobal(globalVariables.get(storeInst.ident.get(currScope)), storeInst);
							}
						}
						
						storeInst.ident.put(scope.getCurrentScope(), varName);
						result.addModifiedValue(varName, storeInst);
						
						if (markToSave)
						{
							Function func = scope.functions.get(funcName);
							if (func != null)
							{
								func.addModifiedGlobal(globalVariables.get(varName), storeInst);
								func.hasReturn = func.hasReturn;
							}
						}
					}
					else // array!
					{
						PLIRInstruction inst4 = null;
						
						ArrayList<PLIRInstruction> offsetInstructions = arrayOffsetCalculation(desigBlock);
						for (PLIRInstruction inst : offsetInstructions)
						{
							result.addInstruction(inst);
						}
						
						PLIRInstruction offset = offsetInstructions.get(offsetInstructions.size() - 1);
						offset.ident.put(scope.getCurrentScope(), varName);
						PLIRInstruction store = new PLIRInstruction(scope);
						store.opcode = InstructionType.STORE;
						store.op1type = OperandType.INST;
						store.op1 = offset;
						store.op2type = storeInst.type;
						store.op2 = storeInst;
						if (store.op2type == OperandType.CONST)
						{
							store.i2 = store.op2.tempVal;
						}
						store.type = OperandType.INST;
						store.forceGenerate(scope);
						store.storedValue = store.op2;
						store.isArray = true;
						store.ident.put(scope.getCurrentScope(), varName);
						store.dummyName = varName;
						result.addInstruction(store);
						
						if (parsingFunctionBody)
						{
							scope.functions.get(funcName).addKilledArray(varName, store.op1);
						}
						
						// Tag the variable name with the dependent instructions so we can get unique accesses later on 
						scope.updateSymbol(varName, store); // (SSA ID) := expr
						duChain.put(inst4, new HashSet<PLIRInstruction>());
						result.addModifiedValue(varName, store);
						
						// Add the resulting set of instructions to the BB result
						result.addModifiedValue(varName, store);
						
						if (markToSave)
						{
							scope.functions.get(funcName).addModifiedGlobal(globalVariables.get(store.ident.get(scope.getCurrentScope())), store);
						}
					}
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
	
	private ArrayList<PLIRInstruction> arrayOffsetCalculation(PLIRBasicBlock block)
	{
		ArrayList<PLIRInstruction> instructions = new ArrayList<PLIRInstruction>();
		PLIRInstruction lastOffset = null;
		boolean started = false;
		String arrayName = block.arrayName;
		
		for (int i = block.arrayOperands.size() - 1; i >= 0; i--)
		{
			PLIRInstruction operand = block.arrayOperands.get(i);
			
			// Need to load from memory - insert the load
			PLIRInstruction inst1 = new PLIRInstruction(scope);
			inst1.opcode = InstructionType.MUL;
			inst1.op1type = OperandType.CONST;
			inst1.i1 = 4; 
			inst1.op2type = operand.type;
			inst1.op2 = operand;
			if (inst1.op2type == OperandType.CONST)
			{
				inst1.i2 = inst1.op2.tempVal; // override and use the constant instead
			}
			inst1.forceGenerate(scope);
			instructions.add(inst1);
			
			if (!started)
			{
				started = true;
				
				PLIRInstruction inst2 = new PLIRInstruction(scope);
				inst2.opcode = InstructionType.ADD;
				inst2.op1type = OperandType.FP;
				inst2.op2type = OperandType.BASEADDRESS;
				inst2.op2address = arrayName + "_baseaddr";
				inst2.forceGenerate(scope);
				instructions.add(inst2);
				
				PLIRInstruction inst3 = new PLIRInstruction(scope);
				inst3.opcode = InstructionType.ADDA;
				inst3.op1type = OperandType.INST;
				inst3.op1 = inst1;
				inst3.op2type = OperandType.INST;
				inst3.op2 = inst2;
				inst3.forceGenerate(scope);
				instructions.add(inst3);
				
				// Save the last offset
				lastOffset = inst3;
			}
			else
			{
				PLIRInstruction shiftInst = new PLIRInstruction(scope);
				shiftInst.opcode = InstructionType.MUL;
				shiftInst.op1type = OperandType.INST; 
				shiftInst.op2type = OperandType.CONST;
				shiftInst.op1 = inst1;
				
				// Shift by the number of dimensions in the array
				int dimensions = 1;
				for (int j = i + 1; j < block.arrayOperands.size(); j++)
				{
					dimensions *= arrayDimensionMap.get(arrayName).get(j);
				}
				shiftInst.i2 = dimensions;
				
				shiftInst.forceGenerate(scope);
				instructions.add(shiftInst);
				
				PLIRInstruction inst3 = new PLIRInstruction(scope);
				inst3.opcode = InstructionType.ADDA;
				inst3.op1type = OperandType.INST;
				inst3.op1 = shiftInst;
				inst3.op2type = OperandType.INST;
				inst3.op2 = lastOffset;
				inst3.forceGenerate(scope);
				instructions.add(inst3);
				
				lastOffset = inst3;
			}
		}
		
		return instructions;
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
			
			if (toksym == PLToken.openParenToken)
			{
				advance(in);
				
				ArrayList<PLIRInstruction> operands = new ArrayList<PLIRInstruction>(); 
				
				if (toksym != PLToken.semiToken && toksym != PLToken.elseToken
						&& toksym != PLToken.fiToken && toksym != PLToken.odToken 
						&& toksym != PLToken.closeBraceToken && toksym != PLToken.closeParenToken)
				{
					
					PLIRBasicBlock callExprBlock = parse_expression(in);
					
					result = new PLIRBasicBlock();
					result.joinNode = null; // just in case
					if (callExprBlock.instructions.get(0).opcode == InstructionType.STORE)
					{
						callExprBlock.instructions.remove(0);
					}
					
					// HANDLE THE FIRST PARAMETER
					PLIRInstruction exprInst = callExprBlock.getLastInst();
					if ((exprInst != null && exprInst.isArray && exprInst.opcode != InstructionType.LOAD) || callExprBlock.arrayOperands != null)
					{	
						ArrayList<PLIRInstruction> offsetInstructions = arrayOffsetCalculation(callExprBlock);
						for (PLIRInstruction inst : callExprBlock.instructions)
						{
							result.addInstruction(inst);
						}
						for (PLIRInstruction inst : offsetInstructions)
						{
							result.addInstruction(inst);
						}
						
						PLIRInstruction offset = offsetInstructions.get(offsetInstructions.size() - 1);
						PLIRInstruction load = new PLIRInstruction(scope);
						load.loadInstructions = offsetInstructions;
						load.opcode = InstructionType.LOAD;
						load.op1type = OperandType.ADDRESS;
						load.op1 = offset;
						load.type = OperandType.INST;
						load.forceGenerate(scope);
						load.isArray = true;
						load.dummyName = exprInst.ident.get(scope.getCurrentScope());
						result.addInstruction(load);
						exprInst = load;
					}
					else
					{
						if (!exprInst.generated)
						{
							exprInst.forceGenerate(scope);
						}
						result.addInstruction(exprInst);
					}
					
					// Add the first expression instruction to the list of operands
					operands.add(exprInst);
					
					// Special case for single parameter
					if (toksym == PLToken.commaToken && funcName.equals("OutputNum"))
					{
						SyntaxError("Function OutputNum only takes a single parameter");
					}
					else if (toksym != PLToken.commaToken && funcName.equals("OutputNum"))
					{
						if (exprInst == null)
						{
							exprInst = callExprBlock.getLastInst();
						}
						
						if (!exprInst.generated)
						{
							exprInst.forceGenerate(scope);
						}
						PLIRInstruction inst = new PLIRInstruction(scope, InstructionType.WRITE, exprInst);
						inst.forceGenerate(scope);
						result.addInstruction(inst);
						
						// Update DU chain
						if (duChain.containsKey(exprInst))
						{
							duChain.get(exprInst).add(inst);
							exprInst.uses++;
						}
					}
					else
					{
						// Read in the rest of the instructions
						while (toksym == PLToken.commaToken)
						{
							advance(in);
							
							PLIRBasicBlock nextCallBlock = parse_expression(in);
							if (nextCallBlock.instructions.get(0).opcode == InstructionType.STORE)
							{
								nextCallBlock.instructions.remove(0);
							}
							
							exprInst = nextCallBlock.getLastInst();
							if ((exprInst != null && exprInst.isArray && exprInst.opcode != InstructionType.LOAD) || nextCallBlock.arrayOperands != null)
							{	
								ArrayList<PLIRInstruction> offsetInstructions = arrayOffsetCalculation(nextCallBlock);
								for (PLIRInstruction inst : nextCallBlock.instructions)
								{
									result.addInstruction(inst);
								}
								for (PLIRInstruction inst : offsetInstructions)
								{
									result.addInstruction(inst);
								}
								
								PLIRInstruction offset = offsetInstructions.get(offsetInstructions.size() - 1);
								PLIRInstruction load = new PLIRInstruction(scope);
								load.loadInstructions = offsetInstructions;
								load.opcode = InstructionType.LOAD;
								load.op1type = OperandType.ADDRESS;
								load.op1 = offset;
								load.type = OperandType.INST;
								load.forceGenerate(scope);
								load.isArray = true;
								load.dummyName = exprInst.ident.get(scope.getCurrentScope());
								exprInst = load;
								result.addInstruction(load);
							}
							else
							{
								exprInst.forceGenerate(scope);
								result.addInstruction(exprInst);
							}
							
							operands.add(exprInst);
						}
						
						if (paramMap.get(funcName) != operands.size())
						{
							SyntaxError("Function: " + funcName + " invoked with the wrong number of arguments. Expected " + paramMap.get(funcName) + ", got " + operands.size());
						}
						
						PLIRInstruction callInst = PLIRInstruction.create_call(scope, funcName, funcFlagMap.get(funcName), operands);
						callInst.forceGenerate(scope); 
						result.addInstruction(callInst);
						result.isEntry = true;
						
						// Update DU chain
						for (PLIRInstruction operand : operands)
						{
							if (duChain.containsKey(operand))
							{
								duChain.get(operand).add(callInst);
								operand.uses++;
							}
						}
					}
				}
				else if (toksym == PLToken.closeParenToken && funcName.equals("InputNum"))
				{
					PLIRInstruction inst = new PLIRInstruction(scope, InstructionType.READ);
					inst.type = OperandType.INST;
					inst.forceGenerate(scope);
					result.addInstruction(inst);
					result.isEntry = true;
				}
				else if (toksym == PLToken.closeParenToken && funcName.equals("OutputNewLine"))
				{
					PLIRInstruction inst = new PLIRInstruction(scope, InstructionType.WLN);
					inst.forceGenerate(scope);
					result.addInstruction(inst);
					result.isEntry = true;
				}
				else
				{
					PLIRInstruction callInst = PLIRInstruction.create_call(scope, funcName, funcFlagMap.get(funcName), operands);
					callInst.forceGenerate(scope);
					result.addInstruction(callInst);
					result.isEntry = true;
				}
				
				// Eat the last token and proceed
				advance(in);
			}
			else if (paramMap.get(funcName) == 0) // a function without parameters, just write out the call
			{
				ArrayList<PLIRInstruction> emptyList = new ArrayList<PLIRInstruction>();
				PLIRInstruction callInst = PLIRInstruction.create_call(scope, funcName, funcFlagMap.get(funcName), emptyList);
				result.addInstruction(callInst);
				result.isEntry = true;
			}
			else
			{
				SyntaxError("Function: " + funcName + " invoked with the wrong number of arguments. Expected " + paramMap.get(funcName) + ", got 0");
			}
		}
		
		return result;
	}

	private PLIRBasicBlock parse_statement(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException, ParserException
	{
		PLIRBasicBlock result = null;
		
		if (toksym == PLToken.letToken)
		{
			result = parse_assignment(in);
		}
		else if (toksym == PLToken.callToken)
		{
			result = parse_funcCall(in);
			result.isEntry = false;
			
			String funcName = callStack.get(callStack.size() - 1);
			
			// Handle replacement of global variables 
			if (funcName.equals("OutputNum") == false && funcName.equals("OutputNewLine") == false && funcName.equals("InputNum") == false)
			{
				Function func = scope.functions.get(funcName);
				for (PLIRInstruction glob : func.modifiedGlobals.keySet())
				{
					if (glob != null) // if the key is null, then we modified an array, and that code is always generated on demand
					{
						String name = glob.ident.get("main");
						if (scope.getCurrentValue(name).kind == ResultKind.CONST)
						{
							func.constantsToSave.put(name, scope.getCurrentValue(name).tempVal);
						}
						glob.type = OperandType.INST;
						glob.kind = ResultKind.VAR;
						glob.overrideGenerate = true;
						glob.forceGenerate(scope);
						scope.updateSymbol(name, glob);
					}
				}
				
				for (PLIRInstruction glob : func.usedGlobals.keySet())
				{
					if (glob != null) // if the key is null, then we modified an array, and that code is always generated on demand
					{
						String name = glob.ident.get(scope.getCurrentScope());
						if (scope.getCurrentValue(name).kind == ResultKind.CONST)
						{
							func.constantsToSave.put(name, scope.getCurrentValue(name).tempVal);
						}
						glob.type = OperandType.INST;
						glob.kind = ResultKind.VAR;
						glob.overrideGenerate = true;
						glob.forceGenerate(scope);
						scope.updateSymbol(name, glob);
					}
				}
			}
			
			callStack.remove(callStack.size() - 1);
		}
		else if (toksym == PLToken.ifToken)
		{
			PLIRInstruction follow = new PLIRInstruction(scope);
			follow.fixupLocation = 0;
			advance(in);
			
			// Push on a new scope for the then block
			scope.pushNewScope("if" + (blockDepth++));
			
			// Fixup setup
			PLIRBasicBlock entry = parse_relation(in);
			PLIRInstruction lastEntryInst = entry.getLastInst();
			PLIRInstruction branch = CondNegBraFwd(lastEntryInst);
			branch.fixupLocation = PLStaticSingleAssignment.globalSSAIndex - 1;
			entry.addInstruction(branch);
			lastEntryInst = branch;
			
			// Check for follow through branch
			if (toksym != PLToken.thenToken)
			{
				SyntaxError("Missing then clause");
			}
			advance(in);
			
			// Parse the follow-through and add it as the first child of the entry block
			PLIRBasicBlock thenBlock = parse_statSequence(in);
			entry.leftChild = thenBlock;
			thenBlock.parents.add(entry);
			
			// Create the artificial join node and make it a child of the follow-through branch,
			// and also the exit/join block of the entry block
			PLIRBasicBlock joinNode = new PLIRBasicBlock();
			if (thenBlock.joinNode != null)
			{
				PLIRBasicBlock join = thenBlock.joinNode;
				ArrayList<Integer> seen = new ArrayList<Integer>();
				while (join.joinNode != null && seen.contains(join.id) == false)
				{
					seen.add(join.id);
					join = join.joinNode;
				}
				join.leftChild = joinNode;
				joinNode.parents.add(join);
			}
			else
			{
				thenBlock.leftChild = joinNode;
				joinNode.parents.add(thenBlock);
			}
			entry.joinNode = joinNode;
			
			// Leave the scope of the then-block
			scope.popScope();
			joinNode.scopeName = scope.getCurrentScope();
			
			// Check for an else branch
			int offset = 0;
			PLIRBasicBlock elseBlock = null;
			ArrayList<String> sharedModifiers = new ArrayList<String>();
			PLIRInstruction uncond = null;
			if (toksym == PLToken.elseToken)
			{
				// Enter a new scope for the else block
				String elseScopeName = "else" + (blockDepth++); 
				scope.pushNewScope(elseScopeName);
				
				// Add the unconditional branch at the end of the then block, which will be fixed up later
				uncond = UnCondBraFwd(follow);
				uncond.tempPosition = PLStaticSingleAssignment.globalSSAIndex;
				uncond.forceGenerate(scope);
				if (thenBlock.joinNode != null)
				{
					PLIRBasicBlock join = thenBlock.joinNode;
					while (join.joinNode != null)
					{
						join = join.joinNode;
					}
					join.addInstruction(uncond);
				}
				else
				{
					thenBlock.addInstruction(uncond);
				}
				advance(in);
				
				// Parse the else block and then configure the BB connections accordingly
				elseBlock = parse_statSequence(in);
				if (elseBlock.joinNode != null)
				{
					PLIRBasicBlock join = elseBlock.joinNode;
					ArrayList<Integer> seen = new ArrayList<Integer>();
					while (join.joinNode != null && seen.contains(join.id) == false)
					{
						seen.add(join.id);
						join = join.joinNode;
					}
					join.rightChild = joinNode;
					joinNode.parents.add(join);
				}
				else
				{
					elseBlock.rightChild = joinNode;
					joinNode.parents.add(elseBlock);
				}
				entry.rightChild = elseBlock;
				elseBlock.parents.add(entry);
				
				// Leave the scope of the else block
				scope.popScope();
				
				// Check for necessary phis to be inserted in the join block
				// We need phis for variables that were modified in both branches so we fall through with the right value
				sharedModifiers = new ArrayList<String>();
				for (String i1 : thenBlock.modifiedIdents.keySet())
				{
					for (String i2 : elseBlock.modifiedIdents.keySet())
					{
						if (i1.equals(i2) && sharedModifiers.contains(i1) == false)
						{
							sharedModifiers.add(i1);
						}
					}
				}
				HashMap<String, PLIRInstruction> phisToAdd = new HashMap<String, PLIRInstruction>(); 
				for (int i = 0; i < sharedModifiers.size(); i++)
				{
					String var = sharedModifiers.get(i);
					offset++; 
					PLIRInstruction thenInst = thenBlock.modifiedIdents.get(var);
					
					thenInst.forceGenerate(scope, thenInst.tempPosition);
					PLIRInstruction elseInst = elseBlock.modifiedIdents.get(var);
					
					elseInst.forceGenerate(scope, elseInst.tempPosition);
					PLIRInstruction phi = PLIRInstruction.create_phi(scope, var, thenInst, elseInst, PLStaticSingleAssignment.globalSSAIndex, true);
					phi.isGlobalVariable = thenInst.isGlobalVariable || elseInst.isGlobalVariable;
					phi.saveName.put(scope.getCurrentScope(), var);
					joinNode.insertInstruction(phi, 0);
					phisToAdd.put(var, phi);
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
				for (String var : modifiers)
				{
					// Check to make sure this thing was actually in scope!
					if (scope.getCurrentValue(var) == null)
					{
						SyntaxError("Uninitialized identifier in path: " + var);
					}
					
					offset++;
					PLIRInstruction elseInst = elseBlock.modifiedIdents.get(var);
					elseInst.forceGenerate(scope, elseInst.tempPosition);
					
					PLIRInstruction followInst = scope.getLastValue(var);
					followInst.forceGenerate(scope, followInst.tempPosition);
					
					PLIRInstruction phi = PLIRInstruction.create_phi(scope, var, followInst, elseInst, PLStaticSingleAssignment.globalSSAIndex, true);
					phi.isGlobalVariable = followInst.isGlobalVariable || elseInst.isGlobalVariable;
					phi.saveName.put(scope.getCurrentScope(), var);
					joinNode.insertInstruction(phi, 0);
					
					// The current value in scope needs to be updated now with the result of the phi
					phisToAdd.put(var, phi);
				}
				
				modifiers = new ArrayList<String>();
				for (String modded : thenBlock.modifiedIdents.keySet())
				{
					if (sharedModifiers.contains(modded) == false) // don't double-add
					{
						modifiers.add(modded);
					}
				}
				for (String var : modifiers)
				{
					// Check to make sure this thing was actually in scope!
					if (scope.getCurrentValue(var) == null)
					{
						debug("Uninitialized identifier in path: " + var);
					}
					
					offset++;
					PLIRInstruction leftInst = thenBlock.modifiedIdents.get(var);
					leftInst.forceGenerate(scope, leftInst.tempPosition);
					
					if (leftInst.kind == ResultKind.CONST)
					{
						String name = "";
						for (String instScope : leftInst.ident.keySet())
						{
							name = leftInst.ident.get(instScope);
							break;
						}
						for (int i = 0; i < scope.currentScope.size(); i++)
						{
							leftInst.ident.put(scope.currentScope.get(i), name);
						}
					}
					
					PLIRInstruction followInst = scope.getLastValue(var);
					followInst.forceGenerate(scope, followInst.tempPosition);
					
					if (followInst.kind == ResultKind.CONST)
					{
						String name = "";
						for (String instScope : followInst.ident.keySet())
						{
							name = followInst.ident.get(instScope);
							break;
						}
						for (int i = 0; i < scope.currentScope.size(); i++)
						{
							followInst.ident.put(scope.currentScope.get(i), name);
						}
					}
					
					PLIRInstruction phi = PLIRInstruction.create_phi(scope, var, leftInst, followInst, PLStaticSingleAssignment.globalSSAIndex, true);
					phi.isGlobalVariable = leftInst.isGlobalVariable || followInst.isGlobalVariable;
					phi.saveName.put(scope.getCurrentScope(), var);
					joinNode.insertInstruction(phi, 0);
					phisToAdd.put(var, phi);
				}
				
				// Update the current scope with these new PHIs
				ArrayList<String> phiNames = new ArrayList<String>();
				for (String var : phisToAdd.keySet())
				{
					phiNames.add(var);
				}
				for (String var : phisToAdd.keySet())
				{
					scope.updateSymbol(var, phisToAdd.get(var));
					entry.modifiedIdents.put(var, phisToAdd.get(var));
					joinNode.modifiedIdents.put(var, phisToAdd.get(var));
					
					if (identTypeMap.get(var) == IdentType.ARRAY)
					{
						
					}
					
					for (String otherName : phiNames)
					{
						if (var.equals(otherName) == false)
						{
							phisToAdd.get(var).guards.add(otherName);
						}
					}
				}
			}
			else // there was no else block, so the right child becomes the join node
			{
				entry.rightChild = joinNode;
				joinNode.parents.add(entry);
				
				// Check for necessary phis to be inserted in the join block
				// We need phis for variables that were modified in both branches so we fall through with the right value
				ArrayList<String> modifiers = new ArrayList<String>();
				for (String modded : thenBlock.modifiedIdents.keySet())
				{
					if (sharedModifiers.contains(modded) == false) // don't double-add
					{
						modifiers.add(modded);
					}
				}
				for (String var : modifiers)
				{
					offset++;
					PLIRInstruction leftInst = thenBlock.modifiedIdents.get(var);
					leftInst.forceGenerate(scope, leftInst.tempPosition);
					
					PLIRInstruction followInst = scope.getLastValue(var);
					followInst.forceGenerate(scope, followInst.tempPosition);
					
					PLIRInstruction phi = PLIRInstruction.create_phi(scope, var, leftInst, followInst, PLStaticSingleAssignment.globalSSAIndex, true);
					phi.isGlobalVariable = leftInst.isGlobalVariable || followInst.isGlobalVariable;
					phi.saveName.put(scope.getCurrentScope(), var);
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
			
			// After the phis have been inserted at the appropriate positions, fixup the entry instructions
			if (elseBlock != null)
			{
				int numInstructions = 0;
				for (int i = 0; i < elseBlock.instructions.size(); i++)
				{
					if (elseBlock.instructions.get(i).id > 0 && elseBlock.instructions.get(i).opcode != InstructionType.GLOBAL)
					{
						numInstructions++;
					}
				}
				numInstructions = numInstructions > 0 ? 0 : 1;
				lastEntryInst.i2 = uncond.id - lastEntryInst.id + 1;
				if (joinNode.instructions.isEmpty())
				{
					uncond.i2 = PLStaticSingleAssignment.globalSSAIndex - uncond.id - offset + 1;
				}
				else
				{
					uncond.i2 = joinNode.instructions.get(0).id - uncond.id - offset + 1;
				}
			}
			else
			{
				Fixup(lastEntryInst.fixupLocation, -offset);
			}
			
			// Configure the dominator tree connections
			entry.dominatorSet.add(thenBlock);
			if (elseBlock != null)
			{
				entry.dominatorSet.add(elseBlock);
			}
			entry.dominatorSet.add(joinNode);
			
			// Check for fi token and then eat it
			if (toksym != PLToken.fiToken)
			{
				SyntaxError("Missing 'fi' close to if statement");
			}
			advance(in);
			
			// Save the resulting basic block
			entry.isEntry = true;
			return entry;
		}
		else if (toksym == PLToken.whileToken)
		{	
			// Eat the while token and then save the current PC
			advance(in);
			int entryStartLocation = PLStaticSingleAssignment.globalSSAIndex;
			
			String whileScopeName = "while" + (blockDepth++);
			scope.pushNewScope(whileScopeName);
			
			// Parse the condition (relation) for the loop
			PLIRBasicBlock entry = parse_relation(in);
			entry.isLoopHeader = true;
			PLIRInstruction entryCmpInst = entry.instructions.get(entry.instructions.size() - 1);
			PLIRInstruction bgeInst = CondNegBraFwd(entryCmpInst);
			
			int innerOffset = 0;
			for (PLIRInstruction headerInst : entry.instructions)
			{
				if (!(headerInst.opcode == InstructionType.PHI || headerInst.id == 0 || headerInst.opcode == InstructionType.CMP))
				{
					innerOffset++;
				}
			}
			
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
			deferredPhiIdents.clear();
			PLIRBasicBlock joinNode = new PLIRBasicBlock();
			entry.joinNode = joinNode;
			
			PLIRInstruction cmpInst = entry.instructions.get(entry.instructions.size() - 1);
			bgeInst.op1 = cmpInst;
			entry.addInstruction(bgeInst);
			
			scope.popScope();
			joinNode.scopeName = scope.getCurrentScope();
			
			// Hook the body of the loop back to the entry
			if (body.joinNode != null)
			{
				PLIRBasicBlock join = body.joinNode;
				while (join.joinNode != null)
				{
					join = join.joinNode;
				}
				join.rightChild = entry;
			}
			else
			{
				body.rightChild = entry;
			}
			entry.leftChild = body;
			body.parents.add(entry);
			
			// Patch up the follow-through branch; 
			entry.rightChild = joinNode;
			joinNode.parents.add(entry); 
			
			////////////////////////////////////////////
			// We've passed through the body and know what variables are updated, now we need to insert phis
			// Phis are inserted when variables in the relation are modified in the loop body
			// Left phi value is entry instruction, right phi value is instruction computed in loop body
			ArrayList<String> modded = new ArrayList<String>();
			for (String i1 : body.modifiedIdents.keySet())
			{
				modded.add(i1);
			}
			int offset = 0;
			ArrayList<PLIRInstruction> phisGenerated = new ArrayList<PLIRInstruction>();
			for (int mi = modded.size() - 1; mi >= 0; mi--)
			{
				String var = modded.get(mi);
				PLIRInstruction bodyInst = body.modifiedIdents.get(var);
				
				PLIRInstruction preInst = scope.getCurrentValue(var);
				
				// Inject the phi at the appropriate spot in the join node...
				PLIRInstruction phi = PLIRInstruction.create_phi(scope, var, preInst, bodyInst, entryStartLocation + offset, true);
				phi.op1name = var;
				phi.op2name = var;
				phi.isGlobalVariable = preInst.isGlobalVariable || bodyInst.isGlobalVariable;
				phi.whilePhi = true;
				phisGenerated.add(phi);
				phi.ident.put(scope.getCurrentScope(), var);
				phi.saveName.put(scope.getCurrentScope(), var);
				offset++;
				
				// Propogate through the start of the while header
				// -> check for replacement in the while loop header (i.e. all instructions leading up to the CMP)
				PLIRInstruction replacement = phi;
				for (int i = 1; i < entry.instructions.size(); i++)
				{
					PLIRInstruction entryInst = entry.instructions.get(i);
					if (entryInst.id != 0 && entryInst.id < entryStartLocation || (entryInst.isConstant))
					{
						continue;
					}
					
					boolean replacedLeft = false;
					boolean replacedRight = false;
					boolean couldHaveReplaced = false;
					if (!(entryInst.opcode == InstructionType.PHI && entryInst.op1 != null && entryInst.op1.isConstant) 
							&& !(entryInst.opcode == InstructionType.PHI))
					{
						if (entryInst.op1 != null && entryInst.op1.ident.get(scope.getCurrentScope()) != null && entryInst.op1.ident.get(scope.getCurrentScope()).equals(var))
						{
							replacedLeft  = entryInst.replaceLeftOperand(replacement);
							couldHaveReplaced = true;
						}
						if (entryInst.op1 != null && entryInst.op1.equals(phi))
						{
							replacedLeft = entryInst.replaceLeftOperand(replacement);
							couldHaveReplaced = true;
						}
						if (entryInst.op1name != null && entryInst.op1name.equals(var))
						{
							replacedLeft = entryInst.replaceLeftOperand(replacement);
							couldHaveReplaced = true;
						}
					}
					
					if (entryInst.opcode != InstructionType.STORE && !(entryInst.opcode == InstructionType.PHI))
					{
						if (entryInst.op2 != null && entryInst.op2.ident.get(scope.getCurrentScope()) != null && entryInst.op2.ident.get(scope.getCurrentScope()).equals(var) && !replacedLeft)
						{
							replacedRight = entryInst.replaceRightOperand(replacement);
						}
						if (entryInst.op2 != null && entryInst.op2.equals(phi) && !replacedLeft)
						{
							replacedRight = entryInst.replaceRightOperand(replacement);
						}
						if (entryInst.op2name != null && entryInst.op2name.equals(var) && !replacedLeft)
						{
							replacedRight = entryInst.replaceRightOperand(replacement);
						}
					}
					
					if (replacedLeft && entryInst.opcode != InstructionType.STORE && entryInst.opcode != InstructionType.PHI) // && !couldHaveReplaced)
					{
						ArrayList<PLIRInstruction> visitedInsts = new ArrayList<PLIRInstruction>();
						entryInst.evaluate(entryInst.id - 1, 0, replacement, scope, visitedInsts, 1);
					}
					
					if (replacedRight && entryInst.opcode != InstructionType.STORE && entryInst.opcode != InstructionType.PHI) // && !couldHaveReplaced)
					{
						ArrayList<PLIRInstruction> visitedInsts = new ArrayList<PLIRInstruction>();
						entryInst.evaluate(entryInst.id - 1, 0, replacement, scope, visitedInsts, 2);
					}
					
					if (replacedLeft && couldHaveReplaced && entryInst.opcode == InstructionType.PHI)
					{ 
						replacement = entryInst;
					}
					
					// If the phi value was used to replace some operand, and this same expression was used to save a result, replace
					// with the newly generated result
					else if (replacedLeft && 
							((entryInst.ident.get(scope.getCurrentScope()) != null && entryInst.ident.get(scope.getCurrentScope()).equals(phi.ident.get(scope.getCurrentScope())))
							|| (entryInst.saveName.get(scope.getCurrentScope()) != null && entryInst.saveName.get(scope.getCurrentScope()).equals(phi.ident.get(scope.getCurrentScope()))))
							&& entryInst.kind != ResultKind.CONST)
					{
						replacement = entryInst;
					}
					
					// If there is an assignment that matches this PHI variable, use its value...
					if (entryInst.id > 0 && entryInst.generated && (entryInst.ident.get(scope.getCurrentScope()) != null && entryInst.ident.get(scope.getCurrentScope()).equals(var)))
					{
						replacement = entryInst;
					}
				}
				
				// Inject the phi into the entry
				entry.insertInstruction(phi, 0);
				
				// Now loop through the entry and fix instructions as needed
				// Those fixed are replaced with the result of this phi if they have used or modified the sym...
				if (cmpInst.op1 != null && cmpInst.op1.checkForReplace(replacement, var, scope.getCurrentScope()))
				{
					cmpInst.evaluate(-1, offset, replacement, scope, new ArrayList<PLIRInstruction>(), 1);
				}
				if (cmpInst.op2 != null && cmpInst.op2.checkForReplace(replacement, var, scope.getCurrentScope()))
				{
					cmpInst.evaluate(-1, offset, replacement, scope, new ArrayList<PLIRInstruction>(), 2);
				}
				
				// Propagate the PHI through the body of the loop
				ArrayList<PLIRBasicBlock> visited = new ArrayList<PLIRBasicBlock>();
				visited.add(entry);
				HashMap<String, PLIRInstruction> scopeMap = new HashMap<String, PLIRInstruction>();
				scopeMap.put(var, replacement);
				body.propagatePhi(offset + innerOffset, visited, scope, 1, scopeMap, scope.getCurrentScope());
				
				// The current value in scope needs to be updated now with the result of the phi
				scope.updateSymbol(var, replacement);
				
				// Add to this block's list of modified identifiers
				// Rationale: since we added a phi, the value potentially changes (is modified), 
				// 	so the latest value in the current scope needs to be modified
				entry.modifiedIdents.put(var, replacement);
			}
			
			// Go through the phis and make sure we adjusted the values accordingly
			for (PLIRInstruction phi : phisGenerated)
			{
				for (String var : modded)
				{
					if (var.equals(phi.ident.get(scope.getCurrentScope())))
					{
						PLIRInstruction bodyInst = body.modifiedIdents.get(var);
						bodyInst.overrideGenerate = true;
						bodyInst.forceGenerate(scope);
						phi.op2type = OperandType.INST;
						phi.op2 = bodyInst;
					}
				}
			}
			
			// Handle the unconditional while branch
			PLIRInstruction beqInst = null;
			if (phisGenerated.size() > 0)
			{
				int target = phisGenerated.get(0).id;
				for (int i = 1; i < phisGenerated.size(); i++)
				{
					PLIRInstruction phi = phisGenerated.get(i);
					if (phi.id < target)
					{
						target = phi.id;
					}
				}
				beqInst = PLIRInstruction.create_BEQ(scope, target - (PLStaticSingleAssignment.globalSSAIndex + 1));
			}
			else
			{
				beqInst = PLIRInstruction.create_BEQ(scope, entryStartLocation - PLStaticSingleAssignment.globalSSAIndex);
			}
			
			// Save the cmpInst for code generation later
			beqInst.jumpInst = cmpInst;
			
			// Do the fixup for the conditional branch (to jump past the end of the while loop header)
			if (body.joinNode != null)
			{
				PLIRBasicBlock join = body.joinNode;
				while (join.joinNode != null)
				{
					join = join.joinNode;
				}
				join.addInstruction(beqInst);
				bgeInst.jumpInst = join.getLastInst();
				
				if (join.instructions.isEmpty())
				{
					bgeInst.i2 = beqInst.id - bgeInst.id + 1;
				}
				else
				{
					bgeInst.i2 = beqInst.id - bgeInst.id + 1;
				}
			}
			else
			{
				body.addInstruction(beqInst);
				bgeInst.jumpInst = body.getLastInst();
				
				if (joinNode.instructions.isEmpty())
				{
					bgeInst.i2 = beqInst.id - bgeInst.id + 1;
				}
				else
				{
					bgeInst.i2 = beqInst.id - bgeInst.id + 1;
				}
			}
			
			// Configure the dominator tree connections
			entry.dominatorSet.add(body);
			entry.dominatorSet.add(joinNode);
			
			// Check for the closing od token and then eat it
			if (toksym != PLToken.odToken)
			{
				SyntaxError("Missing 'od' in while statement");
			}
			advance(in);
			
			// the result of parsing this basic block is the entry node, which doubles as the join and exit node
			entry.isEntry = true;
			entry.isWhileEntry = true;
			result = entry;
		}
		else if (toksym == PLToken.returnToken)
		{
			advance(in);
			if (toksym != PLToken.semiToken && toksym != PLToken.elseToken
					&& toksym != PLToken.fiToken && toksym != PLToken.odToken && toksym != PLToken.closeBraceToken)
			{
				result = parse_expression(in);
				
				if (result == null)
				{
					result = new PLIRBasicBlock();
				}
				
				// Since the return statement was followed by an expression, force the expression to be generated...
				PLIRInstruction retInst = result.getLastInst();
				retInst.overrideGenerate = true;
				retInst.forceGenerate(scope);
				
				PLIRInstruction finalRet = new PLIRInstruction(scope);
				finalRet.opcode = InstructionType.RETURN;
				finalRet.op1type = OperandType.INST;
				finalRet.op1 = retInst;
				finalRet.overrideGenerate = true;
				finalRet.forceGenerate(scope);
				result.addInstruction(finalRet);
				result.hasReturn = true;
				result.isEntry = true;
			}
			else
			{
				result = new PLIRBasicBlock();
			}
		}
		else 
		{
			SyntaxError("Invalid start of a statement");
		}
		
		return result;
	}

	private PLIRBasicBlock parse_statSequence(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException, ParserException
	{
		boolean isReturn = toksym == PLToken.returnToken;
		PLIRBasicBlock result = parse_statement(in);
		
		if (root == null && !globalFunctionParsing)
		{
			root = result;
		}
		
		if (isReturn)
		{
			result.returnInst = result.getLastInst();
			result.hasReturn = true;
		}
		
		while (toksym == PLToken.semiToken)
		{
			advance(in);
			isReturn = toksym == PLToken.returnToken;
			PLIRBasicBlock nextBlock = parse_statement(in);
			
			// Propagate loop header into body blocks as an "enclosing loop header"
			boolean propogateHeader = nextBlock.isWhileEntry;
			
			if (isReturn)
			{
				result.returnInst = nextBlock.getLastInst();
				result.returnInst.isReturn = true;
				result.hasReturn = true;
			}
			
			// Merge the block results 
			result = PLIRBasicBlock.merge(result, nextBlock);
			
			// Propogate enclosing loop headers, if necessary
			if (propogateHeader)
			{
				HashSet<PLIRBasicBlock> enclosedSeen = new HashSet<PLIRBasicBlock>();
				enclosedSeen.add(result);
				result.leftChild.propogateLoopHeader(enclosedSeen, result);
			}
		}
		
		// Fix the spot of the basic block (since we're leaving a statement sequence) and call it a day
		result.scopeName = scope.getCurrentScope();
		
		return result;
	}

	private PLIRBasicBlock parse_typeDecl(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{	
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
			ArrayList<Integer> dimensions = new ArrayList<Integer>();
			
			if (toksym == PLToken.openBracketToken)
			{
				advance(in);
				dimensions.add(Integer.parseInt(sym));
				advance(in);
				if (toksym == PLToken.closeBracketToken)
				{
					advance(in);
					while (toksym == PLToken.openBracketToken)
					{
						advance(in);
						dimensions.add(Integer.parseInt(sym));
						advance(in);
						if (toksym != PLToken.closeBracketToken)
						{
							SyntaxError("']' missing from typeDecl non-terminal");
						}
						advance(in);
					}
					
					// Set up array information
					identTypeMap.put(sym, type);
					scope.addVarToScope(sym);
					arrayDimensionMap.put(sym, dimensions);
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
		Function func = scope.functions.get(funcName);
		
		IdentType type = IdentType.VAR;
		if (toksym == PLToken.varToken)
		{
			type = IdentType.VAR;
		}
		else if (toksym == PLToken.arrToken)
		{
			type = IdentType.ARRAY;
		}
		
		parseVariableDeclaration = true;
		PLIRBasicBlock result = parse_typeDecl(in);
		result = parse_ident(in);
		String symName = result.getLastInst().ident.get(scope.getCurrentScope());
		
		ArrayList<Integer> size = arrayDimensionMap.get(symName);
		if (!globalVariableParsing && this.arrayDimensionMap.containsKey(symName))
		{
			func.arraySizes.put(symName, size);
		}
		
		// Parse any other related declarations
		while (toksym == PLToken.commaToken)
		{
			advance(in);
			scope.addVarToScope(sym);
			
			if (type == IdentType.ARRAY)
			{
				arrayDimensionMap.put(sym, size);
			}
			if (!globalVariableParsing && this.arrayDimensionMap.containsKey(sym))
			{
				func.arraySizes.put(sym, size);
			}
			identTypeMap.put(sym, type);
			result = parse_ident(in);
		}
		if (toksym != PLToken.semiToken)
		{
			SyntaxError("';' missing from varDecl");
		}
		advance(in);
		
		parseVariableDeclaration = false;
		
		return result;
	}

	private PLIRBasicBlock parse_funcDecl(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException, ParserException
	{
		PLIRBasicBlock result = null;
		if (toksym == PLToken.funcToken || toksym == PLToken.procToken)
		{
			int callType = toksym;
			
			advance(in);
			scope.addVarToScope(sym);
			scope.pushNewScope(sym);
			
			funcName = sym; // save for recovery later on
			
			switch (callType)
			{
			case PLToken.funcToken:
				funcFlagMap.put(funcName, true);
				break;
			case PLToken.procToken:
				funcFlagMap.put(funcName, false);
				break;
			}
			
			result = parse_ident(in);
			
			isFunction = toksym == PLToken.funcToken ? true : false;
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
			if (result != null)
			{
				PLIRBasicBlock body = parse_funcBody(in);
				body.isEntry = true;
				boolean propogateHeader = body.isWhileEntry;
				result = PLIRBasicBlock.merge(result, body);
				if (propogateHeader)
				{
					HashSet<PLIRBasicBlock> enclosedSeen = new HashSet<PLIRBasicBlock>();
					enclosedSeen.add(result);
					result.leftChild.propogateLoopHeader(enclosedSeen, result);
				}
			}
			else
			{
				result = parse_funcBody(in);
			}
			
			if (!result.hasReturn)
			{
				PLIRInstruction finalRet = new PLIRInstruction(scope);
				finalRet.opcode = InstructionType.RETURN;
				finalRet.overrideGenerate = true;
				finalRet.forceGenerate(scope);
				
				PLIRBasicBlock joinNode = result;
				while (joinNode.joinNode != null)
				{
					joinNode = joinNode.joinNode;
				}
				
				joinNode.addInstruction(finalRet);
				joinNode.hasReturn = true;
				result.hasReturn = true;
			}
			
			// Eat the semicolon terminating the body
			if (toksym != PLToken.semiToken)
			{
				SyntaxError("';' missing from funcDecl non-terminal");
			}
			advance(in);
		}
		else
		{
			SyntaxError("Invalid start to funcDecl non-terminal");
		}
		
		return result;
	}

	private PLIRBasicBlock parse_formalParam(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock result = new PLIRBasicBlock();
		params = new ArrayList<PLIRInstruction>();
		
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
				PLIRInstruction dummy = new PLIRInstruction(scope);
				dummy.dummyName = sym;
				dummy.ident.put(scope.getCurrentScope(), sym);
				dummy.type = OperandType.FUNC_PARAM;
				dummy.opcode = InstructionType.LOADPARAM;
				dummy.overrideGenerate = true;
				dummy.paramNumber = params.size();
				dummy.forceGenerate(scope);
				result.addInstruction(dummy);
				
				// Add to the list of operands
				params.add(dummy);
				
				advance(in);
				
				while (toksym == PLToken.commaToken)
				{
					advance(in);
					
					dummy = new PLIRInstruction(scope);
					dummy.dummyName = sym;
					dummy.ident.put(scope.getCurrentScope(), sym);
					dummy.type = OperandType.FUNC_PARAM;
					dummy.opcode = InstructionType.LOADPARAM;
					dummy.overrideGenerate = true;
					dummy.paramNumber = params.size();
					dummy.forceGenerate(scope);
					result.addInstruction(dummy);
					
					// Add to the list of operands
					params.add(dummy);
					
					advance(in);
				}
				
				if (toksym != PLToken.closeParenToken)
				{
					SyntaxError("')' missing from formalParam");
				}
				
				advance(in);
			}
		}
		
		paramMap.put(funcName, params.size());
		if (isFunction)
		{
			scope.addFunction(funcName, params);
		}
		else
		{
			scope.addProcedure(funcName, params);
		}
		
		// All parameters passed in on the stack are local variables (in the local scope),
		// so add them as such
		Function func = scope.functions.get(funcName);
		for (PLIRInstruction inst : result.instructions)
		{
			scope.addVarToScope(funcName, inst.dummyName);
			scope.updateSymbol(inst.dummyName, inst); 
		}
		
		return result;
	}

	private PLIRBasicBlock parse_funcBody(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException, ParserException
	{
		PLIRBasicBlock result = null;
		String name = funcName; // save
		
		parsingFunctionBody = true;
		
		variables = new ArrayList<PLIRInstruction>();
		while (toksym == PLToken.varToken || toksym == PLToken.arrToken)
		{
			result = parse_varDecl(in); 
		}
		
		if (toksym != PLToken.openBraceToken)
		{
			SyntaxError("'{' missing from funcBody non-terminal.");
		}
		
		// eat the open brace '{'
		advance(in);
		
		// Must be a statSequence here (if one seems to exist)
		if (toksym != PLToken.closeBraceToken)
		{
			result = parse_statSequence(in);
			if (toksym != PLToken.closeBraceToken)
			{
				SyntaxError("'}' missing from statSequence non-terminal in funcBody");
			}
			advance(in);
		}
		
		parsingFunctionBody = false;
		
		return result;
	}
	
	// Per spec
	private PLIRInstruction CondNegBraFwd(PLIRInstruction x)
	{
		x.fixupLocation = PLStaticSingleAssignment.globalSSAIndex;
		PLIRInstruction inst = PLIRInstruction.create_branch(scope, x, x.condcode);
		inst.branchDirection = 2; // conditional branches will skip the follow-through branch, going right instead, so remember this
		return inst;
	}
	
	// Per spec
	private PLIRInstruction UnCondBraFwd(PLIRInstruction x)
	{
		PLIRInstruction inst = PLIRInstruction.create_BEQ(scope, x.fixupLocation);
		x.fixupLocation = PLStaticSingleAssignment.globalSSAIndex - 1;
		return inst;
	}

	// set the address to which we jump...
	// buffer[loc] = (pc - loc) + offset;
	// MODIFIED FROM SPEC => offset needed to be introduced to account for phi injection
	private void Fixup(int loc, int offset)
	{
		if (PLStaticSingleAssignment.instructions.get(loc).opcode == InstructionType.CMP)
		{
			System.exit(-1);
		}	
		PLStaticSingleAssignment.instructions.get(loc).i2 = (PLStaticSingleAssignment.globalSSAIndex - loc + offset);
	}
}
