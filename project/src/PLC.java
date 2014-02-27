import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.SystemMenuBar;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.uci.cs241.pl241.backend.DLXBasicBlock;
import com.uci.cs241.pl241.backend.DLXGenerator;
import com.uci.cs241.pl241.backend.DLXInstruction;
import com.uci.cs241.pl241.frontend.PLEndOfFileException;
import com.uci.cs241.pl241.frontend.PLParser;
import com.uci.cs241.pl241.frontend.PLScanner;
import com.uci.cs241.pl241.frontend.PLSyntaxErrorException;
import com.uci.cs241.pl241.ir.PLIRBasicBlock;
import com.uci.cs241.pl241.ir.PLIRInstruction;
import com.uci.cs241.pl241.ir.PLStaticSingleAssignment;
import com.uci.cs241.pl241.optimization.CSE;
import com.uci.cs241.pl241.optimization.InterferenceGraph;
import com.uci.cs241.pl241.optimization.RegisterAllocator;
import com.uci.cs241.pl241.visualization.GraphvizRender;


public class PLC
{
	public static void main(String[] args) throws IOException, PLSyntaxErrorException, PLEndOfFileException, ParseException
	{
		// Setup command line argument parser
		Options options = new Options();
		options.addOption("f", true, "input file");
		options.addOption("all", false, "run all compilation steps");
		options.addOption("s1", false, "step 1: parsing and SSA generation");
		options.addOption("s2", false, "step 2: CSE and copy propagation");
		options.addOption("s3", false, "step 3: register allocation");
		options.addOption("s4", false, "step 4: code generation");
		
		// Parse the command line arguments
		CommandLineParser cmdParser = new GnuParser();
		CommandLine cmd = cmdParser.parse( options, args);
		
		// Handle parsing
		if (cmd.hasOption("f")) 
		{
			System.out.println("Running on: " + cmd.getOptionValue("f"));
		}
		else
		{
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("ant", options );
			System.exit(-1);
		}
		
		// Extract pass flags
		boolean runAll = cmd.hasOption("all");
		boolean runStep1 = cmd.hasOption("s1");
		boolean runStep2 = cmd.hasOption("s2");
		boolean runStep3 = cmd.hasOption("s3");
		boolean runStep4 = cmd.hasOption("s4");
		
		// Extract source file
		String sourceFile = cmd.getOptionValue("f");
		
		// Maintain order of compilation - steps 1, 2, 3, and 4 must occur in order
		if (runAll || runStep1 || runStep2 || runStep3 || runStep4)
		{
			PLScanner scanner = new PLScanner(sourceFile);
			PLParser parser = new PLParser();
			ArrayList<PLIRBasicBlock> blocks = parser.parse(scanner);
			
			// Filter basic blocks...
			for (PLIRBasicBlock entry : blocks)
			{
				HashSet<Integer> seenInst = new HashSet<Integer>();
				HashSet<PLIRBasicBlock> seenBlocks = new HashSet<PLIRBasicBlock>();
				ArrayList<PLIRBasicBlock> stack = new ArrayList<PLIRBasicBlock>();
				stack.add(entry);
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
						if (curr.leftChild != null)
						{
							stack.add(curr.leftChild);
						}
						if (curr.rightChild != null)
						{
							stack.add(curr.rightChild);
						}
					}
				}
			}
			
			// Display the instructions BEFORE CSE
			PLIRBasicBlock root = blocks.get(blocks.size() - 1);
			System.out.println("\nBegin Instructions\n");
			PrintWriter instWriter = new PrintWriter(new BufferedWriter(new FileWriter(sourceFile + "_inst_preCSE.txt")));
			PLStaticSingleAssignment.displayInstructions();
			instWriter.println(PLStaticSingleAssignment.renderInstructions());
			instWriter.flush();
			instWriter.close();
			System.out.println("End Instructions\n");
			
			if (runAll || runStep1 || runStep2 || runStep3 || runStep4)
			{
				// Perform CSE on each block
				for (PLIRBasicBlock entry : blocks)
				{
					// Find the root by walking up the tree in any direction
					while (entry.parents.isEmpty() == false)
					{
						entry = entry.parents.get(0);
					}
					
					// Perform CSE, starting at the root
					CSE cse = new CSE();
					cse.performCSE(entry);
				}
				
				// Display the instructions AFTER CSE
				root = blocks.get(blocks.size() - 1);
				System.out.println("\nBegin Instructions\n");
				instWriter = new PrintWriter(new BufferedWriter(new FileWriter(sourceFile + "_inst_postCSE.txt")));
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
			}
			
			// Generate visualization strings
			GraphvizRender render = new GraphvizRender();
			String cfgdot = render.renderCFG(blocks);
			String domdot = render.renderDominatorTree(blocks);
			
			// Write out the CFG string
			PrintWriter cfgWriter = new PrintWriter(new BufferedWriter(new FileWriter(sourceFile + ".cfg.dot")));
			cfgWriter.println(cfgdot);
			cfgWriter.flush();
			cfgWriter.close();
			
			// Write out the dominator tree
			PrintWriter domWriter = new PrintWriter(new BufferedWriter(new FileWriter(sourceFile + ".dom.dot")));
			domWriter.println(domdot);
			domWriter.flush();
			domWriter.close();
			
			if (runAll || (runStep1 && runStep2 && runStep3))
			{
				// Register allocation
				root = blocks.get(blocks.size() - 1);
				RegisterAllocator ra = new RegisterAllocator();
				ra.ComputeLiveRange(root);
				InterferenceGraph ig = ra.ig;
				ra.Color(ra.ig);
				String igdot = render.renderInterferenceGraph(ig, ra.regMap);
				PrintWriter igWriter = new PrintWriter(new BufferedWriter(new FileWriter(sourceFile + ".ig.dot")));
				igWriter.println(igdot);
				igWriter.flush();
				igWriter.close();
			}
			
			if (runAll || (runStep1 && runStep2 && runStep3 && runStep4))
			{
				DLXGenerator dlxGen = new DLXGenerator();
				dlxGen.populateGlobalAddressTable(parser.globalVariables);
				for (PLIRBasicBlock block : blocks)
				{
					DLXBasicBlock db = dlxGen.generateBlockTree(null, block, new HashSet<Integer>());
					dlxGen.generateBlockTreeInstructons(db, block, 0, new HashSet<Integer>());
					ArrayList<DLXInstruction> dlxInstructions = dlxGen.convertToStraightLineCode(db, -1, new HashSet<Integer>());
					dlxGen.fixup(dlxInstructions);
					for (DLXInstruction inst : dlxInstructions)
					{
//						System.out.println(Long.toHexString(inst.encodedForm));
						System.out.println(inst);
					}
					
					// Write the DLX machine code
					PrintWriter dlxWriter = new PrintWriter(new BufferedWriter(new FileWriter(sourceFile + ".dlx")));
					for (DLXInstruction inst : dlxInstructions)
					{
						dlxWriter.println(inst.encodedForm);
					}
					dlxWriter.flush();
					dlxWriter.close();
				}
				
				System.out.println("done");
			}
		}
	}
}
