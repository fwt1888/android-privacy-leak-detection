package edu.njupt.flowanalysis.generators;

import soot.*;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.util.dot.DotGraph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class CFGGenerator {
    public static void createCFG(String androidJAR, String apkFile, SetupApplication app) {
        // Generate cg
        CGGenerator.buildCallGraph(app);
        CallGraph cg = Scene.v().getCallGraph();

        File resultFile = new File("./graphs/cfg.log");
        PrintWriter out = null;
        try {
            out = new PrintWriter(resultFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        assert out != null;
        out.println("CFG begins==================");

        // Generate CFGs for all methods in the call graph
        // sourceMethods: Returns an iterator over all methods that are the sources of at least one edge.
        for (Iterator<MethodOrMethodContext> it = cg.sourceMethods(); it.hasNext(); ) {
            SootMethod sm = (SootMethod) it.next();
            if (sm.isConcrete()) {
                Body body = sm.retrieveActiveBody();
                UnitGraph cfg = new BriefUnitGraph(body);
                out.println(sm.getName());
                for(Unit u : cfg){
                    out.println(u);
                }
            }
            out.println();
        }

        out.println("CFG ends==================");
        out.close();
    }

    public static DotGraph createCFGDotGraph(SootMethod sm){
        Body body = sm.retrieveActiveBody();
        UnitGraph cfg = new BriefUnitGraph(body);

        // Create new DotGraph object
        DotGraph cfgDot = new DotGraph(sm.getName());
        Set<String> visited = new HashSet<>();
        for(Unit u : cfg){
            if(! visited.contains(u.toString())) {
                cfgDot.drawNode(u.toString());
                visited.add(u.toString());
            }
            // Get the successors for the current node
            for (Unit succ : cfg.getSuccsOf(u)) {
                // Add an edge from the current node to its successor
                if(! visited.contains(succ.toString())) {
                    cfgDot.drawNode(succ.toString());
                    visited.add(succ.toString());
                }
                cfgDot.drawEdge(u.toString(), succ.toString());
            }

        }
        return cfgDot;
    }
}
