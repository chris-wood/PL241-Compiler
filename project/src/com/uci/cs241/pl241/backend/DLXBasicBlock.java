package com.uci.cs241.pl241.backend;

import java.util.ArrayList;

public class DLXBasicBlock
{
	public int id;
	public ArrayList<DLXInstruction> instructions = new ArrayList<DLXInstruction>();
	public DLXBasicBlock left;
	public DLXBasicBlock right;
	public ArrayList<DLXBasicBlock> parents = new ArrayList<DLXBasicBlock>();
	
	public DLXBasicBlock(int id)
	{
		this.id = id;
	}
	
}
