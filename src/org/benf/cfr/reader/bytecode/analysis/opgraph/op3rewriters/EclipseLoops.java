package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.InstrIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.GotoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.IfStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.Nop;

import java.util.List;

public class EclipseLoops {
    /*
     * Eclipse has a nasty habit of instead of emitting
     *
     * test : if (a >= 5) goto after
     * ..
     * ..
     * ..
     * ++a;
     * goto test
     * after:
     *
     * emitting this -
     *
     * (a) goto test
     * body :
     * ..
     * ..
     * ..
     * ++a
     * (b) test: if (a < 5) goto body
     * (c) after :
     *
     * We identify this as (a) an unconditional forward jump, to a comparison (b)
     * which jumps directly to the instruction after the forward jump.
     *
     * All other sources for the comparison (b) must be in the range [body, test).
     *
     * If this is the case, replace (a) with negated (b), which jumps on success to (c).
     * replace (b) with an unconditional jump to a.
     */
    private static boolean isSharedEclipseLoopTest(List<Op03SimpleStatement> statements,
                                                   Op03SimpleStatement conditional,
                                                   Op03SimpleStatement bodyStart) {
        Op03SimpleStatement loopTest = conditional.getTargets().get(1);
        if (loopTest.getIndex().compareTo(bodyStart.getIndex()) <= 0 ||
                loopTest.getStatement().getClass() != IfStatement.class ||
                loopTest.getTargets().size() != 2 ||
                loopTest.getTargets().get(1) != bodyStart ||
                !conditional.getBlockIdentifiers().equals(bodyStart.getBlockIdentifiers())) {
            return false;
        }
        int testIndex = statements.indexOf(loopTest);
        if (testIndex < 0 || testIndex + 1 >= statements.size() ||
                loopTest.getTargets().get(0) != statements.get(testIndex + 1)) {
            return false;
        }
        boolean hasBodySource = false;
        for (Op03SimpleStatement source : loopTest.getSources()) {
            if (source == conditional) {
                continue;
            }
            if (source.getIndex().compareTo(bodyStart.getIndex()) < 0 ||
                    source.getIndex().compareTo(loopTest.getIndex()) >= 0) {
                return false;
            }
            hasBodySource = true;
        }
        return hasBodySource;
    }

    private static void exposeLoopEntryAfterEarlyReturn(List<Op03SimpleStatement> statements) {
        for (int index = 0; index < statements.size() - 3; ++index) {
            Op03SimpleStatement conditional = statements.get(index);
            if (conditional.getStatement().getClass() != IfStatement.class ||
                    conditional.getTargets().size() != 2) {
                continue;
            }
            Op03SimpleStatement earlyReturn = statements.get(index + 1);
            if (earlyReturn != conditional.getTargets().get(0) ||
                    !earlyReturn.getTargets().isEmpty()) {
                continue;
            }
            Op03SimpleStatement bodyStart = statements.get(index + 2);
            if (!isSharedEclipseLoopTest(statements, conditional, bodyStart)) {
                continue;
            }

            Op03SimpleStatement loopTest = conditional.getTargets().get(1);
            // We expose the entry to the existing Eclipse pass without copying the loop condition.
            Op03SimpleStatement entry = new Op03SimpleStatement(
                    conditional.getBlockIdentifiers(), new GotoStatement(BytecodeLoc.NONE),
                    earlyReturn.getIndex().justAfter());
            entry.addSource(conditional);
            entry.addTarget(loopTest);
            conditional.replaceTarget(loopTest, entry);
            loopTest.replaceSource(conditional, entry);
            statements.add(index + 2, entry);
            index += 2;
        }
    }

    public static void eclipseLoopPass(List<Op03SimpleStatement> statements) {
        exposeLoopEntryAfterEarlyReturn(statements);
        boolean effect = false;
        for (int x = 0, len = statements.size() - 1; x < len; ++x) {
            Op03SimpleStatement statement = statements.get(x);
            Statement inr = statement.getStatement();
            if (inr.getClass() != GotoStatement.class) continue;

            Op03SimpleStatement target = statement.getTargets().get(0);
            if (target == statement) continue; // hey, paranoia.

            if (target.getIndex().isBackJumpFrom(statement)) continue;
            Statement tgtInr = target.getStatement();
            if (tgtInr.getClass() != IfStatement.class) continue;
            IfStatement ifStatement = (IfStatement) tgtInr;

            Op03SimpleStatement bodyStart = statements.get(x + 1);
            if (bodyStart != ifStatement.getJumpTarget().getContainer()) continue;

            for (Op03SimpleStatement source : target.getSources()) {
                InstrIndex sourceIdx = source.getIndex();
                if (sourceIdx.isBackJumpFrom(statement) ||
                        sourceIdx.isBackJumpTo(target)) continue;
            }
            Op03SimpleStatement afterTest = target.getTargets().get(0);
//            // This has to be a fall through
//            if (statements.indexOf(afterTest) != statements.indexOf(target) + 1) continue;

            // OK - we're in the right boat!
            IfStatement topTest = new IfStatement(ifStatement.getLoc(), ifStatement.getCondition().getNegated().simplify());
            statement.replaceStatement(topTest);
            statement.replaceTarget(target, bodyStart);
            bodyStart.addSource(statement);
            statement.addTarget(afterTest);
            afterTest.replaceSource(target, statement);
            target.replaceStatement(new Nop());
            target.removeSource(statement);
            target.removeTarget(afterTest);
            target.replaceTarget(bodyStart, statement);
            target.replaceStatement(new GotoStatement(BytecodeLoc.NONE));
            bodyStart.removeSource(target);
            statement.addSource(target);

            effect = true;
        }

        if (effect) {
            Op03Rewriters.removePointlessJumps(statements);
        }
    }

}
