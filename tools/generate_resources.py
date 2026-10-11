"""Generate this addon's own JSON files; all Aquamirae visuals are runtime references."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / 'common/src/main/resources'
GLYPHS = [
    ('armor_shatter', 'Armor Shatter', '护甲碎裂', 'shatterspike', 'sharp_bones',
     'Apply Aquamirae armor shatter for 12 seconds. Each successful activation adds one level, up to five; Amplify adds levels. Each level reduces armor by 10%.',
     '施加海灵物语原生护甲碎裂，默认持续12秒。每次成功触发增加一层，最多五层；增强增加层数。每层降低10%护甲。'),
    ('health_shatter', 'Health Shatter', '生命碎裂', 'shatterblade', 'abyssal_amethyst',
     'Apply Aquamirae health shatter for 12 seconds. Each successful activation adds one level, up to ten; Amplify adds levels. Each level reduces maximum health by 5%. This does not heal the target when it expires.',
     '施加海灵物语原生生命碎裂，默认持续12秒。每次成功触发增加一层，最多十层；增强增加层数。每层降低5%生命上限，结束时不自动恢复生命。'),
    ('fury_of_the_depths', 'Fury of the Depths', '深海之怒', 'terrible_fang', 'terrible_fang',
     'Apply Aquamirae Fury of the Depths for 30 seconds. Each level adds 10% to the native Depths Fury attribute, up to five levels. Native combat and equipment rules determine its damage benefits.',
     '施加海灵物语原生深海之怒，默认持续30秒。每层增加10%原生深海之怒属性，最多五层；伤害和装备收益遵循原模组规则。'),
    ('resilience_of_the_depths', 'Resilience of the Depths', '深海韧性', 'abyssal_amethyst', 'luminescent_bubble',
     'Apply Aquamirae Resilience of the Depths for 30 seconds. Each level adds 10% to the native Depths Resilience attribute, up to five levels. Native combat and equipment rules determine protection.',
     '施加海灵物语原生深海韧性，默认持续30秒。每层增加10%原生深海韧性属性，最多五层；防护和装备收益遵循原模组规则。'),
    ('abyssal_shard', 'Abyssal Shard', '深渊碎晶', 'abyssal_amethyst', 'abyssal_amethyst',
     'NeoForge only: launch a native homing shard at the trigger position along the incoming direction. It targets monsters or mobs attacking its owner, inflicts Weakness and splits on breaking. Amplify raises native scale by 0.2, capped at 2. Subsequent Ars effects stay at the original trigger, not the shard hit.',
     '仅NeoForge：在触发位置沿入射方向发射原生追踪碎晶，攻击怪物或正在攻击所有者的生物，命中附加虚弱，碎裂时分裂。增强使原生尺寸增加0.2，上限2。后续Ars效果仍在原触发位置执行，不附着到碎晶命中。'),
]

def write(relative, data):
    path = ROOT / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

for lang in ['en_us', 'zh_cn']:
    values = {'itemGroup.aquamirae_nouveau': 'Aquamirae & Nouveau',
              'aquamirae_nouveau.cooldown': '%s is cooling down (%s s).' if lang == 'en_us' else '%s 冷却中（%s 秒）。'}
    for id_, en, zh, texture, material, desc_en, desc_zh in GLYPHS:
        values['item.aquamirae_nouveau.glyph_' + id_] = en if lang == 'en_us' else zh
        values['aquamirae_nouveau.name.' + id_] = en if lang == 'en_us' else zh
        values['aquamirae_nouveau.description.' + id_] = desc_en if lang == 'en_us' else desc_zh
        values['ars_nouveau.augment_desc.glyph_' + id_ + '_glyph_amplify'] = (
            ('Increase native shard scale and damage.' if id_ == 'abyssal_shard' else 'Add effect levels up to the native limit.') if lang == 'en_us'
            else ('增加原生碎晶尺寸和伤害。' if id_ == 'abyssal_shard' else '增加效果层数，受原生上限限制。'))
        if id_ != 'abyssal_shard':
            values['ars_nouveau.augment_desc.glyph_' + id_ + '_glyph_extend_time'] = 'Extend the effect duration.' if lang == 'en_us' else '延长效果持续时间。'
    write('assets/aquamirae_nouveau/lang/' + lang + '.json', values)

for id_, en, zh, texture, material, desc_en, desc_zh in GLYPHS:
    write('assets/aquamirae_nouveau/models/item/glyph_' + id_ + '.json',
          {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'aquamirae:item/' + texture}})
    write('data/aquamirae_nouveau/recipe/glyph_' + id_ + '.json',
          {'type': 'ars_nouveau:glyph', 'exp': 27,
           'output': {'id': 'aquamirae_nouveau:glyph_' + id_, 'count': 1},
           'inputs': [{'item': 'aquamirae:' + material}, {'item': 'ars_nouveau:source_gem'}]})
