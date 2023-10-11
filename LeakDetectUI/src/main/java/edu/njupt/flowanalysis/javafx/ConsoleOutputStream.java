package edu.njupt.flowanalysis.javafx;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ConsoleOutputStream extends OutputStream {
    private static TextArea textArea;

    public ConsoleOutputStream(TextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    public void write(int b) throws IOException {
        updateTextArea(String.valueOf((char) b));
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        updateTextArea(new String(b, off, len, StandardCharsets.UTF_8));
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    private static void updateTextArea(final String text) {
        Platform.runLater(() -> textArea.appendText(text));
    }
}