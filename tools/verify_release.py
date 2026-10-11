"""Verify release contents and runtime resource references against the locked local JARs."""
import hashlib
import json
from pathlib import Path
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]

def check(platform):
    base = ROOT / platform
    for entry in json.loads((base / 'dependencies.lock.json').read_text(encoding='utf8')):
        path = base / 'libs' / entry['file']
        assert hashlib.sha256(path.read_bytes()).hexdigest() == entry['sha256'], f'Dependency drift: {path}'
    native_path = base / 'libs' / ('aquamirae-forge1201.jar' if platform else 'aquamirae.jar')
    with zipfile.ZipFile(native_path) as native:
        native_files = set(native.namelist())
        native_lang = json.loads(native.read('assets/aquamirae/lang/en_us.json'))
    version = (ROOT / 'VERSION').read_text().strip()
    suffix = 'mc1.20.1-forge' if platform else 'mc1.21.1-neoforge'
    binary = base / 'build/libs' / f'aquamirae-nouveau-{version}-{suffix}.jar'
    source = base / 'build/libs' / f'aquamirae-nouveau-{version}-sources.jar'
    for path in [binary, source]:
        with zipfile.ZipFile(path) as z:
            names = z.namelist()
            assert 'LICENSE' in names and 'THIRD_PARTY_NOTICES.md' in names, 'Missing license or attribution'
            assert not any(n.startswith(('dev/obscuria/', 'assets/aquamirae/', 'data/aquamirae/', '.upstream/', 'libs/', 'run/')) for n in names), f'Upstream content in {path}'
            assert not any(n.endswith(('.jar', '.png', '.ogg', '.nbt')) or 'GameTests' in n for n in names), f'Bundled development/native data in {path}'
            for name in names:
                if name.startswith('assets/aquamirae_nouveau/models/item/') and name.endswith('.json'):
                    model = json.loads(z.read(name))
                    for texture in model.get('textures', {}).values():
                        namespace, loc = texture.split(':', 1)
                        assert f'assets/{namespace}/textures/{loc}.png' in native_files, f'Missing native texture: {texture}'
            recipes = [n for n in names if n.startswith('data/aquamirae_nouveau/recipe') and n.endswith('.json')]
            assert len(recipes) == (4 if platform else 5), f'Wrong recipe count: {path}'
            for name in recipes:
                recipe = json.loads(z.read(name))
                assert recipe['type'] == 'ars_nouveau:glyph' and recipe['exp'] == 27
                inputs = recipe['inputItems'] if platform else recipe['inputs']
                assert len(inputs) == 2
                for ingredient in inputs:
                    namespace, item = (ingredient['item']['item'] if platform else ingredient['item']).split(':', 1)
                    if namespace == 'aquamirae':
                        assert f'item.aquamirae.{item}' in native_lang, f'Missing native recipe item: {item}'
            if platform:
                assert not any('glyph_abyssal_shard.json' in n for n in names), 'Forge exposes unsupported shard'
        print(f'PASS {path} SHA-256 {hashlib.sha256(path.read_bytes()).hexdigest()}')

if __name__ == '__main__':
    check('forge' if '--forge' in sys.argv else '')
