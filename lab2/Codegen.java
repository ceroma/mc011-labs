package x86;

import util.List;

import assem.Instr;
import assem.OPER;
import temp.Temp;
import tree.*;

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
    	if (e instanceof TEMP) {
    		return ((TEMP)e).getTemp();
    	} else if (e instanceof ESEQ) {
    		munchStm(((ESEQ)e).getStatement());
    		return munchExp(((ESEQ)e).getExpression());
    	} else if (e instanceof NAME) {
    		Temp r = new Temp();
    		emit(new OPER(
    	         "move `d0, " + ((NAME)e).getLabel() + "\n",
    	         new List<Temp>(r, null),
    	         null
    		));
    		return r;
    	} else if (e instanceof CONST) {
    		Temp r = new Temp();
    		emit(new OPER(
    	         "move `d0, " + ((CONST)e).getValue() + "\n",
    	         new List<Temp>(r, null),
    	         null
    		));
    		return r;
    	} else if (e instanceof MEM) {
    		Temp r = new Temp();
    		Temp u = munchExp(((MEM)e).getExpression());
    		emit(new OPER(
       	         "move `d0, [`u0]\n",
       	         new List<Temp>(r, null),
       	         new List<Temp>(u, null)
       		));
    		return r;
    	}
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
