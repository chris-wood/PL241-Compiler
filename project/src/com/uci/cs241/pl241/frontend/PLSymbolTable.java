package com.uci.cs241.pl241.frontend;

import java.util.ArrayList;
import java.util.HashMap;

import com.uci.cs241.pl241.ir.PLIRInstruction;
import com.uci.cs241.pl241.ir.PLIRInstruction.PLIRInstructionType;

public class PLSymbolTable
{
	public ArrayList<String> currentScope; // stack of the current scope
	public HashMap<String, ArrayList<String>> funScopeTable;
	public HashMap<String, ArrayList<String>> varScopeTable;
	public HashMap<String, HashMap<String, PLIRInstruction>> symTable;
	public HashMap<String, ArrayList<PLIRInstruction>> prevSymTable;
	public String name;
	
	public HashMap<String, HashMap<PLIRInstructionType, ArrayList<PLIRInstruction>>> instTypeMap = new HashMap<String, HashMap<PLIRInstructionType, ArrayList<PLIRInstruction>>>();
	
	public PLSymbolTable()
	{
		funScopeTable = new HashMap<String, ArrayList<String>>();
		varScopeTable = new HashMap<String, ArrayList<String>>();
		symTable = new HashMap<String, HashMap<String, PLIRInstruction>>();
		prevSymTable = new HashMap<String, ArrayList<PLIRInstruction>>();
		currentScope = new ArrayList<String>();
	}
	
	public ArrayList<PLIRInstruction> getDominatedInstructions(PLIRInstructionType type)
	{
		switch (type)
		{
		case NEG:
		case ADD:
		case SUB:
		case MUL:
		case DIV:
		case CMP:
			String scope = getCurrentScope();
			if (instTypeMap.containsKey(scope) == false)
			{
				instTypeMap.put(scope, new HashMap<PLIRInstructionType, ArrayList<PLIRInstruction>>());
				instTypeMap.get(scope).put(type, new ArrayList<PLIRInstruction>());
			}
			
			return instTypeMap.get(scope).get(type);
		default:
			return null;
		}
	}
	
	public void addDominatedInstruction(PLIRInstruction type)
	{
		switch (type.opcode)
		{
		case NEG:
		case ADD:
		case SUB:
		case MUL:
		case DIV:
		case CMP:
			String scope = getCurrentScope();
			if (instTypeMap.containsKey(scope) == false)
			{
				instTypeMap.put(scope, new HashMap<PLIRInstructionType, ArrayList<PLIRInstruction>>());
				instTypeMap.get(scope).put(type.opcode, new ArrayList<PLIRInstruction>());
			}
			instTypeMap.get(scope).get(type.opcode).add(type);
			break;
		}
	}
	
	public void pushNewScope(String scope)
	{
		currentScope.add(scope);
		symTable.put(scope, new HashMap<String, PLIRInstruction>());
		funScopeTable.put(scope, new ArrayList<String>());
		
		// Take everything that's already in scope and push it into the new scope
		if (currentScope.size() > 1)
		{
			String lastScope = currentScope.get(currentScope.size() - 2);
			for (String value : symTable.get(lastScope).keySet())
			{
//				System.err.println("Adding " + value + " from scope " + lastScope + " to " + scope);
				symTable.get(scope).put(value, symTable.get(lastScope).get(value));
			}
			for (String value : funScopeTable.get(lastScope))
			{
				funScopeTable.get(scope).add(value);
				if (varScopeTable.containsKey(value) == false)
				{
					varScopeTable.put(value, new ArrayList<String>());
				}
				varScopeTable.get(value).add(scope);
			}
		}
	}
	
	public String popScope()
	{
		if (currentScope.size() <= 1)
		{
			System.err.println("early jump out of scope");
			System.exit(-1);
		}
		String lastScope = currentScope.get(currentScope.size() - 1);
		currentScope.remove(currentScope.size() - 1);
		return lastScope;
	}
	
	public void addVarToScope(String scope, String sym)
	{
		if (funScopeTable.containsKey(scope) == false)
		{
			funScopeTable.put(scope, new ArrayList<String>());
		}
		funScopeTable.get(scope).add(sym);
		
		if (varScopeTable.containsKey(sym) == false)
		{
			varScopeTable.put(sym, new ArrayList<String>());
		}
		varScopeTable.get(sym).add(scope);
		
		// Create scope entry in symbol table
		if (symTable.containsKey(scope) == false)
		{
			symTable.put(scope, new HashMap<String, PLIRInstruction>());
		}
		
//		System.out.println("Adding " + sym + " to scope " + scope);
	}
	
	public void addVarToScope(String sym)
	{
		addVarToScope(getCurrentScope(), sym);
	}
	
	public void updateSymbol(String sym, PLIRInstruction inst)
	{
		String scope = getCurrentScope();
		if (isVarInScope(scope, sym))
		{
//			System.err.println("Updating sym " + sym + " with " + inst.toString());
			symTable.get(scope).put(sym, inst);
		}
		else
		{
			System.err.println("Trying to update symbol value of something " + 
					"out of scope: " + scope + ", " + sym);
			System.exit(-1);
		}
		
		// Check to see if the old value needs to be replaced 
		if (prevSymTable.containsKey(sym) == false)
		{
			prevSymTable.put(sym,  new ArrayList<PLIRInstruction>());
		}
		prevSymTable.get(sym).add(inst);
	}
	
	public boolean isVarInScope(String scope, String sym)
	{
		return funScopeTable.get(scope).contains(sym);
	}
	
	public boolean isVarInScope(String sym)
	{
		return isVarInScope(getCurrentScope(), sym);
	}
	
	public String getCurrentScope()
	{
		return currentScope.get(currentScope.size() - 1);
	}
	
	public PLIRInstruction getCurrentValue(String sym)
	{
		if (symTable.get(getCurrentScope()) == null) return null;
		else return symTable.get(getCurrentScope()).get(sym);
	}
	
	public void displayCurrentScopeSymbols()
	{
		String scope = getCurrentScope();
		for (String var : symTable.get(scope).keySet())
		{
			System.out.println("Var: " + var + " => " + 
				symTable.get(scope).get(var).id + ":= " + 
				symTable.get(scope).get(var));
		}
	}
	
}
