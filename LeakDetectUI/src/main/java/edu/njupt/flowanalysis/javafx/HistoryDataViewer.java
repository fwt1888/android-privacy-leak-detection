package edu.njupt.flowanalysis.javafx;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class HistoryDataViewer {
    public static void showHistoryData(String database, VBox vBox) throws IOException, CsvException {
        String dataDir = null;

        Label label = new Label();
        MenuController.setFont(label);
        if(database.equals("DB")) {
            label.setText("DroidBench历史分析结果");
            dataDir = "G:/Codes/UndergraduateThesis/LeakDetectCore/results/";
        }
        else if(database.equals("TB")) {
            label.setText("TaintBench历史分析结果");
            dataDir = "G:/Codes/UndergraduateThesis/TaintBench/results/";
        }

        String coResultsPath = dataDir + "coResults.csv";
        String flowDroidResultsPath = dataDir + "flowDroidResults.csv";
        String timeCountPath = dataDir + "timeCount.csv";

        TextArea newData = new TextArea("===== 最新分析记录 =====\n");
        showLatestRecord(newData, flowDroidResultsPath);
        showLatestRecord(newData, coResultsPath);
        showLatestRecord(newData, timeCountPath);

        Hyperlink hyperlink1 = new Hyperlink("1. FlowDroid分析结果汇总: " + flowDroidResultsPath);
        Hyperlink hyperlink2 = new Hyperlink("2. 合作分析结果汇总: " + coResultsPath);
        Hyperlink hyperlink3 = new Hyperlink("3. 时长统计结果汇总: " + timeCountPath);
        hyperlink1.setFont(Font.font("System", FontWeight.BOLD, 14));
        hyperlink2.setFont(Font.font("System", FontWeight.BOLD, 14));
        hyperlink3.setFont(Font.font("System", FontWeight.BOLD, 14));
        hyperlink1.setOnAction(e -> {
            try {
                Desktop.getDesktop().open(new File(flowDroidResultsPath));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        hyperlink2.setOnAction(e -> {
            try {
                Desktop.getDesktop().open(new File(coResultsPath));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        hyperlink3.setOnAction(e -> {
            try {
                Desktop.getDesktop().open(new File(timeCountPath));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        vBox.getChildren().addAll(label, newData, hyperlink1, hyperlink2, hyperlink3);
    }

    public static void showLatestRecord(TextArea textarea, String filePath) throws IOException, CsvException {
        CSVParser parser = new CSVParserBuilder()
                .withQuoteChar('"')
                .withIgnoreQuotations(true)
                .build();

        CSVReader reader = new CSVReaderBuilder(new FileReader(filePath))
                .withCSVParser(parser)
                .build();

//        CSVReader reader = new CSVReader(new FileReader(filePath));
        List<String[]> rows = reader.readAll();

        // 使用日期时间格式化器
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        // 将记录转换为Record对象
        String title = null;
        List<Record> records = new ArrayList<>();
        for (String[] row : rows) {
            if(row[0].equals("Date") || row[0].equals("Time")) {
                title = Arrays.toString(row);
                continue;
            }
            LocalDateTime dateTime = LocalDateTime.parse(row[0], formatter);
            String[] data = new String[row.length-1];
            System.arraycopy(row, 1, data ,0, data.length);
            Record record = new Record(dateTime, data);
            records.add(record);
        }

        // 根据日期排序，选取最新的一行
        Record latestRecord = records.stream()
                .max(Comparator.comparing(Record::getDate))
                .orElse(null);

        // 在TextArea中显示最新的一行记录
        if (latestRecord != null) {
            textarea.appendText(title);
            textarea.appendText("\n");
            textarea.appendText(latestRecord.toString().replaceAll("T"," "));
            textarea.appendText("\n");
        }

        reader.close();
    }
}


class Record {
    private LocalDateTime date;
    private String[] data;

    public Record(LocalDateTime date, String[] data) {
        this.date = date;
        this.data = data;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public String[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return date.toString() + "\t" + Arrays.toString(data);
    }
}
