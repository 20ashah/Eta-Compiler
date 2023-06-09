package main.java.jdr299zdh5cew256ans96.assembly;

import main.java.jdr299zdh5cew256ans96.ir.IRFuncDecl;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.Iterator;

public class RegisterAllocation {

	public static class Node {
		String temp;
		String color;
		int degree;
		boolean precolored;

		public Node(String temp) {
			this.temp = temp;
			this.color = "";
			this.degree = -1;
		}

		public String toString() {
			return temp;
		}

		public int getDegree() {
			return degree;
		}

		public void decr() {
			if (this.degree > 0) {
				this.degree--;
			}
		}
	}

	public static class Edge {
		Node u;
		Node v;

		public Edge(Node u, Node v) {
			this.u = u;
			this.v = v;
		}

		public String toString() {
			return u + "->" + v;
		}

		public boolean equals(Node u, Node v) {
			return this.u == u && this.v == v;
		}
	}

	public static int memoryOffset = 8;
	public static HashMap<Node, Integer> memLocations = new HashMap<>();
	public static int numIterations = 1;
	public static ArrayList<String> TempList = new ArrayList<>();
	private static ArrayList<String> precoloredList = new ArrayList<>();
	private static HashMap<Node, Set<Node>> intGraph = new HashMap<>();
	private static Set<Edge> adjSet = new LinkedHashSet<>();
	private static Set<Node> nodes = new LinkedHashSet<>();
	private static Set<Node> initial = new LinkedHashSet<>();
	private static Set<Node> precolored = new LinkedHashSet<>();
	private static Set<Mov> worklistMoves = new LinkedHashSet<>();
	private static Set<Mov> frozenMoves = new LinkedHashSet<>();
	private static Set<Mov> coalescedMoves = new LinkedHashSet<>();
	private static HashMap<Node, Set<Mov>> moveList = new HashMap<>();
	private static Set<Mov> activeMoves = new LinkedHashSet<>();
	private static Set<Mov> constrainedMoves = new LinkedHashSet<>();
	private static Set<Node> spillWorklist = new LinkedHashSet<>();
	private static Set<Node> freezeWorklist = new LinkedHashSet<>();
	private static Set<Node> simplifyWorklist = new LinkedHashSet<>();
	private static Set<Node> spilledNodes = new LinkedHashSet<>();
	private static Set<Node> coalescedNodes = new LinkedHashSet<>();
	private static Set<Node> coloredNodes = new LinkedHashSet<>();
	private static Stack<Node> selectStack = new Stack<>();
	private static HashMap<Node, Node> alias = new HashMap<>();

	public static LinkedHashSet<String> x86colors = new LinkedHashSet<>(
			Arrays.asList(
					"rcx", "rbx",
//					"r8", "r9",
//					"r10", "r11",
					"r12", "r13",
					"r14", "r15"
//					"rdi", "rsi"
			)
	);
	private static final int K = x86colors.size();

	public static Assembly trivialAllocate(Assembly abstractAssembly) {
		ArrayList<Instruction> abstractAssemblyList = abstractAssembly.getInsns();
		ArrayList<Instruction> realAsmInsns = new ArrayList<>();
		for (Instruction insn : abstractAssemblyList) {
			ArrayList<Register> abstractOperands = insn.getAbstractTemps();

			ArrayList<String> abstractOperandsStrings = new ArrayList<>();
			for (Register r : abstractOperands) {
				abstractOperandsStrings.add(r.getReg());
			}

			addToTempList(abstractOperands);
			insn.allocateRegisters();

			ArrayList<Mov> moveToRegisters = new ArrayList<>();
			ArrayList<Mov> moveFromRegisters = new ArrayList<>();

			for (int i = 0; i < abstractOperands.size(); i++) {
				moveToRegisters.add(new Mov(new Register("r" + (10 + i)),
						new MemBinop(new Register("rbp"), "-",
								new Const(((TempList.indexOf(abstractOperandsStrings.get(i)) + 1) * 8)))));

			}

			for (int i = 0; i < abstractOperands.size(); i++) {
				moveFromRegisters.add(new Mov(
						new MemBinop(new Register("rbp"), "-",
								new Const(((TempList.indexOf(abstractOperandsStrings.get(i)) + 1) * 8))),
						new Register("r" + (10 + i))));
			}

			realAsmInsns.addAll(moveToRegisters);
			realAsmInsns.add(insn);
			realAsmInsns.addAll(moveFromRegisters);

		}

		return new Assembly(realAsmInsns);
	}

