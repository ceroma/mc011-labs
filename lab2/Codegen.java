package x86;

import util.List;

import assem.Instr;
import assem.OPER;
import temp.Label;
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
            munchStm((EXPSTM)s);
        } else if (s instanceof SEQ) {
            munchStm((SEQ)s);
        } else if (s instanceof LABEL) {
            munchStm((LABEL)s);
        } else if (s instanceof JUMP) {
            munchStm((JUMP)s);
        } else if (s instanceof CJUMP) {
            munchStm((CJUMP)s);
        } else if (s instanceof MOVE) {
            munchStm((MOVE)s);
        } else {
            System.out.println("Unrecognized Stm: " + s.getClass());
        }
    }

    /**
     * Emits instructions for a given Tree.Stm.EXPSTM.
     * 
     * @param e
     */
    void munchStm(EXPSTM e) {
        munchExp(e.getExpression());
    }

    /**
     * Emits instructions for a given Tree.Stm.SEQ.
     *     
     * @param s
     */
    void munchStm(SEQ s) {
        munchStm(s.getLeft());
        munchStm(s.getRight());
    }

    /**
     * Emits instructions for a given Tree.Stm.LABEL.
     * 
     * @param l
     */
    void munchStm(LABEL l) {
        emit(new assem.LABEL(l.getLabel().toString() + ":", l.getLabel()));
    }

    /**
     * Emits instructions for a given Tree.Stm.JUMP.
     * 
     * @param j
     */
    void munchStm(JUMP j) {
        // TODO: larger tiles (like JUMP(NAME) or JUMP(TEMP)).
        Temp u = munchExp(j.getExpression());
        // TODO: check assem.OPER(instruction, jumps).
        emit(new OPER("jmp `u0", null, new List<Temp>(u, null)));
    }

    /**
     * Emits instructions for a given Tree.Stm.CJUMP.
     * 
     * @param c
     */
    void munchStm(CJUMP c) {
        String inst = "";

        switch (c.getOperation()) {
            case CJUMP.EQ:
                inst = "jz";
                break;
            case CJUMP.NE:
                inst = "jnz";
                break;
            case CJUMP.LT:
                inst = "jl";
                break;
            case CJUMP.LE:
                inst = "jle";
                break;
            case CJUMP.GT:
                inst = "jg";
                break;
            case CJUMP.GE:
                inst = "jge";
                break;
            case CJUMP.ULT:
                inst = "jb";
                break;
            case CJUMP.ULE:
                inst = "jbe";
                break;
            case CJUMP.UGT:
                inst = "ja";
                break;
            case CJUMP.UGE:
                inst = "jae";
                break;
        }

        Temp left = munchExp(c.getLeft());
        Temp right = munchExp(c.getRight());

        emit(new OPER(
            "cmp `u0, `u1",
            null,
            new List<Temp>(left, new List<Temp>(right, null))
        ));
        emit(new OPER(inst + " `j0", new List<Label>(c.getLabelTrue(), null)));
        emit(new OPER("jmp `j0", new List<Label>(c.getLabelFalse(), null)));
        return;
    }

    /**
     * Emits instructions for a given Tree.Stm.MOVE.
     * 
     * @param m
     */
    void munchStm(MOVE m) {
        Temp src = munchExp(m.getSource());
        Temp dst = munchExp(m.getDestination());
        emit(new assem.MOVE(dst, src));
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
                "mov `d0, " + ((NAME)e).getLabel().toString(),
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
            List<Exp> args = ((CALL)e).getArguments();
            List<Temp> l = munchArgs(args);

            emit(new OPER(
                "call `u0",
                frame.calleeDefs(),
                new List<Temp>(u, l)
            ));

            // Restore the stack:
            if (args.size() > 0) {
                emit(new OPER(
                    "add `d0, " + (4 * args.size()),
                    new List<Temp>(frame.SP(), null),
                    new List<Temp>(frame.SP(), null)
                ));
            }

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

        // The parameters should be pushed in inverted order:
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

        emit(new assem.MOVE(r, left));
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
