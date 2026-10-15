#!/usr/bin/env python3
# 1.21.1 -> 26.3 Block override signature rewrites (declaration + matching super call).
import re,sys,collections
st=collections.Counter()
def split_params(s):
    out=[];d=0;cur=''
    for ch in s:
        if ch in '(<[':d+=1
        if ch in ')>]':d-=1
        if ch==',' and d==0: out.append(cur.strip());cur=''
        else: cur+=ch
    if cur.strip(): out.append(cur.strip())
    return out
def pname(p): return p.split()[-1]
def find_close(s,i):
    d=0
    for j in range(i,len(s)):
        if s[j]=='(':d+=1
        elif s[j]==')':
            d-=1
            if d==0:return j
    return -1
def body_range(s,j):
    k=s.find('{',j)
    if k<0 or ';' in s[j:k]: return None
    d=0
    for m in range(k,len(s)):
        if s[m]=='{':d+=1
        elif s[m]=='}':
            d-=1
            if d==0:return (k,m)
def rewrite(s,name,n,fn):
    pos=0
    while True:
        m=re.search(r'(?:public|protected)\s+[@\w.<>\s,?]*?\b'+name+r'\s*\(',s[pos:])
        if not m: return s
        a=pos+m.end()-1; b=find_close(s,a)
        ps=split_params(s[a+1:b])
        if len(ps)!=n: pos=b; continue
        br=body_range(s,b)
        newps,bodyfix,superfix=fn(ps)
        if newps is None: pos=b; continue
        head=s[:a+1]+', '.join(newps)+s[b:br[0]] if br else None
        if br is None: pos=b; continue
        body=s[br[0]:br[1]+1]
        for x,y in bodyfix: body=re.sub(x,y,body)
        if superfix:
            sm=re.search(r'super\.'+name+r'\(',body)
            if sm:
                c=find_close(body,sm.end()-1); args=split_params(body[sm.end():c])
                if len(args)==n: body=body[:sm.end()]+', '.join(superfix(args))+body[c:]
        s=head+body+s[br[1]+1:]; st[name]+=1
        pos=len(head)+len(body)
def updateShape(ps):
    S,d,ns,w,p,np_=map(pname,ps)
    return (['BlockState '+S,'LevelReader '+w,'ScheduledTickAccess scheduledTickAccess','BlockPos '+p,'Direction '+d,'BlockPos '+np_,'BlockState '+ns,'RandomSource randomSource'],
            [(r'\b'+w+r'\.scheduleTick\(','scheduledTickAccess.scheduleTick(')],
            lambda a:[a[0],a[3],'scheduledTickAccess',a[4],a[1],a[5],a[2],'randomSource'])
def neighborChanged(ps):
    S,L,P,B,F,M=map(pname,ps)
    return (['BlockState '+S,'Level '+L,'BlockPos '+P,'Block '+B,'net.minecraft.world.level.redstone.@org.jspecify.annotations.Nullable Orientation orientation','boolean '+M],[],
            lambda a:[a[0],a[1],a[2],a[3],'orientation',a[5]])
def analog(ps): return (ps+['Direction direction'],[],lambda a:a+['direction'])
def inside(ps): return (ps+['net.minecraft.world.entity.InsideBlockEffectApplier effectApplier','boolean isPrecise'],[],lambda a:a+['effectApplier','isPrecise'])
def occl(ps): return ([ps[0]],[],lambda a:[a[0]])
for p in sys.argv[1:]:
    s=open(p).read(); o=s
    s=rewrite(s,'updateShape',6,updateShape)
    s=rewrite(s,'neighborChanged',6,neighborChanged)
    s=rewrite(s,'getAnalogOutputSignal',3,analog)
    s=rewrite(s,'entityInside',4,inside)
    s=rewrite(s,'getOcclusionShape',3,occl)
    s=rewrite(s,'propagatesSkylightDown',3,occl)
    if s!=o:
        for imp in ['net.minecraft.world.level.LevelReader','net.minecraft.world.level.ScheduledTickAccess','net.minecraft.util.RandomSource','net.minecraft.core.Direction','net.minecraft.core.BlockPos','net.minecraft.world.level.block.state.BlockState']:
            nm=imp.rsplit('.',1)[1]
            if re.search(r'\b'+nm+r'\b',s) and not re.search(r'^import [\w.]*\.'+nm+';',s,re.M):
                s=re.sub(r'^(package [\w.]+;\n)',r'\1import '+imp+';\n',s,count=1,flags=re.M)
        open(p,'w').write(s)
print(dict(st))
