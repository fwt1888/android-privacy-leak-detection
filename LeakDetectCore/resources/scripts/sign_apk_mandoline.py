
import os
import sys

instrumented_apk_path = sys.argv[1]
apk_name = sys.argv[2]
instrumented_apk = instrumented_apk_path + "\\" + apk_name + ".apk"
mandoline_dir = "G:\\Codes\\UndergraduateThesis\\Mandoline"

os.system(f"zip {instrumented_apk} -d META-INF/*.SF")
os.system(f"zip {instrumented_apk} -d META-INF/*.MF")
os.system(f"zip {instrumented_apk} -d META-INF/*.DSA")
os.system(f"zip {instrumented_apk} -d META-INF/*.RSA")


os.system(f"jarsigner -sigalg SHA1withRSA -digestalg SHA1 " +
        f"-keystore {mandoline_dir}\\scripts\\mandoline.keystore {instrumented_apk} mandoline -storepass mandoline")


os.system(f"jarsigner -verify -keystore {mandoline_dir}\\scripts\\mandoline.keystore {instrumented_apk}")
os.system(f"C:\\Users\\14391\\AppData\\Local\\Android\\Sdk\\build-tools\\32.0.0\\zipalign.exe -f 4 {instrumented_apk} {instrumented_apk_path}\\{apk_name}-aligned.apk")
os.system(f"move /Y {instrumented_apk_path}\\{apk_name}-aligned.apk {instrumented_apk}")