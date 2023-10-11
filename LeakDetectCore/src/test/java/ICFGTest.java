import edu.njupt.flowanalysis.generators.ICFGGenerator;
import soot.jimple.infoflow.android.SetupApplication;

import java.io.IOException;

public class ICFGTest {
    public static void main(String[] args) throws IOException {
        String apkPath = "testSuites/DroidBench/correctWarning/Exceptions1.apk";
        String androidJAR = "C:\\Users\\14391\\AppData\\Local\\Android\\Sdk\\platforms\\android-32\\android.jar";
        ICFGGenerator.createICFG(new SetupApplication(androidJAR, apkPath), "Exceptions1");
    }
}
