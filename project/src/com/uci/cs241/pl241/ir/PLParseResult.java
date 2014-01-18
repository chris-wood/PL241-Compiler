package com.uci.cs241.pl241.ir;


/**
 * Dynamic data structure that stores the intermediate result from the PL241
 * parsing step during the recursive descent.
 * 
 * @author Christopher Wood, woodc1@uci.edu
 *
 */
public class PLParseResult
{	
	public PLParseResultKind kind;
	public int val;
	public int address;
	public int regno;
	public int cond, fixupLocation;
	
	public void combine(PLParseResult other)
	{
		// TODO
	}
}
