import requests
import zipfile
import os

base = os.path.dirname(os.path.abspath(__file__))
gradle_url = 'https://mirrors.cloud.tencent.com/gradle/gradle-8.5-bin.zip'
gradle_zip = os.path.join(base, 'gradle-8.5-bin.zip')
gradle_extract_dir = os.path.join(base, 'gradle-dist')

print(f'Downloading Gradle 8.5 from Tencent Cloud mirror...')
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

print(f'\n  Download complete: {os.path.getsize(gradle_zip) / 1024 / 1024:.1f}MB')

print('Extracting Gradle...')
os.makedirs(gradle_extract_dir, exist_ok=True)
with zipfile.ZipFile(gradle_zip, 'r') as z:
    z.extractall(gradle_extract_dir)

gradle_bin = os.path.join(gradle_extract_dir, 'gradle-8.5', 'bin', 'gradle.bat')
print(f'Gradle binary: {gradle_bin}')
print(f'Exists: {os.path.exists(gradle_bin)}')
print('Done!')
