package edu.njupt.flowanalysis.converters;

import java.io.*;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Apk2JarConverter {
    /**
     *
     * @param apkPath : "/path/to/apk/file.apk"
     * @param jarPath : "/path/to/output/folder(decompile,.jar)"
     */
    public static String convertApk2Jar(String apkPath, String jarPath) {
//        String apktoolPath = "G:/STUDY/Tools/apktool/apktool.jar";
        String dex2jarPath = "G:/STUDY/Tools/dex-tools-2.1/d2j-dex2jar.bat";
        try {
//            // Use apktool to decompile the APK
//            Process process = Runtime.getRuntime().exec("java -jar " + apktoolPath +
//                    " d -f " + apkPath + " -o " + jarPath + "/decompile");
//
//            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            String line;
//
//            while ((line = reader.readLine()) != null) {
//                System.out.println(line);
//            }

            // create the JAR file
            // copy .jar to .zip
            File apkFile = new File(apkPath);
            String apkName = apkFile.getName().replace(".apk","");
//            File jarFile = new File(jarPath, apkName + ".apk");
//            copy(apkFile, jarFile);
//            jarFile.renameTo(new File(jarPath + "/" + apkName + ".zip"));
//
//            // unzip .zip
//            unzipFile(jarPath + "/" + apkName + ".zip",
//                    jarPath + "/" + apkName);

            // convert .dex to .jar
            Process jarProcess = Runtime.getRuntime().exec(dex2jarPath +
                    " -o " + jarPath + "/" + apkName + ".jar " + apkPath);

            BufferedReader jarReader = new BufferedReader(new InputStreamReader(jarProcess.getInputStream()));
            String jarLine;

            while ((jarLine = jarReader.readLine()) != null) {
                System.out.println(jarLine);
            }

            System.out.println("APK converted to JAR successfully!");
            return apkName;
            // delete dir + zip
//            Runtime.getRuntime().exec("rd /s /q" + jarPath + "/" + apkName);
//            Runtime.getRuntime().exec("del " + jarPath + "/" + apkName + ".zip");

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * file copy to newFile
     * @param file
     * @param newFile
     * @throws IOException
     */
    private static void copy(File file, File newFile) throws IOException {
        BufferedInputStream bi = new BufferedInputStream(
                new FileInputStream(file));
        BufferedOutputStream  bo = new BufferedOutputStream(
                new FileOutputStream(newFile));

        byte[] by = new byte[1024];
        int len = 0;
        while((len = bi.read(by)) != -1) {
            bo.write(by,0,len);
        }
        bo.close();
    }

    /**
     * unzip file
     *
     * @param inputStream
     * @param descDir
     */
    private static void unzipWithStream(InputStream inputStream, String descDir) {
        if (!descDir.endsWith(File.separator)) {
            descDir = descDir + File.separator;
        }
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream, Charset.forName("GBK"))) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                String zipEntryNameStr = zipEntry.getName();
                String zipEntryName = zipEntryNameStr;
                if (zipEntryNameStr.contains("/")) {
                    String str1 = zipEntryNameStr.substring(0, zipEntryNameStr.indexOf("/"));
                    zipEntryName = zipEntryNameStr.substring(str1.length() + 1);
                }
                String outPath = (descDir + zipEntryName).replace("\\\\", "/");
                File outFile = new File(outPath.substring(0, outPath.lastIndexOf('/')));
                if (!outFile.exists()) {
                    outFile.mkdirs();
                }
                if (new File(outPath).isDirectory()) {
                    continue;
                }
                writeFile(outPath, zipInputStream);
                zipInputStream.closeEntry();
            }
            System.out.println("unzip successfully");
        } catch (IOException e) {
            System.out.println("exception:" + e);
        }
    }

    /**
     * write stream to file
     * @param filePath
     * @param zipInputStream
     */
    public static void writeFile(String filePath, ZipInputStream zipInputStream) {
        try (OutputStream outputStream = new FileOutputStream(filePath)) {
            byte[] bytes = new byte[4096];
            int len;
            while ((len = zipInputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, len);
            }
        } catch (IOException ex) {
            System.out.println("failed: write stream to file");
        }
    }

    private static void unzipFile(String zipPath, String descDir) throws IOException {
        try {
            File zipFile = new File(zipPath);
            if (!zipFile.exists()) {
                throw new IOException("file to unzip does not exist");
            }
            File pathFile = new File(descDir);
            if (!pathFile.exists()) {
                pathFile.mkdirs();
            }
            InputStream input = new FileInputStream(zipPath);
            unzipWithStream(input, descDir);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
