"""Verify release contents and runtime resource references against the locked local JARs."""
import hashlib
import json
from pathlib import Path
import sys
import zipfile
import tomllib

ROOT = Path(__file__).resolve().parents[1]

def check(platform):
    base = ROOT / platform
    for entry in json.loads((base / 'dependencies.lock.json').read_text(encoding='utf8')):
        path = base / 'libs' / entry['file']
        assert hashlib.sha256(path.read_bytes()).hexdigest() == entry['sha256'], f'Dependency drift: {path}'
    native_path = base / 'libs' / ('aquamirae-forge1201.jar' if platform else 'aquamirae.jar')
    ars_path = base / 'libs' / ('ars-nouveau-forge1201.jar' if platform else 'ars.jar')
    with zipfile.ZipFile(ars_path) as ars:
        # Read the actual API's translation contract, rather than checking only our JSON syntax.
        api = ars.read('com/hollingsworth/arsnouveau/api/spell/AbstractSpellPart.class')
        assert b'.glyph_name.' in api and b'.glyph_desc.' in api, 'Ars translation contract changed'
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
            assert len(recipes) == (12 if platform else 13), f'Wrong recipe count: {path}'
            atlas = json.loads(z.read('assets/minecraft/atlases/mob_effects.json'))
            for entry in atlas['sources']:
                namespace, texture = entry['resource'].split(':', 1)
                assert f'assets/{namespace}/textures/{texture}.png' in native_files, 'Missing status icon source'
            vortex = json.loads(z.read('data/aquamirae_nouveau/' + ('recipes/' if platform else 'recipe/') + 'glyph_cornelia_vortex.json'))
            ingredients = vortex['inputItems'] if platform else vortex['inputs']
            assert any((entry['item']['item'] if platform else entry['item']) == 'aquamirae:frozen_key' for entry in ingredients), 'Vortex must require the captain drop'
            for name in recipes:
                recipe = json.loads(z.read(name))
                glyph = recipe['output'] if platform else recipe['output']['id']
                _, glyph_id = glyph.split(':', 1)
                for language in ['en_us', 'zh_cn']:
                    lang = json.loads(z.read(f'assets/aquamirae_nouveau/lang/{language}.json'))
                    for kind in ['glyph_name', 'glyph_desc']:
                        key = f'aquamirae_nouveau.{kind}.{glyph_id}'
                        assert lang.get(key, '').strip(), f'Missing spellbook translation: {key} ({language})'
                assert recipe['type'] == 'ars_nouveau:glyph' and recipe['exp'] == 27
                inputs = recipe['inputItems'] if platform else recipe['inputs']
                assert len(inputs) == 2
                for ingredient in inputs:
                    namespace, item = (ingredient['item']['item'] if platform else ingredient['item']).split(':', 1)
                    if namespace == 'aquamirae':
                        assert f'item.aquamirae.{item}' in native_lang, f'Missing native recipe item: {item}'
                        model = json.loads(z.read(f'assets/aquamirae_nouveau/models/item/{glyph_id}.json'))
                        assert model['textures']['layer0'] == 'aquamirae:item/' + item, 'Icon and acquisition must use the same native source'
            if platform:
                assert not any('glyph_abyssal_shard.json' in n for n in names), 'Forge exposes unsupported shard'
                assert not any('SpellAbyssalShard' in n or 'ShardSplitMixin' in n or n == 'aquamirae_nouveau.mixins.json' for n in names), 'Forge ships Neo-only hooks'
            if path == binary:
                metadata = tomllib.loads(z.read('META-INF/mods.toml' if platform else 'META-INF/neoforge.mods.toml').decode('utf8'))
                dependency = next(d for d in metadata['dependencies']['aquamirae_nouveau'] if d['modId'] == 'ars_conflux')
                assert dependency['versionRange'] == '[0.1.16,0.2)', 'Alliance API requires Core 0.1.16'
                configs = ['aquamirae_nouveau_chakram.mixins.json'] if platform else [entry['config'] for entry in metadata['mixins']]
                for mixin_file in configs:
                    mixins = json.loads(z.read(mixin_file))
                    for hook in mixins['mixins']:
                        assert mixins['package'].replace('.', '/') + '/' + hook + '.class' in names, 'Missing native/Ars opt-in hook'
                    if platform:
                        refmap = json.loads(z.read(mixins['refmap']))
                        assert refmap['mappings'], 'Forge hooks must have runtime mappings'
                control = z.read('dev/aquamiraenouveau/projectile/ChakramControl.class')
                assert b'dev/arsconflux/api/targeting/Allies' in control, 'Missing Core alliance query'
        print(f'PASS {path} SHA-256 {hashlib.sha256(path.read_bytes()).hexdigest()}')

if __name__ == '__main__':
    check('forge' if '--forge' in sys.argv else '')
