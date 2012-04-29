package x86;

import util.List;

import assem.Instr;
import assem.MOVE;
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
            munchExp(((EXPSTM)s).getExpression());
        } else if (s instanceof SEQ) {
            munchStm(((SEQ)s).getLeft());
            munchStm(((SEQ)s).getRight());
        } else if (s instanceof LABEL) {
            emit(new assem.LABEL(
                ((LABEL)s).getLabel().toString() + ":",
                ((LABEL)s).getLabel()
            ));
        } else if (s instanceof JUMP) {
            Temp u = munchExp(((JUMP)s).getExpression());
            // TODO: check assem.OPER(instruction, jumps).
            emit(new OPER(
                "jmp `u0",
                null,
                new List<Temp>(u, null)
            ));
        } else if (s instanceof CJUMP) {
            munchConditionalJump((CJUMP)s);
        }
        return;
    }
    
    /**
     * Emits instructions for a given Tree.Stm.CJUMP.
     * 
     * @param s
     */
    void munchConditionalJump(CJUMP s) {
        String inst = "";

        switch (s.getOperation()) {
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

        Temp left = munchExp(s.getLeft());
        Temp right = munchExp(s.getRight());

        emit(new OPER(
            "cmp `u0, `u1",
            null,
            new List<Temp>(left, new List<Temp>(right, null))
        ));
        emit(new OPER(inst + " `j0", new List<Label>(s.getLabelTrue(), null)));
        emit(new OPER("jmp `j0", new List<Label>(s.getLabelFalse(), null)));
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
