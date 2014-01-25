package com.uci.cs241.pl241.visualization;

import java.util.ArrayList;

import com.uci.cs241.pl241.ir.PLIRBasicBlock;

// Sample graph
//digraph G {
//
//	subgraph cluster_0 {
//		style=filled;
//		color=lightgrey;
//		node [style=filled,color=white];
//		a0 -> a1 -> a2 -> a3;
//		label = "process #1";
//	}
//
//	subgraph cluster_1 {
//		node [style=filled];
//		b0 -> b1 -> b2 -> b3;
//		label = "process #2";
//		color=blue
//	}
//	start -> a0;
//	start -> b0;
//	a1 -> b3;
//	b2 -> a3;
//	a3 -> a0;
//	a3 -> end;
//	b3 -> end;
//
//	start [shape=Mdiamond];
//	end [shape=Msquare];
//}

public class GraphvizRender 
{	
	public String renderBasicBlockIR(PLIRBasicBlock block, int id)
	{
		StringBuilder builder = new StringBuilder();
		
		// Subgraph start
		builder.append("subgraph " + id + "{\n");
		builder.append("");
		
		
		// Subgraph end
		builder.append("}\n");
		
		return builder.toString();
	}
	
	public String renderCFG(PLIRBasicBlock entry)
	{
		StringBuilder builder = new StringBuilder();
		
		// CFG DAG start
		builder.append("digraph G {");
		
		// Walk each basic block in a DFS manner and have each one generate it's textual representation
		ArrayList<PLIRBasicBlock> queue = new ArrayList<PLIRBasicBlock>();
		ArrayList<PLIRBasicBlock> visited = new ArrayList<PLIRBasicBlock>();
		queue.add(entry);
		int id = 0;
		while (queue.isEmpty() == false)
		{
			PLIRBasicBlock currBlock = queue.remove(0);
			if (visited.contains(currBlock) == false)
			{
				visited.add(currBlock);
				builder.append(renderBasicBlockIR(currBlock, id++));
				for (PLIRBasicBlock child : currBlock.children)
				{
					queue.add(child);
				}
			}
		}
		
		
		// CFG DAG end
		builder.append("}");
		
		return builder.toString();
	}
}
