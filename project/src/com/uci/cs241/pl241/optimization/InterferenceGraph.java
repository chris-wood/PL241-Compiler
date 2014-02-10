package com.uci.cs241.pl241.optimization;

import java.util.ArrayList;
import java.util.HashMap;

import com.uci.cs241.pl241.ir.PLIRInstruction;

public class InterferenceGraph 
{
	public HashMap<Integer, ArrayList<Integer>> adjList;
	
	public InterferenceGraph(ArrayList<PLIRInstruction> instructions)
	{
		adjList = new HashMap<Integer, ArrayList<Integer>>();
		for (PLIRInstruction inst : instructions)
		{
			adjList.put(inst.id, new ArrayList<Integer>());
		}
	}
	
	public int getNeighborCount(int v)
	{
		return adjList.get(v).size();
	}
	
	public int getVertex(int neighborCount)
	{
		for (Integer u : adjList.keySet())
		{
			if (adjList.get(u).size() < neighborCount)
			{
				return u;
			}
		}
		return -1;
	}
	
	public void removeVertex(int v)
	{
		
	}
	
	public void addEdge(int u, int v)
	{
		if (u == 0 || v == 0)
		{
			System.err.println("here");
		}
		
		if (adjList.containsKey(u))
		{
			for (Integer t : adjList.get(u))
			{
				if (t == v) return; // already adjacenct
			}
		}
		if (adjList.containsKey(v))
		{
			for (Integer t : adjList.get(v))
			{
				if (t == u) return; // already adjacenct
			}
		}
		
		System.out.println("Adding: " + u + "," + v);
		adjList.get(u).add(v);
		adjList.get(v).add(u);
	}
	
	public void displayEdges()
	{
		System.out.println("Displaying the edge set: ");
		for (Integer u : adjList.keySet())
		{
			System.out.print(u + ": ");
			for (Integer v : adjList.get(u))
			{
				System.out.println(v + ", ");
			}
			System.out.println();
		}
	}
}
