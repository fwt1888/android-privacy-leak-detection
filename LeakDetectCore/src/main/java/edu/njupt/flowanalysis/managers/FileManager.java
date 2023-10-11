package edu.njupt.flowanalysis.managers;

import com.opencsv.*;
import com.opencsv.exceptions.CsvException;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileManager {
    public static void copyFile(File source, File destination) throws IOException {
        try (InputStream inputStream = new FileInputStream(source);
             OutputStream outputStream = new FileOutputStream(destination)) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }
    }

    public static boolean searchLogFile(String filePath, String keyword) {
        File logFile = new File(filePath);
        if (! logFile.exists()) {
            System.out.println("Log file does not exist.");
            return false;
        }

        boolean found = false;
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(keyword)) {
                    found = true;
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return found;
    }

    public static void writeToCSV(String[] rowData, String csvPath, Boolean append) throws IOException {
        try{
            CSVWriter csvWriter = new CSVWriter(new FileWriter(csvPath, append),
                    CSVWriter.DEFAULT_SEPARATOR,
                    CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.NO_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);
            csvWriter.writeNext(rowData);
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteLineInCSV(String apkName, String csvPath) throws CsvException, IOException {
        CSVParser parser = new CSVParserBuilder()
                .withSeparator(',')
                .withQuoteChar('|')
                .build();
        CSVReader reader = new CSVReaderBuilder(new FileReader(csvPath))
                .withCSVParser(parser)
                .build();

        String [] nextLine;
        List<Integer> deleteRowNumber = new ArrayList<>();
        int rowNumber = 0;
        while ((nextLine = reader.readNext()) != null) {
            if(nextLine[1].equals(apkName)){
                deleteRowNumber.add(rowNumber);
            }
            rowNumber++;
        }

        reader = new CSVReaderBuilder(new FileReader(csvPath))
                .withCSVParser(parser)
                .build();

        List<String[]> allElements = reader.readAll();
        Collections.sort(deleteRowNumber, Collections.reverseOrder());
        if(!deleteRowNumber.isEmpty()) {
            for (Integer row : deleteRowNumber) {
                allElements.remove(row.intValue());
            }
        }
        Boolean append = false;
        for(String[] element: allElements){
            writeToCSV(element, csvPath, append);
            if(!append) append = true;
        }

        reader.close();
    }

    /**
     * 读取.log文件的内容到一个字符串中
     * @param filename
     * @return
     * @throws IOException
     */
    public static String readLog(String filename) throws IOException {
        StringBuilder content = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line);
        }
        reader.close();
        return content.toString();
    }


    public static void appendTextToFile(String inputFilePath, String outputFilePath, String appendText, Boolean append) {
        try {
            FileReader inputFileReader = new FileReader(inputFilePath);
            BufferedReader bufferedReader = new BufferedReader(inputFileReader);

            FileWriter outputFileWriter = new FileWriter(outputFilePath, append);
            BufferedWriter bufferedWriter = new BufferedWriter(outputFileWriter);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String modifiedLine = line + appendText;
                bufferedWriter.write(modifiedLine);
                bufferedWriter.newLine();
            }

            bufferedReader.close();
            inputFileReader.close();

            bufferedWriter.close();
            outputFileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
