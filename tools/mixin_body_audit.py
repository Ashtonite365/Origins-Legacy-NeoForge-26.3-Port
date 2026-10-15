#!/usr/bin/env python3
"""Body-level mixin audit: for every @At(value="INVOKE"/"FIELD"/"NEW", target=...) inside an injector with method=...,
check the target call actually appears in the bytecode of that method in the NeoForge-PATCHED jar (javap -c).
usage: mixin_body_audit.py <worktree> <classpath>"""
import re,sys,glob,subprocess,functools
wt,CP=sys.argv[1],sys.argv[2]
@functools.lru_cache(None)
def jp(cls):
    r=subprocess.run(['/home/box/jdk25/bin/javap','-c','-p','-s','-cp',CP,cls],capture_output=True,text=True)
    return r.stdout if r.returncode==0 else None
def bodies(txt,simple):
    out={};cur=None
    lines=txt.splitlines()
    for i,l in enumerate(lines):
        if l.startswith('  ') and not l.startswith('   ') and '(' in l and l.rstrip().endswith(';'):
            m=re.search(r'([\w$.]+)\(',l); n=m.group(1).split('.')[-1]
            if n==simple: n='<init>'
            if 'static {}' in l: n='<clinit>'
            desc=lines[i+1].split('descriptor:')[1].strip() if i+1<len(lines) and 'descriptor:' in lines[i+1] else ''
            cur=(n,desc); out[cur]=[]
        elif l.strip()=='static {};':
            cur=('<clinit>','()V'); out[cur]=[]
        elif cur and re.match(r'\s+\d+: ',l): out[cur].append(l)
    return out
def norm(owner_name_desc):
    return owner_name_desc
probs=[];n=0
for f in sorted(glob.glob(wt+'/src/main/java/io/github/apace100/*/mixin/**/*.java',recursive=True)):
    s=open(f).read(); s=re.sub(r'/\*.*?\*/','',s,flags=re.S); s=re.sub(r'//[^\n]*','',s)
    imps=dict((x.split('.')[-1],x) for x in re.findall(r'^import ([\w.]+);',s,re.M)); wild=re.findall(r'^import ([\w.]+)\.\*;',s,re.M)
    # split into mixin class chunks
    for chunk in re.split(r'(?=@Mixin\()',s)[1:]:
        tm=re.match(r'@Mixin\(\s*(?:value\s*=\s*)?\{?\s*([\w.]+)\.class',chunk)
        tgts=[]
        if tm:
            nm=tm.group(1); head=nm.split('.')[0]
            if head in imps: tgts=[imps[head]+''.join('$'+p for p in nm.split('.')[1:])]
            elif '.' in nm and nm[0].islower(): tgts=[nm]
            else: tgts=[w+'.'+nm for w in wild if jp(w+'.'+nm)]
        else:
            t2=re.search(r'targets\s*=\s*\{?\s*"([^"]+)"',chunk); tgts=[t2.group(1).replace('/','.')] if t2 else []
        if not tgts: continue
        txt=jp(tgts[0])
        if not txt: continue
        simple=tgts[0].split('.')[-1].split('$')[-1]
        B=bodies(txt,tgts[0].split('.')[-1])
        for inj in re.finditer(r'@(Inject|Redirect|ModifyArg|ModifyArgs|ModifyVariable|ModifyConstant|ModifyExpressionValue|ModifyReturnValue|WrapOperation|WrapWithCondition)\((.*?)\)\s*(?:private|public|protected|static|@)',chunk,re.S):
            ann=inj.group(2)
            mm=re.search(r'method\s*=\s*(\{[^}]*\}|"[^"]*")',ann)
            if not mm: continue
            methods=re.findall(r'"([^"]+)"',mm.group(1))
            for at in re.finditer(r'@At\(\s*(?:value\s*=\s*)?"(INVOKE|FIELD|NEW|INVOKE_ASSIGN)"[^)]*?target\s*=\s*"([^"]+)"',ann):
                kind,t=at.groups(); n+=1
                m=re.match(r'L([\w/$]+);([\w$<>]+)(?::)?(.*)',t)
                if not m: continue
                owner,name,desc=m.groups(); owner=owner.replace('/','.')
                hit=False
                for meth in methods:
                    mn=meth.split('(')[0]; md=meth[len(mn):]
                    for (bn,bd),lines in B.items():
                        if bn!=mn or (md and bd!=md): continue
                        for l in lines:
                            c=re.search(r'// (?:Method|Field|InterfaceMethod|class) ([\w/$.<>"]+):([^\s]+)',l)
                            if not c: continue
                            ref=c.group(1); rd=c.group(2)
                            ro,_,rn=ref.rpartition('.') if '.' in ref else ('',None,ref)
                            ro=ro.replace('/','.') or tgts[0]
                            if rn.strip('"')==name and (not desc or rd==desc) and ro==owner: hit=True
                if not hit: probs.append(f"{f.split('apace100/')[1]}: {kind} {t} not found in {tgts[0].split('.')[-1]}.{'|'.join(methods)}")
print(f'checked {n} INVOKE/FIELD/NEW @At targets; {len(probs)} not found in method bodies')
print('\n'.join(probs))
