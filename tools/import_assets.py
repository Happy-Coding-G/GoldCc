"""Prepare pinned local assets for the Android build from the Chinese S18 catalogue."""
from concurrent.futures import ThreadPoolExecutor
from hashlib import sha256
from io import BytesIO
from pathlib import Path
import os
import subprocess
import sys
import venv

BASE = 'https://jcc.np5.top/season-assets/bb8e731abf914bc185ae6651e4194e39/'
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'app/build/generated/portraitAssets'
PILLOW_VERSION = '13.0.0'

def prepare(row):
    from PIL import Image
    unit, url, expected = row.split('\t')
    target = OUT / (unit + '.png')
    if target.exists():
        with Image.open(target) as image:
            if image.size == (64, 64) and sha256(image.convert('RGB').tobytes()).hexdigest() == expected:
                return
    raw = subprocess.check_output(['curl', '-fsSL', '--max-time', '60', '--retry', '2', BASE + url])
    with Image.open(BytesIO(raw)) as image:
        result = image.convert('RGB').resize((64, 64), Image.Resampling.LANCZOS)
        if sha256(result.tobytes()).hexdigest() != expected:
            raise ValueError('Portrait content changed: ' + unit)
        temp = target.with_suffix('.tmp')
        result.save(temp, format='PNG')
        temp.replace(target)

def main():
    try:
        import PIL
        ready = PIL.__version__ == PILLOW_VERSION
    except ImportError:
        ready = False
    if not ready:
        env = ROOT / '.gradle/portrait-python'
        python = env / ('Scripts/python.exe' if os.name == 'nt' else 'bin/python')
        if not python.exists():
            venv.create(env, with_pip=True)
        subprocess.run([str(python), '-m', 'pip', 'install', 'Pillow==' + PILLOW_VERSION], check=True)
        subprocess.run([str(python), __file__], check=True)
        return
    OUT.mkdir(parents=True, exist_ok=True)
    rows = (ROOT / 'tools/portraits.lock').read_text(encoding='utf8').splitlines()
    with ThreadPoolExecutor(8) as pool:
        list(pool.map(prepare, rows))
    print(f'{len(rows)} portraits verified')

if __name__ == '__main__':
    main()
