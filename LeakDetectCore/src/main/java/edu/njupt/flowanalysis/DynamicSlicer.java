package edu.njupt.flowanalysis;

import com.opencsv.exceptions.CsvException;
import edu.njupt.flowanalysis.converters.Apk2JarConverter;
import edu.njupt.flowanalysis.managers.AVDManager;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.util.MultiMap;

import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static edu.njupt.flowanalysis.managers.FileManager.*;

public class DynamicSlicer {
    private static long instrumentionTime = 0;
    private static List<Long> slicingTime = new ArrayList<>();
    private static String absolutePathPre = "G:/Codes/UndergraduateThesis/LeakDetectCore/";

    public static void sliceApkByMandoline(String apkPath, Boolean entireProcess, Boolean query) throws IOException {
        File apkFile = new File(apkPath);
        String apkName = apkFile.getName().replace(".apk","");
        String outDir = absolutePathPre + "results/slicer/" + apkName;
        String iapkPath = outDir;
        String iapkName = apkName + "_i";

        if(entireProcess || ! new File(iapkPath + "/" + iapkName + ".apk").exists()) {
            // get instrumented apk
            instrumentApkByMandoline(apkPath, outDir);
            // sign apk
            signApk(iapkPath, iapkName);
        }

        // run apk by monkey
        runApkByMonkey(outDir + "/" + iapkName + ".apk", outDir, query);
        storeLogBackup(apkName);

        // run mandoline
//        runMandoline(apkPath, apkName, outDir);

    }

    public static void sliceApkByMandoline(String apkPath, Boolean entireProcess, Boolean query,
                               InfoflowResults infoflowResults, String label, String coResultsPath) throws IOException, CsvException {
        File apkFile = new File(apkPath);
        String apkName = apkFile.getName().replace(".apk","");
        String outDir = absolutePathPre + "results/slicer/" + apkName;
        String iapkPath = outDir;
        String iapkName = apkName + "_i";

        if(entireProcess || ! new File(iapkPath + "/" + iapkName + ".apk").exists()) {
            // get instrumented apk
            System.out.println("对" + apkName + "进行静态插桩...");
            instrumentApkByMandoline(apkPath, outDir);
            // sign apk
            System.out.println("对" + apkName + "进行签名...");
            signApk(iapkPath, iapkName);
        }

        // run apk by monkey
        if(entireProcess || ! new File(outDir + "/trace.log").exists()) {
            System.out.println("安装" + apkName + "并运行Monkey压力测试...");
            runApkByMonkey(outDir + "/" + iapkName + ".apk", outDir, query);
            storeLogBackup(apkName);
        }
        // run mandoline
        runMandolineAfterFlowDroid(apkPath, apkName, outDir, infoflowResults, label, coResultsPath);

    }

    public static void sliceApkBySlicer4J(String apkPath) throws IOException {
        String jarPath = "results/converters";
        String outDir = "results/slicer";
        // convert apk to jar
        String apkName = Apk2JarConverter.convertApk2Jar(apkPath, jarPath);
        outDir = outDir + "/" + apkName;

        // get instrumented jar
        execCommand("python3 resources/scripts/instrument_slicer4j.py -j " + jarPath + "/" + apkName + ".jar"
                + " -o " + outDir);

        // convert jar to apk

        // sign apk

        // run apk by monkey

        // run slicer4j

    }

