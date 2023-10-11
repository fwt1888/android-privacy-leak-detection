package edu.njupt.flowanalysis.generators;/*
 * Generate detailed Call Graph
 */


import org.apache.commons.io.FilenameUtils;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.dot.DotGraph;
import soot.util.queue.QueueReader;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.HashSet;
import java.util.Set;

import static soot.util.dot.DotGraph.DOT_EXTENSION;

public class CGGenerator {
    /**
     *
     * @param apkFile : the apk file
     * @param androidJAR : android.jar
     */
    public static void createCG(String apkFile, String androidJAR, SetupApplication app, String outDir) throws IOException {
        File file = new File(apkFile);
        String apkPath = file.getAbsolutePath();

        buildCallGraph(app);
        DotGraph dot = new DotGraph("callgraph");
        analyzeCG(dot, Scene.v().getCallGraph(), outDir);
        String dest = file.getName();
        String fileNameWithOutExt = FilenameUtils.removeExtension(dest);
        String destination = outDir + "/" + fileNameWithOutExt;
        dot.plot(destination + DOT_EXTENSION);

        createPumlFile(outDir + "/" + fileNameWithOutExt + ".dot",
                outDir + "/" + fileNameWithOutExt + ".puml");

        System.out.println("test");
    }

    /**
     * Iterate over the call Graph by visit edges one by one.
     * @param dot dot instance to create a dot file
     * @param cg call graph
     */
    public static void analyzeCG(DotGraph dot, CallGraph cg, String outDir) {
        QueueReader<Edge> edges = cg.listener();
        Set<String> visited = new HashSet<>();

        File resultFile = new File(outDir + "/cg.log");
        PrintWriter out = null;
        try {
            out = new PrintWriter(resultFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        assert out != null;
        out.println("CG begins==================");
        // iterate over edges of the call graph
        while (edges.hasNext()) {
            Edge edge = edges.next();
            SootMethod target = (SootMethod) edge.getTgt();
            MethodOrMethodContext src = edge.getSrc();
            if (!visited.contains(src.toString())) {
                dot.drawNode(src.toString());
                visited.add(src.toString());
            }
            if (!visited.contains(target.toString())) {
                dot.drawNode(target.toString());
                visited.add(target.toString());
            }
            out.println(src + "  -->   " + target);
            dot.drawEdge(src.toString(), target.toString());
        }

        out.println("CG ends==================");
        out.close();
        System.out.println(cg.size());
    }

    public static void buildCallGraph(SetupApplication app) {
//        SetupApplication app = new SetupApplication(platformDir, apkDir);
        app.constructCallgraph();
    }

    public  static void createPumlFile(String dotPath, String pumlPath) throws IOException {
        File pumlFile = new File(pumlPath);
        File dotFile = new File(dotPath);
        FileWriter out = null;
        try {
            out = new FileWriter(pumlPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        assert out != null;
        out.write("@startdot\n");
        out.close();
        copyFileUsingChannel(dotFile, pumlFile);
        out = new FileWriter(pumlPath, true);
        out.write("@enddot");
        out.close();
    }

    private static void copyFileUsingChannel(File source, File dest) throws IOException {
        FileChannel sourceChannel = null;
        FileChannel destChannel = null;
        try {
            sourceChannel = new FileInputStream(source).getChannel();
            destChannel = new FileOutputStream(dest, true).getChannel();
            destChannel.transferFrom(sourceChannel, destChannel.size(), sourceChannel.size());
        }finally{
            sourceChannel.close();
            destChannel.close();
        }
    }



}