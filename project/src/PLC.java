import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
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
import com.uci.cs241.pl241.backend.DLXInstruction.InstructionType;
import com.uci.cs241.pl241.frontend.Function;
import com.uci.cs241.pl241.frontend.PLEndOfFileException;
import com.uci.cs241.pl241.frontend.PLParser;
import com.uci.cs241.pl241.frontend.PLScanner;
import com.uci.cs241.pl241.frontend.PLSyntaxErrorException;
import com.uci.cs241.pl241.frontend.PLParser.IdentType;
import com.uci.cs241.pl241.frontend.ParserException;
import com.uci.cs241.pl241.ir.PLIRBasicBlock;
import com.uci.cs241.pl241.ir.PLIRInstruction;
import com.uci.cs241.pl241.ir.PLStaticSingleAssignment;
import com.uci.cs241.pl241.optimization.CSE;
import com.uci.cs241.pl241.optimization.InterferenceGraph;
import com.uci.cs241.pl241.optimization.RegisterAllocator;
import com.uci.cs241.pl241.visualization.GraphvizRender;


public class PLC
{
	public static void main(String[] args) throws IOException, PLSyntaxErrorException, PLEndOfFileException, ParseException, ParserException
	{
		// Setup command line argument parser
		Options options = new Options();
		options.addOption("f", true, "input file");
		options.addOption("out", true, "output directory");
		options.addOption("all", false, "run all compilation steps");
		options.addOption("s1", false, "step 1: parsing and SSA generation");
		options.addOption("s2", false, "step 2: CSE and copy propagation");
		options.addOption("s3", false, "step 3: register allocation");
		options.addOption("s4", false, "step 4: code generation");
		
		// Parse the command line arguments
		CommandLineParser cmdParser = new GnuParser();
		CommandLine cmd = cmdParser.parse( options, args);
		
		// Handle parsing
		String outPath = "./";
		if (cmd.hasOption("out"))
		{
			outPath = cmd.getOptionValue("out");
			System.out.println("Exporting results to: " + outPath);
		}
		
		if (cmd.hasOption("f")) 
		{
			System.out.println("Running on: " + cmd.getOptionValue("f"));
		}
		else
		{
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("see the options below", options );
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
						ArrayList<PLIRInstruction> toRemove = new ArrayList<PLIRInstruction>(); 
						for (int i = 0; i < curr.instructions.size(); i++)
						{
							if (PLStaticSingleAssignment.isIncluded(curr.instructions.get(i).id) == false || 
									seenInst.contains(curr.instructions.get(i).id) || curr.instructions.get(i).id == 0)
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
			
			// Now rename everything so that we're in good shape
//			PLStaticSingleAssignment.globalSSAIndex = 0;
//			for (PLIRBasicBlock entry : blocks)
//			{
//				
//			}
			
			// Display the instructions BEFORE CSE
			PLIRBasicBlock root = blocks.get(blocks.size() - 1);
			System.out.println("\nBegin Instructions\n");
			PrintWriter instWriter = new PrintWriter(new BufferedWriter(new FileWriter(outPath + "/" + sourceFile + "_inst_preCSE.txt")));
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
//					cse.performCSE(entry);
				}
				
				// Display the instructions AFTER CSE
				root = blocks.get(blocks.size() - 1);
				System.out.println("\nBegin Instructions\n");
				instWriter = new PrintWriter(new BufferedWriter(new FileWriter(outPath + "/" + sourceFile + "_inst_postCSE.txt")));
				PLStaticSingleAssignment.displayInstructions();
				instWriter.println(PLStaticSingleAssignment.renderInstructions());
				instWriter.flush();
				instWriter.close();
				System.out.println("End Instructions\n");
				
				// Display the DU chain
//				System.out.println("\nDU chain");
//				for (PLIRInstruction def : parser.duChain.keySet())
//				{
//					System.out.println(def.id + " := " + def.toString());
//					for (PLIRInstruction use : parser.duChain.get(def))
//					{
//						System.out.println("\t" + use.id + " := " + use.toString());
//					}
//				}
//				System.out.println("End DU chain\n");
			}
			
			// Generate visualization strings
			GraphvizRender render = new GraphvizRender();
			String cfgdot = render.renderCFG(blocks);
			String domdot = render.renderDominatorTree(blocks);
			
			// Write out the CFG string
			PrintWriter cfgWriter = new PrintWriter(new BufferedWriter(new FileWriter(outPath + "/" + sourceFile + ".cfg.dot")));
			cfgWriter.println(cfgdot);
			cfgWriter.flush();
			cfgWriter.close();
			
			// Write out the dominator tree
			PrintWriter domWriter = new PrintWriter(new BufferedWriter(new FileWriter(outPath + "/" + sourceFile + ".dom.dot")));
			domWriter.println(domdot);
			domWriter.flush();
			domWriter.close();
			
			RegisterAllocator ra = null;
			if (runAll || (runStep1 && runStep2 && runStep3))
			{
				// Create the IG from the initial set of instructions
				InterferenceGraph ig = new InterferenceGraph(PLStaticSingleAssignment.instructions);
				
				// Register allocation on each block
				ra = new RegisterAllocator(ig, parser.scope);
				
				// Compute live range of each function body, including main
				for (PLIRBasicBlock block : blocks)
				{
					ra.ComputeLiveRange(block);
				}
				
				// Perform allocation via coloring (with spilling)
				ra.Color(ig);
				String igdot = render.renderInterferenceGraph(ig, ra.regMap);
				PrintWriter igWriter = new PrintWriter(new BufferedWriter(new FileWriter(outPath + "/" + sourceFile + ".ig.dot")));
				igWriter.println(igdot);
				igWriter.flush();
				igWriter.close();
			}
			
			if (runAll || (runStep1 && runStep2 && runStep3 && runStep4))
			{	
				int mainStart = 0;
				
				HashMap<Integer, Integer> globalOffset = new HashMap<Integer, Integer>();
				HashMap<String, Integer> globalArrayOffset = new HashMap<String, Integer>();
				HashMap<String, Integer> globalRefMap = new HashMap<String, Integer>();
				int globalIndex = 0;
				for (PLIRInstruction inst : parser.globalVariables.values())
				{
					if (parser.identTypeMap.get(inst.origIdent) == IdentType.ARRAY)
					{
						int dimensions = 1;
						for (Integer d : parser.arrayDimensionMap.get(inst.origIdent))
						{
							dimensions *= d;
						}
						globalIndex += dimensions;
						globalArrayOffset.put(inst.origIdent, globalIndex);
					}
					else
					{
						globalOffset.put(inst.id, globalIndex++);
					}
					globalRefMap.put(inst.origIdent, inst.id);
				}
				
				DLXGenerator dlxGen = new DLXGenerator(globalOffset, globalArrayOffset, globalRefMap, ra.constants, ra.arrays);
				
				ArrayList<ArrayList<DLXInstruction>> program = new ArrayList<ArrayList<DLXInstruction>>();
				for (int i = 0; i < blocks.size(); i++)
				{	
					PLIRBasicBlock block = blocks.get(i);
					
					boolean isFunc = false;
					boolean isProc = false;
					boolean isMain = true;
					Function func = null;
					if (block.label != null && block.label.length() > 0)
					{
						isFunc = parser.funcFlagMap.get(block.label);
						isProc = !isFunc;
						isMain = false;
						func = parser.scope.functions.get(block.label);
						func.hasReturn = isFunc;
					}
					
					// Save the start of the program so we can jump there later
					if (isMain)
					{
						mainStart = dlxGen.pc; 
					}
					
					dlxGen.exitBlock = null;
					DLXBasicBlock db = dlxGen.generateBlockTree(null, block, new HashSet<Integer>());
					if (!isMain)
					{
						dlxGen.setupStack(db, block, func);
					}
					dlxGen.generateBlockTreeInstructons(db, block, func, isMain, new HashSet<Integer>());
					if (!isMain && !func.hasReturn)
					{
						dlxGen.tearDownStack(db, block, func);
					}
					
					HashSet<Integer> visited = new HashSet<Integer>();
					ArrayList<DLXInstruction> dlxInstructions = dlxGen.convertToStraightLineCode(db, func, new ArrayList<Integer>(), visited);
					System.out.println(visited);
					
					if (!isMain && !func.hasReturn)
					{
						DLXInstruction retInst = new DLXInstruction();
						retInst.opcode = InstructionType.RET;
						retInst.format = dlxGen.formatMap.get(InstructionType.RET);
						retInst.ra = 0;
						retInst.rb = 0;
						retInst.rc = dlxGen.RA; // jump to RA
						dlxGen.appendInstructionToBlock(dlxGen.exitBlock, retInst);
						dlxInstructions.add(retInst);
					}
					
					dlxGen.fixup(dlxInstructions);
					program.add(dlxInstructions);
					
					// Update the address table
					dlxGen.functionAddressTable.put(block.label, dlxGen.pc - dlxInstructions.size());
					for (DLXInstruction inst : dlxInstructions)
					{
						if (inst.jumpNeedsFix)
						{
							int addr = dlxGen.functionAddressTable.get(inst.refFunc);
							inst.rc = addr * 4;
							inst.encodedForm = dlxGen.encodeInstruction(inst);
						}
						System.out.println(inst);
					}
					
				}
				
				// Flatten the lists into one SLP
				ArrayList<DLXInstruction> slp = new ArrayList<DLXInstruction>();
				for (ArrayList<DLXInstruction> list : program)
				{
					for (DLXInstruction inst : list)
					{
						slp.add(inst);
					}
				}
				
				// Insert jump to the start of the program
				DLXInstruction mainJump = new DLXInstruction();
				mainJump.opcode = InstructionType.JSR;
				mainJump.ra = mainJump.rb = 0;
				mainJump.rc = 4 * mainStart;
				mainJump.format = dlxGen.formatMap.get(InstructionType.JSR);
				mainJump.encodedForm = dlxGen.encodeInstruction(mainJump);
				mainJump.pc = 2;
				slp.add(0, mainJump);
				
				// Initialize SP and FP (FP points SP to start)
				DLXInstruction fpInit = new DLXInstruction();
				fpInit.opcode = InstructionType.ADDI;
				fpInit.ra = dlxGen.FP;
				fpInit.rb = 0;
				fpInit.rc = (slp.size() + 2) * 4;
				fpInit.format = dlxGen.formatMap.get(InstructionType.ADDI);
				fpInit.encodedForm = dlxGen.encodeInstruction(fpInit);
				fpInit.pc = 1;
				slp.add(0, fpInit);
				
				// Initialize SP and FP (FP points SP to start)
				DLXInstruction spInit = new DLXInstruction();
				spInit.opcode = InstructionType.ADDI;
				spInit.ra = dlxGen.SP;
				spInit.rb = 0;
				spInit.rc = (slp.size() + 1) * 4;
				spInit.format = dlxGen.formatMap.get(InstructionType.ADDI);
				spInit.encodedForm = dlxGen.encodeInstruction(spInit);
				spInit.pc = 0;
				slp.add(0, spInit);
				
				// Write the DLX machine code
				System.out.println("\n\n--FINAL PROGARM--\n");
				PrintWriter dlxWriter = new PrintWriter(new BufferedWriter(new FileWriter(outPath + "/" + sourceFile + ".dlx")));
				PrintWriter mnemonicDlxWriter = new PrintWriter(new BufferedWriter(new FileWriter(outPath + "/" + sourceFile + ".dlxm")));
				for (DLXInstruction inst : slp)
				{
					dlxWriter.println(inst.encodedForm);
					System.out.println(inst);// + ", " + Long.toHexString(inst.encodedForm));
					mnemonicDlxWriter.println(inst);
				}
				mnemonicDlxWriter.flush();
				mnemonicDlxWriter.close();
				dlxWriter.flush();
				dlxWriter.close();
				
				System.out.println("\n--COMPILATION COMPLETE--");
			}
		}
	}
}
