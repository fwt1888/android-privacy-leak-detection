package edu.njupt.flowanalysis.generators;

import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.DefaultGraph;
import org.graphstream.ui.view.Viewer;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.dot.DotGraph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import static soot.util.dot.DotGraph.DOT_EXTENSION;

public class ICFGGenerator {
    public static void createICFG(SetupApplication app, String apkName) throws IOException {

        String outDir = "results/flowAnalysis/" + apkName;

        // get call graph
        CGGenerator.buildCallGraph(app);
        CallGraph cg = Scene.v().getCallGraph();

        //需要注意的是，这里生成的是一个基于Soot的ICFG的简化版本，
        // 是基于控制流图（Control Flow Graph，CFG）和调用图（Call Graph，CG）结合而成的，
        // 而不是严格意义上的ICFG，因为在这里没有考虑数据流和上下文敏感性。
        DefaultGraph graph = new DefaultGraph("ICFG");

        Set<String> visited = new HashSet<>();

        //向图中添加每个CG边
        for (Edge edge : cg) {
            SootMethod srcMethod = (SootMethod) edge.getSrc();
            SootMethod tgtMethod = (SootMethod) edge.getTgt();

            if (!visited.contains(srcMethod.toString())) {
                Node node = graph.addNode(srcMethod.toString());
                setNodeAttributes(srcMethod, node, outDir);
                visited.add(srcMethod.toString());
            }
            if (!visited.contains(tgtMethod.toString())) {
                Node node = graph.addNode(tgtMethod.toString());
                setNodeAttributes(tgtMethod, node, outDir);
                visited.add(tgtMethod.toString());
            }

            if(graph.getEdge(edge.getSrc().toString() + " -> " + edge.getTgt().toString()) == null) {
                //将边的源和目标节点连接起来
                graph.addEdge(edge.getSrc().toString() + " -> " + edge.getTgt().toString(),
                        (Node) graph.getNode(srcMethod.toString()), graph.getNode(tgtMethod.toString()), true);

                //将边的kind属性作为边的标签添加到图中
                graph.getEdge(edge.getSrc().toString() + " -> " + edge.getTgt().toString()).setAttribute("ui.label",
                        edge.kind().toString());
            }
        }

        Viewer viewer = graph.display();
        viewer.setCloseFramePolicy(Viewer.CloseFramePolicy.HIDE_ONLY);

    }

    public static void setNodeAttributes(SootMethod sm, Node node, String outDir){
        node.setAttribute("label", sm.getName());
        if (sm.isConcrete()) {

            DotGraph cfg = CFGGenerator.createCFGDotGraph(sm);
            String dotFile = outDir + "/" + String.valueOf(sm.getNumber()) + DOT_EXTENSION;
            cfg.plot(dotFile);
            String dotContent = readDotFile(dotFile);
//            System.out.println(dotContent);
            node.setAttribute("graph", dotContent);

            node.setAttribute("ui.style", "shape: box; fill-color: blue;");
        } else {
            node.setAttribute("ui.style", "fill-color: red;");
        }
    }

    public static String readDotFile(String filePath) {
        File file = new File(filePath);
        StringBuilder sb = new StringBuilder();

        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                sb.append(line);
                sb.append("\n");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }


}
