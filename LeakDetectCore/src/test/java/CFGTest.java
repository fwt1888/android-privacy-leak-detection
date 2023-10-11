import edu.njupt.flowanalysis.generators.CFGGenerator;
import soot.jimple.infoflow.android.SetupApplication;

public class CFGTest {
    public static void main(String[] args) {
        String apkFile = "src/test/testAPKs/FlowDroidAliasActivity.apk";
        String androidJAR = "C:\\Users\\14391\\AppData\\Local\\Android\\Sdk\\platforms";
        CFGGenerator.createCFG(androidJAR,apkFile, new SetupApplication(androidJAR, apkFile));
    }
}
