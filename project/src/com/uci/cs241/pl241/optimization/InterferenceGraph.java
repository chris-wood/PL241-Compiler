package com.uci.cs241.pl241.optimization;

import java.util.ArrayList;
import java.util.HashMap;

import com.uci.cs241.pl241.ir.PLIRInstruction;

public class InterferenceGraph 
{
	public HashMap<Integer, ArrayList<Integer>> adjList;
	public HashMap<Integer, Integer> costMap;
	
	public InterferenceGraph(ArrayList<PLIRInstruction> instructions)
	{
		costMap = new HashMap<Integer, Integer>();
		adjList = new HashMap<Integer, ArrayList<Integer>>();
		for (PLIRInstruction inst : instructions)
		{
			adjList.put(inst.id, new ArrayList<Integer>());
			System.out.println("IG: " + inst);
			costMap.put(inst.id, inst.cost);
		}
	}
	
	public ArrayList<Edge> getEdges()
	{
		ArrayList<Edge> edges = new ArrayList<Edge>();
		
		for (Integer u : adjList.keySet())
		{
			for (Integer v : adjList.get(u))
			{
				boolean found = false;
				for (Edge e : edges)
				{
					if ((e.u == u && e.v == v) || (e.v == u && e.u == v))
					{
						found = true;
						break;
					}
				}
				if (!found)
				{
					edges.add(new Edge(u, v));
				}
			}
		}
		
		return edges;
	}
	
	public int getCost(int v)
	{
		return costMap.get(v);
	}
	
	public int getSmallestCost()
	{
		int smallest = Integer.MAX_VALUE;
		for (Integer v : costMap.keySet())
		{
			if (costMap.get(v) < smallest)
			{
				smallest = v;
			}
		}
		return smallest;
	}
	
	public int getVertexWithMaxDegree(int maxDegree)
	{
		for (Integer v : adjList.keySet())
		{
			if (adjList.get(v).size() < maxDegree)
			{
				return v;
			}
		}
		return -1;
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
	
	public ArrayList<Integer> removeVertex(int v)
	{
//		System.err.println("Removing vertex: " + v);
		ArrayList<Integer> neighbors = adjList.get(v);
		
		// Drop v from the set
		adjList.remove(v);
		
		// Remove all edges that were incident to v
		for (Integer u : neighbors)
		{
			for (int n = 0; n < adjList.get(u).size(); n++)
			{
				if (v == adjList.get(u).get(n))
				{
					adjList.get(u).remove(n);
				}
			}
		}
		
		return neighbors;
	}
	
	public void addVertex(int v, ArrayList<Integer> neighbors)
	{
		adjList.put(v, neighbors);
		for (Integer n : neighbors)
		{
			adjList.get(n).add(v);
		}
	}
	
	public boolean isEmpty()
	{
		return adjList.keySet().isEmpty();
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
				if (t == v) return; // already adjacent
			}
		}
		if (adjList.containsKey(v))
		{
			for (Integer t : adjList.get(v))
			{
				if (t == u) return; // already adjacent
			}
		}
		
//		System.out.println("Adding: " + u + "," + v);
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
