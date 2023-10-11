package edu.njupt.flowanalysis;

import com.opencsv.exceptions.CsvException;
import edu.njupt.flowanalysis.generators.CGGenerator;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.results.DataFlowResult;
import soot.jimple.infoflow.results.InfoflowResults;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.Set;

import static edu.njupt.flowanalysis.managers.FileManager.*;

public class FlowAnalysis {
    private static String jarPath = "C:\\Users\\14391\\AppData\\Local\\Android\\Sdk\\platforms\\android-32\\android.jar";
    private static String absolutePathPre = "G:/Codes/UndergraduateThesis/LeakDetectCore/";
    public static long runInfoflowTime = 0;

    /**
     *
     * @param apkPath : the apk file
     */
    public static InfoflowResults analyzeApk(String apkPath, String sourcesAndSinksPath, Boolean entireProcess, Boolean query, Boolean callGraph) throws Exception {
        // Preparations
        String apkName = getApkName(apkPath);
        checkFolders(apkName);

        // 配置设定
        InfoflowAndroidConfiguration infoflowAndroidConfiguration = new InfoflowAndroidConfiguration();
        infoflowAndroidConfiguration.getAnalysisFileConfig().setAndroidPlatformDir(jarPath);
        infoflowAndroidConfiguration.getAnalysisFileConfig().setTargetAPKFile(apkPath);
//        infoflowAndroidConfiguration.setOneComponentAtATime(true);

        // start infoflow analysis
//        SetupApplication app = new SetupApplication(jarPath, apkPath);
        SetupApplication app = new SetupApplication(infoflowAndroidConfiguration);

        // Create CallGraph
        if(callGraph) {
            System.out.println("正在生成cg图...");
            CGGenerator.createCG(apkPath, jarPath, app, "results/flowAnalysis/" + apkName);
        }

        // Find sources and sinks
        if(sourcesAndSinksPath.isEmpty()) {
            String apkDetect = "results/sourceSink/apkResults/" + apkName + "/apkDetect.txt";
            findSourceSink(apkPath, apkDetect, entireProcess, query);
            sourcesAndSinksPath = apkDetect;
        }

        //runInfoflow包含了污点分析的过程，即数据流分析
        String logFile = "results/flowAnalysis/" + apkName + "/flowdroid.log";
        try {
            File file = new File(logFile);
            if (!file.exists()) {
                file.createNewFile();
            }

            if(query){
                Scanner scanner = new Scanner(System.in);
                System.out.print("开始静态污点分析? [y/n]: ");
                if(scanner.next().equals("n"))
                    return null;
            }
            System.out.println("正在进行静态污点分析(根据icfg, 源与汇列表)...");
            long startTime = System.currentTimeMillis();
//            InfoflowResults infoflowResults = app.runInfoflow("resources/SourcesAndSinks.txt");
            InfoflowResults infoflowResults = app.runInfoflow(sourcesAndSinksPath);
            long endTime = System.currentTimeMillis();
            runInfoflowTime = endTime - startTime;

            // Print the InfoflowResults
            System.out.println("FlowDroid发现" + infoflowResults.size() + "个泄露.");

            // Print the entryPoints
            app.printEntrypoints();

            FileWriter fileWriter = new FileWriter(logFile, false);
            infoflowResults.printResults(fileWriter);
            fileWriter.close();

//            getSinkAccessPath(infoflowResults, logFile);

            return infoflowResults;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getApkName(String apkPath){
        File apkFile = new File(apkPath);
        return apkFile.getName().replace(".apk","");
    }

    /**
     * check folders under results/flowAnalysis and results/sourceSink/apkResults
     * @param apkName
     */
    public static void checkFolders(String apkName){
        String[] paths = {
                absolutePathPre + "results/flowAnalysis/" + apkName,
                absolutePathPre + "results/sourceSink/apkResults/" + apkName};

        for (int i = 0; i < paths.length; i++) {
            String path = paths[i];
            File folder = new File(path);
            if (!folder.exists()) {
                if (folder.mkdir()) {
                    System.out.println(path + " has been created.");
                } else {
                    System.out.println(path + " cannot be created.");
                }
            } else {
                System.out.println(path + " exists.");
            }
        }
    }

    public static void findSourceSink(String apkPath, String apkDetect, Boolean entireProcess, Boolean query) throws Exception {
        if (entireProcess || ! new File(apkDetect).exists()) {
            String trainResource[] = {absolutePathPre + "resources/permissionMethodWithLabel.pscout"};
            String modelPath = absolutePathPre + "resources/models";

            // print info
            System.out.print("SuSi模型训练数据: ");
            for(String trainFile : trainResource){
                System.out.print(trainFile + " ");
            }
            System.out.println("训练完毕后的模型文件: " + modelPath);

            if(query) {
                Scanner scanner = new Scanner(System.in);
                System.out.print("开始生成" + apkPath + "对应的源与汇列表? [y/n]: ");
                if (scanner.next().equals("n"))
                    return;
            }

            SourceSinkParser sourceSinkParser = new SourceSinkParser();
            sourceSinkParser.setConfiguration(modelPath, jarPath, apkDetect);
            sourceSinkParser.apkSourceSinkParser(apkPath, trainResource);

            // copy SourcesAndSinks.txt to apkDetect.txt
            copyFile(absolutePathPre + "resources/SourcesAndSinks.txt", apkDetect);
//            copyFile("testSuites/DroidBench/sourcesAndSinks.txt", apkDetect);

            System.out.println("源与汇列表生成完毕: " + apkDetect);
        }
    }

    private static void copyFile(String sourceFile, String destFile){
        try {
            BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(destFile, true));
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
            reader.close();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void getSinkAccessPath(InfoflowResults infoflowResults, String logFile) throws IOException {
        Set<DataFlowResult> dataFlowResults= infoflowResults.getResultSet();
        for(DataFlowResult result : dataFlowResults){
            AccessPath path = result.getSink().getAccessPath();
            String content = "数据流路径: " + path.toString();
            writeToFile(logFile, true, content);
        }
    }

    public static void writeToFile(String filePath, Boolean append, String content) throws IOException {
        FileWriter fileWriter = new FileWriter(filePath, append);
        fileWriter.write(content);
        fileWriter.close();
    }

    public static long getRunInfoflowTime(){
        return runInfoflowTime;
    }

    public static void recordInfoflowResultsToCSV(String apkName, String csvPath, InfoflowResults infoflowResults) throws IOException, CsvException {
        File csvFile = new File(csvPath);
        if(!csvFile.exists()){
            String[] header = {"Date", "Apk Name", "Number of leaks","Infoflow Results"};
            writeToCSV(header, csvPath, false);
        }
        else{
            deleteLineInCSV(apkName, csvPath);
        }
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String time = formatter.format(date);
        String leakNum = String.valueOf(infoflowResults.size());
        String leakResults = readLog(absolutePathPre + "results/flowAnalysis/" + apkName + "/flowdroid.log");

        String[] rowData = {time, apkName, leakNum, leakResults};
        writeToCSV(rowData, csvPath ,true);
    }

}
