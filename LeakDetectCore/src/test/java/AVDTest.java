import edu.njupt.flowanalysis.managers.AVDManager;

import java.io.IOException;

public class AVDTest {
    public static void main(String[] args) throws IOException, InterruptedException {
        AVDManager.runAVD();
        AVDManager.stopAVD(AVDManager.getDeviceId());
    }
}
