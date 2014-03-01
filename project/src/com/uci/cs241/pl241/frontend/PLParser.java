package com.uci.cs241.pl241.frontend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.uci.cs241.pl241.ir.PLIRBasicBlock;
import com.uci.cs241.pl241.ir.PLIRInstruction;
import com.uci.cs241.pl241.ir.PLIRInstruction.OperandType;
import com.uci.cs241.pl241.ir.PLIRInstruction.InstructionType;
import com.uci.cs241.pl241.ir.PLIRInstruction.ResultKind;
import com.uci.cs241.pl241.ir.PLStaticSingleAssignment;

public class PLParser
{
	// Current symbol/token values used for parsing
	private String sym;
	private int toksym;
	private PLIRBasicBlock root;
	
	// BB depth (to uniquify scopes)
	private int blockDepth = 0;
	
	// Useful things to help the parser
	private boolean globalVariableParsing;
	private boolean globalFunctionParsing; 
	
	// Other necessary things
	public PLSymbolTable scope;
	
	public enum IdentType {VAR, ARRAY, FUNC};
	private HashMap<String, IdentType> identTypeMap = new HashMap<String, IdentType>();
	private HashMap<String, ArrayList<Integer>> arrayDimensionMap = new HashMap<String, ArrayList<Integer>>();
	
	public HashMap<String, PLIRInstruction> globalVariables = new HashMap<String, PLIRInstruction>();
	
	// TODO!!!
	private ArrayList<String> deferredPhiIdents = new ArrayList<String>();
	
	// TODO!!!
	private String funcName = "";
	private ArrayList<String> callStack = new ArrayList<String>();
	private HashMap<String, PLIRBasicBlock> funcBlockMap = new HashMap<String, PLIRBasicBlock>();
	private HashMap<String, PLIRBasicBlock> procBlockMap = new HashMap<String, PLIRBasicBlock>();
	private HashMap<String, Integer> paramMap = new HashMap<String, Integer>();
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
	
