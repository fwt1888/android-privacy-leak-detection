package edu.njupt.flowanalysis.javafx;

import com.opencsv.exceptions.CsvException;
import edu.njupt.flowanalysis.DynamicSlicer;
import edu.njupt.flowanalysis.FlowAnalysis;
import edu.njupt.flowanalysis.generators.CGGenerator;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;


import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import net.sourceforge.plantuml.cucadiagram.Link;
import org.xmlpull.v1.XmlPullParserException;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.results.InfoflowResults;


import java.awt.*;
import java.io.*;

import static edu.njupt.flowanalysis.FlowAnalysis.*;

public class LeakFinderProcess {
    public static VBox vBox;
    public static Button next;
    public static Label label1;
    public static Label label2;
    public static Button button;

    public static String apkPath = null;
    public static String jarPath = "C:\\Users\\14391\\AppData\\Local\\Android\\Sdk\\platforms\\android-32\\android.jar";
    public static String absolutePathPre = "G:/Codes/UndergraduateThesis/LeakDetectCore/";

    private static String sourcesAndSinksPath = null;
    private static SetupApplication app;
    public static InfoflowResults infoflowResults;

    public static void wholeProcess(VBox vBox, Button next) throws IOException {
        LeakFinderProcess.vBox = vBox;
        LeakFinderProcess.next = next;

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
        app = new SetupApplication(infoflowAndroidConfiguration);

        createCGProcess(apkName);

    }