	private static void addToTempList(ArrayList<Register> abstractOperandList) {
		for (Register r : abstractOperandList) {
			if (!TempList.contains(r.getReg())) {
				TempList.add(r.getReg());
			}
		}
	}

	private static void addToPrecoloredList(ArrayList<Register> precoloredOperandList) {
		for (Register r : precoloredOperandList) {
			if (!precoloredList.contains(r.getReg())) {
				precoloredList.add(r.getReg());
			}
		}
	}

	public static Assembly registerAllocate(Assembly abstractAssembly) {
		if (numIterations == 1) {
			memLocations.clear();
			setTempList(abstractAssembly);
			initial = convertToNodes(TempList);
		}
		createAbstractCFG(abstractAssembly);
		liveVarAnalysis(abstractAssembly);
		buildInterferenceGraph(abstractAssembly);
		makeWorkList();
		while (!simplifyWorklist.isEmpty() || !worklistMoves.isEmpty() ||
				!freezeWorklist.isEmpty() || !spillWorklist.isEmpty()) {
			if (!simplifyWorklist.isEmpty()) {
				simplify();
			} else if (!worklistMoves.isEmpty()) {
				coalesce();
			} else if (!freezeWorklist.isEmpty()) {
				freeze();
			} else if (!spillWorklist.isEmpty()) {
				selectSpill();
			}
		}
		assignColors();
		if (!spilledNodes.isEmpty()) {
			rewriteProgram(abstractAssembly);
			numIterations++;
			registerAllocate(abstractAssembly);
		}
		allocateRegisters(abstractAssembly);
		clearData();
//		if (!abstractAssembly.getInsns().get(2).toString().contains("push rbx")) {
////			IRCompUnit.addedCallingConventions.add(abstractAssembly.getInsns().get(0).toString());
////			System.out.println("Added "+abstractAssembly.getInsns().get(0).toString()+" to calling conv ");
//			//insertCallingConventions(abstractAssembly);
//		}
		memoryOffset = 8;
		return abstractAssembly;
	}

	private static void insertCallingConventions(Assembly abstractAssembly) {
		ArrayList<Instruction> insns = abstractAssembly.getInsns();
		insns.add(2,new Push("r12"));
		insns.add(2,new Push("r13"));
		insns.add(2,new Push("r14"));
		insns.add(2,new Push("r15"));
		insns.add(2,new Push("rbx"));
		insns.add(2,new Push("rbx"));

		for (int i=0;i<insns.size();i++) {
			if (insns.get(i) instanceof Call c) {
				String abiName = c.getFunc();
				int numArgs = IRFuncDecl.getNumArgs(abiName);
				int cur = i-1;
				while (numArgs > 0) {
					Instruction insn = insns.get(cur);
					if (insn instanceof Mov m) {
						if (m.getRegs().size() == 2) {
							if (m.getRegs().get(0).getReg().equals(m.getRegs().get(1).getReg())) {
							} else {
							}
								if (!m.getRegs().get(0).getReg().equals(m.getRegs().get(1).getReg())) {
								numArgs--;
							}
						} else {
							numArgs--;
						}
					} else {
						numArgs--;
					}
					cur--;
				}

				insns.add(cur+2,new Push("rsi"));
				insns.add(cur+2,new Push("rdi"));
				insns.add(cur+2,new Push("r8"));
				insns.add(cur+2,new Push("r9"));
				insns.add(cur+2,new Push("r10"));
				insns.add(cur+2,new Push("r11"));
				insns.add(cur+2,new Push("rax"));
				insns.add(cur+2,new Push("rdx"));

				i = i + 8;

				cur = i+1;
				ArrayList<String> returnValuePops = new ArrayList<>();
				ArrayList<String> callerSavedRegs = new ArrayList<>();
				callerSavedRegs.add("rsi");
				callerSavedRegs.add("rdi");
				callerSavedRegs.add("r8");
				callerSavedRegs.add("r9");
				callerSavedRegs.add("r10");
				callerSavedRegs.add("r11");
				callerSavedRegs.add("rax");
				callerSavedRegs.add("rdx");

				while (insns.get(cur) instanceof Pop p) {
					if (isCallerSaved(p.getVal())) {
						returnValuePops.add(p.getVal());
					}
					cur++;
				}

				for (String s : callerSavedRegs) {
					if (returnValuePops.contains(s)) {
						insns.add(cur, new Binop("add",new Register("rsp"),
								new Const(8)));
					} else {
						insns.add(cur, new Pop(s));
					}
					cur++;
				}


			}
		}


		int setIndex = insns.size()-2;
		insns.add(setIndex,new Pop("rbx"));
		insns.add(setIndex,new Pop("rbx"));
		insns.add(setIndex,new Pop("r15"));
		insns.add(setIndex,new Pop("r14"));
		insns.add(setIndex,new Pop("r13"));
		insns.add(setIndex,new Pop("r12"));
	}

