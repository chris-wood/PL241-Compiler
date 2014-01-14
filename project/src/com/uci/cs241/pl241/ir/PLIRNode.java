package com.uci.cs241.pl241.ir;

public abstract class PLIRNode
{
	private PLIRNode left;
	private PLIRNode right;
	private PLIRNode parent;
	
	
	
	public PLIRNode()
	{
		this.left = this.right = this.parent = null;
	}
	
	public PLIRNode(PLIRNode left, PLIRNode right, PLIRNode parent)
	{
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
