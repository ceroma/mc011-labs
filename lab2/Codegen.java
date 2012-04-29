package x86;

import util.List;

import assem.Instr;
import assem.MOVE;
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
        if (s instanceof EXPSTM) {
            munchExp(((EXPSTM)s).getExpression());
        } else if (s instanceof SEQ) {
            munchStm(((SEQ)s).getLeft());
            munchStm(((SEQ)s).getRight());
        } else if (s instanceof LABEL) {
            emit(new assem.LABEL(
                ((LABEL)s).getLabel() + ":",
                ((LABEL)s).getLabel()
            ));
        } else if (s instanceof JUMP) {
            Temp u = munchExp(((JUMP)s).getExpression());
            emit(new OPER(
                "jmp `u0",
                null,
                new List<Temp>(u, null)
            ));
        }
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
                "mov `d0, " + ((NAME)e).getLabel(),
                new List<Temp>(r, null),
                null
            ));
            return r;
        } else if (e instanceof CONST) {
            Temp r = new Temp();
            emit(new OPER(
                "mov `d0, " + ((CONST)e).getValue(),
                new List<Temp>(r, null),
                null
            ));
            return r;
        } else if (e instanceof MEM) {
            Temp r = new Temp();
            Temp u = munchExp(((MEM)e).getExpression());
            emit(new OPER(
                "mov `d0, [`u0]",
                new List<Temp>(r, null),
                new List<Temp>(u, null)
            ));
            return r;
        } else if (e instanceof CALL) {
            Temp u = munchExp(((CALL)e).getCallable());
            List<Temp> l = munchArgs(((CALL)e).getArguments());
            emit(new OPER(
                "call `u0",
                frame.calleeDefs(),
                new List<Temp>(u, l)
            ));
            return frame.RV();
        } else if (e instanceof BINOP) {
            return munchExpBinop((BINOP)e);
        }
        return new Temp();
    }

    /**
     * Emits instructions to move all the CALL arguments to their correct
     * positions.
     * 
     * @param args
     * @return
     */
    List<Temp> munchArgs(List<Exp> args) {
        if (args == null) {
            return null;
        }

        List<Temp> l = munchArgs(args.tail);
        Temp u = munchExp(args.head);
        emit(new OPER(
            "push `u0",
            new List<Temp>(frame.SP(), null),
            new List<Temp>(u, new List<Temp>(frame.SP(), null))
        ));

        return new List<Temp>(u, l);
    }

    /**
     * Emits instructions for a given Tree.Exp.BINOP.
     * 
     * @param e
     * @return
     */
    Temp munchExpBinop(BINOP e) {
        String inst = "";

        switch (e.getOperation()) {
            case BINOP.AND:
                inst = "and";
                break;
            case BINOP.ARSHIFT:
                inst = "sar";
                break;
            case BINOP.DIV:
                inst = "div";
                break;
            case BINOP.LSHIFT:
                inst = "shl";
                break;
            case BINOP.MINUS:
                inst = "sub";
                break;
            case BINOP.OR:
                inst = "or";
                break;
            case BINOP.PLUS:
                inst = "add";
                break;
            case BINOP.RSHIFT:
                inst = "shr";
                break;
            case BINOP.TIMES:
                inst = "mul";
                break;
            case BINOP.XOR:
                inst = "xor";
                break;
        }

        // TODO: larger tiles and logic operations.
        Temp r = new Temp();
        Temp left = munchExp(e.getLeft());
        Temp right = munchExp(e.getRight());

        emit(new MOVE(r, left));
        emit(new OPER(
            inst + " `d0, `u1",
            new List<Temp>(r, null),
            new List<Temp>(r, new List<Temp>(right, null))
        ));

        return r;
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
