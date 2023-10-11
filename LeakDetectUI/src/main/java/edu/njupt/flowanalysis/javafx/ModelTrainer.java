package edu.njupt.flowanalysis.javafx;

import edu.njupt.flowanalysis.SourceSinkParser;

import javafx.scene.control.Label;

import javafx.scene.paint.Color;


import java.io.File;

public class ModelTrainer {
    private static String absolutePathPre = "G:/Codes/UndergraduateThesis/LeakDetectCore/";
    private static String jarPath = "C:\\Users\\14391\\AppData\\Local\\Android\\Sdk\\platforms\\android-32\\android.jar";
    private static String trainDetect = absolutePathPre + "results/sourceSink/trainResults/trainDetect.txt";
    private static String[] trainResource = {absolutePathPre + "resources/permissionMethodWithLabel.pscout"};
    public static String modelPath = absolutePathPre + "resources/models";

    public static void trainModel() {

        SourceSinkParser sourceSinkParser = new SourceSinkParser();
        sourceSinkParser.setConfiguration(modelPath, jarPath, null);

        sourceSinkParser.trainSusi(trainResource, trainDetect);
    }

    public static Boolean searchModel(){
        String[] models ={ modelPath + "/SinkCatModel.model",
                modelPath + "/SourceCatModel.model",
                modelPath + "/SourceSinkModel.model"
        };
        for(String model : models){
            File file = new File(model);
            if(!file.exists())
                return false;
        }
        return true;
    }

    public static void changeLabelStatus(Label label){
        if(searchModel()){
            label.setText("--已检测到预生成的模型文件--");
            label.setTextFill(Color.GREEN);
        }else{
            label.setText("--未检测到模型文件, 请进行模型训练--");
            label.setTextFill(Color.DARKRED);
        }
    }
}
