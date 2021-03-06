package com.uci.cs241.pl241.visualization;

import java.util.ArrayList;
import java.util.HashMap;

import com.uci.cs241.pl241.ir.PLIRBasicBlock;
import com.uci.cs241.pl241.optimization.Edge;
import com.uci.cs241.pl241.optimization.InterferenceGraph;

public class GraphvizRender 
{	
	private String prefix = "BB";
	
	public String renderBasicBlockIR(PLIRBasicBlock block, ArrayList<Integer> seen)
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("\n" + prefix + block.id + "[shape = box, label = \"BB(" + block.id); 
		if (block.label.length() > 0)
		{
			builder.append(" [ " + block.label + " ]");
		}
		builder.append(")\\n\"");
		if (block.instructions.size() > 0)
		{
			ArrayList<String> instSeq = block.instSequence(seen);
			if (instSeq.size() > 0)
			{
				builder.append(" + ");
				for (int i = 0; i < instSeq.size() - 1; i++)
				{
					builder.append("\"" + instSeq.get(i) + "\\n\" + ");
				}
				builder.append("\"" + instSeq.get(instSeq.size() - 1) + "\\n\"");
			}
		}
		builder.append("];");
		
		return builder.toString();
	}
	
	public String renderCFG(ArrayList<PLIRBasicBlock> blocks)
	{
		StringBuilder builder = new StringBuilder();
		ArrayList<Integer> seen = new ArrayList<Integer>(); 
		
		// CFG DAG start
		builder.append("digraph cfg {");
		
		for (PLIRBasicBlock entry : blocks)
		{
			// Walk each basic block in a DFS manner and have each one generate it's textual representation
			ArrayList<PLIRBasicBlock> queue = new ArrayList<PLIRBasicBlock>();
			ArrayList<PLIRBasicBlock> visited = new ArrayList<PLIRBasicBlock>();
			queue.add(entry);
			StringBuilder cfgBuilder = new StringBuilder();
			while (queue.isEmpty() == false)
			{
				PLIRBasicBlock currBlock = queue.remove(0);
				if (visited.contains(currBlock) == false)
				{
					visited.add(currBlock);
					builder.append(renderBasicBlockIR(currBlock, seen));
					if (currBlock.leftChild != null)
					{
						cfgBuilder.append(prefix + currBlock.id + " -> " + prefix + currBlock.leftChild.id + ";\n");
						queue.add(currBlock.leftChild);
					}
					if (currBlock.rightChild != null)
					{
						cfgBuilder.append(prefix + currBlock.id + " -> " + prefix + currBlock.rightChild.id + ";\n");
						queue.add(currBlock.rightChild);
					}
				}
			}
			
			// Write down the connections
			builder.append("\n" + cfgBuilder.toString());
		}
		
		// CFG DAG end
		builder.append("}");
		
		return builder.toString();
	}
	
	public String renderDominatorTree(ArrayList<PLIRBasicBlock> blocks)
	{
		StringBuilder builder = new StringBuilder();
		ArrayList<Integer> seen = new ArrayList<Integer>();
		
		// Tree start
		builder.append("digraph dom {");
		
		for (PLIRBasicBlock entry : blocks)
		{
			// Walk each basic block in a DFS manner and have each one generate it's textual representation
			ArrayList<PLIRBasicBlock> queue = new ArrayList<PLIRBasicBlock>();
			ArrayList<PLIRBasicBlock> visited = new ArrayList<PLIRBasicBlock>();
			queue.add(entry);
			ArrayList<String> dotBuilder = new ArrayList<String>();
			while (queue.isEmpty() == false)
			{
				PLIRBasicBlock currBlock = queue.remove(0);
				if (visited.contains(currBlock) == false)
				{
					visited.add(currBlock);
					builder.append(renderBasicBlockIR(currBlock, seen));
					for (PLIRBasicBlock child : currBlock.dominatorSet)
					{
						String toAdd = "\n" + prefix + currBlock.id + " -> " + prefix + child.id + ";";
						if (dotBuilder.contains(toAdd) == false)
						{
							dotBuilder.add(toAdd);
						}
						queue.add(child);
					}
				}
			}
			
			// Write down the connections
			for (String s : dotBuilder)
			{
				builder.append(s);
			}
		}
		
		// CFG DAG end
		builder.append("\n}");
		
		return builder.toString();
	}
	
	public String renderInterferenceGraph(InterferenceGraph graph, HashMap<Integer, Integer> regMap)
	{
		StringBuilder builder = new StringBuilder();
		builder.append("graph ig {\n");
		builder.append("    node [shape = circle];\n");
		
		// Add the nodes (all SSA instructions in the program)
		for (Integer u : graph.adjList.keySet())
		{
			builder.append("    N" + u + "R" + regMap.get(u) + ";\n");
		}
		
		// Add the edge connections
		for (Edge e : graph.getEdges())
		{
			builder.append("    N" + e.u + "R" + regMap.get(e.u) + " -- N" + e.v + "R" + regMap.get(e.v) + ";\n");
		}
		
		builder.append("}\n");
		return builder.toString();
	}
}
