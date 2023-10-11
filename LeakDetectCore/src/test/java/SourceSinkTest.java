import edu.njupt.flowanalysis.SourceSinkParser;

public class SourceSinkTest{
    public static void main(String[] args) throws Exception {
        String absolutePathPre = "G:/Codes/UndergraduateThesis/LeakDetectCore/";
        String apkFile = "src/test/testAPKs/Merge1.apk";
        String jarPath = "C:\\Users\\14391\\AppData\\Local\\Android\\Sdk\\platforms\\android-32\\android.jar";
        String trainDetect = "results/sourceSink/trainResults/trainDetect.txt";
        String apkDetect = "results/sourceSink/apkDetect.txt";
        String trainResource[] = {absolutePathPre + "resources/permissionMethodWithLabel.pscout"};
        String modelPath = "resources/models";

        SourceSinkParser sourceSinkParser = new SourceSinkParser();
        sourceSinkParser.setConfiguration(modelPath, jarPath, apkDetect);

        sourceSinkParser.trainSusi(trainResource, trainDetect);
//        sourceSinkParser.apkSourceSinkParser(apkFile,trainResource);
    }
}
