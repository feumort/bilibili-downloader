import zipfile
import os
import shutil
import requests
import sys

base = os.path.dirname(os.path.abspath(__file__))
sdk_dir = os.path.join(base, 'sdk')
zip_path = os.path.join(base, 'cmdline-tools.zip')

# Step 1: Extract cmdline-tools
print('=== Step 1: Extracting cmdline-tools ===')
latest_dir = os.path.join(sdk_dir, 'cmdline-tools', 'latest')
os.makedirs(latest_dir, exist_ok=True)

with zipfile.ZipFile(zip_path, 'r') as z:
    z.extractall(latest_dir)

# The zip extracts to cmdline-tools/bin, cmdline-tools/lib etc.
# We need them directly under latest/
extracted_root = os.path.join(latest_dir, 'cmdline-tools')
if os.path.exists(extracted_root):
    for item in os.listdir(extracted_root):
        src = os.path.join(extracted_root, item)
        dst = os.path.join(latest_dir, item)
        if os.path.exists(dst):
            if os.path.isdir(dst):
                shutil.rmtree(dst)
            else:
                os.remove(dst)
        shutil.move(src, dst)
    os.rmdir(extracted_root)

print(f'SDK tools extracted to: {latest_dir}')
for item in os.listdir(latest_dir):
    print(f'  - {item}')

# Step 2: Create local.properties
print('\n=== Step 2: Creating local.properties ===')
local_props = os.path.join(base, 'local.properties')
with open(local_props, 'w') as f:
    f.write(f'sdk.dir={sdk_dir}')
print(f'local.properties created: sdk.dir={sdk_dir}')

# Step 3: Download Gradle 8.5
print('\n=== Step 3: Downloading Gradle 8.5 ===')
gradle_url = 'https://services.gradle.org/distributions/gradle-8.5-bin.zip'
gradle_zip = os.path.join(base, 'gradle-8.5-bin.zip')

r = requests.get(gradle_url, stream=True, timeout=60)
r.raise_for_status()
total = int(r.headers.get('Content-Length', 0))
downloaded = 0

with open(gradle_zip, 'wb') as f:
    for chunk in r.iter_content(chunk_size=1024 * 1024):
        if chunk:
            f.write(chunk)
            downloaded += len(chunk)
            pct = downloaded * 100 // total if total else 0
            print(f'\r  {downloaded / 1024 / 1024:.1f}MB / {total / 1024 / 1024:.1f}MB ({pct}%)', end='', flush=True)

print(f'\n  Gradle downloaded: {os.path.getsize(gradle_zip) / 1024 / 1024:.1f}MB')

# Extract Gradle
print('\n=== Step 4: Extracting Gradle ===')
gradle_extract_dir = os.path.join(base, 'gradle-dist')
os.makedirs(gradle_extract_dir, exist_ok=True)
with zipfile.ZipFile(gradle_zip, 'r') as z:
    z.extractall(gradle_extract_dir)

gradle_bin = os.path.join(gradle_extract_dir, 'gradle-8.5', 'bin', 'gradle.bat')
print(f'Gradle extracted to: {gradle_extract_dir}')
print(f'Gradle binary: {gradle_bin}')
print(f'Exists: {os.path.exists(gradle_bin)}')

print('\n=== Setup complete! ===')
