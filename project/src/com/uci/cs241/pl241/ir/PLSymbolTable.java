package com.uci.cs241.pl241.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PLSymbolTable
{
	
	public HashMap<String, PLIRInstruction> symTable;
	
	public PLSymbolTable()
	{
		symTable = new HashMap<String, PLIRInstruction>();
	}
	
	public void updateSymbol(String scope, String sym, PLIRInstruction inst)
	{	
		if (symTable.containsKey(sym))
		{
			System.err.println("Replacing value of symbol in table: " + sym);
		}
		symTable.put(sym, inst); 
	}
}
