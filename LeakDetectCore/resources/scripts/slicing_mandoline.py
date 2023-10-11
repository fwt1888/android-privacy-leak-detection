# -*- coding: gbk -*-
import os
import sys

android_jars= "C:\\Users\\14391\\AppData\\Local\\Android\\Sdk\\platforms"
sep = os.path.sep
base_dir = "G:\\Codes\\UndergraduateThesis\\Mandoline"

apk_path = sys.argv[1]
apk_name = sys.argv[2]
slice_line = sys.argv[3]
if(len(sys.argv) - 1 == 4):
    slice_vars = sys.argv[4]

out_dir = "G:\\Codes\\UndergraduateThesis\\LeakDetectCore\\results\\slicer\\" + apk_name
class_path = f"{base_dir}/Mandoline/target/mandoline-jar-with-dependencies.jar;{base_dir}/Mandoline/target/lib/*".replace("/", sep)
flowdroid_callbacks = f"{base_dir}/FlowDroid/soot-infoflow-android/AndroidCallbacks.txt".replace("/", sep)
stubdroid_summaries = f"{base_dir}/FlowDroid/soot-infoflow-summaries/summariesManual".replace("/", sep)
flowdroid_taint_wrapper = f"{base_dir}/FlowDroid/soot-infoflow/EasyTaintWrapperSource.txt".replace("/", sep)

if(len(sys.argv) - 1 == 4):
    cmd = f"java -cp \"{class_path}\" ca.ubc.ece.resess.slicer.dynamic.mandoline.Slicer -m s -a {apk_path} -t {out_dir}{sep}trace.log -p {android_jars} -c {flowdroid_callbacks} -o {out_dir}{sep} -sl {out_dir}{sep}static-log.log -sd {stubdroid_summaries} -tw {flowdroid_taint_wrapper} -sp {slice_line} -sv {slice_vars} -debug > {out_dir}{sep}slice-file.log 2>&1"
else:
    cmd = f"java -cp \"{class_path}\" ca.ubc.ece.resess.slicer.dynamic.mandoline.Slicer -m s -a {apk_path} -t {out_dir}{sep}trace.log -p {android_jars} -c {flowdroid_callbacks} -o {out_dir}{sep} -sl {out_dir}{sep}static-log.log -sd {stubdroid_summaries} -tw {flowdroid_taint_wrapper} -sp {slice_line} -debug > {out_dir}{sep}slice-file.log 2>&1"
os.system(cmd)