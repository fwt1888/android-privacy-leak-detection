import edu.njupt.flowanalysis.converters.Apk2JarConverter;

import java.io.IOException;

public class ConverterTest {
    public static void main(String[] args) throws IOException {
        Apk2JarConverter.convertApk2Jar("src/test/testAPKs/Merge1.apk",
                "results/converters" );
//        Jar2ApkConverter.convertJar2Apk("results/converters/Merge1",
//                "results/converters/Merge1.apk");

    }
}