    /**
     * run slicer4j.py
     */
    public static void sliceJar(){
        try {
            String scriptDir = "resources/scripts/slicer4j.py";

            // input parameters
            String jarPath = "G:\\Codes\\UndergraduateThesis\\Slicer4J\\testSuites\\SliceMe\\target\\SliceMe-1.0.0.jar";
            String outDir = "results/slicer/sliceme_slice/";

            execCommand("dot -c && " +
                    "python3 " + scriptDir + " -j " + jarPath +
                    " -o " + outDir + " -b SliceMe:9 -m \"SliceMe\"");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void instrumentApkByMandoline(String apkPath, String outDir) throws IOException {
        long startTime = System.currentTimeMillis();
        execCommand("python3 " + absolutePathPre + "resources/scripts/instrument_mandoline.py -a "
                + apkPath + " -o " + outDir);
        long endTime = System.currentTimeMillis();
        instrumentionTime = endTime - startTime;
        System.out.println("Time of instrumention: " + instrumentionTime);
    }

    public static void signApk(String iapkPath, String iapkName) throws IOException {
        execCommand("python3 " + absolutePathPre + "resources/scripts/sign_apk_mandoline.py "
                + iapkPath + " " + iapkName);
    }

    public static void runInstrumentedApk(String apkPath) throws IOException {
         String DEVICE_ID = "emulator-5554"; // 模拟器设备ID
         String PACKAGE_NAME = "com.example.myapp"; // 目标应用程序包名

         execCommand("adb -s " + DEVICE_ID + " install -r " + apkPath); // 安装目标APK文件

         // 运行Monkey测试
         execCommand("adb -s " + DEVICE_ID + " shell monkey -p "
                 + PACKAGE_NAME + " -c android.intent.category.LAUNCHER 1");


    }

    private static void execCommand(String command) throws IOException {
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
        builder.redirectErrorStream(true);
        Process p = builder.start();
        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while (true) {
            line = r.readLine();
            if (line == null) { break; }
            System.out.println(line);
        }
    }

    /**
     * clean_install -> extract_trace -> remove
     */
    public static void runApkByMonkey(String iapkPath, String outDir, Boolean query) throws IOException {
        //clean install
        String deviceId = AVDManager.getDeviceId();
        execCommand("python3 " + absolutePathPre + "resources/scripts/clean_install_mandoline.py "
                + deviceId + " " + iapkPath);
        System.out.println("clean_install has been done.");

        //remove
        if(query) {
            Scanner scanner = new Scanner(System.in);
            System.out.print("卸载" + iapkPath + "? [y/n]: ");
            if (scanner.next().equals("y")) {
                execCommand("python3 " + absolutePathPre + "resources/scripts/remove_mandoline.py "
                        + deviceId + " " + iapkPath);
                System.out.println("remove has been done.");
            }
        }
        else{
            execCommand("python3 " + absolutePathPre + "resources/scripts/remove_mandoline.py "
                    + deviceId + " " + iapkPath);
            System.out.println("remove has been done.");
        }

        //extract trace
        System.out.println("从trace_full.log中提取插桩运行结果trace.log...");
        execCommand("python3 " + absolutePathPre + "resources/scripts/extract_trace_mandoline.py "
                + deviceId + " " + outDir);
        System.out.println("extract_trace has been done.");
    }

    /**
     * generating icdg -> slicing
     */
    public static void runMandoline(String apkPath, String apkName, String outDir) throws IOException {
        readLogBackUp(apkName);

        // generate icdg
        execCommand("python3 resources/scripts/generate_icdg_mandoline.py " + apkPath + " " + apkName);
        System.out.println("ICDG has been generated.");

        // slicing
        String sliceLine = "9";
        String sliceVars = "r3";
        execCommand("python3 resources/scripts/slicing_mandoline.py " + apkPath + " " + apkName + " "
            + sliceLine + " " + sliceVars);

        System.out.println("Slicing has been done.");
    }

    public static void runMandolineAfterFlowDroid(String apkPath, String apkName, String outDir,
                                                  InfoflowResults infoflowResults, String label,
                                                  String coResultsPath)
            throws IOException, CsvException {
        readLogBackUp(apkName);

        // generate icdg
        System.out.println("根据trace.log生成icdg...");
        execCommand("python3 " + absolutePathPre + "resources/scripts/generate_icdg_mandoline.py "
                + apkPath + " " + apkName);
        System.out.println("ICDG has been generated.");

        // slicing
        System.out.println("根据icdg开始后向切片...");
        sliceBackwards(apkPath, apkName, outDir, infoflowResults, label, coResultsPath);

        System.out.println("Slicing has been done.");
    }

    /**
     * analyze static-log.log(slice backwards)
     * @param outDir: results/slicer/apkName
     * @param label: 论文中对应的标签
     * csvPath: results/coAnalysis/apkName.csv
     */
    public static void sliceBackwards(String apkPath, String apkName, String outDir, InfoflowResults infoflowResults, String label,
                                      String coResultsPath) throws IOException, CsvException {
        // create and open CSV
        // false warning : cannot find sourceMethod in slices
        // correct warning : can find sourceMethod in slices
        slicingTime = new ArrayList<>();

        String csvPath = outDir + "/coAnalysis.csv";
        String[] header = {"Sink Method", "Source Method", "Slicing Criterion", "Warning Type(false/correct)", "Sink Info", "Source Info"};
        writeToCSV(header, csvPath, false);

//        String coResultsPath = "results/coResults.csv";
        if(! new File(coResultsPath).exists()) {
            String[] firstLine = {"Time", "Apk Name", "label(false/correct)", "Sink Method", "Source Method", "Slicing Criterion",
                    "Warning Type(false/correct)", "Sink Info", "Source Info"};
            writeToCSV(firstLine, coResultsPath, false);
        }
        deleteLineInCSV(apkName, coResultsPath);

        System.out.println("自动生成切片准则...");
        System.out.println("根据InfoflowResults获取源与汇信息...");

        // slicing criterion + sourceInfo : "9 r3" "getDeviceId()"
        MultiMap<ResultSinkInfo, ResultSourceInfo> leakInfo = infoflowResults.getResults();
        if (leakInfo == null)
            return;

        // get sinkInfo -> search for lines in static-log.log
        Set<ResultSinkInfo> resultSinkInfoSet= leakInfo.keySet();
        for(ResultSinkInfo sinkInfo: resultSinkInfoSet){
            String[] rowData = new String[6];
            String sinkInfoStr = sinkInfo.toString();
            String sinkMethod = searchForFunctionName(sinkInfoStr);
            rowData[0] = sinkMethod;
            rowData[4] = sinkInfoStr;

            // get vars
            String sinkVars = searchForFunctionVar(sinkInfoStr, sinkMethod);

            // get keywords
            List<String> keywords = new ArrayList<>();
            keywords.add(sinkMethod);
            String[] vars = sinkVars.split("-");
            for(String var: vars) {
                keywords.add(var);
            }

            System.out.println("汇点信息: " + sinkInfoStr);
            System.out.println("根据汇点信息从trace.log_icdg.log匹配行号...");
            System.out.print("汇点关键词: ");
            for(String keyword: keywords)
                System.out.print(keyword + " ");

//            //get line
//            String staticLogPath = outDir + "/static-log.log";
//            String line = searchForLineInStaticLog(staticLogPath, keywords);
//            String criterion = line + " " + sinkVars;
//            rowData[2] = criterion;
//            System.out.println("切片准则: " + criterion);

            //get line
            String icdgLogPath = outDir + "/trace.log_icdg.log";
            String line = searchForLineInICDG(icdgLogPath, keywords);

            String criterion = line + " " + sinkVars;
            rowData[2] = criterion;
            System.out.println("切片准则: " + criterion);

            // slice backwards
            if(line.isEmpty()){
                System.out.println("没有在icdg中找到目标节点，不进行切片");
            }
            else {
                System.out.println("正在进行后向切片...");
                long startTime = System.currentTimeMillis();
                execCommand("python3 " + absolutePathPre +
                        "resources/scripts/slicing_mandoline.py " + apkPath + " " + apkName + " "
                        + criterion);
                long endTime = System.currentTimeMillis();
                slicingTime.add(endTime - startTime);
                System.out.println("Slicing time count: " + slicingTime.size());
            }

            // record in coAnalysis.csv
            for(ResultSourceInfo sourceInfo: leakInfo.get(sinkInfo)) {
                String sourceInfoStr = sourceInfo.toString();
                String sourceMethod = searchForFunctionName(sourceInfoStr);
                rowData[1] = sourceMethod;
                rowData[5] = sourceInfoStr;

                // search for sourceMethod in raw-slice.log
                rowData[3] = "false";
                if(!line.isEmpty()) {
                    System.out.println("源点信息: " + sourceInfoStr);
                    System.out.println("从raw-slice.log中寻找源点关键词" + sourceMethod + "...");
                    boolean found = searchLogFile(outDir + "/raw-slice.log", sourceMethod);
                    if(found) {
                        rowData[3] = "correct";
                    }
                }

                if(rowData[3].equals("correct")){
                    System.out.println("FlowDroid发现的泄露真实存在.");
                }else{
                    System.out.println("FlowDroid发现的泄露被认定为误报...");
                    System.out.println("该泄露不存在.");
                }

                writeToCSV(rowData, csvPath, true);

                // write to coResults.csv
                Date date = new Date();
                SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                String time = formatter.format(date);
                String[] coResult = new String[9];
                coResult[0] = time; coResult[1] = apkName; coResult[2] = label;
                System.arraycopy(rowData, 0, coResult, 3, rowData.length);
                writeToCSV(coResult, coResultsPath, true);
            }

        }

    }

    private static String searchForLineInICDG(String logPath, List<String> keywords) throws IOException {
        StringBuilder content = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(logPath));
        String contentLine;
        List<String> records = new ArrayList<>();
        while ((contentLine = reader.readLine()) != null) {
            records.add(contentLine);
        }
        reader.close();

        // 遍历每个记录，查找记录头部的数字
        String line = "";
        outerLoop:
        for (String record: records) {
            String[] splitArray = record.split(",");
            String lastSubstring = splitArray[splitArray.length - 1];
            for (String keyword: keywords) {
                if (!lastSubstring.contains(keyword))
                    continue outerLoop;
            }
            String regex = "^\\d+";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(record);
            if (matcher.find()) {
                line = matcher.group();
                break;
            }
        }
        return line;
    }


    private static String searchForFunctionName(String str){
        Pattern pattern = Pattern.compile("(\\w+)>?\\(");
        Matcher matcher = pattern.matcher(str);
        String functionName = null;
        if (matcher.find()) {
            functionName = matcher.group(1);
            System.out.println("Function name: " + functionName);
        }
        return functionName;
    }

    private static String searchForFunctionVar(String str, String functionName){
        int index = str.indexOf(functionName);
        String functionStr = str.substring(index);

        Pattern pattern = Pattern.compile("\\$?(r\\d+)");
        Matcher matcher = pattern.matcher(functionStr);
        String functionVars = "";
        while (matcher.find()) {
            String parameter = matcher.group(0);
            functionVars = functionVars + parameter;
        }
        functionVars = functionVars.replaceAll("\\$","-");
        if(functionVars.startsWith("-"))
            functionVars = functionVars.substring(1);
        System.out.println("Parameters: " + functionVars);
        return functionVars;
    }

    private static String searchForLineInStaticLog(String logPath, List<String> keywords) throws IOException {
        String logContent = readLog(logPath);

        // 使用]分隔字符串
        String[] records = logContent.split("]");
        // 遍历每个记录，查找记录头部的数字
        String line = "";
        outerLoop:
        for (String record : records) {
            for (String keyword: keywords) {
                if (!record.contains(keyword))
                    continue outerLoop;
            }
            String regex = "\"(\\d+)\":\\[";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(record);
            if (matcher.find()) {
                line = matcher.group(1);
                break;
            }
        }
        return line;
    }

    /**
     * 备份 trace.log 至 G:\curriculum\大四下\毕设\杂\slicer
     * @param apkName
     */
    public static void storeLogBackup(String apkName) throws IOException {
        String sourceFilePath = absolutePathPre + "results/slicer/" + apkName + "/trace.log";
        String targetFilePath = "G:\\curriculum\\大四下\\毕设\\杂\\slicer\\" + apkName + "-trace.log";

        // Create File objects for source and target files
        File sourceFile = new File(sourceFilePath);
        File targetFile = new File(targetFilePath);

        long logSize = Files.size(sourceFile.toPath());
        // Check if the target file exists
        if(!targetFile.exists() && logSize != 0) {
            // Create the target file if it doesn't exist
            targetFile.createNewFile();
            // Copy contents of source file to target file
            copyFile(sourceFile,targetFile);
        }

    }

    /**
     * 从 G:\curriculum\大四下\毕设\杂\slicer 读取备份 trace.log
     * @param apkName
     */
    public static void readLogBackUp(String apkName) throws IOException {
        String targetFilePath = absolutePathPre + "results/slicer/" + apkName + "/trace.log";
        String sourceFilePath = "G:\\curriculum\\大四下\\毕设\\杂\\slicer\\" + apkName + "-trace.log";

        // Create File objects for source and target files
        File sourceFile = new File(sourceFilePath);
        File targetFile = new File(targetFilePath);

        long logSize = Files.size(targetFile.toPath());
        // Check if the target file exists
        if(logSize == 0 && sourceFile.exists()) {
            // Copy contents of source file to target file
            copyFile(sourceFile, targetFile);
        }

    }



    public static List<Long> getSlicingTime(){
        return slicingTime;
    }

    public static long getInstrumentionTime(){
        return instrumentionTime;
    }

    public static void writeTimeCountCSV(String apkPath, String csvPath) throws IOException, CsvException {
        File apkFile = new File(apkPath);
        String apkName = apkFile.getName().replace(".apk","");

        long runInfoflowTime = FlowAnalysis.getRunInfoflowTime();
        long instrumentionTime = DynamicSlicer.getInstrumentionTime();
        List<Long> slicingTime = DynamicSlicer.getSlicingTime();
        double avgTime;
        if(slicingTime.isEmpty()){
            avgTime = 0;
        }else {
            avgTime = slicingTime.stream().mapToLong(Long::longValue).average().getAsDouble();
        }

//        csvPath = "results/timeCount.csv";
        File csvFile = new  File(csvPath);
        if(!csvFile.exists()){
            String[] header = {"Date", "Apk Name", "Number of Jimple statements","Time of runInfoflow(ms)", "Time of instrumention(ms)", "Time of slicing(ms)"};
            writeToCSV(header, csvPath, false);
        }
        else{
            deleteLineInCSV(apkName, csvPath);
        }
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String time = formatter.format(date);
        String stmtSize = getJimpleStmtNumber(apkName);
        String[] rowData = {time, apkName, stmtSize, String.valueOf(runInfoflowTime),
                String.valueOf(instrumentionTime), String.valueOf(avgTime)};
        writeToCSV(rowData, csvPath ,true);
    }

    public static String getJimpleStmtNumber(String apkName){
        String txtPath = absolutePathPre + "results/slicer/" + apkName + "/apk-size.txt";
        String stmtSize = "";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(txtPath));
            String line = reader.readLine();
            reader.close();

            // 提取数字
            String[] parts = line.split(":");
            stmtSize = parts[1].trim();

        } catch (IOException e) {
            System.out.println("Error reading file.");
            e.printStackTrace();
        }
        return stmtSize;
    }
}
