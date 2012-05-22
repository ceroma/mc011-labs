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

    // Set of all nodes that define the temporary t:
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
        // Initializes D(t):
        defs = new HashMap<Temp, Set<Node>>();
        for (Node n : cfg.nodes()) {
            // Consider only unambiguous definitions:
            if (!cfg.getInstr(n).isMoveBetweenTemps()) {
                continue;
            }
            
            // Add current definition node to D(t):
            for (Temp t : cfg.getDefined(n)) {
                Set<Node> Dt = defs.get(t);
                if (Dt == null) {
                    Dt = new HashSet<Node>();
                }
                Dt.add(n);
                defs.put(t, Dt);
            }
        }
    }
}
