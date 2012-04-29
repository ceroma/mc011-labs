package x86;

import util.List;

import tree.Exp;
import tree.Stm;
import temp.Temp;
import assem.Instr;

public class Codegen {
	Frame frame;
	private List<Instr> ilist = null, last = null;
	
    public Codegen(Frame f) {
    	frame = f;
    }
    
    /**
     * Adds an instruction to the end of the list of instructions.
     * 
     * @param inst
     */
    private void emit(Instr inst) {
    	if (last != null) {
    		last = last.tail = new List<Instr>(inst, null);
    	} else {
    		last = ilist = new List<Instr>(inst, null);
    	}
    }

    /**
     * Emits instructions for a given Tree.Stm node using the Maximal Munch
     * algorithm.
     * 
     * @param s
     */
    void munchStm(Stm s) {
    	return;
    }
    
    /**
     * Emits instructions for a given Tree.Exp node using the Maximal Munch
     * algorithm. Returns the temporary register where the expression's result
     * is stored.
     * 
     * @param s
     */
    Temp munchExp(Exp e) {
    	return new Temp();
    }
    
    /**
     * Generates (selects) list of instructions for a given IR root node.
     * 
     * @param s
     * @return
     */
    public List<Instr> codegen(Stm s) {
    	List<Instr> l;
    	munchStm(s);
    	l = ilist;
    	ilist = last = null;
    	return l;
    }

    /**
     * Generates (selects) list of instructions for a list of IR nodes.
     * 
     * @param body
     * @return
     */
    public List<Instr> codegen(List<Stm> body) {    	
    	for (Stm s : body) {
    		munchStm(s);
    	}
        return ilist;
    }
}