	private static void addPops(int cur, String reg,
	                            ArrayList<String> returnValuePops,
	                            ArrayList<Instruction> insns) {
		if (returnValuePops.contains(reg)) {
			insns.add(cur, new Binop("add",new Register("rsp"),
					new Const(8)));
		} else {
			insns.add(cur, new Pop(reg));
		}
	}

	private static boolean isCallerSaved(String s) {
		return s.equals("rsi") || s.equals("rdi") || s.equals("r8")
				|| s.equals("r9") || s.equals("r10") || s.equals("r11");
	}

	private static Node removeNode(Set<Node> nodeSet) {
		Iterator<Node> itr = nodeSet.iterator();
		Node n = itr.next();
		itr.remove();
		return n;
	}

	private static void simplify() {
		Node n = removeNode(simplifyWorklist);
		if (n.precolored) {
			return;
		}
		selectStack.push(n);
		Set<Node> adjacentNodes = adjacent(n);
		for (Node m : adjacentNodes) {
			decrementDegree(m);
		}
	}

	private static Set<Node> adjacent(Node n) {
		Set<Node> adj = new LinkedHashSet<>(adjList(n));
		Set<Node> union = new LinkedHashSet<>(coalescedNodes);

		union.addAll(selectStack);

		adj.removeIf(union::contains);
		return adj;
	}

	private static void decrementDegree(Node n) {
		int d = n.getDegree();
		n.decr();

		if (d == K) {
			Set<Node> union = adjacent(n);
			union.add(n);
			enableMoves(union);
			spillWorklist.remove(n);
			if (moveRelated(n)) {
				freezeWorklist.add(n);
			} else {
				simplifyWorklist.add(n);
			}
		}
	}

	private static void enableMoves(Set<Node> nodes) {
		for (Node n : nodes) {
			Set<Mov> nodeMoves = nodeMoves(n);
			for (Mov m : nodeMoves) {
				if (activeMoves.contains(m)) {
					activeMoves.remove(m);
					worklistMoves.add(m);
				}
			}
		}
	}

	private static void coalesce() {
		Iterator<Mov> itr = worklistMoves.iterator();
		Mov m = itr.next();
		ArrayList<Register> regs = m.getRegs();
		if (regs.size() == 2 && !regs.get(0).isMem() && !regs.get(1).isMem()) {
			Node x = findNode(regs.get(0).getReg());
			Node y = findNode(regs.get(1).getReg());
			x = getAlias(x);
			y = getAlias(y);

			Node u;
			Node v;
			if (precolored.contains(y)) {
				u = y;
				v = x;

			} else {
				u = x;
				v = y;

			}

			worklistMoves.remove(m);
			if (u == v) {
				coalescedMoves.add(m);
				addWorkList(u);
			} else if (precolored.contains(v) || inAdjSet(u,v)) {
				constrainedMoves.add(m);
				addWorkList(u);
				addWorkList(v);
			} else if (
					!u.toString().contains("rax")
					&& !v.toString().contains("rax")
				    && !u.toString().contains("rdx")
				    && !v.toString().contains("rdx")
				    &&
					(
						precolored.contains(u) && allAdjOK(u,v) ||
						!precolored.contains(u) && conservative(u,v))
					)
			{
				coalescedMoves.add(m);
				combine(u,v);
				addWorkList(u);
			} else {
				activeMoves.add(m);
			}
		} else {
			worklistMoves.remove(m);
			frozenMoves.add(m);
		}
	}

