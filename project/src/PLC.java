import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;

import com.uci.cs241.pl241.frontend.PLEndOfFileException;
import com.uci.cs241.pl241.frontend.PLParser;
import com.uci.cs241.pl241.frontend.PLScanner;
import com.uci.cs241.pl241.frontend.PLSyntaxErrorException;
import com.uci.cs241.pl241.frontend.PLTokenizer;
import com.uci.cs241.pl241.ir.PLIRBasicBlock;
import com.uci.cs241.pl241.ir.PLIRInstruction;
import com.uci.cs241.pl241.ir.PLStaticSingleAssignment;
import com.uci.cs241.pl241.optimization.CSE;
import com.uci.cs241.pl241.optimization.InterferenceGraph;
import com.uci.cs241.pl241.optimization.RegisterAllocator;
import com.uci.cs241.pl241.visualization.GraphvizRender;


public class PLC
{
	public static void main(String[] args) throws IOException, PLSyntaxErrorException, PLEndOfFileException
	{
		if (args.length != 1)
		{
			System.err.println("usage: PLC <program.txt>");
			System.exit(-1);
		}
		
		// Scanner test
		PLScanner scanner = new PLScanner(args[0]);
		ArrayList<String> tokens = new ArrayList<String>();
		try
		{
			while (true)
			{
				scanner.next();
				tokens.add(scanner.symstring + "(" + scanner.sym + ")");
			}
		} 
		catch (Exception e)
		{	
		}
		
		// Format and display the tokens
		StringBuilder builder = new StringBuilder("[");
		for (String t : tokens)
		{
			builder.append("'" + t + "', ");
		}
		builder.deleteCharAt(builder.toString().length() - 1).append("]");
		System.out.println(builder.toString());
		
		//// RUN THE PARSER
		scanner = new PLScanner(args[0]);
		PLParser parser = new PLParser();
		ArrayList<PLIRBasicBlock> blocks = parser.parse(scanner);
		
		// Recover the main root
		PLIRBasicBlock root = blocks.get(blocks.size() - 1);
		
		// Filter basic blocks...
		HashSet<Integer> seenInst = new HashSet<Integer>();
		HashSet<PLIRBasicBlock> seenBlocks = new HashSet<PLIRBasicBlock>();
		ArrayList<PLIRBasicBlock> stack = new ArrayList<PLIRBasicBlock>();
		stack.add(root);
		while (stack.isEmpty() == false)
		{
			PLIRBasicBlock curr = stack.get(0);
			stack.remove(0);
			if (seenBlocks.contains(curr) == false)
			{
				seenBlocks.add(curr);
				HashSet<PLIRInstruction> toRemove = new HashSet<PLIRInstruction>(); 
				for (int i = 0; i < curr.instructions.size(); i++)
				{
					if (PLStaticSingleAssignment.isIncluded(curr.instructions.get(i).id) == false || 
							seenInst.contains(curr.instructions.get(i).id))
					{
						toRemove.add(curr.instructions.get(i));
					}
					else
					{
						seenInst.add(curr.instructions.get(i).id);
					}
				}
				for (PLIRInstruction inst : toRemove)
				{
					curr.instructions.remove(inst);
				}
				
				// Push on children
				for (PLIRBasicBlock child : curr.children)
				{
					stack.add(child);
				}
			}
		}
		
		root = blocks.get(blocks.size() - 1);
		for (PLIRBasicBlock block : blocks)
		{
			// Find the root by walking up the tree in any direction
			root = block;
			while (root.parents.isEmpty() == false)
			{
				root = root.parents.get(0);
			}
			
			// Perform CSE, starting at the root
			CSE cse = new CSE();
			cse.performCSE(root);
		}
		
		// Display the instructions
		root = blocks.get(blocks.size() - 1);
		System.out.println("\nBegin Instructions\n");
		PrintWriter instWriter = new PrintWriter(new BufferedWriter(new FileWriter(args[0] + "_inst")));
		PLStaticSingleAssignment.displayInstructions();
		instWriter.println(PLStaticSingleAssignment.renderInstructions());
		instWriter.flush();
		instWriter.close();
		System.out.println("End Instructions\n");
		
		// Display the DU chain
		System.out.println("\nDU chain");
		for (PLIRInstruction def : parser.duChain.keySet())
		{
			System.out.println(def.id + " := " + def.toString());
			for (PLIRInstruction use : parser.duChain.get(def))
			{
				System.out.println("\t" + use.id + " := " + use.toString());
			}
		}
		System.out.println("End DU chain\n");
		
		// Walk the basic block and print out the contents
		ArrayList<PLIRBasicBlock> queue = new ArrayList<PLIRBasicBlock>();
		ArrayList<PLIRBasicBlock> visited = new ArrayList<PLIRBasicBlock>();
		ArrayList<Integer> seen = new ArrayList<Integer>();
		queue.add(root);
		while (queue.isEmpty() == false)
		{
			PLIRBasicBlock curr = queue.remove(0);
			if (visited.contains(curr) == false || curr.omit == true)
			{
				visited.add(curr);
				System.out.println("Visiting: " + curr.id);
				System.out.println(curr.instSequenceString(seen));
				
				for (PLIRBasicBlock child : curr.children)
				{
					queue.add(child);
				}
			}
		}
		
		// Generate visualization strings
		GraphvizRender render = new GraphvizRender();
		String cfgdot = render.renderCFG(root);
		String domdot = render.renderDominatorTree(root);
		
		// Write out the CFG string
		PrintWriter cfgWriter = new PrintWriter(new BufferedWriter(new FileWriter(args[0] + ".cfg.dot")));
		cfgWriter.println(cfgdot);
		cfgWriter.flush();
		cfgWriter.close();
		
		// Write out the dominator tree
		PrintWriter domWriter = new PrintWriter(new BufferedWriter(new FileWriter(args[0] + ".dom.dot")));
		domWriter.println(domdot);
		domWriter.flush();
		domWriter.close();
		
		//////// register allocation
//		root = blocks.get(blocks.size() - 1);
//		RegisterAllocator ra = new RegisterAllocator();
//		ra.ComputeLiveRange(root);
//		InterferenceGraph ig = ra.ig;
//		String igdot = render.renderInterferenceGraph(ig);
//		PrintWriter igWriter = new PrintWriter(new BufferedWriter(new FileWriter(args[0] + ".ig.dot")));
//		igWriter.println(igdot);
//		igWriter.flush();
//		igWriter.close();	
	}
}
