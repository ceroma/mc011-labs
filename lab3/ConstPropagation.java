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
     * Realiza a otimização Constant Propagation
     */
    public void optimize(List<Instr> l) {
        cfg = new AssemFlowGraph(l);
        dfa = new ReachingDefinition(l, cfg);

        /* IMPLEMENTAR AQUI SEU CÓDIGO */
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
