package com.uci.cs241.pl241.ir;

public class Tuple
{
	PLIRInstruction v1;
	PLIRInstruction v2;
	public Tuple(PLIRInstruction i1, PLIRInstruction i2)
	{
		v1 = i1;
		v2 = i2;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (o instanceof Tuple)
		{
			Tuple other = (Tuple)o;
			if (v1.equals(other.v1) && v2.equals(other.v2))
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		return false;
	}
}
