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
        
        // Initializes gen[n] and kill[n]:
        gen = new HashMap<Node, Set<Node>>();
        kill = new HashMap<Node, Set<Node>>();
        for (Node n : cfg.nodes()) {
            // gen[n] = {d}:
            Set<Node> gen_n = new HashSet<Node>();
            if (cfg.getInstr(n).isMoveBetweenTemps()) {
                gen_n.add(n);
            }
            gen.put(n, gen_n);
            
            // kill[n] = {defs(t) - d}:
            Set<Node> kill_n = new HashSet<Node>();
            if (cfg.getInstr(n).isMoveBetweenTemps()) {
                for (Temp t : cfg.getDefined(n)) {
                    kill_n.addAll(defs.get(t));
                    kill_n.remove(n);
                }
            }
            kill.put(n, kill_n);
        }
        
        // Compute the Reaching Definitions:
        in = new HashMap<Node, Set<Node>>();
        out = new HashMap<Node, Set<Node>>();
        Map<Node, Set<Node>> old_in = new HashMap<Node, Set<Node>>();
        Map<Node, Set<Node>> old_out = new HashMap<Node, Set<Node>>();
        do {
            // Since in[n] and out[n] always increase, we can just .putAll:
            old_in.putAll(in);
            old_out.putAll(out);
            
            for (Node n : cfg.nodes()) {
                // in[n] = U_{p \in pred[n]} out[p]:
                Set<Node> in_n = new HashSet<Node>();
                if (n.getPreds() != null) {
                    for (Node p : n.getPreds()) {
                        if (out.get(p) != null) {
                            in_n.addAll(out.get(p));
                        }
                    }
                }
                in.put(n, in_n);
                
                // in[n] - kill[n]:
                Set<Node> diff = new HashSet<Node>();
                diff.addAll(in_n);
                diff.removeAll(kill.get(n));

                // out[n] = gen[n] U (in[n] - kill[n]):
                Set<Node> out_n = new HashSet<Node>();
                out_n.addAll(gen.get(n));
                out_n.addAll(diff);
                out.put(n, out_n);
            }            
        } while (!in.equals(old_in) || !out.equals(old_out));
    }
}
