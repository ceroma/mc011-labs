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
    
    private void emit(Instr inst) {
    	if (last != null) {
    		last = last.tail = new List<Instr>(inst, null);
    	} else {
    		last = ilist = new List<Instr>(inst, null);
    	}
    }

    void munchStm(Stm s) {
    	return;
    }
    
    Temp munchExp(Exp e) {
    	return new Temp();
    }
    
    public List<Instr> codegen(Stm s) {
    	List<Instr> l;
    	munchStm(s);
    	l = ilist;
    	ilist = last = null;
    	return l;
    }

    public List<Instr> codegen(List<Stm> body) {
        return null;
    }
}
