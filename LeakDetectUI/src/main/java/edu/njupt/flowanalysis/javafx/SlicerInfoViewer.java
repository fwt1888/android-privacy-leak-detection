package edu.njupt.flowanalysis.javafx;

import com.opencsv.exceptions.CsvException;
import edu.njupt.flowanalysis.DynamicSlicer;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import static edu.njupt.flowanalysis.javafx.LeakFinderProcess.absolutePathPre;
import static edu.njupt.flowanalysis.javafx.LeakFinderProcess.apkPath;

public class SlicerInfoViewer {
    private static TextArea textArea;

    public static void showConsoleInfo() throws UnsupportedEncodingException {
        Stage viewer = new Stage();
        viewer.initModality(Modality.NONE);

        // Create a TextArea to show console output
        TextArea textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setEditable(false);

        // Create a ConsoleOutputStream to redirect console output to the TextArea
        ConsoleOutputStream consoleStream = new ConsoleOutputStream(textArea);
        System.setOut(new PrintStream(consoleStream, true, "UTF-8"));
        System.setErr(new PrintStream(consoleStream, true, "UTF-8"));

        // Start a long-running task that prints to the console
        new Thread(() -> {
            String label="none";
            if(apkPath.contains("correct"))
                label = "correct";
            if(apkPath.contains("false"))
                label = "false";
            try {
                String outDir = null;
                if(apkPath.contains("DroidBench"))
                    outDir = absolutePathPre;
                else if (apkPath.contains("TaintBench"))
                    outDir = "G:/Codes/UndergraduateThesis/TaintBench/";

                DynamicSlicer.sliceApkByMandoline(apkPath, true, true,
                        LeakFinderProcess.infoflowResults, label,
                        outDir + "results/coResults.csv");
                DynamicSlicer.writeTimeCountCSV(apkPath, outDir
                        + "results/timeCount.csv");
            } catch (IOException | CsvException ex) {
                ex.printStackTrace();
            }
        }).start();

        // Create a VBox to hold the TextArea
        ScrollPane scrollPane = new ScrollPane(textArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        // Create a Scene with the VBox as the root node
        Scene scene = new Scene(scrollPane, 600, 400);

        // Show the Scene
        viewer.setScene(scene);
        viewer.setTitle("Info From Slicer");
        viewer.show();
    }

}
