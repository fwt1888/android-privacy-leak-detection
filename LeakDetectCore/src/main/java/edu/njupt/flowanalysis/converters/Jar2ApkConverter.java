package edu.njupt.flowanalysis.converters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Jar2ApkConverter {
    /**
     *
     * @param jarPath : "/path/to/jar/file"
     * @param apkPath : "/path/to/output/file.apk"
     */
    public static void convertJar2Apk(String jarPath, String apkPath) {
        String apkToolPath = "G:/STUDY/Tools/apktool/apktool.jar";
        try {
            // Use apkbuilder to create the APK
            Process process = Runtime.getRuntime().exec("java -jar " + apkToolPath +
                    " b " + jarPath + " -o " + apkPath);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            System.out.println("JAR converted to APK successfully!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
