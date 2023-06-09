package main.java.jdr299zdh5cew256ans96.ir;

import main.java.jdr299zdh5cew256ans96.ir.visit.AggregateVisitor;
import main.java.jdr299zdh5cew256ans96.ir.visit.CheckCanonicalIRVisitor;
import main.java.jdr299zdh5cew256ans96.ir.visit.IRVisitor;
import main.java.jdr299zdh5cew256ans96.util.edu.cornell.cs.cs4120.util.CodeWriterSExpPrinter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An intermediate representation for a function call CALL(e_target, e_1, ...,
 * e_n)
 */
public class IRCall extends IRExpr_c {
    protected IRExpr target;
    protected List<IRExpr> args;

    /**
     * @param target address of the code for this function call
     * @param args   arguments of this function call
     */
    public IRCall(IRExpr target, IRExpr... args) {
        this(target, Arrays.asList(args));
    }

    /**
     * @param target address of the code for this function call
     * @param args   arguments of this function call
     */
    public IRCall(IRExpr target, List<IRExpr> args) {
        this.target = target;
        this.args = args;
    }

    public IRExpr target() {
        return target;
    }

    public List<IRExpr> args() {
        return args;
    }

    @Override
    public String label() {
        return "CALL";
    }

    @Override
    public IRNode visitChildren(IRVisitor v) {
        boolean modified = false;

        IRExpr target = (IRExpr) v.visit(this, this.target);
        if (target != this.target)
            modified = true;

        List<IRExpr> results = new ArrayList<>(args.size());
        for (IRExpr arg : args) {
            IRExpr newExpr = (IRExpr) v.visit(this, arg);
            if (newExpr != arg)
                modified = true;
            results.add(newExpr);
        }

        if (modified)
            return v.nodeFactory().IRCall(target, results);

        return this;
    }

    @Override
    public <T> T aggregateChildren(AggregateVisitor<T> v) {
        T result = v.unit();
        result = v.bind(result, v.visit(target));
        for (IRExpr arg : args)
            result = v.bind(result, v.visit(arg));
        return result;
    }

    @Override
    public boolean isCanonical(CheckCanonicalIRVisitor v) {
        return !v.inExpr() && !v.inExp() && !v.inMove();
    }

    @Override
    public IRESeq lower(IRNodeFactory factory) {
        ArrayList<IRStmt> seq = new ArrayList<>();
        ArrayList<IRExpr> freshTemps = new ArrayList<>();
        int i = 1;
        for (IRExpr arg : args) {
            IRESeq eseq = arg.lower(factory);
            IRStmt sideEffect = eseq.stmt();
            IRExpr pureExpr = eseq.expr();
            seq.add(sideEffect);
            IRTemp freshTemp = factory.generateFreshTemp();
            IRTemp argReg = factory.IRTemp("_ARG" + i);
            freshTemps.add(freshTemp);
            seq.add(factory.IRMove(freshTemp, pureExpr));
            seq.add(factory.IRMove(argReg, freshTemp));
            i += 1;
        }

        seq.add(factory.IRCallStmt(target, (long) 1, freshTemps));

        return factory.IRESeq(factory.IRSeq(seq), factory.IRTemp("_RV1"));

    }

    public String toString() {
        String s = "CALL";
        for (IRExpr arg : args) {
            s = s + "_" + arg.toString();
        }
        return s;
    }

    @Override
    public void printSExp(CodeWriterSExpPrinter p) {
        p.startList();
        p.printAtom("CALL");
        target.printSExp(p);
        for (IRExpr arg : args)
            arg.printSExp(p);
        p.endList();
    }
}
