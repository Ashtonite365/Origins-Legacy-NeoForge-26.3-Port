import re,glob,subprocess,os,sys,functools
JAR='/workspace/mod-port/origins-legacy/port/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar'
JP=os.path.expanduser('~/jdk25/bin/javap')
src='src/main/java/'
@functools.lru_cache(None)
def members(cls):  # cls internal name a/b/C$D
    r=subprocess.run([JP,'-p','-s','-cp',JAR,cls.replace('/','.')],capture_output=True,text=True)
    if r.returncode: return None
    out={}; lines=r.stdout.splitlines()
    for i,l in enumerate(lines):
        if l.strip().startswith('descriptor:'):
            d=l.split(':',1)[1].strip(); decl=lines[i-1]
            m=re.search(r'([\w$<>]+)\(',decl)
            if m:
                n=m.group(1); n='<init>' if n.split('.')[-1]==cls.split('/')[-1].replace('$','.').split('.')[-1] or n==cls.replace('/','.') else n
                out.setdefault(n,set()).add(d)
            else:
                m=re.search(r'([\w$]+);$',decl)
                if m: out.setdefault(m.group(1),set()).add(d)
    sup=re.search(r'(?:extends|implements) ([\w.$, ]+)\{',r.stdout)
    return out
def imports(s):
    m={}
    for imp in re.findall(r'^import ([\w.]+);',s,re.M): m[imp.split('.')[-1]]=imp
    return m
def resolve(name,imps,pkg):
    parts=name.split('.')
    if parts[0] in imps:
        base=imps[parts[0]].replace('.','/'); return base+''.join('$'+p for p in parts[1:])
    if '.' in name and name[0].islower(): return name.replace('.','/')
    return None
probs=[]; ok=0; total=0
for f in sorted(glob.glob(src+'io/github/apace100/*/mixin/**/*.java',recursive=True)):
    s=open(f).read(); imps=imports(s)
    tm=re.search(r'@Mixin\(\s*(?:value\s*=\s*)?\{?([^)}]*)',s)
    if not tm: continue
    tnames=[t.strip().replace('.class','') for t in tm.group(1).split(',') if '.class' in t]
    targets=[resolve(t,imps,None) for t in tnames]
    strs=re.findall(r'targets\s*=\s*"([^"]+)"',s); targets+= [x.replace('.','/') for x in strs]
    targets=[t for t in targets if t and t.startswith(('net/minecraft','com/mojang'))]
    if not targets: continue
    rel=f.split('apace100/')[1]
    for t in targets:
        if members(t) is None: probs.append(f'{rel}: target class missing {t}')
    for mm in re.findall(r'method\s*=\s*(\{[^}]*\}|"[^"]*")',s):
        for meth in re.findall(r'"([^"]+)"',mm):
            total+=1
            n=meth.split('(')[0]; d=meth[len(n):] or None
            hit=False
            for t in targets:
                ms=members(t) or {}
                if n in ms and (d is None or d in ms[n]): hit=True
            if hit: ok+=1
            else: probs.append(f'{rel}: method {meth} not in {",".join(t.split("/")[-1] for t in targets)}')
    for at in re.findall(r'target\s*=\s*"(L[^"]+)"',s):
        total+=1
        m=re.match(r'L([\w/$]+);([\w$<>]+)(:?)(.*)',at)
        if not m: probs.append(f'{rel}: unparsed {at}'); continue
        owner,n,colon,d=m.groups()
        ms=members(owner)
        if ms is None:
            if owner.startswith(('net/minecraft','com/mojang')): probs.append(f'{rel}: at-owner missing {owner}')
            else: ok+=1
            continue
        if n in ms and (not d or d in ms[n]): ok+=1
        else:
            # could be inherited; flag softly
            probs.append(f'{rel}: @At {owner.split("/")[-1]}.{n}{colon}{d}' + ('' if n not in ms else ' (name ok, desc differs: '+'|'.join(sorted(ms[n]))[:150]+')'))
print('checked',total,'ok',ok,'flagged',len(probs))
print('\n'.join(probs))
