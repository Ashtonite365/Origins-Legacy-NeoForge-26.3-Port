#!/usr/bin/env python3
# Mechanical 1.21.1 -> 26.3 rewrites safe to apply anywhere.
import os,re,sys,collections
R=[(r'\bnet\.neoforged\.neoforge\.items\.', 'com.hollingsworth.nuggets.compat.items.'),
   (r'([\w]+(?:\(\))?(?:\.[\w]+(?:\(\))?)*)\.getCapability\(\s*Capabilities\.ItemHandler\.BLOCK\s*,\s*', r'com.hollingsworth.nuggets.compat.items.LegacyItemHandlers.getBlock(\1, '),
   (r'\bnet\.neoforged\.neoforge\.items\.wrapper\.(InvWrapper|PlayerMainInvWrapper|RangedWrapper|CombinedInvWrapper|EmptyItemHandler)\b', r'com.hollingsworth.nuggets.compat.items.wrapper.\1'),
   (r'\bnet\.neoforged\.neoforge\.items\.(IItemHandler|IItemHandlerModifiable|ItemStackHandler|ItemHandlerHelper)\b', r'com.hollingsworth.nuggets.compat.items.\1'),
   (r'@org\.jspecify\.annotations\.Nullable net\.minecraft\.world\.level\.redstone\.Orientation', 'net.minecraft.world.level.redstone.@org.jspecify.annotations.Nullable Orientation'),
   (r'\bPushReaction\.DESTROY\b', 'PushReaction.POPPED'), (r'\bPushReaction\.BLOCK\b', 'PushReaction.IMMOVEABLE'), (r'\bPushReaction\.NORMAL\b', 'PushReaction.PUSH_PULL'),
   (r'\bRenderShape\.ENTITYBLOCK_ANIMATED\b', 'RenderShape.INVISIBLE'),
   (r'\.noCollission\(\)', '.noCollision()'),
   (r'\.getCommandSenderWorld\(\)', '.level()'),
   (r'\.readResourceLocation\(\)', '.readIdentifier()'), (r'\.writeResourceLocation\(', '.writeIdentifier('),
   (r'\.registryOrThrow\(', '.lookupOrThrow('),
   (r'\bEntityType\.(?!Builder\b|BlockEntitySupplier\b|EntityFactory\b)([A-Z][A-Z0-9_]+)\b', r'net.minecraft.world.entity.EntityTypes.\1'),
   (r'\bMobSpawnType\b', 'EntitySpawnReason'), (r'import net\.minecraft\.world\.entity\.EntitySpawnReason;', 'import net.minecraft.world.entity.EntitySpawnReason;'),
   (r'\bimport net\.minecraft\.world\.entity\.MobSpawnType;', 'import net.minecraft.world.entity.EntitySpawnReason;'),
   (r'(dimension\(\)|getKey\(\)|key\(\)|unwrapKey\(\)\.get\(\)|\.orElseThrow\(\))\.location\(\)', r'\1.identifier()'),
   (r'BlockEntityType\.Builder\.of\(((?:[^()]|\([^()]*\))*)\)\.build\(null\)', r'new BlockEntityType<>(\1)'),
   (r'\bnew ChunkPos\(((?:[^(),]|\((?:[^()]|\([^()]*\))*\))+)\)', r'ChunkPos.containing(\1)'),
   (r'\.getNormal\(\)', '.getUnitVec3i()'),
   (r'\bItemStack\.parseOptional\(', 'com.hollingsworth.arsnouveau.common.block.tile.LegacyNbt.parseStack('),
   (r'\bItemStack\.parse\(', 'com.hollingsworth.arsnouveau.common.block.tile.LegacyNbt.parseStack('),
   (r'([\w.]+(?:\(\))?)\.saveOptional\((\w+)\)', r'com.hollingsworth.arsnouveau.common.block.tile.LegacyNbt.saveStack(\2, \1)'),
   (r'\bNbtUtils\.writeBlockPos\(', 'com.hollingsworth.arsnouveau.common.block.tile.LegacyNbt.writeBlockPos('),
   (r'\.getList\(("[^"]*"|[\w.]+), *(?:Tag\.TAG_\w+|\d+|[\w.]*TAG_TYPE)\)', r'.getListOrEmpty(\1)'),
   (r'(\b[\w.]+(?:\(\))?)\.addPotionTooltip\(([^,()]+(?:::\w+)?), ', r'net.minecraft.world.item.alchemy.PotionContents.addPotionTooltip(\1.getAllEffects(), \2, '),
   (r'\bimport net\.neoforged\.neoforge\.common\.util\.INBTSerializable;\n', ''),
   (r'\.getAllRecipesFor\(', '.recipeMap().byType('),
   (r'\bItemInteractionResult\.PASS_TO_DEFAULT_BLOCK_INTERACTION\b', 'InteractionResult.TRY_WITH_EMPTY_HAND'),
   (r'\bItemInteractionResult\.SKIP_DEFAULT_BLOCK_INTERACTION\b', 'InteractionResult.CONSUME'),
   (r'\bItemInteractionResult\.sidedSuccess\([^)]*\)', 'InteractionResult.SUCCESS'),
   (r'\bInteractionResult\.sidedSuccess\([^)]*\)', 'InteractionResult.SUCCESS'),
   (r'^import net\.minecraft\.world\.ItemInteractionResult;\n', ''),
   (r'\bItemInteractionResult\b', 'InteractionResult'),
   (r'\bimport net\.minecraft\.world\.level\.block\.state\.properties\.DirectionProperty;', 'import net.minecraft.world.level.block.state.properties.EnumProperty;'),
   (r'\bDirectionProperty\b', 'EnumProperty<Direction>'),
   (r'\.isClientSide\b(?!\s*\()', '.isClientSide()'),
   (r'\b((?:p?[lL]evel|world|pWorld|serverLevel|getLevel\(\)|level\(\)|getCommandSenderWorld\(\)|player\.level\(\)|entity\.level\(\)))\.random\b(?!\s*\()', r'\1.getRandom()'),
]
st=collections.Counter()
def _walk(root):
    if os.path.isfile(root): yield os.path.dirname(root),None,[os.path.basename(root)]
    else: yield from os.walk(root)
for root in sys.argv[1:]:
    for dp,_,fs in _walk(root):
        for f in fs:
            if not f.endswith('.java'): continue
            p=os.path.join(dp,f); s=open(p,encoding='utf-8').read(); s2=s
            for a,b in R:
                s2,n=re.subn(a,b,s2,flags=re.M); st[a[:25]]+=n
            if s2!=s: open(p,'w',encoding='utf-8').write(s2)
print(dict(st))
