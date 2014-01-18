package com.uci.cs241.pl241.ir;

public class PLIRNode
{
	// SUBCLASSES: assignment, func/proc, array, block, designator, functionblock, load/store, main, param 
	
	public int sym;
	private PLIRNode left;
	private PLIRNode right;
	private PLIRNode parent;
	
	public PLIRNode(int sym)
	{
		this.sym = sym;
		this.left = this.right = this.parent = null;
	}
	
	public PLIRNode(int sym, PLIRNode left, PLIRNode right, PLIRNode parent)
	{
		this.sym = sym;
		this.left = left;
		this.right = right;
		this.parent = parent;
	}
	
	public void setLeft(PLIRNode left)
	{
		this.left = left;
	}
	
	public void setRight(PLIRNode right)
	{
		this.right = right;
	}
	
	public void setParent(PLIRNode parent)
	{
		this.parent = parent;
	}
	
	public PLIRNode getLeft()
	{
		return left;
	}
	
	public PLIRNode getRight()
	{
		return right;
	}
	
	public PLIRNode getParent()
	{
		return parent;
	}
}
