import requests
import os
import sys

url = 'https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip'
dst = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'cmdline-tools.zip')

print(f'Downloading to: {dst}')
r = requests.get(url, stream=True, timeout=60)
r.raise_for_status()

total = int(r.headers.get('Content-Length', 0))
downloaded = 0
chunk_size = 1024 * 1024  # 1MB

with open(dst, 'wb') as f:
    for chunk in r.iter_content(chunk_size=chunk_size):
        if chunk:
            f.write(chunk)
            downloaded += len(chunk)
            pct = downloaded * 100 // total if total else 0
            print(f'\r{downloaded / 1024 / 1024:.1f}MB / {total / 1024 / 1024:.1f}MB ({pct}%)', end='', flush=True)

print(f'\nDone! File saved to {dst}')
print(f'File size: {os.path.getsize(dst) / 1024 / 1024:.1f}MB')
