#!/usr/bin/env python3
# Line-targeted fixes driven by javac log. usage: linefix.py log filelist
import re,sys,collections
log=open(sys.argv[1]).read().split('\n'); allow=set(open(sys.argv[2]).read().split())
D={'Integer':('getIntOr','0'),'Boolean':('getBooleanOr','false'),'String':('getStringOr','""'),'Double':('getDoubleOr','0.0'),'Float':('getFloatOr','0f'),'Long':('getLongOr','0L'),'Short':('getShortOr','(short)0'),'Byte':('getByteOr','(byte)0')}
G={'Integer':'getInt','Boolean':'getBoolean','String':'getString','Double':'getDouble','Float':'getFloat','Long':'getLong','Short':'getShort','Byte':'getByte'}
todo=collections.defaultdict(set)
for i0,l in enumerate(log):
    m=re.match(r'^(/\S+\.java):(\d+): error: (.*)',l)
    if m and m.group(1) in allow:
        msg=m.group(3)
        if 'cannot find symbol' in msg:
            for j in range(i0+1,min(i0+5,len(log))):
                if 'symbol:' in log[j]: msg+=' '+log[j].strip(); break
        todo[m.group(1)].add((int(m.group(2)),msg))
st=collections.Counter()
for p,items in todo.items():
    L=open(p).read().split('\n'); ch=False
    for ln,msg in items:
        s=L[ln-1]; o=s
        m=re.match(r'incompatible types: Optional<(\w+)> cannot be converted to',msg)
        if m and m.group(1) in D:
            nm,dv=D[m.group(1)]
            s=re.sub(r'\.'+G[m.group(1)]+r'\(((?:[^()]|\([^()]*\))+)\)',lambda x:'.'+nm+'('+x.group(1)+', '+dv+')',s)
        if ('Optional<' in msg or 'bad operand type' in msg or 'bad type in conditional' in msg or 'method getString(String)' in msg or 'method getInt(String)' in msg) and 'Optional<Holder' not in msg:
            for k,(nm,dv) in D.items():
                s=re.sub(r'\.'+G[k]+r'\(((?:[^(),]|\([^()]*\))+)\)',lambda x:'.'+nm+'('+x.group(1)+', '+dv+')',s)
            s=re.sub(r'\.getCompound\(','.getCompoundOrEmpty(',s)
            s=re.sub(r'\.getIntArray\(((?:[^()]|\([^()]*\))+)\)',r'.getIntArray(\1).orElse(new int[0])',s)
        if 'Optional<CompoundTag> cannot be converted to CompoundTag' in msg:
            s=re.sub(r'\.getCompound\(',' .getCompoundOrEmpty('.strip(),s)
        if 'has private access in ChunkPos' in msg:
            s=re.sub(r'\.([xz])\b(?!\s*\()',r'.\1()',s)
        if 'selected has private access in Inventory' in msg:
            s=re.sub(r'\.selected\b(?!\s*\()','.getSelectedSlot()',s)
        SYM={'method getCenter()':(r'((?:\w+\.)*\w+(?:\(\))?)\.getCenter\(\)',r'net.minecraft.world.phys.Vec3.atCenterOf(\1)'),
             'method getBoundingBoxForCulling()':(r'\.getBoundingBoxForCulling\(\)','.getBoundingBox().inflate(0.5)'),
             'method getMainCamera()':(r'\.getMainCamera\(\)','.mainCamera()'),
             'method getArmorSlots()':(r'(\w+)\.getArmorSlots\(\)',r'net.minecraft.world.entity.EquipmentSlotGroup.ARMOR.slots().stream().map(\1::getItemBySlot).toList()'),
             'method getSelected()':(r'\.getSelected\(\)','.getSelectedItem()'),
             'method getDayTime()':(r'\.getDayTime\(\)','.getOverworldClockTime()'),
             'variable cameraEntity':(r'\.cameraEntity\b','.getCameraEntity()'),
             'variable screen':(r'\.screen\b(?!\s*\()','.gui.screen()'),
        }
        for k,(a,b) in SYM.items():
            if k in msg: s=re.sub(a,b,s)
        if 'has private access in Inventory' in msg and 'items' in msg:
            s=re.sub(r'\.items\b(?!\s*\()','.getNonEquipmentItems()',s)
        if s!=o: L[ln-1]=s; ch=True; st[msg[:50]]+=1
    if ch: open(p,'w').write('\n'.join(L))
print(dict(st))
