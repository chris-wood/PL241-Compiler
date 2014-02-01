import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import com.uci.cs241.pl241.frontend.PLEndOfFileException;
import com.uci.cs241.pl241.frontend.PLParser;
import com.uci.cs241.pl241.frontend.PLScanner;
import com.uci.cs241.pl241.frontend.PLSyntaxErrorException;
import com.uci.cs241.pl241.frontend.PLTokenizer;
import com.uci.cs241.pl241.ir.PLIRBasicBlock;
import com.uci.cs241.pl241.ir.PLIRInstruction;
import com.uci.cs241.pl241.ir.PLStaticSingleAssignment;
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
		PLIRBasicBlock exit = parser.parse(scanner);
		
		// Find the root by walking up the tree in any direction
		PLIRBasicBlock root = exit;
		while (root.parents.isEmpty() == false)
		{
			root = root.parents.get(0);
		}
		
		// Display the instructions
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
		PrintWriter cfgWriter = new PrintWriter(new BufferedWriter(new FileWriter(args[0] + "_cfg.dot")));
		cfgWriter.println(cfgdot);
		cfgWriter.flush();
		cfgWriter.close();
		
		// Write out the dominator tree
		PrintWriter domWriter = new PrintWriter(new BufferedWriter(new FileWriter(args[0] + "_dom.dot")));
		domWriter.println(domdot);
		domWriter.flush();
		domWriter.close();	
	}
}