	private static boolean conservative(Node u, Node v) {
		Set<Node> adjU = adjacent(u);
		Set<Node> adjV = adjacent(v);

		Set<Node> union = new LinkedHashSet<>(adjU);
		union.addAll(adjV);

		int k = 0;
		for (Node n : union) {
			if (n.getDegree() >= K) {
				k++;
			}
		}

		return k < K;
	}

	private static boolean allAdjOK(Node u, Node v) {
		for (Node t : adjacent(v)) {
			if (!OK(t,u)) {
				return false;
			}
		}
		return true;
	}

	private static boolean inAdjSet(Node u, Node v) {
		return adjSet.contains(new Edge(u,v));
	}

	private static Node getAlias(Node n) {
		if (coalescedNodes.contains(n)) {
			return getAlias(alias.get(n));
		} else {
			return n;
		}
	}

	private static void addWorkList(Node u) {
		if (!precolored.contains(u) && !moveRelated(u) && u.getDegree() < K) {
			freezeWorklist.remove(u);
			simplifyWorklist.add(u);
		}
	}

	private static boolean OK(Node t, Node r) {
		return t.getDegree() < K || precolored.contains(t) || inAdjSet(t,r);
	}

	private static void combine(Node u, Node v) {
		if (freezeWorklist.contains(v)) {
			freezeWorklist.remove(v);
		} else {
			spillWorklist.remove(v);
		}

		coalescedNodes.add(v);
		alias.put(v,u);

		if (moveList.get(u) == null) {
			moveList.put(u, moveList.get(v));
		} else {
			moveList.get(u).addAll(moveList.get(v));
		}

		Set<Node> vSet = new LinkedHashSet<>();
		vSet.add(v);
		enableMoves(vSet);
		for (Node t : adjacent(v)) {
			addEdge(t,u);
			decrementDegree(t);
		}

		if (u.getDegree() >= K && freezeWorklist.contains(u)) {
			freezeWorklist.remove(u);
			spillWorklist.add(u);
		}
	}

	private static void addEdge(Node u, Node v) {
		if (!inAdjSet(u,v) && (u != v)) {
			adjSet.add(new Edge(u,v));
			adjSet.add(new Edge(v,u));
			if (!precolored.contains(u)) {
				Set<Node> adjList = adjList(u);
				adjList.add(v);
				u.degree++;
			}

			if (!precolored.contains(v)) {
				Set<Node> adjList = adjList(v);
				adjList.add(u);
				v.degree++;
			}
		}
	}

	private static void freeze() {
		Node u = removeNode(freezeWorklist);
		simplifyWorklist.add(u);
		freezeMoves(u);
	}

	private static void selectSpill() {
		Node n = removeNode(spillWorklist);
		simplifyWorklist.add(n);
		freezeMoves(n);
	}

	private static void freezeMoves(Node u) {
		Set<Mov> nodeMoves = nodeMoves(u);
		for (Mov m : nodeMoves) {
			ArrayList<Register> regs = m.getRegs();
			if (regs.size() == 2) {
				Node x = findNode(regs.get(0).getReg());
				Node y = findNode(regs.get(1).getReg());

				Node v;
				if (getAlias(y) == getAlias(u)) {
					v = getAlias(x);
				} else {
					v = getAlias(y);
				}

				activeMoves.remove(m);
				frozenMoves.add(m);
				if (freezeWorklist.contains(v) && nodeMoves(v).isEmpty()) {
					freezeWorklist.remove(v);
					simplifyWorklist.add(v);
				}
			} else {
				activeMoves.remove(m);
				frozenMoves.add(m);
			}
		}
	}

