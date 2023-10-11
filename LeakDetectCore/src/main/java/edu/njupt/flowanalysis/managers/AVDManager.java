package edu.njupt.flowanalysis.managers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class AVDManager {
    private static final String SDK_ROOT = "C:\\Users\\14391\\AppData\\Local\\Android\\Sdk";
    private static final String AVD_MANAGER = SDK_ROOT + "\\tools\\bin\\avdmanager.bat";
//    private static final String DEVICE_NAME = "Pixel_2_API_30";
    private static String DEVICE_NAME = "Pixel_2_API_28";

    /**
     * run AVD and return device id
     * @return device id of AVD
     * @throws IOException
     */
    public static void runAVD() throws IOException, InterruptedException {
//        String command = String.format("start /b " + SDK_ROOT + "\\emulator\\emulator.exe " +
//                        "-avd %s -no-boot-anim -no-snapshot -noaudio -no-window -gpu off", DEVICE_NAME);
        String command = String.format("start /b " + SDK_ROOT + "\\emulator\\emulator.exe " +
                "-avd %s -no-boot-anim -no-snapshot", DEVICE_NAME);
        System.out.println(command);
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
        builder.start();
        System.out.println("AVD " + DEVICE_NAME + " is booting ...");
    }

    public static void stopAVD(String deviceId) throws IOException {
        execCommand(SDK_ROOT + "\\platform-tools\\adb.exe -s " + deviceId + " emu kill");
    }

    public static void createAVD(String name, String apiLevel, String tag, String arch) throws IOException {
        execCommand(String.format("%s create avd -n %s -k \"system-images;android-%s;%s;%s\"",
                AVD_MANAGER, name, apiLevel, tag, arch));
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

    public static String getDeviceId() throws IOException {
        String deviceId = null;
        String command = SDK_ROOT + "\\platform-tools\\adb.exe devices | findstr \"emulator\"";
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
        builder.redirectErrorStream(true);
        Process p = builder.start();
        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = r.readLine()) != null) {
            if (line.endsWith("device")) {
                deviceId =  line.split("\\t")[0];
                break;
            }
        }
        System.out.println("Device Id: " + deviceId);
        return deviceId;
    }

    public static void waitForAVD(){
        System.out.println("Please wait for AVD ...");
        boolean avdStarted = false;

        while (!avdStarted) {
            try {
                System.out.println("Checking state ...");
//                String command = SDK_ROOT + "\\platform-tools\\adb.exe devices | findstr device$";
                String command = SDK_ROOT + "\\platform-tools\\adb.exe -e shell getprop init.svc.bootanim";
                ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
                builder.redirectErrorStream(true);
                Process p = builder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = reader.readLine();

                if (line != null && line.contains("stopped")) {
                    avdStarted = true;
                }

                Thread.sleep(3000); // Wait 3 second before checking again

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("AVD has started!");
    }

    public static void checkInternet() throws IOException {
        String command = SDK_ROOT + "\\platform-tools\\adb.exe shell ping -c 3 www.google.com";
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
        builder.redirectErrorStream(true);
        Process p = builder.start();
        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = r.readLine()) != null) {
            if (line.contains("ping: unknown host www.google.com")) {
                System.out.println("The emulator has not been connected to the Internet.");
                break;
            }
        }
        System.out.println("The emulator has been connected to the Internet.");
    }

    public static void setDeviceName(String deviceName){
        DEVICE_NAME = deviceName;
    }
}
