"""Generate this addon's own JSON files; all Aquamirae visuals are runtime references."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / 'common/src/main/resources'
GLYPHS = [
    ('armor_shatter', 'Armor Shatter', '护甲碎裂', 'shatterspike', 'shatterspike',
     'Apply Aquamirae armor shatter for 12 seconds. Each successful activation adds one level, up to five; Amplify adds levels. Each level reduces armor by 10%.',
     '施加海灵物语原生护甲碎裂，默认持续12秒。每次成功触发增加一层，最多五层；增强增加层数。每层降低10%护甲。'),
    ('health_shatter', 'Health Shatter', '生命碎裂', 'shatterblade', 'shatterblade',
     'Apply Aquamirae health shatter for 12 seconds. Each successful activation adds one level, up to ten; Amplify adds levels. Each level reduces maximum health by 5%. This does not heal the target when it expires.',
     '施加海灵物语原生生命碎裂，默认持续12秒。每次成功触发增加一层，最多十层；增强增加层数。每层降低5%生命上限，结束时不自动恢复生命。'),
    ('fury_of_the_depths', 'Fury of the Depths', '深海之怒', 'esca', 'esca',
     'Apply Aquamirae Fury of the Depths for 30 seconds. Each level adds 10% to the native Depths Fury attribute, up to five levels. It improves damage against creatures tagged as affected by Depths Fury (ice maze creatures by default), and scales native weapon abilities. It is not universal Strength.',
     '施加海灵物语原生深海之怒，默认持续30秒。每层增加10%深海之怒属性，最多五层。改善对受深海之怒影响的生物（默认冰迷宫生物）的伤害，并强化本体相关武器能力，不是通用力量。'),
    ('resilience_of_the_depths', 'Resilience of the Depths', '深海之韧', 'cooked_spinefish', 'cooked_spinefish',
     'Apply Aquamirae Resilience of the Depths for 30 seconds. Each level adds 10% to the native Depths Resilience attribute, up to five levels. It improves armor effectiveness and reduces the chance and multiplier of native critical hits from creatures tagged as affected by Depths Resilience (ice maze creatures by default). It is not universal Resistance.',
     '施加海灵物语原生深海之韧，默认持续30秒。每层增加10%深海之韧属性，最多五层。面对受深海之韧影响的攻击者（默认冰迷宫生物），提高护甲有效性，降低本体暴击概率与倍率，不是通用抗性提升。'),
    ('abyssal_shard', 'Abyssal Shard', '深渊碎晶', 'dreadwake', 'dreadwake',
     'NeoForge only. Replace a projectile carrier with the native shard from Dreadwake. Write Projectile, Abyssal Shard, then effects: the primary shard resolves them at its impact. Normal projectiles home on hostile mobs within 16 blocks, deal 10 base damage and inflict Weakness I for 2.5 seconds; they split into three half-sized native shards. Split children do not carry Ars effects. Orbit and other managed carriers retain their Ars trajectory; repeated carriers pay at each impact and remain intact. Amplify raises scale by 0.2, capped at 2.',
     '仅NeoForge。将弹射物替换为破晓之惧的原生碎晶。写法：弹射物→深渊碎晶→后续效果；主碎晶命中时执行后续魔符。普通弹射物追踪16格内敌对生物，基础伤害10，附加虚弱I持续2.5秒；碎裂生成三个半尺寸子碎晶，子碎晶只保留原生伤害，不续接魔符。环绕等形态保留Ars轨迹，重复命中每次扣费且不碎裂。增强使尺寸增加0.2，上限2。'),
]

SOURCES = {
    'armor_shatter': ('Shatterspike', '裂解尖刺'),
    'health_shatter': ('Shatterblade', '裂解之刃'),
    'fury_of_the_depths': ('Esca (native food buff)', '鮟鱇鱼精华（原生料理增益）'),
    'resilience_of_the_depths': ('Cooked Spinefish (native food buff)', '熟棘鱼（原生料理增益）'),
    'abyssal_shard': ('Dreadwake tier-two ability', '破晓之惧的二阶能力'),
}

def write(relative, data):
    path = ROOT / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

for lang in ['en_us', 'zh_cn']:
    values = {'itemGroup.aquamirae_nouveau': 'Aquamirae & Nouveau',
              'aquamirae_nouveau.cooldown': '%s is cooling down (%s s).' if lang == 'en_us' else '%s 冷却中（%s 秒）。'}
    for id_, en, zh, texture, material, desc_en, desc_zh in GLYPHS:
        source_en, source_zh = SOURCES[id_]
        description = (desc_en + ' Source and scribing ingredient: ' + source_en + '.') if lang == 'en_us' else (desc_zh + '来源及抄写材料：' + source_zh + '。')
        values['item.aquamirae_nouveau.glyph_' + id_] = en if lang == 'en_us' else zh
        values['aquamirae_nouveau.name.' + id_] = en if lang == 'en_us' else zh
        values['aquamirae_nouveau.description.' + id_] = description
        values['aquamirae_nouveau.glyph_name.glyph_' + id_] = en if lang == 'en_us' else zh
        values['aquamirae_nouveau.glyph_desc.glyph_' + id_] = description
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
