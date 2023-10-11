import os
from argparse import ArgumentParser

script_dir = os.path.dirname(os.path.realpath(__file__))
mandoline_dir = "G:\\Codes\\UndergraduateThesis\\Mandoline"
logger_jar = os.path.dirname(mandoline_dir) + "\\DynamicSlicingCore\\DynamicSlicingLoggingClasses\\DynamicSlicingLogger.jar"
android_jars = "C:\\Users\\14391\\AppData\\Local\\Android\\Sdk\\platforms"
android_callbacks = os.path.dirname(mandoline_dir) + "\\FlowDroid\\soot-infoflow-android\\AndroidCallbacks.txt"


def main():
    options = parse()

    apk_file = options["apk_file"]
    if not os.path.isfile(apk_file):
        print("Apk file does not exist!")
        return
    apk_file = os.path.abspath(apk_file)

    out_dir = options["out_dir"]
    if not os.path.isdir(out_dir):
        print(f"{out_dir} does not exist, creating it")
        os.makedirs(out_dir)
    out_dir = os.path.abspath(out_dir)

    instrumented_apk = instrument(apk_file=apk_file, out_dir=out_dir)
    print(f"Instrumented apk is at: {instrumented_apk}", flush=True)


def instrument(apk_file: str, out_dir: str) -> str:
    instr_file = "instr-debug.log"
    print("Instrumenting the Apk", flush=True)
    instr_cmd = f"java -Xmx8g -cp \"{mandoline_dir}\\Mandoline\\target\\mandoline-jar-with-dependencies.jar;{mandoline_dir}\\Mandoline\\target\\lib\\*\" ca.ubc.ece.resess.slicer.dynamic.mandoline.Slicer -m i -a {apk_file} -p {android_jars} -c {android_callbacks} -o {out_dir}/ -lc {logger_jar} > {out_dir}/{instr_file} 2>&1"
    os.system(instr_cmd)
    instrumented_apk = os.path.basename(apk_file).replace(".apk", "_i.apk")
    return out_dir + os.sep + instrumented_apk


def parse():
    parser = ArgumentParser()
    parser.add_argument("-a", "--apk_file", dest="apk_file",
                        help="Apk file", metavar="path/to/apk", required=True)
    parser.add_argument("-o", "--out_dir", dest="out_dir",
                        help="Output folder", metavar="path/to/out/folder", required=True)
    args = parser.parse_args()
    return {
        "apk_file": args.apk_file, "out_dir": args.out_dir
    }


if __name__ == "__main__":
    main()