    public static void createCGProcess(String apkName) throws IOException {
        // 场景一
        vBox.getChildren().clear();
        label1 = new Label("首先使用FlowDroid进行静态污点分析, 分析步骤如下:\n" +
                "1. 生成icfg图\n" + "2. 向训练好的SuSi模型输入apk文件, 生成源与汇列表\n" +
                "3. 基于以上二者进行静态污点分析, 检测是否有数据泄露流");
        MenuController.setFont(label1);
        label2 = new Label("当前状态: 未生成Call Graph");
        label2.setTextFill(Color.DARKRED);
        MenuController.setFont(label2);
        button = new Button("图生成");
        button.setOnAction(e -> {
            String outDir = absolutePathPre + "results/flowAnalysis/" + apkName;
            try {
                CGGenerator.createCG(apkPath, jarPath, app, outDir);
                label2.setText("已生成Call Graph");
                label2.setTextFill(Color.GREEN);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        vBox.getChildren().addAll(label1, label2, button);
        next.setOnAction(e -> {
            if(apkPath.contains("DroidBench"))
                parseDBSourceSinkProcess(apkName);
            else if(apkPath.contains("TaintBench"))
                parseTBSourceSinkProcess(apkName);
        });
    }

    public static void parseDBSourceSinkProcess(String apkName){
        label2.setText("当前状态: 未生成apkDetect.txt");
        label2.setTextFill(Color.DARKRED);
        button.setText("生成源与汇列表");

        String apkDetect = absolutePathPre + "results/sourceSink/apkResults/" + apkName + "/apkDetect.txt";
        sourcesAndSinksPath = apkDetect;
        button.setOnAction(e -> {
            try {
                findSourceSink(apkPath, apkDetect, true, false);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            label2.setText("当前状态: apkDetect.txt生成完毕");
            label2.setTextFill(Color.GREEN);
        });

        next.setOnAction(e -> runFlowDroid(apkName));
    }

    public static void parseTBSourceSinkProcess(String apkName){
        label2.setText("当前状态: 已检测到预定义的sourcesAndSinks.txt\n" +
                "源与汇列表来源: TaintBench提供的Lists of potential sources and sinks");
        label2.setTextFill(Color.GREEN);
        button.setText("查看源与汇列表");

        sourcesAndSinksPath = "G:/Codes/UndergraduateThesis/TaintBench/sourcesAndSinks.txt";
        button.setOnAction(e -> {
            try {
                Desktop.getDesktop().open(new File(sourcesAndSinksPath));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        next.setOnAction(e -> runFlowDroid(apkName));
    }

    public static void runFlowDroid(String apkName){
        label2.setText("当前状态: 未进行静态污点分析");
        label2.setTextFill(Color.DARKRED);
        button.setText("静态污点分析");
        button.setOnAction(e -> {
                String logFile = absolutePathPre + "results/flowAnalysis/" + apkName + "/flowdroid.log";
                try {
                    File file = new File(logFile);
                    if (!file.exists()) {
                        file.createNewFile();
                    }

                    System.out.println("正在进行静态污点分析(根据icfg, 源与汇列表)...");
                    long startTime = System.currentTimeMillis();

                    String outDir = null;
                    if(apkPath.contains("DroidBench"))
                        outDir = absolutePathPre;
                    else if (apkPath.contains("TaintBench"))
                        outDir = "G:/Codes/UndergraduateThesis/TaintBench/";

                    try {
                        infoflowResults = app.runInfoflow(sourcesAndSinksPath);
                        FlowAnalysis.recordInfoflowResultsToCSV(apkName,
                                outDir + "results/flowdroidResults.csv", infoflowResults);
                    } catch (XmlPullParserException | CsvException ex) {
                        ex.printStackTrace();
                    }

                    long endTime = System.currentTimeMillis();
                    FlowAnalysis.runInfoflowTime = endTime - startTime;

                    // Print the InfoflowResults
                    System.out.println("FlowDroid发现" + infoflowResults.size() + "个泄露.");

                    // Print the entryPoints
                    app.printEntrypoints();

                    FileWriter fileWriter = new FileWriter(logFile, false);
                    infoflowResults.printResults(fileWriter);
                    fileWriter.close();

                    label2.setText("当前状态: 静态污点分析完毕\n" +
                            "FlowDroid发现" + infoflowResults.size() + "个泄露.");
                    label2.setTextFill(Color.GREEN);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });

        next.setOnAction(e -> showFlowDroidResults(apkName));
    }

    public static void showFlowDroidResults(String apkName){
        vBox.getChildren().clear();
        vBox.setSpacing(10);

        Label label = new Label("===== FlowDroid静态污点分析结果 =====");
        MenuController.setFont(label);

        String flowAnalysisDir = absolutePathPre + "results/flowAnalysis/" + apkName;
        String cgPath = flowAnalysisDir + "/" + apkName + ".puml";
        String fdResultPath = flowAnalysisDir + "/flowdroid.log";

        Hyperlink cgLink = new Hyperlink("1. Call Graph: \n" + cgPath);
        Hyperlink ssLink = new Hyperlink("2. 源与汇列表: \n" + sourcesAndSinksPath);
        Hyperlink fdLink = new Hyperlink("3. FlowDroid分析结果: \n" + fdResultPath);
        cgLink.setFont(Font.font("System", FontWeight.BOLD, 12));
        ssLink.setFont(Font.font("System", FontWeight.BOLD, 12));
        fdLink.setFont(Font.font("System", FontWeight.BOLD, 12));

        cgLink.setOnAction(e -> {
            try {
                PlantUmlViewer.viewUmlFile(cgPath);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        ssLink.setOnAction(e -> {
            try {
                Desktop.getDesktop().open(new File(sourcesAndSinksPath));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        fdLink.setOnAction(e -> {
            try {
                Desktop.getDesktop().open(new File(fdResultPath));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        javafx.scene.control.TextArea textArea = new javafx.scene.control.TextArea();
        textArea.setText(readLog(fdResultPath));
        textArea.setPrefHeight(100);
        textArea.setEditable(false);

        vBox.getChildren().addAll(label, cgLink, ssLink, fdLink, textArea);
        next.setOnAction(e -> {
            sliceApk();
        });
    }

    public static String readLog(String logPath){
        try {
            // 使用BufferedReader读取文件内容
            BufferedReader reader = new BufferedReader(new FileReader(logPath));

            // 创建一个StringBuilder对象，用于拼接文件内容
            StringBuilder stringBuilder = new StringBuilder();

            // 读取文件内容
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }

            // 关闭BufferedReader
            reader.close();

            return stringBuilder.toString();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void sliceApk(){
        vBox.getChildren().clear();
        vBox.setSpacing(40);

        label1 = new Label();
        checkInfoflowResults(label1);

        label2 = new Label("接着使用Mandoline进行动态程序切片, 切片步骤如下:\n" +
                "1. 对目标apk进行静态插桩并重新签名\n" + "2. 在安卓模拟器中安装apk, 并进行Monkey压力测试\n" +
                "3. 从模拟器日志中提取插桩运行结果, 并生成icdg\n" + "4. 根据FlowDroid分析结果生成切片准则\n" +
                "5. 基于icdg和切片准则进行后向切片\n" + "6. 根据程序切片判断泄露是否真实存在");
        MenuController.setFont(label1);
        MenuController.setFont(label2);

        button.setText("开始动态切片");
        button.setOnAction(e -> {
            try {
                SlicerInfoViewer.showConsoleInfo();
            } catch (UnsupportedEncodingException ex) {
                ex.printStackTrace();
            }
        });

        vBox.getChildren().addAll(label1, label2, button);
        next.setOnAction(e -> {
            showMandolineResults(getApkName(apkPath));
        });

    }

    public static void showMandolineResults(String apkName){
        vBox.getChildren().clear();
        vBox.setSpacing(10);

        Label label = new Label("===== Mandoline动态切片结果 =====");
        MenuController.setFont(label);

        String slicerDir = absolutePathPre + "results/slicer/" + apkName;
        String staticLog = slicerDir + "/static-log.log";
        String traceLog = slicerDir + "/trace.log";
        String rawSlice = slicerDir + "/raw-slice.log";
        String coResults = slicerDir + "/coAnalysis.csv";
        String icdgLog = slicerDir + "/trace.log_icdg.log";

        Hyperlink stLink = new Hyperlink("1. 静态分析结果: \n" + staticLog);
        Hyperlink trLink = new Hyperlink("2. 插桩后的运行结果: \n" + traceLog);
        Hyperlink icLink = new Hyperlink("3. 日志形式存储的icdg图: \n" + icdgLog);
        Hyperlink raLink = new Hyperlink("4. 切片结果: \n" + slicerDir);
        Hyperlink crLink = new Hyperlink("5. 合作分析结果: \n" + slicerDir);

        stLink.setFont(Font.font("System", FontWeight.BOLD, 12));
        trLink.setFont(Font.font("System", FontWeight.BOLD, 12));
        icLink.setFont(Font.font("System", FontWeight.BOLD, 12));
        raLink.setFont(Font.font("System", FontWeight.BOLD, 12));
        crLink.setFont(Font.font("System", FontWeight.BOLD, 12));

        stLink.setOnAction(e -> {
            try {
                Desktop.getDesktop().open(new File(staticLog));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        trLink.setOnAction(e -> {
            try {
                Desktop.getDesktop().open(new File(traceLog));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        icLink.setOnAction(e -> {
            try {
                Desktop.getDesktop().open(new File(icdgLog));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        raLink.setOnAction(e -> {
            try {
                Desktop.getDesktop().open(new File(rawSlice));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        crLink.setOnAction(e -> {
            try {
                Desktop.getDesktop().open(new File(coResults));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        vBox.getChildren().addAll(label, stLink, trLink, icLink, raLink, crLink);
        next.setText("Finish.");
    }

    public static void checkInfoflowResults(Label label){
        if(infoflowResults == null){
            label.setText("未获取FlowDroid分析结果");
            label.setTextFill(Color.DARKRED);
        }else{
            label.setText("已获取FlowDroid分析结果");
            label.setTextFill(Color.GREEN);
        }
    }
}

