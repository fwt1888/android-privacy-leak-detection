package edu.njupt.flowanalysis.javafx;

import com.opencsv.exceptions.CsvException;
import edu.njupt.flowanalysis.AVDController;
import edu.njupt.flowanalysis.DynamicSlicer;
import edu.njupt.flowanalysis.managers.AVDManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class MenuController {
    public StackPane stackPane;

    public void showFlowDroidConfig(){
        deleteAllInPane();

        VBox vBox = new VBox();
        vBox.setSpacing(10);
        Label label = new Label("本项目使用的FlowDroid版本为2.11.0, " +
                "以下为FlowDroid的运行配置");
        setFont(label);
        vBox.getChildren().add(label);
        FlowDroidConfig.showConfig(vBox);

        stackPane.getChildren().add(vBox);
    }

    public void androidEmulator() throws IOException {
        deleteAllInPane();
        VBox vBox = new VBox();
        vBox.setSpacing(25);
        vBox.setAlignment(Pos.TOP_CENTER);

        Label label = new Label("模拟器型号: Pixel_2_API_28\n"
                + "系统镜像: Android 9.0 Google APIs\n" + "Ram Size: 1536\n" + "Heap Size: 256");
        setFont(label);
        vBox.getChildren().add(label);

        Label status = new Label();
        Button button = new Button();
        button.setOnAction(actionEvent -> {
            try {
                AVDController.main(null);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
        setFont(status);
        setEmulatorStatus(button, status);

        vBox.getChildren().add(status);
        vBox.getChildren().add(button);

        stackPane.getChildren().add(vBox);
    }

    public void trainModel(){
        deleteAllInPane();
        VBox vBox = new VBox();
        vBox.setSpacing(15);
        vBox.setAlignment(Pos.TOP_CENTER);

        Label label1 = new Label("SuSi——自动发现和分类Android框架中的源和汇");
        setFont(label1);

        String linkFile = "G:\\Codes\\UndergraduateThesis\\LeakDetectCore\\resources\\permissionMethodWithLabel.pscout";
        Hyperlink trainlink = new Hyperlink("手工标注的训练集: permissionMethodWithLabel.pscout");
        trainlink.setOnAction(e -> {
            try {
                Desktop.getDesktop().open(new File(linkFile));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        trainlink.setFont(Font.font("System", FontWeight.BOLD, 14));

        Label label2 = new Label("训练完毕后将生成以下三个模型文件:\n" +
                "1. SinkCatModel.model  汇点分类模型\n" +
                "2. SourceCatModel.model  源点分类模型\n" +
                "3. SourceSinkModel.model  源与汇识别模型");
        setFont(label2);

        Button button1 = new Button("训练机器学习模型");
        button1.setOnAction(actionEvent -> {
            showProcessDialog();
        });

        vBox.getChildren().addAll(label1, trainlink, label2, button1);

        HBox hBox = new HBox();
        hBox.setSpacing(30);
        hBox.setAlignment(Pos.CENTER);

        Label label3 = new Label(); //检测是否已有预生成模型
        setFont(label3);
        ModelTrainer.changeLabelStatus(label3);

        Button button2 = new Button("打开输出文件夹");
        button2.setOnAction(actionEvent -> {
            try {
                java.awt.Desktop.getDesktop().open(new File(ModelTrainer.modelPath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        hBox.getChildren().addAll(label3, button2);

        vBox.getChildren().add(hBox);


        stackPane.getChildren().addAll(vBox);
    }


    public void leakFinder(){
        deleteAllInPane();

        VBox vBox = new VBox();
        vBox.setSpacing(40);

        Label mainLabel = new Label("请从测试集中选取一个apk文件");
        setFont(mainLabel);

        Label target = new Label();
        setFont(target);
        showTargetApk(target);

        HBox hBox = new HBox();
        hBox.setSpacing(50);
        VBox vBox1 = new VBox();
        vBox1.setSpacing(10);
        VBox vBox2 = new VBox();
        vBox2.setSpacing(10);

        // vBox1: DroidBench
        Label label1 = new Label("DroidBench3.0\n" +
                "--人造应用程序基准测试套件--");
        setFont(label1);

        Button button1 = new Button("选取apk");
        button1.setOnAction(e -> {
            chooseApk("G:\\Codes\\UndergraduateThesis\\LeakDetectCore\\testSuites\\DroidBench");
            showTargetApk(target);
        });
        vBox1.getChildren().addAll(label1, button1);

        // vBox2: TaintBench
        Label label2 = new Label("TaintBench\n" +
                "--真实世界恶意软件基准套件--");
        setFont(label2);

        Button button2 = new Button("选取apk");
        button2.setOnAction(e -> {
            chooseApk("G:\\Codes\\UndergraduateThesis\\TaintBench\\apks");
            showTargetApk(target);
        });
        vBox2.getChildren().addAll(label2, button2);


        // vBox
        Button nextStep = new Button("下一步");
        nextStep.setOnAction(e -> {
            try {
                LeakFinderProcess.wholeProcess(vBox, nextStep);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        hBox.getChildren().addAll(vBox1, vBox2);
        vBox.getChildren().addAll(mainLabel, hBox, target);
        stackPane.getChildren().addAll(vBox, nextStep);

        vBox1.setAlignment(Pos.TOP_CENTER);
        vBox2.setAlignment(Pos.TOP_CENTER);
        hBox.setAlignment(Pos.TOP_CENTER);
        vBox.setAlignment(Pos.TOP_CENTER);
        StackPane.setAlignment(vBox, Pos.TOP_CENTER);
        StackPane.setAlignment(nextStep, Pos.BOTTOM_RIGHT);
        stackPane.setPadding(new Insets(20));

    }

    public void historyDBData() throws IOException, CsvException {
        deleteAllInPane();

        VBox vBox = new VBox();
        vBox.setSpacing(15);

        HistoryDataViewer.showHistoryData("DB", vBox);
        stackPane.getChildren().add(vBox);
        vBox.setAlignment(Pos.TOP_CENTER);
    }

    public void historyTBData() throws IOException, CsvException {
        deleteAllInPane();

        VBox vBox = new VBox();
        vBox.setSpacing(15);

        HistoryDataViewer.showHistoryData("TB", vBox);
        stackPane.getChildren().add(vBox);
        vBox.setAlignment(Pos.TOP_CENTER);
    }

    private void deleteAllInPane(){
        stackPane.getChildren().clear();
    }

    private void setEmulatorStatus(Button button, Label status) throws IOException {
        if(AVDManager.getDeviceId() == null){
            status.setText("模拟器未启动");
            status.setTextFill(Color.DARKRED);
            button.setText("启动模拟器");
        }else{
            status.setText("模拟器正在运行...");
            status.setTextFill(Color.GREEN);
            button.setText("关闭模拟器");
        }
    }

    public static void setFont(Label label){
        label.setFont(Font.font("System", FontWeight.BOLD, 14));
    }

    private void showProcessDialog(){
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);

        ProgressBar progressBar = new ProgressBar();
        Button cancelButton = new Button("中止程序运行");

        VBox vBox = new VBox(progressBar, cancelButton);
        vBox.setAlignment(Pos.CENTER);
        vBox.setSpacing(10);

        Scene dialogScene = new Scene(vBox, 200, 100);
        dialog.setScene(dialogScene);
        dialog.setTitle("运行提示");
        dialog.show();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                ModelTrainer.trainModel();
                return null;
            }
        };

        Thread thread = new Thread(task);
        thread.start();

        cancelButton.setOnAction(e -> {
            task.cancel();
        });

        task.setOnSucceeded(e -> {
            vBox.getChildren().clear();
            Label label = new Label("训练程序已运行完毕.");
            setFont(label);
            label.setTextFill(Color.GREEN);
            vBox.getChildren().add(label);
        });

        task.setOnCancelled(e -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            System.exit(0);
        });

        progressBar.progressProperty().bind(task.progressProperty());

    }

    private void chooseApk(String dirPath){
        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("Open File");
        fileChooser.setInitialDirectory(new File(dirPath));

        FileChooser.ExtensionFilter apkFilter = new FileChooser.ExtensionFilter("Apk Files", "*.apk");
        fileChooser.getExtensionFilters().add(apkFilter);

        File selectedFile = fileChooser.showOpenDialog(new Stage());

        if (selectedFile != null) {
            LeakFinderProcess.apkPath =  selectedFile.getAbsolutePath();
        }
    }

    private void showTargetApk(Label label){
        if(LeakFinderProcess.apkPath != null){
            label.setText("当前选定的Android应用:\n" + LeakFinderProcess.apkPath);
            label.setTextFill(Color.GREEN);
        }else{
            label.setText("未选定待分析的Android应用");
            label.setTextFill(Color.DARKRED);
        }
    }

}
