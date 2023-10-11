module edu.njupt.flowanalysis.javafx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires leakdetect.core;
    requires java.desktop;
    requires soot.infoflow.android;
    requires soot.infoflow;
    requires xmlpull;
    requires plantuml;
    requires com.opencsv;

    opens edu.njupt.flowanalysis.javafx to javafx.fxml;
    exports edu.njupt.flowanalysis.javafx;
}