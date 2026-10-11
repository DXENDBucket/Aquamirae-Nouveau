"""Generate this addon's own JSON files; all Aquamirae visuals are runtime references."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / 'common/src/main/resources'
GLYPHS = [
    ('armor_shatter', 'Armor Shatter', '护甲碎裂', 'shatterspike', 'shatterspike',
     'Shatter the target\'s armor, reducing armor by 10% per level. Repeated hits stack up to five levels.',
     '使目标护甲碎裂，每层降低10%护甲。重复命中可叠加，最多五层。'),
    ('health_shatter', 'Health Shatter', '生命碎裂', 'shatterblade', 'shatterblade',
     'Shatter the target\'s health, reducing maximum health by 5% per level. Repeated hits stack up to ten levels.',
     '使目标生命碎裂，每层降低5%生命上限。重复命中可叠加，最多十层。'),
    ('fury_of_the_depths', 'Fury of the Depths', '深海之怒', 'esca', 'esca',
     'Grant Fury of the Depths, improving the target\'s damage against ice maze creatures.',
     '赋予目标深海之怒，提升对冰迷宫生物的伤害。'),
    ('resilience_of_the_depths', 'Resilience of the Depths', '深海之韧', 'cooked_spinefish', 'cooked_spinefish',
     'Grant Resilience of the Depths, protecting the target against attacks from ice maze creatures.',
     '赋予目标深海之韧，提高抵御冰迷宫生物攻击的能力。'),
    ('abyssal_shard', 'Abyssal Shard', '深渊碎晶', 'dreadwake', 'dreadwake',
     'Replace a projectile with Dreadwake\'s shard, dealing damage and inflicting Weakness on impact. It splits into three smaller shards when broken; Amplify increases damage.',
     '以破晓之惧的碎晶替换弹射物，命中造成伤害并附加虚弱，碎裂时分出三个小碎晶。增强提高伤害。'),
    ('scyphoid_sting', 'Scyphoid Sting', '水母蜇刺', 'jellyfish_jelly', 'jellyfish_jelly',
     'Inflict an abyssal scyphoid\'s Poison III and Blindness for three seconds. Amplify strengthens the poison; Extend Time prolongs both effects.',
     '以深海水母的蜇刺使目标中毒III并失明，持续三秒。增强提高毒性，延时延长持续时间。'),
    ('terrible_chakram', 'Terrible Chakram', '可怖环刃', 'terrible_chakram', 'terrible_chakram',
     'Replace a projectile with a returning terrible chakram. It deals damage and poisons targets it passes through, ignoring allies.',
     '以可怖环刃替换弹射物，穿过目标造成伤害并附加中毒，随后飞回。不会命中友军。'),
    ('maze_rose', 'Maze Rose', '迷宫玫瑰', 'maze_rose', 'maze_rose',
     'Replace a projectile with a returning maze rose that tracks monsters. It deals damage and briefly levitates targets, ignoring allies.',
     '以迷宫玫瑰替换弹射物，追踪怪物，命中造成伤害并短暂浮空，随后飞回。不会锁定或命中友军。'),
    ('armor_burst', 'Armor Burst', '碎甲迸裂', 'shatterspike', 'shatterspike',
     'Deal Shatterspike\'s bonus damage to a target with five levels of Armor Shatter.',
     '护甲碎裂达到五层时，造成裂解尖刺的额外伤害。'),
    ('health_burst', 'Health Burst', '碎命迸裂', 'shatterblade', 'shatterblade',
     'Deal Shatterblade\'s bonus damage to a target with ten levels of Health Shatter.',
     '生命碎裂达到十层时，造成裂解之刃的额外伤害。'),
    ('eel_roar', 'Eel Roar', '鳗鱼咆哮', 'dead_sea_scroll', 'dead_sea_scroll',
     'Grant an ally an eel\'s roar, repelling enemies ahead and inflicting Slowness and Darkness. Requires mana each second.',
     '使友军持续向前咆哮，推开敌人并施加缓慢、黑暗与寒意。每秒消耗魔力。'),
    ('eel_bite', 'Siphoning Bite', '吸血咬击', 'sharp_bones', 'sharp_bones',
     'Bite and poison a target, healing the attacker for twice the health actually lost.',
     '撕咬目标并使其中毒，按实际造成伤害的两倍为攻击者回血。'),
    ('cornelia_vortex', "Cornelia's Vortex", '船长旋流', 'frozen_key', 'frozen_key',
     'Grant an ally a vortex that swirls nearby enemies with up to 100 maximum health, then repels them. Requires mana each second.',
     '使友军卷起旋流，牵动附近生命上限不超过100的敌人，结束时将其推开。每秒消耗魔力。'),
]

def write(relative, data):
    path = ROOT / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

for lang in ['en_us', 'zh_cn']:
    values = {'itemGroup.aquamirae_nouveau': 'Aquamirae & Nouveau',
              'aquamirae_nouveau.cooldown': '%s is cooling down (%s s).' if lang == 'en_us' else '%s 冷却中（%s 秒）。',
              'aquamirae_nouveau.preparation': 'Preparing sea magic (%s s)' if lang == 'en_us' else '海灵法术吟唱（%s 秒）',
              'aquamirae_nouveau.preparation_cancelled': 'Preparation interrupted.' if lang == 'en_us' else '吟唱已中断。',
              'aquamirae_nouveau.validation.amplify': 'This projectile has no Amplify effect.' if lang == 'en_us' else '该弹射物不支持增强。'}
    for id_, en, zh, texture, material, desc_en, desc_zh in GLYPHS:
        description = desc_en if lang == 'en_us' else desc_zh
        values['item.aquamirae_nouveau.glyph_' + id_] = en if lang == 'en_us' else zh
        values['aquamirae_nouveau.name.' + id_] = en if lang == 'en_us' else zh
        values['aquamirae_nouveau.description.' + id_] = description
        values['aquamirae_nouveau.glyph_name.glyph_' + id_] = en if lang == 'en_us' else zh
        values['aquamirae_nouveau.glyph_desc.glyph_' + id_] = description
        if id_ in ('eel_roar', 'cornelia_vortex'):
            values['effect.aquamirae_nouveau.' + id_] = en if lang == 'en_us' else zh
            values['ars_nouveau.augment_desc.glyph_' + id_ + '_glyph_aoe'] = 'Increase range.' if lang == 'en_us' else '扩大作用范围。'
            values['ars_nouveau.augment_desc.glyph_' + id_ + '_glyph_extend_time'] = 'Extend the granted action.' if lang == 'en_us' else '延长持续时间。'
            if id_ == 'cornelia_vortex':
                values['ars_nouveau.augment_desc.glyph_' + id_ + '_glyph_amplify'] = 'Increase the maximum health limit by 50.' if lang == 'en_us' else '将可影响的生命上限提高50。'
            continue
        if id_ == 'eel_bite':
            values['ars_nouveau.augment_desc.glyph_' + id_ + '_glyph_amplify'] = 'Increase bite damage.' if lang == 'en_us' else '提高咬击伤害。'
            values['ars_nouveau.augment_desc.glyph_' + id_ + '_glyph_extend_time'] = 'Extend the poison.' if lang == 'en_us' else '延长中毒时间。'
            continue
        if id_ in ('terrible_chakram', 'maze_rose'):
            values['ars_nouveau.augment_desc.glyph_' + id_ + '_glyph_accelerate'] = 'Increase launch speed.' if lang == 'en_us' else '提高发射速度。'
            continue
        if id_ in ('armor_burst', 'health_burst'):
            continue
        values['ars_nouveau.augment_desc.glyph_' + id_ + '_glyph_amplify'] = (
            ('Increase native shard scale and damage.' if id_ == 'abyssal_shard' else 'Strengthen the poison up to Poison V.' if id_ == 'scyphoid_sting' else 'Add effect levels up to the native limit.') if lang == 'en_us'
            else ('增加原生碎晶尺寸和伤害。' if id_ == 'abyssal_shard' else '提高毒性，最高中毒V。' if id_ == 'scyphoid_sting' else '增加效果层数，受原生上限限制。'))
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

# Alias the original source item textures into vanilla's status icon atlas without copying images.
write('assets/minecraft/atlases/mob_effects.json', {'sources': [
    {'type': 'minecraft:single', 'resource': 'aquamirae:item/' + material, 'sprite': 'aquamirae_nouveau:' + id_}
    for id_, material in [('eel_roar', 'dead_sea_scroll'), ('cornelia_vortex', 'frozen_key')]
]})
