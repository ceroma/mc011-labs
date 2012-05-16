package optimization;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import temp.Temp;
import util.List;
import assem.Instr;
import flow_graph.AssemFlowGraph;
import graph.Node;

public class ReachingDefinition {

	private AssemFlowGraph cfg;

	private Map<Node, Set<Node>> in;
	private Map<Node, Set<Node>> out;
	private Map<Node, Set<Node>> gen;
	private Map<Node, Set<Node>> kill;

	/* defs(t) é o conjunto de nós que definem o temporário t */
	private Map<Temp, Set<Node>> defs;

	public Set<Node> getIn(Node n){
		return in.get(n);
	}

	public Set<Node> getOut(Node n){
		return out.get(n);
	}

	public Set<Node> getKill(Node n){
		return kill.get(n);
	}
	
	public ReachingDefinition(List<Instr> l, AssemFlowGraph cfg) {
	}

}
