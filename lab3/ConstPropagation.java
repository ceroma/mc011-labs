package optimization;

import java.util.Set;

import temp.Temp;
import util.List;
import assem.Instr;
import flow_graph.AssemFlowGraph;
import graph.Node;

public class ConstPropagation {

    private ReachingDefinition dfa;
    private AssemFlowGraph cfg;

    /**
     * Constant Propagation:
     *     Suppose we have a statement "d : t <- c", where c is a constant, and
     * another statement n that uses t, such as "n : y <- t + x".
     *     We know that t is constant in n if d reaches n, and no other defini-
     * tions of t reach n.
     *     In this case, we can rewrite n as "y <- c + x".
     */
    public void optimize(List<Instr> l) {
        String cte;
        Node reaching_def_t = null;
        int num_reaching_def_t = 0;
        
        cfg = new AssemFlowGraph(l);
        dfa = new ReachingDefinition(l, cfg);

        for (Node n : cfg.nodes()) {
            if (cfg.getUsed(n) == null) continue;
            
            // Try to propagate each temporary used in this node:
            for (Temp t : cfg.getUsed(n)) {
                // Get definitions that reach this node and define t:
                reaching_def_t = null;
                num_reaching_def_t = 0;
                for (Node d : dfa.getIn(n)) {
                    if (cfg.getDefined(d).hasElement(t)) {
                        reaching_def_t = d;
                        num_reaching_def_t++;
                    }
                }
                
                // Don't propagate if reaching definition is not unique:
                if (num_reaching_def_t != 1) {
                    continue;
                }
                
                // Don't propagate if it's not a constant definition:
                if (!cfg.getInstr(reaching_def_t).isMoveFromConstant()) {
                    continue;
                }

                // Propagate constant:
                cte = this.getConstant(cfg.getInstr(reaching_def_t));
                this.propagateConstant(cfg.getInstr(n), cte);                
            }
        }
    }

    private void propagateConstant(Instr instr, String cte) {
        String assembly = instr.getAssembly();
        int index = assembly.indexOf(",");
        String newAssembly = assembly.substring(0, index + 2) + cte;
        instr.setAssembly(newAssembly.replaceAll("mov", "mov dword"));
    }

    private String getConstant(Instr instruction) {
        String instr = instruction.getAssembly();
        int index = instr.indexOf(",");
        return instr.substring(index + 1, instr.length());
    }
}