	// this is what's called - starting with the computation non-terminal
	public ArrayList<PLIRBasicBlock> parse(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
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
			
			System.out.println("Global variables");
			for (String v : globalVariables.keySet())
			{
				System.out.println("\t" + v);
			}
			
			// Parse global functions and procedures
			globalFunctionParsing = true;
			while (toksym == PLToken.funcToken || toksym == PLToken.procToken)
			{
				int funcType = toksym;
				PLIRBasicBlock funcEntry = this.parse_funcDecl(in); // a separate BB for each function/procedure
				funcEntry.label = funcName;
				blocks.add(funcEntry);
				
				switch (funcType)
				{
				case PLToken.funcToken:
					funcBlockMap.put(funcName, funcEntry); // save this so that others may use it
//					funcFlagMap.put(funcName, true);
					break;
				case PLToken.procToken:
					procBlockMap.put(funcName, funcEntry); // save this so that others may use it
//					funcFlagMap.put(funcName, false);
					break;
				}
				
				PLIRBasicBlock endBlock = funcEntry;
				while (endBlock.joinNode != null)
				{
					endBlock = endBlock.joinNode;
				}
				
				// Insert global variable save functions at the end of the function, if necessary
				Function func = scope.functions.get(funcName);
				if (funcName.equals("OutputNum") == false && funcName.equals("OutputNewLine") == false && funcName.equals("InputNum") == false)
				{
					for (PLIRInstruction inst : func.modifiedGlobals.keySet())
					{
						PLIRInstruction saveInst = new PLIRInstruction(scope);
						saveInst.opcode = InstructionType.SAVEGLOBAL;
						saveInst.op1 = func.modifiedGlobals.get(inst);
						saveInst.op1type = OperandType.ADDRESS;
						saveInst.op2 = inst;
						saveInst.op2type = OperandType.ADDRESS;
						
						saveInst.forceGenerate(scope);
						
						endBlock.instructions.add(saveInst);
					}
				}
				
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
				
				// parse the close of the computation
				if (toksym == PLToken.closeBraceToken)
				{
					advance(in);
					if (toksym == PLToken.periodToken)
					{
						PLIRInstruction inst = new PLIRInstruction(scope, InstructionType.END);
						result.addInstruction(inst);
						blocks.add(root);
						PLStaticSingleAssignment.endInstructions();
						
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
			
			if (this.parseVariableDeclaration)
			{
				// Initialize the variable to 0
				inst = new PLIRInstruction(scope);
				inst.opcode = InstructionType.ADD;
				inst.i1 = 0;
				inst.op1type = OperandType.CONST;
				inst.i2 = 0;
				inst.op2type = OperandType.CONST;
				inst.kind = ResultKind.VAR;
				inst.type = OperandType.CONST; // TODO: was LOCALPARAM
				inst.overrideGenerate = true;
				inst.forceGenerate(scope);
				inst.origIdent = symName;
				
				func.addLocalVariable(inst);
				
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
				
//				PLIRInstruction loadInst = new PLIRInstruction(scope);
//				loadInst.kind = ResultKind.VAR;
//				loadInst.type = OperandType.ADDRESS;
//				loadInst.opcode = InstructionType.LOAD;
//				loadInst.op1 = inst;
//				loadInst.op1type = OperandType.ADDRESS;	
//				loadInst.globalMark = true;
				
//				PLIRInstruction locInst = new PLIRInstruction(scope);
//				locInst.kind = ResultKind.VAR;
//				locInst.type = OperandType.ADDRESS;
//				locInst.opcode = inst.opcode;
//				locInst.op1 = inst.op1;
//				locInst.op1type = inst.op1type;
//				locInst.op2 = inst.op2;
//				locInst.op2type = inst.op2type;
//				locInst.globalMark = true;
				
				// Eat the symbol, create the block with the single instruction, add the ident to the list
				// of used identifiers, and return
				advance(in);
				
				block = new PLIRBasicBlock();
				block.addInstruction(inst);
//				block.addInstruction(loadInst);
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
//			else if (func.isLocalVariable(symName))
//			{
//				inst = scope.getCurrentValue(symName);
////				inst = func.getLocalVariableByName(symName);
//			}
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
//			inst.i2 = 0;
//			inst.op2type = OperandType.CONST;
			inst.kind = ResultKind.VAR;
			inst.type = OperandType.ADDRESS;
			inst.overrideGenerate = true;
			inst.forceGenerate(scope);
			inst.origIdent = symName;
			
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
					instArray.origIdent = sym;
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
					inst.origIdent = sym;
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
		else
		{
			debug("Previously unencountered variable: " + sym);
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
			debug(sym);
			if (result.arrayOperands == null)
			{
				result.arrayOperands = new ArrayList<PLIRInstruction>();
			}
			advance(in);
			debug(sym);
			
			result.arrayOperands.add(parse_expression(in).getLastInst());
			debug(sym);
			
			if (toksym != PLToken.closeBracketToken)
			{
				SyntaxError("']' missing from designator non-terminal.");
			}
			advance(in);
			debug(sym);
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
			debug("returning from: " + funcName);
			if (callStack.get(callStack.size() - 1).equals("InputNum"))
			{
				// pass, this is a special case
			}
			else if (factor.hasReturn == false || funcFlagMap.get(funcName) == false)
			{
				SyntaxError("Function that was invoked had no return value!");
			}
			
			// Handle replacement of global variables 
			if (funcName.equals("OutputNum") == false && funcName.equals("OutputNewLine") == false && funcName.equals("InputNum") == false)
			{
				for (PLIRInstruction glob : scope.functions.get(funcName).modifiedGlobals.keySet())
				{
					glob.overrideGenerate = true;
					glob.forceGenerate(scope);
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
			if (leftInst.isArray)
			{
				PLIRInstruction lastAddress = null;
//				for (PLIRInstruction operand : factor.arrayOperands)
				for (int i = 0; i < factor.arrayOperands.size(); i++)
				{
					PLIRInstruction operand = factor.arrayOperands.get(i);
					
					// Need to load from memory - insert the load
					PLIRInstruction inst1 = new PLIRInstruction(scope);
					inst1.opcode = InstructionType.MUL;
					inst1.op1type = OperandType.CONST;
					inst1.i1 = 4; // CONSTANT! DOESN'T CHANGE!
					inst1.op2type = operand.type;
					inst1.op2 = operand;
					if (inst1.op2type == OperandType.CONST)
					{
						inst1.i2 = inst1.op2.tempVal;
					}
					inst1.forceGenerate(scope);
					termNode.addInstruction(inst1);
					
					PLIRInstruction inst2 = new PLIRInstruction(scope);
					inst2.opcode = InstructionType.ADD;
					inst2.op1type = OperandType.FP;
					if (i == 0)
					{
						inst2.op2type = OperandType.BASEADDRESS;
						inst2.op2address = leftInst.origIdent + "_baseaddr";
					}
					else
					{
						inst2.op2type = OperandType.ADDRESS;
						inst2.op2 = lastAddress;
					}
					inst2.forceGenerate(scope);
					termNode.addInstruction(inst2);
					
					PLIRInstruction inst3 = new PLIRInstruction(scope);
					inst3.opcode = InstructionType.ADDA;
					inst3.op1type = OperandType.INST;
					inst3.op1 = inst1;
					inst3.op2type = OperandType.INST;
					inst3.op2 = inst2;
					inst3.forceGenerate(scope);
					termNode.addInstruction(inst3);
					
					PLIRInstruction load = new PLIRInstruction(scope);
					load.opcode = InstructionType.LOAD;
					load.op1type = OperandType.ADDRESS;
					load.op1 = inst3;
					load.forceGenerate(scope);
					termNode.addInstruction(load);
					
					// save the load value (or the last address, if necessary)
					leftInst = load;
					lastAddress = load;
				}
			}
			
			PLIRInstruction rightInst = rightNode.getLastInst();
			if (rightInst.isArray)
			{
				PLIRInstruction lastAddress = null;
				for (int i = 0; i < factor.arrayOperands.size(); i++)
				{
					PLIRInstruction operand = factor.arrayOperands.get(i);
					
					// Need to load from memory - insert the load
					PLIRInstruction inst1 = new PLIRInstruction(scope);
					inst1.opcode = InstructionType.MUL;
					inst1.op1type = OperandType.CONST;
					inst1.i1 = 4; // CONSTANT! DOESN'T CHANGE!
					inst1.op2type = operand.type;
					inst1.op2 = operand;
					if (inst1.op2type == OperandType.CONST)
					{
						inst1.i2 = inst1.op2.tempVal;
					}
					inst1.forceGenerate(scope);
					termNode.addInstruction(inst1);
					
					PLIRInstruction inst2 = new PLIRInstruction(scope);
					inst2.opcode = InstructionType.ADD;
					inst2.op1type = OperandType.FP;
//					inst2.op2type = OperandType.BASEADDRESS;
//					inst2.op2address = rightInst.origIdent + "_baseaddr";
					if (i == 0)
					{
						inst2.op2type = OperandType.BASEADDRESS;
						inst2.op2address = rightInst.origIdent + "_baseaddr";
					}
					else
					{
						inst2.op2type = OperandType.ADDRESS;
						inst2.op2 = lastAddress;
					}
					inst2.forceGenerate(scope);
					termNode.addInstruction(inst2);
					
					PLIRInstruction inst3 = new PLIRInstruction(scope);
					inst3.opcode = InstructionType.ADDA;
					inst3.op1type = OperandType.INST;
					inst3.op1 = inst1;
					inst3.op2type = OperandType.INST;
					inst3.op2 = inst2;
					inst3.forceGenerate(scope);
					termNode.addInstruction(inst3);
					
					PLIRInstruction load = new PLIRInstruction(scope);
					load.opcode = InstructionType.LOAD;
					load.op1type = OperandType.ADDRESS;
					load.op1 = inst3;
					load.forceGenerate(scope);
					termNode.addInstruction(load);
					
					rightInst = load;
					lastAddress = load;
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
			
			// ???
			for (PLIRInstruction inst : factor.instructions)
			{
				termNode.addInstruction(inst);
			}
			for (PLIRInstruction inst : rightNode.instructions)
			{
				termNode.addInstruction(inst);
			}
			termNode.addInstruction(termInst);
			
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
			if (leftInst.isArray)
			{
				PLIRInstruction lastAddress = null;
				for (int i = 0; i < term.arrayOperands.size(); i++)
				{
					PLIRInstruction operand = term.arrayOperands.get(i);
						
					// Need to load from memory - insert the load
					PLIRInstruction inst1 = new PLIRInstruction(scope);
					inst1.opcode = InstructionType.MUL;
					inst1.op1type = OperandType.CONST;
					inst1.i1 = 4; // CONSTANT! DOESN'T CHANGE!
					inst1.op2type = operand.type;
					inst1.op2 = operand;
					if (inst1.op2type == OperandType.CONST)
					{
						inst1.i2 = inst1.op2.tempVal;
					}
					inst1.forceGenerate(scope);
					exprNode.addInstruction(inst1);
					
					PLIRInstruction inst2 = new PLIRInstruction(scope);
					inst2.opcode = InstructionType.ADD;
					inst2.op1type = OperandType.FP;
//					inst2.op2type = OperandType.BASEADDRESS;
//					inst2.op2address = leftInst.origIdent + "_baseaddr";
					if (i == 0)
					{
						inst2.op2type = OperandType.BASEADDRESS;
						inst2.op2address = leftInst.origIdent + "_baseaddr";
					}
					else
					{
						inst2.op2type = OperandType.ADDRESS;
						inst2.op2 = lastAddress;
					}
					inst2.forceGenerate(scope);
					exprNode.addInstruction(inst2);
					
					PLIRInstruction inst3 = new PLIRInstruction(scope);
					inst3.opcode = InstructionType.ADDA;
					inst3.op1type = OperandType.INST;
					inst3.op1 = inst1;
					inst3.op2type = OperandType.INST;
					inst3.op2 = inst2;
					inst3.forceGenerate(scope);
					exprNode.addInstruction(inst3);
					
					PLIRInstruction load = new PLIRInstruction(scope);
					load.opcode = InstructionType.LOAD;
					load.op1type = OperandType.ADDRESS;
					load.op1 = inst3;
					load.forceGenerate(scope);
					exprNode.addInstruction(load);
					
					leftInst = load;
					lastAddress = load;
				}
			} 
			
			PLIRInstruction rightInst = rightNode.getLastInst();
			if (rightInst.isArray)
			{
				PLIRInstruction lastAddress = null;
				for (int i = 0; i < rightNode.arrayOperands.size(); i++)
				{
					PLIRInstruction operand = rightNode.arrayOperands.get(i);
					
					// Need to load from memory - insert the load
					PLIRInstruction inst1 = new PLIRInstruction(scope);
					inst1.opcode = InstructionType.MUL;
					inst1.op1type = OperandType.CONST;
					inst1.i1 = 4; // CONSTANT! DOESN'T CHANGE!
					inst1.op2type = operand.type;
					inst1.op2 = operand;
					if (inst1.op2type == OperandType.CONST)
					{
						inst1.i2 = inst1.op2.tempVal;
					}
					inst1.forceGenerate(scope);
					exprNode.addInstruction(inst1);
					
					PLIRInstruction inst2 = new PLIRInstruction(scope);
					inst2.opcode = InstructionType.ADD;
					inst2.op1type = OperandType.FP;
//					inst2.op2type = OperandType.BASEADDRESS;
//					inst2.op2address = rightInst.origIdent + "_baseaddr";
					if (i == 0)
					{
						inst2.op2type = OperandType.BASEADDRESS;
						inst2.op2address = rightInst.origIdent + "_baseaddr";
					}
					else
					{
						inst2.op2type = OperandType.ADDRESS;
						inst2.op2 = lastAddress;
					}
					inst2.forceGenerate(scope);
					exprNode.addInstruction(inst2);
					
					PLIRInstruction inst3 = new PLIRInstruction(scope);
					inst3.opcode = InstructionType.ADDA;
					inst3.op1type = OperandType.INST;
					inst3.op1 = inst1;
					inst3.op2type = OperandType.INST;
					inst3.op2 = inst2;
					inst3.forceGenerate(scope);
					exprNode.addInstruction(inst3);
					
					
					PLIRInstruction load = new PLIRInstruction(scope);
					load.opcode = InstructionType.LOAD;
					load.op1type = OperandType.ADDRESS;
					load.op1 = inst3;
					load.forceGenerate(scope);
					exprNode.addInstruction(load);
					
					rightInst = load;
					lastAddress = load;
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
			
			// ???
			for (PLIRInstruction inst : term.instructions)
			{
				exprNode.addInstruction(inst);
			}
			for (PLIRInstruction inst : rightNode.instructions)
			{
				exprNode.addInstruction(inst);
			}
			exprNode.addInstruction(exprInst);
			
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
		
		// Parse the right-hand part of the relation expression
		PLIRBasicBlock right = parse_expression(in);
		
		// Build the comparison instruction with the memorized condition
		PLIRInstruction leftInst = left.instructions.get(left.instructions.size() - 1);
		if (leftInst.isArray)
		{
			PLIRInstruction lastAddress = null;
			for (int i = 0; i < left.arrayOperands.size(); i++)
			{
				PLIRInstruction operand = left.arrayOperands.get(i);
				
				// Need to load from memory - insert the load
				PLIRInstruction inst1 = new PLIRInstruction(scope);
				inst1.opcode = InstructionType.MUL;
				inst1.op1type = OperandType.CONST;
				inst1.i1 = 4; // CONSTANT! DOESN'T CHANGE!
				inst1.op2type = operand.type;
				inst1.op2 = operand;
				if (inst1.op2type == OperandType.CONST)
				{
					inst1.i2 = inst1.op2.tempVal;
				}
				inst1.forceGenerate(scope);
				relation.addInstruction(inst1);
				
				PLIRInstruction inst2 = new PLIRInstruction(scope);
				inst2.opcode = InstructionType.ADD;
				inst2.op1type = OperandType.FP;
//				inst2.op2type = OperandType.BASEADDRESS;
//				inst2.op2address = leftInst.origIdent + "_baseaddr";
				if (i == 0)
				{
					inst2.op2type = OperandType.BASEADDRESS;
					inst2.op2address = leftInst.origIdent + "_baseaddr";
				}
				else
				{
					inst2.op2type = OperandType.ADDRESS;
					inst2.op2 = lastAddress;
				}
				inst2.forceGenerate(scope);
				relation.addInstruction(inst2);
				
				PLIRInstruction inst3 = new PLIRInstruction(scope);
				inst3.opcode = InstructionType.ADDA;
				inst3.op1type = OperandType.INST;
				inst3.op1 = inst1;
				inst3.op2type = OperandType.INST;
				inst3.op2 = inst2;
				inst3.forceGenerate(scope);
				relation.addInstruction(inst3);
				
				
				PLIRInstruction load = new PLIRInstruction(scope);
				load.opcode = InstructionType.LOAD;
				load.op1type = OperandType.ADDRESS;
				load.op1 = inst3;
				load.forceGenerate(scope);
				relation.addInstruction(load);
				
				leftInst = load;
				lastAddress = load;
			}
		}
		
		PLIRInstruction rightInst = right.instructions.get(right.instructions.size() - 1);
		if (rightInst.isArray)
		{
			PLIRInstruction lastAddress = null;
			for (int i = 0; i < right.arrayOperands.size(); i++)
			{
				PLIRInstruction operand = right.arrayOperands.get(i);

				// Need to load from memory - insert the load
				PLIRInstruction inst1 = new PLIRInstruction(scope);
				inst1.opcode = InstructionType.MUL;
				inst1.op1type = OperandType.CONST;
				inst1.i1 = 4; // CONSTANT! DOESN'T CHANGE!
				inst1.op2type = operand.type;
				inst1.op2 = operand;
				if (inst1.op2type == OperandType.CONST)
				{
					inst1.i2 = inst1.op2.tempVal;
				}
				inst1.forceGenerate(scope);
				relation.addInstruction(inst1);
				
				PLIRInstruction inst2 = new PLIRInstruction(scope);
				inst2.opcode = InstructionType.ADD;
				inst2.op1type = OperandType.FP;
//				inst2.op2type = OperandType.BASEADDRESS;
//				inst2.op2address = rightInst.origIdent + "_baseaddr";
				if (i == 0)
				{
					inst2.op2type = OperandType.BASEADDRESS;
					inst2.op2address = rightInst.origIdent + "_baseaddr";
				}
				else
				{
					inst2.op2type = OperandType.ADDRESS;
					inst2.op2 = lastAddress;
				}
				inst2.forceGenerate(scope);
				relation.addInstruction(inst2);
				
				PLIRInstruction inst3 = new PLIRInstruction(scope);
				inst3.opcode = InstructionType.ADDA;
				inst3.op1type = OperandType.INST;
				inst3.op1 = inst1;
				inst3.op2type = OperandType.INST;
				inst3.op2 = inst2;
				inst3.forceGenerate(scope);
				relation.addInstruction(inst3);
				
				PLIRInstruction load = new PLIRInstruction(scope);
				load.opcode = InstructionType.LOAD;
				load.op1type = OperandType.ADDRESS;
				load.op1 = inst3;
				load.forceGenerate(scope);
				relation.addInstruction(load);
				
				rightInst = load;
				lastAddress = load;
			}
		}
		
		// Create the comparison instruction that joins the two together
		PLIRInstruction inst = PLIRInstruction.create_cmp(scope, leftInst, rightInst);
		inst.condcode = condcode;
		inst.fixupLocation = 0;
		
		// Create the relation block containing the instruction
		if (leftInst.type != OperandType.CONST) relation.addInstruction(leftInst);
		if (rightInst.type != OperandType.CONST) relation.addInstruction(rightInst);
		relation.addInstruction(inst);
		
		// Save whatever values are used in these expressions
		for (String sym : left.usedIdents.keySet())
		{
//			debug("adding " + sym + " with " + left.usedIdents.get(sym).toString());
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
				
				// Check to see if this assignment needs to be saved at the end of the function
				boolean markToSave = false;
				if (parsingFunctionBody && globalVariables.containsKey(desigBlock.getLastInst().origIdent))
				{
					markToSave = true;
				}
				
				if (globalVariables.containsKey(desigBlock.getLastInst().origIdent))
				{
					desigBlock.getLastInst().isGlobalVariable = true;
				}
				
				if (toksym == PLToken.becomesToken)
				{
					advance(in);
					result = parse_expression(in);
					
					// Check if the result is an array, in which case we need to load it from memory
					PLIRInstruction storeInst = result.getLastInst();
					storeInst.isGlobalVariable = desigBlock.getLastInst().isGlobalVariable;
					if (storeInst.isArray)
					{
						PLIRInstruction lastAddress = null;
						for (int i = 0; i < result.arrayOperands.size(); i++)
						{
							PLIRInstruction operand = result.arrayOperands.get(i);
							
							// Need to load from memory - insert the load
							PLIRInstruction inst1 = new PLIRInstruction(scope);
							inst1.opcode = InstructionType.MUL;
							inst1.op1type = OperandType.CONST;
							inst1.i1 = 4; // CONSTANT! DOESN'T CHANGE!
							inst1.op2type = operand.type;
							inst1.op2 = operand;
							if (inst1.op2type == OperandType.CONST)
							{
								inst1.i2 = inst1.op2.tempVal;
							}
							inst1.forceGenerate(scope);
							result.addInstruction(inst1);
							
							PLIRInstruction inst2 = new PLIRInstruction(scope);
							inst2.opcode = InstructionType.ADD;
							inst2.op1type = OperandType.FP;
							if (i == 0)
							{
								inst2.op2type = OperandType.BASEADDRESS;
								inst2.op2address = storeInst.origIdent + "_baseaddr";
							}
							else
							{
								inst2.op2type = OperandType.ADDRESS;
								inst2.op2 = lastAddress;
							}
							inst2.forceGenerate(scope);
							result.addInstruction(inst2);
							
							PLIRInstruction inst3 = new PLIRInstruction(scope);
							inst3.opcode = InstructionType.ADDA;
							inst3.op1type = OperandType.INST;
							inst3.op1 = inst1;
							inst3.op2type = OperandType.INST;
							inst3.op2 = inst2;
							inst3.forceGenerate(scope);
							result.addInstruction(inst3);
							
							PLIRInstruction load = new PLIRInstruction(scope);
							load.opcode = InstructionType.LOAD;
							load.op1type = OperandType.ADDRESS;
							load.op1 = inst3; // CULPRIT!!!
							load.forceGenerate(scope);
							result.addInstruction(load);
							
							// save the load value (or the last address, if necessary, for continued loads)
							storeInst = load;
							lastAddress = load;
						}
					}
					
					if (desigBlock.arrayOperands == null)
					{
						// The last instruction added to the BB is the one that holds the value for this assignment
//						PLIRInstruction inst = result.instructions.get(result.instructions.size() - 1);
						storeInst.origIdent = varName;
						storeInst.tempPosition = PLStaticSingleAssignment.globalSSAIndex;
						
						// If we aren't deferring generation because of a potential PHI usage, just replace with the current value in scope
						if ((storeInst.op1 != null && deferredPhiIdents.contains(storeInst.op1.origIdent)) || 
								(storeInst.op2 != null && deferredPhiIdents.contains(storeInst.op2.origIdent)))
						{
							storeInst.kind = ResultKind.VAR;
							storeInst.overrideGenerate = true;
							storeInst.forceGenerate(scope);
						}
						else if (deferredPhiIdents.contains(varName)) // else, force the current instruction to be generated so it can be used in a phi later
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
						result.addModifiedValue(varName, storeInst);
						
						if (markToSave)
						{
							scope.functions.get(funcName).addModifiedGlobal(globalVariables.get(desigBlock.getLastInst().origIdent), storeInst);
						}
					}
					else // array!
					{
						PLIRInstruction storeValue = storeInst;
						PLIRInstruction inst4 = null;
						PLIRInstruction lastAddress = null;
						for (int i = 0; i < desigBlock.arrayOperands.size(); i++)
						{
							PLIRInstruction operand = desigBlock.arrayOperands.get(i);
							
							// arrayOperands.size() == # of indices to generate
							PLIRInstruction inst1 = new PLIRInstruction(scope);
							inst1.opcode = InstructionType.MUL;
							inst1.op1type = OperandType.CONST;
							inst1.i1 = 4; // CONSTANT! DOESN'T CHANGE!
							inst1.op2type = operand.type;
							inst1.op2 = operand;
							if (inst1.op2type == OperandType.CONST)
							{
								inst1.i2 = inst1.op2.tempVal;
							}
							inst1.forceGenerate(scope);
							result.addInstruction(inst1);
							
							PLIRInstruction inst2 = new PLIRInstruction(scope);
							inst2.opcode = InstructionType.ADD;
							inst2.op1type = OperandType.FP;
//							inst2.op2type = OperandType.BASEADDRESS;
//							inst2.op2address = varName + "_baseaddr";
							
							if (i == 0)
							{
								inst2.op2type = OperandType.BASEADDRESS;
								inst2.op2address = varName + "_baseaddr";
							}
							else
							{
								inst2.op2type = OperandType.ADDRESS;
								inst2.op2 = lastAddress;
							}
							
							inst2.forceGenerate(scope);
							result.addInstruction(inst2);
							
							PLIRInstruction inst3 = new PLIRInstruction(scope);
							inst3.opcode = InstructionType.ADDA;
							inst3.op1type = OperandType.INST;
							inst3.op1 = inst1;
							inst3.op2type = OperandType.INST;
							inst3.op2 = inst2;
							inst3.forceGenerate(scope);
							result.addInstruction(inst3);
							
							// End of the load chain - insert the store
							if (i == desigBlock.arrayOperands.size() - 1)
							{
								inst4 = new PLIRInstruction(scope);
								inst4.opcode = InstructionType.STORE;
								inst4.op1type = OperandType.INST;
								inst4.op1 = inst3;
								inst4.op2type = storeValue.type;
								inst4.op2 = storeValue;
								if (inst4.op2type == OperandType.CONST)
								{
									inst4.i2 = inst4.op2.tempVal;
								}
								inst4.forceGenerate(scope);
								inst4.storedValue = inst4.op2;
								inst4.isArray = true;
								
								result.addInstruction(inst4);
							}
							else // load the correct address to index into the array
							{
								lastAddress = new PLIRInstruction(scope);
								lastAddress.opcode = InstructionType.LOAD;
								lastAddress.op1type = OperandType.ADDRESS;
								lastAddress.op1 = result.getLastInst();
								lastAddress.forceGenerate(scope);
								result.addInstruction(lastAddress);
							}
						}
						
						// Tag the variable name with the dependent instructions so we can get unique accesses later on 
						scope.updateSymbol(varName, inst4); // (SSA ID) := expr
						duChain.put(inst4, new HashSet<PLIRInstruction>());
						result.addModifiedValue(varName, inst4);
						
						// Add the resulting set of instructions to the BB result
						result.addModifiedValue(varName, inst4);
						
						if (markToSave)
						{
							scope.functions.get(funcName).addModifiedGlobal(globalVariables.get(inst4.origIdent), inst4);
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
					
					// HANDLE THE FIRST PARAMETER
					PLIRInstruction exprInst = callExprBlock.getLastInst();
					if (exprInst.isArray)
					{	
						PLIRInstruction lastAddress = null;
						for (int i = 0; i < callExprBlock.arrayOperands.size(); i++)
						{
							PLIRInstruction operand = callExprBlock.arrayOperands.get(i);
							
							// Need to load from memory - insert the load
							PLIRInstruction inst1 = new PLIRInstruction(scope);
							inst1.opcode = InstructionType.MUL;
							inst1.op1type = OperandType.CONST;
							inst1.i1 = 4; // CONSTANT! DOESN'T CHANGE!
							inst1.op2type = operand.type;
							inst1.op2 = operand;
							if (inst1.op2type == OperandType.CONST)
							{
								inst1.i2 = inst1.op2.tempVal;
							}
							inst1.forceGenerate(scope);
							result.addInstruction(inst1);
							
							PLIRInstruction inst2 = new PLIRInstruction(scope);
							inst2.opcode = InstructionType.ADD;
							inst2.op1type = OperandType.FP;
							if (i == 0)
							{
								inst2.op2type = OperandType.BASEADDRESS;
								inst2.op2address = exprInst.origIdent + "_baseaddr";
							}
							else
							{
								inst2.op2type = OperandType.ADDRESS;
								inst2.op2 = lastAddress;
							}
							inst2.forceGenerate(scope);
							result.addInstruction(inst2);
							
							PLIRInstruction inst3 = new PLIRInstruction(scope);
							inst3.opcode = InstructionType.ADDA;
							inst3.op1type = OperandType.INST;
							inst3.op1 = inst1;
							inst3.op2type = OperandType.INST;
							inst3.op2 = inst2;
							inst3.forceGenerate(scope);
							result.addInstruction(inst3);
							
							PLIRInstruction load = new PLIRInstruction(scope);
							load.opcode = InstructionType.LOAD;
							load.op1type = OperandType.ADDRESS;
							load.op1 = inst3;
							load.type = OperandType.INST;
							load.forceGenerate(scope);
							result.addInstruction(load);
							
							lastAddress = load;
							exprInst = load;
							
//							// Update DU chain
//							if (duChain.containsKey(exprInst))
//							{
//								duChain.get(exprInst).add(exprInst);
//								exprInst.uses++;
//							}
						}
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
						exprInst = callExprBlock.getLastInst();
						if (exprInst == null)
						{
							SyntaxError("Invalid parameter to OutputNum");
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
							
							// TODO: should we be merging the BBs here?
							result = PLIRBasicBlock.merge(result, parse_expression(in));
							
							exprInst = result.getLastInst();
							if (exprInst.isArray)
							{	
								PLIRInstruction lastAddress = null;
								for (int i = 0; i < callExprBlock.arrayOperands.size(); i++)
								{
									PLIRInstruction operand = callExprBlock.arrayOperands.get(i);
									
									// Need to load from memory - insert the load
									PLIRInstruction inst1 = new PLIRInstruction(scope);
									inst1.opcode = InstructionType.MUL;
									inst1.op1type = OperandType.CONST;
									inst1.i1 = 4; // CONSTANT! DOESN'T CHANGE!
									inst1.op2type = operand.type;
									inst1.op2 = operand;
									if (inst1.op2type == OperandType.CONST)
									{
										inst1.i2 = inst1.op2.tempVal;
									}
									inst1.forceGenerate(scope);
									result.addInstruction(inst1);
									
									PLIRInstruction inst2 = new PLIRInstruction(scope);
									inst2.opcode = InstructionType.ADD;
									inst2.op1type = OperandType.FP;
									if (i == 0)
									{
										inst2.op2type = OperandType.BASEADDRESS;
										inst2.op2address = exprInst.origIdent + "_baseaddr";
									}
									else
									{
										inst2.op2type = OperandType.ADDRESS;
										inst2.op2 = lastAddress;
									}
									inst2.forceGenerate(scope);
									result.addInstruction(inst2);
									
									PLIRInstruction inst3 = new PLIRInstruction(scope);
									inst3.opcode = InstructionType.ADDA;
									inst3.op1type = OperandType.INST;
									inst3.op1 = inst1;
									inst3.op2type = OperandType.INST;
									inst3.op2 = inst2;
									inst3.forceGenerate(scope);
									result.addInstruction(inst3);
									
									PLIRInstruction load = new PLIRInstruction(scope);
									load.opcode = InstructionType.LOAD;
									load.op1type = OperandType.ADDRESS;
									load.op1 = inst3;
									load.type = OperandType.INST;
									load.forceGenerate(scope);
									result.addInstruction(load);
									
									lastAddress = load;
									exprInst = load;
									
									// Update DU chain
//									if (duChain.containsKey(exprInst))
//									{
//										duChain.get(exprInst).add(exprInst);
//										exprInst.uses++;
//									}
								}
							}
							else
							{
								exprInst.forceGenerate(scope);
								result.addInstruction(exprInst);
							}
							
							operands.add(exprInst);
//							advance(in);
						}
						
						if (paramMap.get(funcName) != operands.size())
						{
							SyntaxError("Function: " + funcName + " invoked with the wrong number of arguments. Expected " + paramMap.get(funcName) + ", got " + operands.size());
						}
						
						PLIRInstruction callInst = PLIRInstruction.create_call(scope, funcName, funcFlagMap.get(funcName), operands);
						callInst.forceGenerate(scope);
//						result = new PLIRBasicBlock();
						result.hasReturn = funcFlagMap.get(funcName); 
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
//					result = new PLIRBasicBlock();
					result.hasReturn = true; // special case... this is a machine instruction, not a user-defined function
					result.addInstruction(inst);
					result.isEntry = true;
				}
				else if (toksym == PLToken.closeParenToken && funcName.equals("OutputNewLine"))
				{
					PLIRInstruction inst = new PLIRInstruction(scope, InstructionType.WLN);
					inst.forceGenerate(scope);
//					result = new PLIRBasicBlock();
					result.addInstruction(inst);
					result.isEntry = true;
				}
				else
				{
					PLIRInstruction callInst = PLIRInstruction.create_call(scope, funcName, funcFlagMap.get(funcName), operands);
					callInst.forceGenerate(scope);
//					result = new PLIRBasicBlock();
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
//				result = new PLIRBasicBlock();
				result.hasReturn = funcFlagMap.get(funcName); // special case... this is a machine instruction, not a user-defined function
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
			result.isEntry = false;// caw
			
			String funcName = callStack.get(callStack.size() - 1);
			debug("returning from: " + funcName);
			
			// Handle replacement of global variables 
			if (funcName.equals("OutputNum") == false && funcName.equals("OutputNewLine") == false && funcName.equals("InputNum") == false)
			{
				for (PLIRInstruction glob : scope.functions.get(funcName).modifiedGlobals.keySet())
				{
					glob.type = OperandType.ADDRESS;
					glob.kind = ResultKind.VAR;
					glob.overrideGenerate = true;
					glob.forceGenerate(scope);
					debug(glob.toString());
//					debug(scope.getCurrentValue("x").toString());
				}
			}
			
			callStack.remove(callStack.size() - 1);
		}
		else if (toksym == PLToken.ifToken)
		{
			PLIRInstruction follow = new PLIRInstruction(scope);
			follow.fixupLocation = 0;
			advance(in);
			
			scope.pushNewScope("if" + (blockDepth++));
			
			// Parse the condition relation
			PLIRBasicBlock entry = parse_relation(in);
			for (PLIRInstruction inst : entry.instructions)
			{
				if (inst.kind == ResultKind.CONST)
				{
					inst.overrideGenerate = true;
					inst.forceGenerate(scope);
				}
			}
			PLIRInstruction x = entry.instructions.get(entry.instructions.size() - 1);
			PLIRInstruction branch = CondNegBraFwd(x);
			branch.fixupLocation = PLStaticSingleAssignment.globalSSAIndex - 1;
			entry.addInstruction(branch);
			
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
			
			// Check for an else branch
			int offset = 0;
			PLIRBasicBlock elseBlock = null;
			ArrayList<String> sharedModifiers = new ArrayList<String>();
			PLIRInstruction uncond = null;
			if (toksym == PLToken.elseToken)
			{
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
				
				// DEBUG
				debug("parsing the else block");
				scope.displayCurrentScopeSymbols();
				
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
					join.fixSpot();
					joinNode.parents.add(join);
				}
				else
				{
					elseBlock.rightChild = joinNode;
					elseBlock.fixSpot();
					joinNode.parents.add(elseBlock);
				}
				entry.rightChild = elseBlock;
				elseBlock.parents.add(entry);
				
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
				debug("(shared if) Inserting " + sharedModifiers.size() + " phis");
				ArrayList<PLIRInstruction> phisToAdd = new ArrayList<PLIRInstruction>(); 
				for (int i = 0; i < sharedModifiers.size(); i++)
				{
					String var = sharedModifiers.get(i);
					offset++; 
					PLIRInstruction thenInst = thenBlock.modifiedIdents.get(var);
					debug(thenInst.toString());
					thenInst.forceGenerate(scope, thenInst.tempPosition);
					PLIRInstruction elseInst = elseBlock.modifiedIdents.get(var);
					debug(elseInst.toString());
					elseInst.forceGenerate(scope, elseInst.tempPosition);
					PLIRInstruction phi = PLIRInstruction.create_phi(scope, thenInst, elseInst, PLStaticSingleAssignment.globalSSAIndex);
					phi.isGlobalVariable = thenInst.isGlobalVariable || elseInst.isGlobalVariable;
					joinNode.insertInstruction(phi, 0);
					phisToAdd.add(phi);
					
					// The current value in scope needs to be updated now with the result of the phi
					entry.modifiedIdents.put(var, phi);
					joinNode.modifiedIdents.put(var, phi);
				}
				
				// Jump back from the if statement scope so we can update the variables with the appropriate phi result
				scope.popScope();
//				blockDepth--;
				for (int i = 0; i < phisToAdd.size(); i++)
				{
					scope.updateSymbol(sharedModifiers.get(i), phisToAdd.get(i));
					
					// Add to this block's list of modified identifiers
					// Rationale: since we added a phi, the value potentially changes (is modified), 
					// 	so the latest value in the current scope needs to be modified
					entry.modifiedIdents.put(sharedModifiers.get(i), phisToAdd.get(i));
				}
				
				debug("after else block");
				scope.displayCurrentScopeSymbols();
				
//				scope.popScope();
				
				// Check for modifications in the else block (but not in the if)
				ArrayList<String> modifiers = new ArrayList<String>();
				for (String modded : elseBlock.modifiedIdents.keySet())
				{
					if (sharedModifiers.contains(modded) == false)
					{
						modifiers.add(modded);
					}
				}
				debug("(only the else) Inserting " + modifiers.size() + " phis");
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
					PLIRInstruction followInst = scope.getCurrentValue(var);
					followInst.forceGenerate(scope, followInst.tempPosition);
					PLIRInstruction phi = PLIRInstruction.create_phi(scope, followInst, elseInst, PLStaticSingleAssignment.globalSSAIndex);
					phi.isGlobalVariable = followInst.isGlobalVariable || elseInst.isGlobalVariable;
					debug(phi.toString());
					joinNode.insertInstruction(phi, 0);
					
					// The current value in scope needs to be updated now with the result of the phi
					scope.updateSymbol(var, phi);
					
					// Add to this block's list of modified identifiers
					// Rationale: since we added a phi, the value potentially changes (is modified), 
					// 	so the latest value in the current scope needs to be modified
					entry.modifiedIdents.put(var, phi);
					joinNode.modifiedIdents.put(var, phi);
				}
				
				modifiers = new ArrayList<String>();
				for (String modded : thenBlock.modifiedIdents.keySet())
				{
					if (sharedModifiers.contains(modded) == false) // don't double-add
					{
						modifiers.add(modded);
					}
				}
				debug("(only the if) Inserting " + modifiers.size() + " phis");
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
					
//					PLIRInstruction followInst = scope.getCurrentValue(var);
					PLIRInstruction followInst = scope.getLastValue(var);
					
					followInst.forceGenerate(scope, followInst.tempPosition);
					PLIRInstruction phi = PLIRInstruction.create_phi(scope, leftInst, followInst, PLStaticSingleAssignment.globalSSAIndex);
					phi.isGlobalVariable = leftInst.isGlobalVariable || followInst.isGlobalVariable;
					debug(phi.toString());
					joinNode.insertInstruction(phi, 0);
					
					// The current value in scope needs to be updated now with the result of the phi
					System.out.println(scope.getCurrentScope());
					scope.updateSymbol(var, phi);
					
					// Add to this block's list of modified identifiers
					// Rationale: since we added a phi, the value potentially changes (is modified), 
					// 	so the latest value in the current scope needs to be modified
					entry.modifiedIdents.put(var, phi);
					joinNode.modifiedIdents.put(var, phi);
				}
			}
			else // there was no else block, so the right child becomes the join node
			{
				entry.rightChild = joinNode;
				joinNode.parents.add(entry);
				scope.popScope();
//			}
				
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
				debug("(if statement without else) Inserting " + modifiers.size() + " phis");
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
					
//					PLIRInstruction followInst = scope.getCurrentValue(var);
					PLIRInstruction followInst = scope.getLastValue(var);
					
					followInst.forceGenerate(scope, followInst.tempPosition);
					PLIRInstruction phi = PLIRInstruction.create_phi(scope, leftInst, followInst, PLStaticSingleAssignment.globalSSAIndex);
					phi.isGlobalVariable = leftInst.isGlobalVariable || followInst.isGlobalVariable;
					debug(phi.toString());
					joinNode.insertInstruction(phi, 0);
					
					// The current value in scope needs to be updated now with the result of the phi
					System.out.println(scope.getCurrentScope());
					scope.updateSymbol(var, phi);
					
					// Add to this block's list of modified identifiers
					// Rationale: since we added a phi, the value potentially changes (is modified), 
					// 	so the latest value in the current scope needs to be modified
					entry.modifiedIdents.put(var, phi);
					joinNode.modifiedIdents.put(var, phi);
				}
//				scope.popScope();
//				blockDepth--;
			}
			
//			// Check for necessary phis to be inserted in the join block
//			// We need phis for variables that were modified in both branches so we fall through with the right value
//			ArrayList<String> modifiers = new ArrayList<String>();
//			for (String modded : thenBlock.modifiedIdents.keySet())
//			{
//				if (sharedModifiers.contains(modded) == false) // don't double-add
//				{
//					modifiers.add(modded);
//				}
//			}
//			debug("(if statement without else) Inserting " + modifiers.size() + " phis");
//			for (String var : modifiers)
//			{
//				// Check to make sure this thing was actually in scope!
//				if (scope.getCurrentValue(var) == null)
//				{
//					debug("Uninitialized identifier in path: " + var);
//				}
//				
//				offset++;
//				PLIRInstruction leftInst = thenBlock.modifiedIdents.get(var);
//				leftInst.forceGenerate(scope, leftInst.tempPosition);
//				PLIRInstruction followInst = scope.getCurrentValue(var);
//				followInst.forceGenerate(scope, followInst.tempPosition);
//				PLIRInstruction phi = PLIRInstruction.create_phi(scope, leftInst, followInst, PLStaticSingleAssignment.globalSSAIndex);
//				debug(phi.toString());
//				joinNode.insertInstruction(phi, 0);
//				
//				// The current value in scope needs to be updated now with the result of the phi
//				scope.updateSymbol(var, phi);
//				
//				// Add to this block's list of modified identifiers
//				// Rationale: since we added a phi, the value potentially changes (is modified), 
//				// 	so the latest value in the current scope needs to be modified
//				entry.modifiedIdents.put(var, phi);
//				joinNode.modifiedIdents.put(var, phi);
//			}
			
//			if (elseBlock != null)
//			{
//				scope.popScope();
//			}
			
			// After the phis have been inserted at the appropriate positions, fixup the entry instructions
			if (elseBlock != null)
			{
				FixupExact(x.fixupLocation, uncond.tempPosition - x.fixupLocation);
				FixupExact(uncond.tempPosition - 1, PLStaticSingleAssignment.globalSSAIndex - uncond.tempPosition - offset + 1);
			}
			else
			{
				Fixup(x.fixupLocation, -offset);
			}
			
			// Fix the join BB index
			joinNode.fixSpot();
			
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
			int loopLocation = PLStaticSingleAssignment.globalSSAIndex;
			
			scope.pushNewScope("while" + (blockDepth++));
			
			// Parse the condition (relation) for the loop
			PLIRBasicBlock entry = parse_relation(in);
			entry.isLoopHeader = true;
			PLIRInstruction entryCmpInst = entry.instructions.get(entry.instructions.size() - 1);
			PLIRInstruction bgeInst = CondNegBraFwd(entryCmpInst);
			
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
//			blockDepth--;
			
			////////////////////////////////////////////
			// We've passed through the body and know what variables are updated, now we need to insert phis
			// Phis are inserted when variables in the relation are modified in the loop body
			// Left phi value is entry instruction, right phi value is instruction computed in loop body
			
			ArrayList<String> modded = new ArrayList<String>();
			for (String i1 : body.modifiedIdents.keySet())
			{
				modded.add(i1);
			}
			debug("(while loop) Inserting " + modded.size() + " phis");
			int offset = 0;
			ArrayList<PLIRInstruction> phisGenerated = new ArrayList<PLIRInstruction>(); 
			for (String var : modded)
			{
				PLIRInstruction bodyInst = body.modifiedIdents.get(var);
				bodyInst.forceGenerate(scope, bodyInst.tempPosition);
				PLIRInstruction preInst = scope.getCurrentValue(var);
				preInst.forceGenerate(scope, preInst.tempPosition);
				
				// Inject the phi at the appropriate spot in the join node...
				PLIRInstruction phi = PLIRInstruction.create_phi(scope, preInst, bodyInst, loopLocation + offset);
				phi.isGlobalVariable = preInst.isGlobalVariable || bodyInst.isGlobalVariable;
				phi.whilePhi = true;
				phisGenerated.add(phi);
				debug("new phi: " + phi.id + " := " + phi.toString());
				phi.origIdent = var; // needed for propagation
				offset++;
				entry.insertInstruction(phi, 0);

				// The current value in scope needs to be updated now with the result of the phi
				scope.updateSymbol(var, phi);
				
				// Add to this block's list of modified identifiers
				// Rationale: since we added a phi, the value potentially changes (is modified), 
				// 	so the latest value in the current scope needs to be modified
				entry.modifiedIdents.put(var, phi);
				
				// Now loop through the entry and fix instructions as needed
				// Those fixed are replaced with the result of this phi if they have used or modified the sym...
				if (cmpInst.op1 != null && cmpInst.op1.origIdent.equals(var))
				{
					cmpInst.replaceLeftOperand(phi);
				}
				if (cmpInst.op2 != null && cmpInst.op2.origIdent.equals(var))
				{
					cmpInst.replaceRightOperand(phi);
				}
				
				ArrayList<PLIRBasicBlock> visited = new ArrayList<PLIRBasicBlock>();
				visited.add(entry);
				HashMap<String, PLIRInstruction> scopeMap = new HashMap<String, PLIRInstruction>(); 
				scopeMap.put(var, phi);
				
				body.propagatePhi(var, phi, visited, scopeMap);
			}
			
			// Go through the phis and make sure we adjusted the values accordingly
			// Second pass, really..
			for (PLIRInstruction phi : phisGenerated)
			{
				for (String var : modded)
				{
					if (var.equals(phi.origIdent))
					{
						PLIRInstruction bodyInst = body.modifiedIdents.get(var);
						debug("replacing phi " + phi.origIdent + " with " + var + ", " + bodyInst.toString());
						bodyInst.overrideGenerate = true;
						bodyInst.forceGenerate(scope);
						debug(bodyInst.toString());
						debug(phi.toString());
						if (bodyInst.op2type == OperandType.CONST) debug("hoorah");
						phi.op2type = OperandType.INST;
						phi.op2 = bodyInst;
						debug(bodyInst.toString());
					}
				}
			}
			
			////////////////////////////////////////////
			
			// Hook the body of the loop back to the entry
			if (body.joinNode != null)
			{
				PLIRBasicBlock join = body.joinNode;
				while (join.joinNode != null)
				{
					join = join.joinNode;
				}
//				join.children.add(entry);
				join.leftChild = entry;
				join.fixSpot();
			}
			else
			{
//				body.children.add(entry);
				body.leftChild = entry;
				body.fixSpot();
			}
//			entry.children.add(body);
			entry.leftChild = body;
			body.parents.add(entry);
			
			// Patch up the follow-through branch
//			entry.children.add(joinNode); 
			entry.rightChild = joinNode;
			joinNode.parents.add(entry); 
			
			// Insert the unconditional branch at (location - pc)
			PLIRInstruction beqInst = PLIRInstruction.create_BEQ(scope, loopLocation - PLStaticSingleAssignment.globalSSAIndex);
			if (body.joinNode != null)
			{
				PLIRBasicBlock join = body.joinNode;
				while (join.joinNode != null)
				{
					join = join.joinNode;
				}
				join.addInstruction(beqInst);
			}
			else
			{
				body.addInstruction(beqInst);
			}
			
			// Fixup the conditional branch at the appropriate location
			bgeInst.fixupLocation = entryCmpInst.fixupLocation;
			Fixup(bgeInst.fixupLocation, 0); 
			
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
				
//				result.instructions.get(result.instructions.size() - 1).overrideGenerate = true;
//				result.instructions.get(result.instructions.size() - 1).forceGenerate(scope);
				
//				result.returnInst = result.getLastInst();
//				result.returnInst.isReturn = true;
				result.isEntry = true;
				debug("Forcing generation of return statement");
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

	private PLIRBasicBlock parse_statSequence(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
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
			ArrayList<Integer> dimensions = new ArrayList<Integer>();
			
			if (toksym == PLToken.openBracketToken)
			{
				advance(in);
//				result = parse_number(in);
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
					debug("adding array: " + sym);
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

	public boolean parseVariableDeclaration = false;
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
		
		parseVariableDeclaration = true;
		
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
		
		parseVariableDeclaration = false;
		
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
			
			System.out.println("Function: " + sym);
			scope.displayCurrentScopeSymbols();
			
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
				result = PLIRBasicBlock.merge(result, body);
			}
			else
			{
				result = parse_funcBody(in);
			}
			
//			for (int i = result.instructions.size() - 1; i>= 0; i--)
//			{
//				result.instructions.get(i).forceGenerate(scope);
//			}
			
			// Eat the semicolon terminating the body
			if (toksym != PLToken.semiToken)
			{
				SyntaxError("';' missing from funcDecl non-terminal");
			}
			advance(in);
			
			// Leave the scope of this new function
			String leavingScope = scope.popScope();
//			blockDepth--;
			debug("Leaving scope: " + leavingScope);
		}
		else
		{
			SyntaxError("Invalid start to funcDecl non-terminal");
		}
		
		return result;
	}

	public boolean isFunction = false;
	public ArrayList<PLIRInstruction> params;
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
				// Pull out the identifier of the next token...
//				params.add(sym);
				
				PLIRInstruction dummy = new PLIRInstruction(scope);
				dummy.dummyName = sym;
				dummy.origIdent = sym;
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
					dummy.origIdent = sym;
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
		
		return result;
	}

	public boolean parsingFunctionBody = false;
	public ArrayList<PLIRInstruction> variables = new ArrayList<PLIRInstruction>();
	private PLIRBasicBlock parse_funcBody(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock result = null;
		String name = funcName; // save
		
		parsingFunctionBody = true;
		
		variables = new ArrayList<PLIRInstruction>();
		while (toksym == PLToken.varToken || toksym == PLToken.arrToken)
		{
			result = parse_varDecl(in); // TODO: not sure what we really want to do here...
		}
		
//		// Actually make the function now..
//		paramMap.put(funcName, params.size());
//		if (isFunction)
//		{
//			scope.addFunction(funcName, params, variables);
//		}
//		else
//		{
//			scope.addProcedure(funcName, params, variables);
//		}
		
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
	
	// per spec
	private PLIRInstruction CondNegBraFwd(PLIRInstruction x)
	{
		x.fixupLocation = PLStaticSingleAssignment.globalSSAIndex;
		PLIRInstruction inst = PLIRInstruction.create_branch(scope, x, x.condcode);
		return inst;
	}
	
	// per spec
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
		debug("Fixing: " + loc + ", " + (PLStaticSingleAssignment.globalSSAIndex - loc + offset) + ", " + offset);
		if (PLStaticSingleAssignment.instructions.get(loc).opcode == InstructionType.CMP)
		{
			System.exit(-1);
		}
		
		PLStaticSingleAssignment.instructions.get(loc).i2 = (PLStaticSingleAssignment.globalSSAIndex - loc + offset);
		debug("Setting: " + PLStaticSingleAssignment.instructions.get(loc)+ " to " + (PLStaticSingleAssignment.globalSSAIndex - loc + offset));
	}
	
	private void FixupExact(int loc, int newVal)
	{
		PLStaticSingleAssignment.instructions.get(loc).i2 = newVal;
	}
}
