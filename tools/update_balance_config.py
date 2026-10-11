"""Apply dev.9 defaults to this addon's existing TOML, preserving other/custom values.

Only old dev.8 defaults are migrated. Print the exact plan unless --apply is supplied.
"""
import argparse
from copy import deepcopy
import hashlib
import json
from pathlib import Path
import re
import shutil
import tomllib

MIGRATIONS = {
    'mana.terrible_chakram': (60, 40), 'mana.maze_rose': (70, 50),
    'mana.armor_burst': (60, 20), 'mana.health_burst': (60, 15),
    'mana.eel_roar': (40, 80), 'mana.eel_bite': (70, 100), 'mana.cornelia_vortex': (80, 120),
    'cooldowns.seconds.scyphoid_sting': (3, 5), 'cooldowns.seconds.maze_rose': (3, 5),
    'cooldowns.seconds.eel_roar': (5, 30), 'cooldowns.seconds.eel_bite': (3, 5),
    'cooldowns.seconds.cornelia_vortex': (10, 45),
    'creatureActions.roarUpkeep': (4, 12), 'creatureActions.vortexUpkeep': (8, 16),
}
ADDITIONS = {
    'creatureActions.roarMaxTicks': 300, 'creatureActions.vortexMaxTicks': 400,
    'creatureActions.roarRecoverySeconds': 20, 'creatureActions.vortexRecoverySeconds': 15,
    'chanting.enabled': True, 'chanting.eelRoarTicks': 13, 'chanting.corneliaVortexTicks': 20,
}

def lookup(data, name):
    value = data
    for key in name.split('.'):
        if not isinstance(value, dict) or key not in value:
            return None
        value = value[key]
    return value

def assign(data, name, value):
    keys = name.split('.')
    for key in keys[:-1]:
        data = data.setdefault(key, {})
    data[keys[-1]] = value

def replace_setting(text, name, value):
    section, key = name.rsplit('.', 1)
    literal = str(value).lower() if isinstance(value, bool) else str(value)
    headers = list(re.finditer(r'(?m)^[ \t]*\[([^\[\]\r\n]+)\][ \t]*(?:#.*)?$', text))
    for i, header in enumerate(headers):
        if header.group(1) != section:
            continue
        begin = header.end()
        end = headers[i + 1].start() if i + 1 < len(headers) else len(text)
        block = text[begin:end]
        pattern = r'(?m)^([ \t]*' + re.escape(key) + r'[ \t]*=[ \t]*)([^#\r\n]*)(#.*)?$'
        match = re.search(pattern, block)
        if match:
            prefix = block[:match.start()]
            prefix = re.sub(r'(?m)(^[ \t]*#[ \t]*Default:[ \t]*)[^\n]*(?=\n(?:[ \t]*#[^\n]*\n)*\Z)',
                            lambda m: m.group(1) + literal, prefix)
            block = prefix + block[match.start():]
            block = re.sub(pattern, lambda m: m.group(1) + literal + (' ' + m.group(3) if m.group(3) else ''), block, count=1)
        else:
            block = block.rstrip() + '\n' + key + ' = ' + literal + '\n\n'
        return text[:begin] + block + text[end:]
    return text.rstrip() + '\n\n[' + section + ']\n' + key + ' = ' + literal + '\n'

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('config', type=Path)
    parser.add_argument('--apply', action='store_true')
    parser.add_argument('--backup', type=Path, help='Exact backup file, required with --apply')
    args = parser.parse_args()
    path = args.config.resolve(strict=True)
    if path.name != 'aquamirae_nouveau-server.toml':
        parser.error('Only aquamirae_nouveau-server.toml may be updated')
    original = path.read_bytes()
    text = original.decode('utf-8-sig').replace('\r\n', '\n')
    data = tomllib.loads(text)
    expected = deepcopy(data)
    changes, preserved = [], []
    for name, (old, new) in MIGRATIONS.items():
        current = lookup(data, name)
        if current is None or current == old:
            changes.append(dict(key=name, old=current, new=new))
        elif current != new:
            preserved.append(name)
    for name, new in ADDITIONS.items():
        if lookup(data, name) is None:
            changes.append(dict(key=name, old=None, new=new))
    for change in changes:
        text = replace_setting(text, change['key'], change['new'])
        assign(expected, change['key'], change['new'])
    if tomllib.loads(text) != expected:
        raise ValueError('TOML rewrite would alter unrelated settings')
    result = dict(config=str(path), changes=changes, custom_values_preserved=preserved,
                  before_sha256=hashlib.sha256(original).hexdigest(), applied=False)
    if args.apply and changes:
        if args.backup is None:
            parser.error('--apply requires an explicit --backup file')
        backup = args.backup.resolve()
        if backup == path:
            parser.error('Backup must differ from the configuration')
        backup.parent.mkdir(parents=True, exist_ok=True)
        if backup.exists() and backup.read_bytes() != original:
            raise FileExistsError(f'Backup already differs: {backup}')
        shutil.copy2(path, backup)
        if backup.read_bytes() != original:
            raise ValueError('Backup verification failed')
        newline = '\r\n' if b'\r\n' in original else '\n'
        path.write_bytes(text.replace('\n', newline).encode('utf-8'))
        if tomllib.loads(path.read_text(encoding='utf-8')) != expected:
            shutil.copy2(backup, path)
            raise ValueError('Written settings failed validation; original restored')
        result.update(applied=True, backup=str(backup), after_sha256=hashlib.sha256(path.read_bytes()).hexdigest())
    # ASCII-safe JSON also works when a Windows subprocess pipe defaults to the legacy code page.
    print(json.dumps(result, ensure_ascii=True, indent=2))

if __name__ == '__main__':
    main()
