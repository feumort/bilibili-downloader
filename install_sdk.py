import subprocess
import os

base = os.path.dirname(os.path.abspath(__file__))
sdk_dir = os.path.join(base, 'sdk')
sdkmanager = os.path.join(sdk_dir, 'cmdline-tools', 'latest', 'bin', 'sdkmanager.bat')

env = os.environ.copy()
env['JAVA_HOME'] = r'D:\java21'
env['ANDROID_SDK_ROOT'] = sdk_dir

# Step 1: Accept all licenses
print('=== Accepting SDK licenses ===')
proc = subprocess.Popen(
    [sdkmanager, f'--sdk_root={sdk_dir}', '--licenses'],
    stdin=subprocess.PIPE,
    stdout=subprocess.PIPE,
    stderr=subprocess.STDOUT,
    env=env,
    text=True
)

# Send "y" for each license prompt
output_lines = []
while True:
    line = proc.stdout.readline()
    if not line and proc.poll() is not None:
        break
    if line:
        print(line.rstrip())
        output_lines.append(line)
        if '(y/N)' in line:
            proc.stdin.write('y\n')
            proc.stdin.flush()

proc.wait()
print(f'License acceptance exit code: {proc.returncode}')

# Step 2: Install SDK packages
print('\n=== Installing SDK packages ===')
packages = ['platform-tools', 'platforms;android-34', 'build-tools;34.0.0']
proc = subprocess.Popen(
    [sdkmanager, f'--sdk_root={sdk_dir}'] + packages,
    stdin=subprocess.PIPE,
    stdout=subprocess.PIPE,
    stderr=subprocess.STDOUT,
    env=env,
    text=True
)

while True:
    line = proc.stdout.readline()
    if not line and proc.poll() is not None:
        break
    if line:
        print(line.rstrip())
        if '(y/N)' in line:
            proc.stdin.write('y\n')
            proc.stdin.flush()

proc.wait()
print(f'\nSDK install exit code: {proc.returncode}')

# Verify installation
print('\n=== Verifying installation ===')
for check in ['platform-tools', 'platforms\\android-34', 'build-tools\\34.0.0']:
    path = os.path.join(sdk_dir, check)
    exists = os.path.exists(path)
    print(f'  {check}: {"OK" if exists else "MISSING"}')

print('\nDone!')
