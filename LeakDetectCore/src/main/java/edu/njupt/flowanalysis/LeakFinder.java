package edu.njupt.flowanalysis;

import soot.jimple.infoflow.results.InfoflowResults;

import java.io.File;
import java.util.Scanner;

public class LeakFinder {
    public static void main(String[] args) throws Exception {
        String nextLoop = "y";
        String flowdroid = "y";
        String mandoline = "y";
        String apkFile = "testSuites/DroidBench/";
        String coResultsPath = "results/coResults.csv";

        // start avd first
//        edu.njupt.flowanalysis.managers.AVDManager.runAVD();

        // main loop
        while(nextLoop.equals("y")){
            // choose apk from testSuites
            System.out.print("请从测试集中选取一个apk文件: ");
            Scanner scanner = new Scanner(System.in);
            String apkPath = apkFile + scanner.next();

            // static analysis by FlowDroid
            System.out.print("启动FlowDroid进行静态污点分析? [y/n]: ");
            flowdroid = scanner.next();
            if(flowdroid.equals("y")) {
                InfoflowResults infoflowResults = FlowAnalysis.analyzeApk(apkPath, "",
                        true, true, true);
                infoflowResults.printResults();

                // dynamic slicing by Mandoline
                System.out.print("启动Mandoline对" + apkPath + "进行动态切片? [y/n]: ");
                mandoline = scanner.next();
                if(mandoline.equals("y")) {
                    String label="none";
                    if(apkPath.contains("correct"))
                        label = "correct";
                    if(apkPath.contains("false"))
                        label = "false";
                    DynamicSlicer.sliceApkByMandoline(apkPath, true, true, infoflowResults, label,
                            coResultsPath);
                    DynamicSlicer.writeTimeCountCSV(apkPath, "results/timeCount.csv");
                }

                // print all results
                String apkName = new File(apkPath).getName().replace(".apk","");
                String flowAnalysisDir = "results/flowAnalysis/" + apkName;
                String sourceSinkDir = "results/sourceSink/apkResults/" + apkName;
                String slicerDir = "results/slicer/" + apkName;

                // icfg + infoflowResults
                System.out.println("所有分析结果如下:");
                if(flowdroid.equals("y")) {
                    System.out.println("===== FlowDroid静态污点分析结果 =====");
                    System.out.println("Call Graph: " +
                            getAbsolutePathOfFile(flowAnalysisDir + "/" + apkName + ".puml"));
                    System.out.println("源与汇列表: " +
                            getAbsolutePathOfFile(sourceSinkDir + "/apkDetect.txt"));
                    System.out.println("FlowDroid分析结果: " +
                            getAbsolutePathOfFile(flowAnalysisDir + "/flowdroid.log"));
                }
                if(mandoline.equals("y")) {
                    System.out.println("===== Mandoline动态切片结果 =====");
                    System.out.println("静态分析结果: " + getAbsolutePathOfFile(slicerDir + "/static-log.log"));
                    System.out.println("插桩后的运行结果: " + getAbsolutePathOfFile(slicerDir + "/trace.log"));
                    System.out.println("切片结果: " + getAbsolutePathOfFile(slicerDir + "/raw-slice.log"));
                }
                if(flowdroid.equals("y") && mandoline.equals("y")){
                    System.out.println("===== Results of collaborative analysis =====");
                    System.out.println("合作分析结果: " + getAbsolutePathOfFile(slicerDir + "/coAnalysis.csv"));
                    System.out.println("合作分析汇总: " + getAbsolutePathOfFile("results/coResults.csv"));
                    System.out.println("运行时长统计: " + getAbsolutePathOfFile("results/timeCount.csv"));
                }
            }

            // continue/stop the loop
            System.out.print("继续分析下一个apk文件? [y/n]: ");
            nextLoop = scanner.next();
        }

        // stop the avd
//        edu.njupt.flowanalysis.managers.AVDManager.stopAVD(edu.njupt.flowanalysis.managers.AVDManager.getDeviceId());
    }

    public static String getAbsolutePathOfFile(String filePath){
        File file = new File(filePath);
        return file.getAbsolutePath();
    }
}