	private static void assignColors() {
		while (!selectStack.isEmpty()) {
			Node n = selectStack.pop();
			Set<String> okColors = new LinkedHashSet<>(x86colors);
			for (Node w : adjList(n)) {
				Set<Node> union = new LinkedHashSet<>(coloredNodes);
				union.addAll(precolored);
				if (union.contains(getAlias(w))) {
					okColors.remove(getAlias(w).color);
				}
			}

			if (okColors.isEmpty()) {
				if (!n.precolored) {
					spilledNodes.add(n);
				}
			} else {
				coloredNodes.add(n);
				Iterator<String> itr = okColors.iterator();
				n.color = itr.next();
			}
		}
		for (Node n : coalescedNodes) {
			n.color = getAlias(n).color;
		}
	}

	private static void allocateRegisters(Assembly abstractAssembly) {
		for (Instruction insn : abstractAssembly.getInsns()) {
			ArrayList<Register> precoloredTemps = insn.getPrecoloredTemps();
			ArrayList<Register> abstractTemps = insn.getAbstractTemps();

			for (Register r : abstractTemps) {
				if (findNode(r.getReg()) == null) {
				}
				r.setReg(findNode(r.getReg()).color);
			}
		}
	}

	private static void rewriteProgram(Assembly abstractAssembly) {
		memLocations = new HashMap<>();
		for (Node v : spilledNodes) {
			memLocations.put(v,memoryOffset);
			IRFuncDecl.numSpilled++;
			memoryOffset += 8;
		}

		Set<Node> newTemps = new LinkedHashSet<>();

		ArrayList<Instruction> newAsm = new ArrayList<>();

		for (Instruction insn : abstractAssembly.getInsns()) {
			if (insn instanceof Mov m) {
				ArrayList<Register> useRegs = m.getSrc().getAbstractTemps();
				for (Register r : useRegs) {
					Node n = findNode(r.getReg());
					if (spilledNodes.contains(n)) {
						int offset = memLocations.get(n);
						String freshTemp = Assembly.generateFreshTemp();
						newTemps.add(new Node(freshTemp));
						Mov mov = new Mov(new Register(freshTemp),
								new MemBinop(new Register("rbp"),
										"-", new Const(offset)));
						newAsm.add(mov);
						r.setReg(freshTemp);
					}
				}

				newAsm.add(insn);

				ArrayList<Register> defRegs = m.getTarget().getAbstractTemps();
				for (Register r : defRegs) {
					Node n = findNode(r.getReg());
					if (spilledNodes.contains(n)) {
						int offset = memLocations.get(n);
						String freshTemp = Assembly.generateFreshTemp();
						newTemps.add(new Node(freshTemp));
						Mov mov = new Mov(new MemBinop(new Register("rbp"),
								"-", new Const(offset)),
								new Register(freshTemp));
						newAsm.add(mov);
						r.setReg(freshTemp);
					}
				}
			} else {
				ArrayList<Register> regs = insn.getAbstractTemps();
				for (Register r : regs) {
					Node n = findNode(r.getReg());
					if (spilledNodes.contains(n)) {
						int offset = memLocations.get(n);
						String freshTemp = Assembly.generateFreshTemp();
						newTemps.add(new Node(freshTemp));
						Mov mov = new Mov(new Register(freshTemp),
								new MemBinop(new Register("rbp"),
										"-", new Const(offset)));
						newAsm.add(mov);
						r.setReg(freshTemp);
					}
				}
				newAsm.add(insn);
			}

			abstractAssembly.clear();
			abstractAssembly.addInstructions(new Assembly(newAsm));
		}

		spilledNodes.clear();
		initial = new LinkedHashSet<>(coloredNodes);
		initial.addAll(coalescedNodes);
		initial.addAll(newTemps);
		coloredNodes.clear();
		coalescedNodes.clear();

	}

