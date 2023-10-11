package edu.njupt.flowanalysis.javafx;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PlantUmlViewer {
    private static final String PLANTUML_SERVICE_URL = "http://www.plantuml.com/plantuml/svg/";

    private static String plantUmlToSvg(String pumlText) throws IOException {
        // Create a SourceStringReader and pass in the PlantUML text
        SourceStringReader reader = new SourceStringReader(pumlText);

        // Write the SVG output to a ByteArrayOutputStream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        reader.generateImage(outputStream, new FileFormatOption(FileFormat.SVG));

        // Convert the ByteArrayOutputStream to a string and print it
        return outputStream.toString();
    }

    public static void viewUmlFile(String filePath) throws IOException {
        Stage viewer = new Stage();
        viewer.initModality(Modality.NONE);

        // Load PlantUML file content
        Path path = Paths.get(filePath);
        String content = Files.readString(path, StandardCharsets.UTF_8);

        // Convert PlantUML to SVG
        String svgString = plantUmlToSvg(content);
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        webEngine.loadContent(svgString);

        // Show the scene
        StackPane pane = new StackPane();
        pane.getChildren().add(webView);
        viewer.setScene(new Scene(pane, 600, 400));
        viewer.setTitle("Puml Viewer");
        viewer.show();
    }
}
