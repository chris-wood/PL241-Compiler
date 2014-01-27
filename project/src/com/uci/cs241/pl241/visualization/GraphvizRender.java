package com.uci.cs241.pl241.visualization;

import java.util.ArrayList;

import com.uci.cs241.pl241.ir.PLIRBasicBlock;
import com.uci.cs241.pl241.ir.PLIRInstruction;

// Sample graph
//digraph G {
//node_bb_0[shape=box, label = "7 := write #0\n" + "8 := beq #0 #2"]
//}


public class GraphvizRender 
{	
	public String renderBasicBlockIR(PLIRBasicBlock block)
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("\nbb_" + block.id + "[shape = box, label = \"BB(" + block.id + ")\\n\" + ");
		ArrayList<String> instSeq = block.instSequence();
		for (int i = 0; i < instSeq.size() - 1; i++)
		{
			builder.append("\"" + instSeq.get(i) + "\\n\" + ");
		}
		builder.append("\"" + instSeq.get(instSeq.size() - 1) + "\\n\"];");
		
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
		while (queue.isEmpty() == false)
		{
			PLIRBasicBlock currBlock = queue.remove(0);
			if (visited.contains(currBlock) == false)
			{
				visited.add(currBlock);
				builder.append(renderBasicBlockIR(currBlock));
				for (PLIRBasicBlock child : currBlock.children)
				{
					queue.add(child);
				}
			}
		}
		
		// TODO: walk the BBs again and write out the connections
		
		// CFG DAG end
		builder.append("\n}");
		
		return builder.toString();
	}
}
