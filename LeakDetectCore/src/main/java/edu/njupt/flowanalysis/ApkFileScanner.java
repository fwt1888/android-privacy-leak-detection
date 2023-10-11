package edu.njupt.flowanalysis;

import soot.jimple.infoflow.results.InfoflowResults;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ApkFileScanner {
    public static final Boolean query = false;

    public static void main(String[] args) throws Exception {

        // scan DroidBench with whole system
//        File root = new File("testSuites");
//        scanDBWithWholeSystem(root);

        // scan DroidBench with FlowDroid only
//        File root = new File("G:\\Codes\\UndergraduateThesis\\DroidBench3.0\\apk");
//        scanWithFlowDroidOnly(root);

        // scan TaintBench with FlowDroid only
//        File root = new File("G:\\Codes\\UndergraduateThesis\\TaintBench\\apks");
//        scanTBWithFlowDroidOnly(root);

        // scan TaintBench with whole system
        File root = new File("G:\\Codes\\UndergraduateThesis\\TaintBench\\apks");
        scanTBWithWholeSystem(root);
    }

    public static void scanDBWithFlowDroidOnly(File dir) throws Exception {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    scanDBWithFlowDroidOnly(file);
                }
            }
        } else if (dir.isFile() && dir.getName().endsWith(".apk")) {
            String apkPath = dir.getAbsolutePath();
            String apkName = dir.getName().replace(".apk","");
            System.out.println("File name: " + dir.getName());
            System.out.println("File path: " + apkPath);
            String androidJAR = "C:\\Users\\14391\\AppData\\Local\\Android\\Sdk\\platforms\\android-32\\android.jar";
            try {
                InfoflowResults infoflowResults = FlowAnalysis.analyzeApk(apkPath, "",
                        true, false, false);
                FlowAnalysis.recordInfoflowResultsToCSV(apkName, "results/flowdroidResults.csv", infoflowResults);
            }
            catch (Exception e){
                System.out.println(e);
            }
        }
    }

    public static void scanDBWithWholeSystem(File dir) throws Exception {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    scanDBWithWholeSystem(file);
                }
            }
        } else if (dir.isFile() && dir.getName().endsWith(".apk")) {
            String apkPath = dir.getAbsolutePath();
            String apkName = dir.getName().replace(".apk","");
            System.out.println("File name: " + dir.getName());
            System.out.println("File path: " + apkPath);

            String traceLogPath = "results/slicer/" + apkName + "/trace.log";
            if(! new File(traceLogPath).exists())
                prepareForSlicing(apkPath);
            else if (Files.size(new File(traceLogPath).toPath()) == 0)
                prepareForSlicing(apkPath);
            else
                System.out.println("Preparaions for slicing has been done.");

            DynamicSlicer.storeLogBackup(apkName);

            File logFile = new File(traceLogPath);
            long logSize = Files.size(logFile.toPath());
            String coAnalysisPath = "results/slicer/" + apkName + "/coAnalysis.csv";
            if(logSize != 0 ){
//                    && ! new File(coAnalysisPath).exists()){
                runWholeSystem(apkPath, "","results/timeCount.csv", "results/coResults.csv");
            }


        }
    }

    public static void prepareForSlicing(String apkPath) throws IOException {
        try {
            edu.njupt.flowanalysis.DynamicSlicer.sliceApkByMandoline(apkPath, false, query);
        }catch(Exception e){
            System.out.println(e);
        }
    }

    public static void runWholeSystem(String apkPath, String sourcesAndSinksPath, String timeCountCSVPath, String coResultsPath) throws Exception {
        String androidJAR = "C:\\Users\\14391\\AppData\\Local\\Android\\Sdk\\platforms\\android-32\\android.jar";
        InfoflowResults infoflowResults = FlowAnalysis.analyzeApk(apkPath, sourcesAndSinksPath,
                false, false, false);
        infoflowResults.printResults();

        String label="none";
        if(apkPath.contains("correct"))
            label="correct";
        if(apkPath.contains("false"))
            label="false";
        DynamicSlicer.sliceApkByMandoline(apkPath, false, query, infoflowResults,label,coResultsPath);
        DynamicSlicer.writeTimeCountCSV(apkPath, timeCountCSVPath);
    }

    public static void scanTBWithFlowDroidOnly(File dir) throws Exception {
        String dirPath = "G:\\Codes\\UndergraduateThesis\\TaintBench\\";
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    scanTBWithFlowDroidOnly(file);
                }
            }
        } else if (dir.isFile() && dir.getName().endsWith(".apk")) {
            String apkPath = dir.getAbsolutePath();
            String apkName = dir.getName().replace(".apk","");
            System.out.println("File name: " + dir.getName());
            System.out.println("File path: " + apkPath);
            String androidJAR = "C:\\Users\\14391\\AppData\\Local\\Android\\Sdk\\platforms\\android-32\\android.jar";
            try {
                InfoflowResults infoflowResults = FlowAnalysis.analyzeApk(apkPath,
                        dirPath + "sourcesAndSinks.txt",
                        false, false, false);
                FlowAnalysis.recordInfoflowResultsToCSV(apkName,
                        dirPath + "results\\flowdroidResults.csv",
                        infoflowResults);
            }
            catch (Exception e){
                System.out.println(e);
            }
        }
    }

    public static void scanTBWithWholeSystem(File dir) throws Exception {
        String dirPath = "G:\\Codes\\UndergraduateThesis\\TaintBench\\";
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    scanTBWithWholeSystem(file);
                }
            }
        } else if (dir.isFile() && dir.getName().endsWith(".apk")) {
            String apkPath = dir.getAbsolutePath();
            String apkName = dir.getName().replace(".apk","");
            System.out.println("File name: " + dir.getName());
            System.out.println("File path: " + apkPath);

            String traceLogPath = "results/slicer/" + apkName + "/trace.log";
            if(! new File(traceLogPath).exists())
                prepareForSlicing(apkPath);
            else if (Files.size(new File(traceLogPath).toPath()) == 0)
                prepareForSlicing(apkPath);
            else
                System.out.println("Preparaions for slicing has been done.");

            DynamicSlicer.storeLogBackup(apkName);

            File logFile = new File(traceLogPath);
            long logSize = Files.size(logFile.toPath());
            String coAnalysisPath = "results/slicer/" + apkName + "/coAnalysis.csv";
            if(logSize != 0 ){
//                    && ! new File(coAnalysisPath).exists()){
                runWholeSystem(apkPath, dirPath + "sourcesAndSinks.txt",
                        dirPath + "results\\timeCount.csv",
                        dirPath + "results\\coResults.csv");
            }

        }
    }
}

