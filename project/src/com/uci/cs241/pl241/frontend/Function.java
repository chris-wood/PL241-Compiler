package com.uci.cs241.pl241.frontend;

import java.util.ArrayList;

import com.uci.cs241.pl241.ir.PLIRInstruction;

public class Function 
{
	public boolean hasReturn;
	public ArrayList<PLIRInstruction> params;
	public String name;
	
	public Function(String name, ArrayList<PLIRInstruction> parameters, boolean hasReturn)
	{
		this.name = name;
		this.hasReturn = hasReturn;
		this.params = new ArrayList<PLIRInstruction>();
		for (PLIRInstruction s : parameters)
		{
			this.params.add(s);
		}
	}
	
	public boolean isParameter(String p)
	{
		for (PLIRInstruction s : params)
		{
			if (s.origIdent.equals(p)) return true;
			if (s.dummyName.equals(p)) return true;
		}
		return false;
	}
	
	public PLIRInstruction getOperandByName(String name)
	{
		for (PLIRInstruction inst : params)
		{
			if (inst.origIdent.equals(name)) return inst;
			if (inst.dummyName.equals(name)) return inst;
		}
		return null;
	}
}
