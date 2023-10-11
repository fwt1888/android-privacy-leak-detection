import edu.njupt.flowanalysis.FlowAnalysis;
import soot.jimple.infoflow.results.InfoflowResults;

public class FlowAnalysisTest {
    public static void main(String[] args) throws Exception {
//        String apkPath = "testSuites/DroidBench/falseWarning/Merge1.apk";
        String apkPath = "testSuites/DroidBench/correctWarning/ActivityEventSequence1.apk";
//        String apkPath = "G:\\Codes\\UndergraduateThesis\\DroidBench3.0\\apk\\Callbacks\\Button2.apk";
//        String apkPath = "G:\\Codes\\UndergraduateThesis\\TaintBench\\apks\\xbot_android_samp.apk";
        String androidJAR = "C:\\Users\\14391\\AppData\\Local\\Android\\Sdk\\platforms\\android-32\\android.jar";

//        String sourcesAndSinksPath = "G:\\Codes\\UndergraduateThesis\\TaintBench\\sourcesAndSinks.txt";
        String sourcesAndSinksPath = "";
        InfoflowResults infoflowResults = FlowAnalysis.analyzeApk(apkPath, sourcesAndSinksPath,
                false, false, false);
//        infoflowResults.printResults();

//        String coResultsPath = "results/coResults.csv";
//        String label="none";
//        if(apkPath.contains("correct"))
//            label="correct";
//        if(apkPath.contains("false"))
//            label="false";
////        DynamicSlicer.sliceApkByMandoline(apkPath, true, true, infoflowResults,label,coResultsPath);
//        DynamicSlicer.sliceApkByMandoline(apkPath, true, true, null,label,coResultsPath);
//        DynamicSlicer.writeTimeCountCSV(apkPath, "results/timeCount.csv");
    }

}
