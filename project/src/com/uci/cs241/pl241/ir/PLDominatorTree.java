package com.uci.cs241.pl241.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PLDominatorTree
{
	
	// Map from BB to integer ID to index into the arrays below
	HashMap<PLIRBasicBlock, Integer> nodeMap = new HashMap<PLIRBasicBlock, Integer>();
	HashMap<Integer, PLIRBasicBlock> idMap = new HashMap<Integer, PLIRBasicBlock>();
	
	// Compute the successor data structure via depth first search
	HashMap<PLIRBasicBlock, ArrayList<PLIRBasicBlock>> succ = new HashMap<PLIRBasicBlock, ArrayList<PLIRBasicBlock>>();
	HashMap<PLIRBasicBlock, ArrayList<PLIRBasicBlock>> pred = new HashMap<PLIRBasicBlock, ArrayList<PLIRBasicBlock>>();
	HashMap<PLIRBasicBlock, PLIRBasicBlock> dom = new HashMap<PLIRBasicBlock, PLIRBasicBlock>();
	
	// Data structures computed during the algorithm
	HashMap<PLIRBasicBlock, PLIRBasicBlock> parent = new HashMap<PLIRBasicBlock, PLIRBasicBlock>();
	HashMap<PLIRBasicBlock, Integer> semi = new HashMap<PLIRBasicBlock, Integer>();
	HashMap<PLIRBasicBlock, ArrayList<PLIRBasicBlock>> forest = new HashMap<PLIRBasicBlock, ArrayList<PLIRBasicBlock>>(); // adjacency list for the forest
	HashMap<PLIRBasicBlock, PLIRBasicBlock> forestAncestors = new HashMap<PLIRBasicBlock, PLIRBasicBlock>();
	HashMap<PLIRBasicBlock, PLIRBasicBlock> forestLabel = new HashMap<PLIRBasicBlock, PLIRBasicBlock>();
	HashMap<PLIRBasicBlock, ArrayList<PLIRBasicBlock>> bucket = new HashMap<PLIRBasicBlock, ArrayList<PLIRBasicBlock>>(); // adjacency list for the forest
	
//	PLIRBasicBlock[] pred; // = new PLIRBasicBlock[root.treeSize];
//	PLIRBasicBlock[] bucket; // = new PLIRBasicBlock[root.treeSize];
//	PLIRBasicBlock[] dom; // = new PLIRBasicBlock[root.treeSize];
	
	// FOr the DFS
	ArrayList<PLIRBasicBlock> visited = new ArrayList<PLIRBasicBlock>();
	int nid = 0;

	public PLDominatorTree()
	{
		// nothing for now
	}
	
	public void DomDFS(PLIRBasicBlock v)
	{
		nodeMap.put(v, nid);
		idMap.put(nid, v);
		visited.add(v);
		semi.put(v, nid);
		nid++;
		
		// Populate successors...
		succ.put(v, new ArrayList<PLIRBasicBlock>());
//		for (PLIRBasicBlock block : v.children)
//		{
//			succ.get(v).add(block);
//		}
		
		// Continue onwards
		for (PLIRBasicBlock w : succ.get(nid))
		{
			if (semi.containsKey(w) && semi.get(w) == 0)
			{
				parent.put(w, v);
				DomDFS(w);
			}
			
			// Add v to predecessor set of w
			if (pred.containsKey(w) == false) pred.put(w, new ArrayList<PLIRBasicBlock>());
			pred.get(w).add(v);
		}
	}
	
	// add the edge (v,w) to the forest
	public void DomLink(PLIRBasicBlock v, PLIRBasicBlock w)
	{
		if (forest.containsKey(v) == false)
		{
			forest.put(v, new ArrayList<PLIRBasicBlock>());
		}
		forest.get(v).add(w);
		
		// from the paper
		forestAncestors.put(w, v);
	}
	
	// If v is the root of a tree in the forest, return v. Otherwise, let r be the root of the tree in the forest which contains v. 
	// REturn any vertex u \not= r of minimum semi(u) on the path r -> v.
	public PLIRBasicBlock DomEval(PLIRBasicBlock v)
	{
		if (forestAncestors.containsKey(v) || forestAncestors.get(v) == null)
		{
			return v;
		}
		else
		{
			DomCompress(v);
			return forestLabel.get(v);
		}
	}
	
	public void DomCompress(PLIRBasicBlock v)
	{
		if (forestAncestors.containsKey(v) && forestAncestors.get(v) == null)
		{
			System.err.println("Error: ancestor of vertex was null in compress algorithm");
			System.exit(-1);
		}
		else
		{
			if (forestAncestors.get(forestAncestors.get(v)) != null)
			{
				DomCompress(forestAncestors.get(v));
				if (semi.get(forestLabel.get(forestAncestors.get(v))) < semi.get(forestLabel.get(v)))
				{
					forestLabel.put(v, forestLabel.get(forestAncestors.get(v)));
				}
				forestAncestors.put(v, forestAncestors.get(forestAncestors.get(v)));
			}
		}
	}
	
	// Use algorithm defined in [1]: "A Fast Algorithm for Finding Dominators in a Flowgraph" to compute the dominator tree
	public HashMap<PLIRBasicBlock, PLIRBasicBlock> generateDominatorTree(PLIRBasicBlock r)
	{
		// step 1
		nid = 0;
		for (PLIRBasicBlock block : r.treeVertexSet)
		{
			pred.put(block, new ArrayList<PLIRBasicBlock>());
			semi.put(block, 0);
		}
		visited = new ArrayList<PLIRBasicBlock>();
		DomDFS(r);
		
		// Steps 2/3 (p.129 of [1])
		for (int i = r.treeSize; i >= 2; i--)
		{
			// ***step 2
			PLIRBasicBlock w = idMap.get(i);
			for (PLIRBasicBlock v : pred.get(w))
			{
				PLIRBasicBlock u = DomEval(v);
				if (semi.get(u) < semi.get(w))
				{
					semi.put(w, semi.get(u));
				}
			}
			
			// Add w to the bucket
			PLIRBasicBlock vertex = idMap.get(semi.get(w));
			if (bucket.containsKey(vertex) == false)
			{
				bucket.put(vertex, new ArrayList<PLIRBasicBlock>());
			}
			bucket.get(vertex).add(w);
			
			// Link
			DomLink(parent.get(w), w);
			
			// ***step 3
			for (PLIRBasicBlock v : bucket.get(parent.get(w)))
			{
				bucket.get(parent.get(w)).remove(v);
				PLIRBasicBlock u = DomEval(v);
				
				if (semi.get(u) < semi.get(v))
				{
					dom.put(v, u);
				}
				else
				{
					dom.put(v,  parent.get(w));
				}
			}
		}
		
		// Step 4
		for (int i = 2; i <= r.treeSize; i++)
		{
			PLIRBasicBlock w = idMap.get(i);
			if (dom.get(w).equals(idMap.get(semi.get(w))) == false)
			{
				dom.put(w, dom.get(dom.get(w)));
			}
		}
		dom.put(r, null);
		
		return dom;
	}
}
