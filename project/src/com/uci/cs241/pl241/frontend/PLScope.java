package com.uci.cs241.pl241.frontend;

import java.util.ArrayList;
import java.util.HashMap;

public class PLScope
{
	public ArrayList<String> currentScope; // maintain stack of the current scope
	public HashMap<String, ArrayList<String>> funScopeTable;
	public HashMap<String, ArrayList<String>> varScopeTable;
	public String name;
	
	public PLScope()
	{
		funScopeTable = new HashMap<String, ArrayList<String>>();
		varScopeTable = new HashMap<String, ArrayList<String>>();
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
	}
	
}
