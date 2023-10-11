import java.io.IOException;

public class SlicerTest {
    public static void main(String[] args) throws IOException, InterruptedException {
//        AVDManager.setDeviceName("Pixel_2_API_24");
//        edu.njupt.flowanalysis.DynamicSlicer.sliceJar();
//        edu.njupt.flowanalysis.managers.AVDManager.runAVD();
//        edu.njupt.flowanalysis.managers.AVDManager.waitForAVD();
//
//        System.out.println("Please wait for 30 seconds ...");
//        Thread.sleep(30000);
//
//        edu.njupt.flowanalysis.DynamicSlicer.sliceApkByMandoline("src/test/testAPKs/birthdroid.apk");
//        edu.njupt.flowanalysis.managers.AVDManager.stopAVD(edu.njupt.flowanalysis.managers.AVDManager.getDeviceId());

//        String dirPath = "G:\\Codes\\UndergraduateThesis\\TaintBench\\apks\\";
//        edu.njupt.flowanalysis.DynamicSlicer.sliceApkByMandoline(dirPath + "beita_com_beita_contact.apk",
//                true, true);

        String apkPath = "src/test/testAPKs/anki.apk";
        edu.njupt.flowanalysis.DynamicSlicer.sliceApkByMandoline(apkPath,
                true, true);

    }
}
