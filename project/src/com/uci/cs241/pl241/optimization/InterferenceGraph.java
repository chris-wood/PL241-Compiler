package com.uci.cs241.pl241.optimization;

import java.util.ArrayList;

public class InterferenceGraph 
{
	public ArrayList<Edge> edgeSet;
	
	public InterferenceGraph()
	{
		edgeSet = new ArrayList<Edge>();
	}
	
	public void AddEdge(int u, int v)
	{
		if (u == 0 || v == 0)
		{
			System.err.println("here");
		}
		for (Edge e : edgeSet)
		{
			if (e.u == u && e.v == v)
			{
				return;
			}
		}
		System.out.println("Adding: " + u + "," + v);
		edgeSet.add(new Edge(u, v));
	}
	
	public void displayEdges()
	{
		System.out.println("Displaying the edge set: ");
		for (Edge e : edgeSet)
		{
			System.out.println(e.u + "," + e.v);
		}
	}
}
