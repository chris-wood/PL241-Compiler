package com.uci.cs241.pl241.frontend;

import java.util.ArrayList;
import java.util.HashMap;

import com.uci.cs241.pl241.ir.PLIRInstruction;

public class PLSymbolTable
{
	public ArrayList<String> currentScope; // stack of the current scope
	public HashMap<String, ArrayList<String>> funScopeTable;
	public HashMap<String, ArrayList<String>> varScopeTable;
	public HashMap<String, HashMap<String, PLIRInstruction>> symTable;
	public String name;
	
	public PLSymbolTable()
	{
		funScopeTable = new HashMap<String, ArrayList<String>>();
		varScopeTable = new HashMap<String, ArrayList<String>>();
		symTable = new HashMap<String, HashMap<String, PLIRInstruction>>();
		currentScope = new ArrayList<String>();
	}
	
	public void pushNewScope(String scope)
	{
		currentScope.add(scope);
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
			symTable.get(scope).put(sym, inst);
		}
		else
		{
			System.err.println("Trying to update symbol value of something " + 
					"out of scope: " + scope + ", " + sym);
			System.exit(-1);
		}
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
		return symTable.get(getCurrentScope()).get(sym);
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