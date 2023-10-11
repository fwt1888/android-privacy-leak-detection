import os
from argparse import ArgumentParser

script_dir = os.path.dirname(os.path.realpath(__file__))
slicer4j_dir = "G:\\Codes\\UndergraduateThesis\\Slicer4J"
logger_jar = os.path.dirname(slicer4j_dir) + "\\DynamicSlicingCore\\DynamicSlicingLoggingClasses\\DynamicSlicingLogger.jar"


def main():
    options = parse()

    jar_file = options["jar_file"]
    if not os.path.isfile(jar_file):
        print("Jar file does not exist!")
        return
    jar_file = os.path.abspath(jar_file)

    out_dir = options["out_dir"]
    if not os.path.isdir(out_dir):
        print(f"{out_dir} does not exist, creating it")
        os.makedirs(out_dir)
    out_dir = os.path.abspath(out_dir)

    instrumented_jar = instrument(jar_file=jar_file, out_dir=out_dir)
    print(f"Instrumented jar is at: {instrumented_jar}", flush=True)


def instrument(jar_file: str, out_dir: str) -> str:
    instr_file = "instr-debug.log"
    print("Instrumenting the JAR", flush=True)
    instr_cmd = f"java -Xmx8g -cp \"{slicer4j_dir}\\Slicer4J\\target\\slicer4j-jar-with-dependencies.jar;{slicer4j_dir}\\Slicer4J\\target\\lib\\*\" ca.ubc.ece.resess.slicer.dynamic.slicer4j.Slicer -m i -j {jar_file} -o {out_dir}\\ -sl {out_dir}\\static_log.log -lc {logger_jar} > {out_dir}\\{instr_file} 2>&1"
    os.system(instr_cmd)
    instrumented_jar = os.path.basename(jar_file).replace(".jar", "_i.jar")
    return out_dir + os.sep + instrumented_jar


def parse():
    parser = ArgumentParser()
    parser.add_argument("-j", "--jar_file", dest="jar_file",
                        help="JAR file", metavar="path/to/jar", required=True)
    parser.add_argument("-o", "--out_dir", dest="out_dir",
                        help="Output folder", metavar="path/to/out/folder", required=True)
    args = parser.parse_args()
    return {
        "jar_file": args.jar_file, "out_dir": args.out_dir
    }


if __name__ == "__main__":
    main()