	private static void clearData() {
		numIterations = 1;
		TempList.clear();
		precoloredList.clear();
		intGraph.clear();
		adjSet.clear();
		nodes.clear();
		initial.clear();
		precolored.clear();
		worklistMoves.clear();
		frozenMoves.clear();
		coalescedMoves.clear();
		moveList.clear();
		activeMoves.clear();
		constrainedMoves.clear();
		spillWorklist.clear();
		freezeWorklist.clear();
		simplifyWorklist.clear();
		spilledNodes.clear();
		coalescedNodes.clear();
		coloredNodes.clear();
		selectStack.clear();
		alias.clear();
	}

	private static void setDegrees() {
		for (Node n : intGraph.keySet()) {
			n.degree = intGraph.get(n).size();
		}
	}

	private static void makeWorkList() {
		for (Node n : initial) {
			if (n.degree >= K) {
				spillWorklist.add(n);
			} else if (moveRelated(n)) {
				freezeWorklist.add(n);
			} else {
				simplifyWorklist.add(n);
			}
		}
		initial.clear();
	}

	private static Set<Edge> getAdjSet() {
		Set<Edge> edgeList = new LinkedHashSet<>();
		for (Node u : intGraph.keySet()) {
			for (Node v : intGraph.get(u)) {
				edgeList.add(new Edge(u,v));
			}
		}
		return edgeList;
	}

	private static Set<Node> adjList(Node n) {
		return intGraph.get(n);
	}

	private static boolean moveRelated(Node n) {
		return !nodeMoves(n).isEmpty();
	}

	private static Set<Mov> nodeMoves(Node n) {
		Set<Mov> union = new LinkedHashSet<>(activeMoves);
		union.addAll(worklistMoves);
		Set<Mov> moveListn = moveList.get(n);
		Set<Mov> intersection = new LinkedHashSet<>();

		if (moveListn == null) {
			return intersection;
		}

		for (Mov m : moveListn) {
			if (union.contains(m)) {
				intersection.add(m);
			}
		}
		return intersection;
	}

	private static void clearPredecessorsAndSuccessors(ArrayList<Instruction> abstractAssemblyList) {
		for (Instruction instruction : abstractAssemblyList) {
			instruction.clearPredecessors();
			instruction.clearSuccessors();
		}
	}

	private static void createAbstractCFG(Assembly abstractAssembly) {
		ArrayList<Instruction> abstractAssemblyList = abstractAssembly.getInsns();
		clearPredecessorsAndSuccessors(abstractAssemblyList);
		ArrayList<Label> labelList = createLabelList(abstractAssembly);
		for (int i = 0; i < abstractAssemblyList.size(); i++) {
			Instruction currentInsn = abstractAssemblyList.get(i);

			currentInsn.calculateUse();
			currentInsn.calculateDef();

			if (i - 1 >= 0) {
				Instruction prevInsn = abstractAssemblyList.get(i - 1);
				if (prevInsn instanceof Jmp jmp) {
					if (!jmp.alwaysJump()) {
						currentInsn.addPredecessor(prevInsn);
					}
				} else {
					currentInsn.addPredecessor(prevInsn);
				}
			}

			if (currentInsn instanceof Jmp jmp) {
				if (!jmp.alwaysJump()) {
					if (i + 1 < abstractAssemblyList.size()) {
						currentInsn.addSuccessor(abstractAssemblyList.get(i + 1));
					}
				}

				for (Label l : labelList) {
					if (jmp.getLabel().equals(l.getName())) {
						currentInsn.addSuccessor(l);
						l.addPredecessor(currentInsn);
					}
				}

			} else {
				if (i + 1 < abstractAssemblyList.size()) {
					currentInsn.addSuccessor(abstractAssemblyList.get(i + 1));
				}
			}
		}
	}

	private static void liveVarAnalysis(Assembly abstractAssembly) {
		clearInAndOut(abstractAssembly);
		Queue<Instruction> worklist = new ArrayDeque<>(abstractAssembly.getInsns());
		while (!worklist.isEmpty()) {
			Instruction n = worklist.remove();
			n.calculateOut();
			boolean inChange = n.calculateIn();

			if (inChange) {
				for (Instruction i : n.getPredecessors()) {
					if (!worklist.contains(i)) {
						worklist.add(i);
					}
				}
			}
		}
	}

