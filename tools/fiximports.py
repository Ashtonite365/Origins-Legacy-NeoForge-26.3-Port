#!/usr/bin/env python3
# Rewrites imports of moved MC/NeoForge classes to their 26.3 location when the simple name is unique.
# usage: fiximports.py <src-root>...   (only touches files given / under roots)
import os,re,sys,zipfile,collections,json
REF=['/workspace/mod-port/ref/src','/workspace/mod-port/ref/nf']
idx=collections.defaultdict(set); known=set(); pkgs=set()
for r in REF:
    for dp,_,fs in os.walk(r):
        for f in fs:
            if f.endswith('.java'):
                pkg=os.path.relpath(dp,r).replace('/','.'); n=f[:-5]
                idx[n].add(pkg+'.'+n); known.add(pkg+'.'+n); pkgs.add(pkg)
RENAME={'ResourceLocation':'Identifier','ResourceLocationArgument':'IdentifierArgument','ResourceLocationException':'IdentifierException'}
ext=json.load(open('/workspace/mod-port/fiximports_extra.json')) if os.path.exists('/workspace/mod-port/fiximports_extra.json') else {}
imp=re.compile(r'^import (static )?((?:net\.minecraft|net\.neoforged\.neoforge|com\.mojang\.blaze3d|com\.mojang\.math)\.[\w.]+);',re.M)
stats=collections.Counter()
def fix(m):
    st=m.group(1) or ''; name=m.group(2); parts=name.split('.')
    # find first segment starting with uppercase = top-level class
    k=next((i for i,p in enumerate(parts) if p[:1].isupper()),None)
    if k is None: return m.group(0)
    top='.'.join(parts[:k+1]); rest='.'.join(parts[k+1:]); rest=('.'+rest) if rest else ''
    if top in known: return m.group(0)
    cls=parts[k]; n=RENAME.get(cls,cls)
    if top in ext: stats['ext']+=1; return f'import {st}{ext[top]}{rest};'
    c=idx.get(n,set())
    if len(c)==1: stats['fixed']+=1; return f'import {st}{next(iter(c))}{rest};'
    if len(c)>1:
        # prefer candidate sharing the longest package prefix
        pk='.'.join(parts[:k]); best=max(c,key=lambda x: len(os.path.commonprefix([x,pk])))
        if best.split('.')[:3]!=parts[:3]: stats['ambiguous']+=1; print('  ~?',top,sorted(c),file=sys.stderr); return m.group(0)
        stats['ambiguous-picked']+=1; print('  ~',top,'->',best,file=sys.stderr); return f'import {st}{best}{rest};'
    stats['unresolved']+=1; print('  ?',top,file=sys.stderr); return m.group(0)
def _walk(root):
    if os.path.isfile(root): yield os.path.dirname(root),None,[os.path.basename(root)]
    else: yield from os.walk(root)
for root in sys.argv[1:]:
    for dp,_,fs in _walk(root):
        for f in fs:
            if not f.endswith('.java'): continue
            p=os.path.join(dp,f); s=open(p,encoding='utf-8').read(); s2=imp.sub(fix,s)
            for a,b in RENAME.items(): s2=re.sub(r'\b'+a+r'\b',b,s2)
            if s2!=s: open(p,'w',encoding='utf-8').write(s2); stats['files']+=1
print(dict(stats))
