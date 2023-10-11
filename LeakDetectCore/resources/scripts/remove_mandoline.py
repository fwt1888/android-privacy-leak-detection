import os
import sys
import subprocess

def concat_args(*args):
    s = ""
    for a in args:
        s += str(a) + " "
    return s.strip()


def exec_command_output(*args):
    s = concat_args(*args)
    return subprocess.run(s.split(" "), stdout=subprocess.PIPE).stdout.decode('utf-8')


device_id = sys.argv[1]
apk = sys.argv[2]

package_name = exec_command_output("C:\\Users\\14391\\AppData\\Local\\Android\\Sdk\\build-tools\\32.0.0\\aapt.exe", "dump", "badging", apk).split("\n")[0]
package_name = package_name.split("'")[1]

os.system(f"adb -s {device_id} uninstall {package_name} >NUL 2>&1")
