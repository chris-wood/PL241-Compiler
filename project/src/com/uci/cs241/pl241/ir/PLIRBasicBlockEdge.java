package com.uci.cs241.pl241.ir;

public class PLIRBasicBlockEdge
{
	public PLIRBasicBlock u;
	public PLIRBasicBlock v;
	
	public PLIRBasicBlockEdge(PLIRBasicBlock uu, PLIRBasicBlock vv)
	{
		u = uu; v = vv;
	}
}