	private static void clearInAndOut(Assembly abstractAssembly) {
		for (Instruction insn : abstractAssembly.getInsns()) {
			insn.setIn(new LinkedHashSet<>());
			insn.setOut(new LinkedHashSet<>());
		}
	}

	public static void buildInterferenceGraph(Assembly abstractAssembly) {
		setPrecoloredList(abstractAssembly);
		precolored = convertToNodes(precoloredList);
		colorPrecolored();
		nodes = new LinkedHashSet<>(initial);
		nodes.addAll(precolored);

		HashMap<String, Set<String>> interferenceGraph = new HashMap<>();

		for (Instruction i : abstractAssembly.getInsns()) {
			Set<String> liveIn = i.getIn();
			Set<String> liveOut = i.getOut();
			for (String s : liveIn) {
				Set<String> adds = getRest(s, liveIn);
				if (interferenceGraph.containsKey(s)) {
					interferenceGraph.get(s).addAll(adds);
				} else {
					interferenceGraph.put(s, adds);
				}
			}

			ArrayList<Register> defs = i.getDef();
			if (!defs.isEmpty()) {
				for (Register r : defs) {
					String definedVar = r.getReg();
					if (interferenceGraph.containsKey(definedVar)) {
						interferenceGraph.get(definedVar).addAll(liveOut);
					} else {
						interferenceGraph.put(definedVar, liveOut);
					}
				}
			}
		}

		for (Map.Entry<String,Set<String>> entry: interferenceGraph.entrySet()) {
			String s = entry.getKey();
			for (String t : interferenceGraph.get(s)) {
				if (!interferenceGraph.get(t).contains(s)) {
					interferenceGraph.get(t).add(s);
				}
			}
		}

		intGraph.putAll(convertToNodes(interferenceGraph));
		adjSet.addAll(getAdjSet());

		setDegrees();
		worklistMoves.addAll(getWorkListMoves(abstractAssembly));
		moveList.putAll(getMoveList(abstractAssembly));
	}

	private static void printMap(HashMap<String, Set<String>> map) {
		for (String s : map.keySet()) {
			System.out.print(s+": ");
			for (String v : map.get(s)) {
				System.out.print(v+", ");
			}
			System.out.println("");
		}
	}

	private static void printMap2(HashMap<Node, Set<Node>> map) {
		for (Node n : map.keySet()) {
			System.out.print(n+": ");
			for (Node m : map.get(n)) {
				System.out.print(m+", ");
			}
			System.out.println("");
		}
	}

	private static void printList(Set<String> list) {
		for (String s : list) {
			System.out.print(s+", ");
		}
		System.out.println();
	}
	// delete this before submission
	private static void printNodes(Set<Node> list) {
		for (Node n : list) {
			System.out.print(n+", ");
		}
		System.out.println();
	}

	private static void printSizes() {
		System.out.println("===================================================");
		System.out.println("TempList: "+TempList.size());
		System.out.println("precoloredList: "+precoloredList.size());
		System.out.println("precolored: "+precolored.size());
		System.out.println("intGraph: "+intGraph.size());
		System.out.println("adjSet: "+adjSet.size());
		System.out.println("worklistMoves: "+worklistMoves.size());
		System.out.println("frozenMoves: "+frozenMoves.size());
		System.out.println("coalescedMoves: "+coalescedMoves.size());
		System.out.println("activeMoves: "+activeMoves.size());
		System.out.println("constrainedMoves: "+constrainedMoves.size());
		System.out.println("moveList: "+moveList.size());
		System.out.println("spillWorkList: "+spillWorklist.size());
		System.out.println("freezeWorkList: "+freezeWorklist.size());
		System.out.println("simplifyWorkList: "+simplifyWorklist.size());
		System.out.println("spilledNodes: "+spilledNodes.size());
		System.out.println("coalescedNodes: "+coalescedNodes.size());
		System.out.println("coloredNodes: "+coloredNodes.size());
		System.out.println("selectStack: "+selectStack.size());
		System.out.println("alias: "+alias.size());
		System.out.println("===================================================");

	}

