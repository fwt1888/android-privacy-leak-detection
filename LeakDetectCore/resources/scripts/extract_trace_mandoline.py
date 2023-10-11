import os 
import sys



device_id = sys.argv[1]
out_dir = sys.argv[2]
trace = out_dir + "/trace"


def get_exec_time(trace):
   exec_time = 0
   with open(trace + ".log", "r") as f:
       for l in f:
           if "Timer-Method:" in l:
               l = l.rstrip()
               s = l.split(":")
               exec_time += int(s[-1]) - int(s[-2])

   exec_time = exec_time/1e9
   with open(trace+"_time.log", "w") as f:
       f.write("Elapsed execution time: " + str(exec_time) + "\n")

print("Dumping logcat")
os.system("adb -s " + device_id + " logcat -d > " + trace + "_full.log")
print("Extracting slicing trace")
os.system("adb -s " + device_id + " logcat -d | findstr /c:\"SLICING\" /c:\"Timer:\" > " + trace + ".log")
print("Extracting execution time")
get_exec_time(trace)


# print(f"adb -s {device_id} logcat -d > {trace}_full.log")
# print(f"adb -s {device_id} logcat -d | findstr /c:\"SLICING\" /c:\"Timer:\" > {trace}.log")
