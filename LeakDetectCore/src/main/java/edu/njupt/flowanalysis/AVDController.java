package edu.njupt.flowanalysis;

import edu.njupt.flowanalysis.managers.AVDManager;

import java.io.IOException;

public class AVDController {
    public static void main(String[] args) throws IOException, InterruptedException {
//        AVDManager.setDeviceName("Nexus_4_API_25");
        AVDManager.setDeviceName("Pixel_2_API_28");
        String deviceId = AVDManager.getDeviceId();
        if( deviceId == null) {
            AVDManager.runAVD();
        }
        else{
            AVDManager.stopAVD(deviceId);
        }
    }
}
