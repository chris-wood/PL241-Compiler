package com.uci.cs241.pl241.frontend;

import java.util.ArrayList;
import java.util.HashMap;
import com.uci.cs241.pl241.ir.PLIRInstruction;

public class Function 
{
	public boolean hasReturn;
	public ArrayList<PLIRInstruction> params;
	public ArrayList<PLIRInstruction> vars;
	public ArrayList<String> scope;
	public HashMap<String, Integer> constantsToSave = new HashMap<String, Integer>(); 
	public HashMap<PLIRInstruction, PLIRInstruction> modifiedGlobals;
	public HashMap<PLIRInstruction, PLIRInstruction> usedGlobals;
	public HashMap<String, ArrayList<Integer>> arraySizes;
	public String name;
	
	public Function(String name, ArrayList<PLIRInstruction> parameters, boolean hasReturn)
	{
		this.name = name;
		this.hasReturn = hasReturn;
		this.params = new ArrayList<PLIRInstruction>();
		this.vars = new ArrayList<PLIRInstruction>();
		this.modifiedGlobals = new HashMap<PLIRInstruction, PLIRInstruction>();
		this.usedGlobals = new HashMap<PLIRInstruction, PLIRInstruction>();
		this.scope = new ArrayList<String>();
		this.arraySizes = new HashMap<String, ArrayList<Integer>>();
		for (PLIRInstruction s : parameters)
		{
			this.params.add(s);
		}
	}
	
	public Function(String name, ArrayList<PLIRInstruction> parameters, ArrayList<PLIRInstruction> variables, boolean hasReturn)
	{
		this.name = name;
		this.hasReturn = hasReturn;
		this.params = new ArrayList<PLIRInstruction>();
		this.vars = new ArrayList<PLIRInstruction>();
		this.modifiedGlobals = new HashMap<PLIRInstruction, PLIRInstruction>();
		this.usedGlobals = new HashMap<PLIRInstruction, PLIRInstruction>();
		this.scope = new ArrayList<String>();
		this.arraySizes = new HashMap<String, ArrayList<Integer>>();
		for (PLIRInstruction s : parameters)
		{
			this.params.add(s);
		}
		for (PLIRInstruction s : variables)
		{
			this.vars.add(s);
		}
	}
	
	public int getStackOffset(String var)
	{
		int offset = 0;
		
		for (String array : arraySizes.keySet())
		{
			if (array.equals(var))
			{
				return offset;
			}
			int dimension = 1;
			for (Integer d : arraySizes.get(array))
			{
				dimension *= d;
			}
			offset += dimension;
		}
		
		return -1;
	}
	
	public int getTotalStackOffset()
	{
		int offset = 0;
		
		for (String array : arraySizes.keySet())
		{
			int dimension = 1;
			for (Integer d : arraySizes.get(array))
			{
				dimension *= d;
			}
			offset += dimension;
		}
		
		return offset;
	}
	
	public void addUsedGlobal(PLIRInstruction glob, PLIRInstruction inst)
	{
		usedGlobals.put(glob, inst);
	}
	
	public void addModifiedGlobal(PLIRInstruction glob, PLIRInstruction inst)
	{
		modifiedGlobals.put(glob, inst);
	}
	
	public void addParameter(PLIRInstruction inst)
	{
		vars.add(inst);
	}
	
	public void addVarToScope(String sym)
	{
		scope.add(sym);
	}
	
	public boolean isVarInScope(String sym)
	{
		return scope.contains(sym);
	}
	
	public boolean isLocalVariable(String v)
	{
		for (PLIRInstruction s : vars)
		{
			for (String scope : s.ident.keySet())
			{
				if (s.ident.get(scope).equals(v)) return true;
			}
		}
		return false;
	}
	
	public boolean isParameter(String p)
	{
		for (PLIRInstruction s : params)
		{
			for (String scope : s.ident.keySet())
			{
				if (s.ident.get(scope).equals(p)) return true;
			}
			if (s.dummyName.equals(p)) return true;
		}
		return false;
	}
	
	public PLIRInstruction getOperandByName(String name)
	{
		for (PLIRInstruction inst : params)
		{
			for (String scope : inst.ident.keySet())
			{
				if (inst.ident.get(scope).equals(name)) return inst;
			}
			if (inst.dummyName.equals(name)) return inst;
		}
		return null;
	}
	
	public PLIRInstruction getLocalVariableByName(String name)
	{
		for (PLIRInstruction inst : vars)
		{
			for (String scope : inst.ident.keySet())
			{
				if (inst.ident.get(scope).equals(name)) return inst;
			}
		}
		return null;
	}
}