	private static Set<Mov> getWorkListMoves(Assembly abstractAssembly) {
		Set<Mov> moves = new LinkedHashSet<>();
		for (Instruction insn : abstractAssembly.getInsns()) {
			if (insn instanceof Mov mov) {
				moves.add(mov);
			}
		}
		return moves;
	}

	private static HashMap<Node, Set<Mov>> getMoveList(Assembly abstractAssembly) {
		HashMap<Node, Set<Mov>> moveList = new HashMap<>();
		for (Instruction insn : abstractAssembly.getInsns()) {
			if (insn instanceof Mov mov) {
				ArrayList<Register> abstractTemps = mov.getAbstractTemps();
				for (Register r : abstractTemps) {
					Node n = findNode(r.getReg());
					moveList.computeIfAbsent(n, k -> new LinkedHashSet<>());
					moveList.get(n).add(mov);
				}
			}
		}
		return moveList;
	}

	public static Set<String> getRest(String s, Set<String> set) {
		Set<String> list = new LinkedHashSet<>();
		for (String str : set) {
			if (!str.equals(s)) {
				list.add(str);
			}
		}
		return list;
	}

	private static void setTempList(Assembly abstractAssembly) {
		ArrayList<Instruction> abstractAssemblyList = abstractAssembly.getInsns();
		for (Instruction insn : abstractAssemblyList) {
			ArrayList<Register> abstractOperands = insn.getAbstractTemps();
			addToTempList(abstractOperands);
		}
	}
	private static void setPrecoloredList(Assembly abstractAssembly) {
		ArrayList<Instruction> abstractAssemblyList = abstractAssembly.getInsns();
		for (Instruction insn : abstractAssemblyList) {
			ArrayList<Register> precoloredTemps = insn.getPrecoloredTemps();
			addToPrecoloredList(precoloredTemps);
		}
	}

	private static void colorPrecolored() {
		for (Node n : precolored) {
			n.color = n.temp;
			/**
			 *  From text:
			 *  We cannot simplify a precolored node – this would mean pulling it from
			 * the graph in the hope that we can assign it a color later, but in fact we have
			 * no freedom about what color to assign it. And we should not spill precolored
			 * nodes to memory, because the machine registers are by definition registers.
			 * Thus, we should treat them as having infinite degree.
			 * */
			n.degree = 999;
			n.precolored = true;
		}
	}

	private static Set<Node> convertToNodes(ArrayList<String> list) {
		Set<Node> nodeSet = new LinkedHashSet<>();
		for (String s : list) {
			if (findNode(s) == null) {
				nodeSet.add(new Node(s));
			} else {
				nodeSet.add(findNode(s));
			}
		}
		return nodeSet;
	}

	private static HashMap<Node, Set<Node>> convertToNodes(HashMap<String,
			Set<String>> strGraph) {
		HashMap<Node, Set<Node>> nodeIntGraph = new HashMap<>();
		for (String keyStr : strGraph.keySet()) {
			nodeIntGraph.put(findNode(keyStr),
					findNodes(strGraph.get(keyStr)));
		}
		return nodeIntGraph;
	}

	private static Node findNode(String nodeStr) {
		for (Node node : nodes) {
			if (node.temp.equals(nodeStr)) {
				return node;
			}
		}

		return null;
	}

	private static Set<Node> findNodes(Set<String> nodeStr) {
		Set<Node> nodes = new LinkedHashSet<>();
		for (String s : nodeStr) {
			nodes.add(findNode(s));
		}

		return nodes;
	}

	private static ArrayList<Label> createLabelList(Assembly abstractAssembly) {
		ArrayList<Label> labelList = new ArrayList<>();
		for (Instruction a : abstractAssembly.getInsns()) {
			if (a instanceof Label label) {
				labelList.add(label);
			}
		}
		return labelList;
	}

}