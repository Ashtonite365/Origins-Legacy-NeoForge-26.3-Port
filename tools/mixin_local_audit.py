#!/usr/bin/env python3
"""Local-capture / ordinal / lambda-param audit for mixins vs the NeoForge-patched jar (needs LocalVariableTable).
Checks: @At ordinal vs number of matching call sites; @ModifyVariable and @Local (implicit / ordinal / index / name) vs LVT
locals in scope at the injection point; @Inject into lambda$ targets: handler params vs lambda descriptor.
LocalCapture handlers are listed for manual review. Approximations: scope = LVT range covering the instruction
(Shift.AFTER uses the next instruction); implicit/ordinal consider args+locals (only `this` skipped, as Mixin baseArgIndex), argsOnly limits to args.
usage: mixin_local_audit.py <worktree> <classpath> [skip-file-substring ...]"""
import re,sys,glob,subprocess,functools
wt,CP=sys.argv[1],sys.argv[2]; SKIP=sys.argv[3:]
PRIM={'boolean':'Z','int':'I','float':'F','double':'D','long':'J','short':'S','byte':'B','char':'C','void':'V'}
@functools.lru_cache(None)
def jp(cls):
    r=subprocess.run(['/home/box/jdk25/bin/javap','-c','-l','-p','-s','-cp',CP,cls],capture_output=True,text=True)
    return r.stdout if r.returncode==0 else None
def parse(txt,cls):
    simple=cls.split('.')[-1]; M={}; cur=None; mode=None
    lines=txt.splitlines()
    for i,l in enumerate(lines):
        if l.startswith('  ') and not l.startswith('   ') and l.rstrip().endswith(';') and ('(' in l or 'static {}' in l):
            if 'static {}' in l: n='<clinit>'
            else:
                n=re.search(r'([\w$.]+)\(',l).group(1).split('.')[-1]
                if n==simple: n='<init>'
            d=lines[i+1].split('descriptor:')[1].strip() if 'descriptor:' in lines[i+1] else ''
            static=' static ' in ' '+l
            cur={'name':n,'desc':d,'static':static,'code':[],'lvt':[]}; M.setdefault(n,[]).append(cur); mode=None; continue
        if cur is None: continue
        s=l.strip()
        if s=='Code:': mode='code'; continue
        if s.startswith('LineNumberTable'): mode=None; continue
        if s.startswith('LocalVariableTable'): mode='lvt'; continue
        if mode=='code':
            m=re.match(r'(\d+): (\w+)(.*)',s)
            if m: cur['code'].append((int(m.group(1)),m.group(2),m.group(3)))
        elif mode=='lvt':
            m=re.match(r'(\d+)\s+(\d+)\s+(\d+)\s+(\S+)\s+(\S+)',s)
            if m: cur['lvt'].append((int(m.group(1)),int(m.group(2)),int(m.group(3)),m.group(4),m.group(5)))
    return M
def args_of(desc):
    a=desc[1:desc.index(')')]; out=[]; i=0
    while i<len(a):
        j=i
        while a[j]=='[': j+=1
        if a[j]=='L': j=a.index(';',j)
        out.append(a[i:j+1]); i=j+1
    return out
def jtype(t,imps,wild):
    t=re.sub(r'<.*>','',t).strip().replace('final ','').strip(); arr=''
    while t.endswith('[]'): arr+='['; t=t[:-2].strip()
    if t.endswith('...'): arr+='['; t=t[:-3]
    if t in PRIM: return arr+PRIM[t]
    head=t.split('.')[0]
    if head in imps: q=imps[head]+''.join('$'+p for p in t.split('.')[1:])
    elif '.' in t and t[0].islower(): q=t
    elif t in ('Object','String','Integer','Boolean','Float','Double','Long','Iterable','Runnable'): q='java.lang.'+t
    else:
        q=None
        for w in wild:
            if jp(w+'.'+t): q=w+'.'+t; break
        if not q: return None
    return arr+'L'+q.replace('.','/')+';'
def handler_params(sig):
    depth=0; cur=''; out=[]
    for ch in sig:
        if ch in '<(': depth+=1
        if ch in '>)': depth-=1
        if ch==',' and depth==0: out.append(cur); cur=''
        else: cur+=ch
    if cur.strip(): out.append(cur)
    res=[]
    for p in out:
        p=p.strip(); anns={a:(b or '') for a,b in re.findall(r'@(\w+)(\([^)]*\))?',p)}
        p2=re.sub(r'@\w+(\([^)]*\))?','',p).strip()
        parts=p2.rsplit(None,1)
        if len(parts)<2: continue
        res.append({'type':parts[0],'name':parts[1],'anns':anns})
    return res
def points(meth,at,shift,ordinal,owner,name,desc):
    code=meth['code']; offs=[]
    if at=='HEAD': return [code[0][0]] if code else []
    if at in ('RETURN','TAIL'):
        r=[c[0] for c in code if c[1] in ('return','ireturn','freturn','dreturn','lreturn','areturn')]
        if at=='TAIL': return r[-1:]
        if ordinal is not None: return r[ordinal:ordinal+1] if ordinal<len(r) else 'ORD'
        return r
    for k,(o,op,rest) in enumerate(code):
        c=re.search(r'// (?:Method|Field|InterfaceMethod) ([\w/$.<>"]+):(\S+)',rest)
        if not c: continue
        ref=c.group(1); ro,_,rn=ref.rpartition('.')
        if rn.strip('"')!=name or (desc and c.group(2)!=desc): continue
        if ro and ro.replace('/','.')!=owner: continue
        nxt=code[k+1][0] if k+1<len(code) else o
        offs.append(nxt if shift=='AFTER' else o)
    if ordinal is not None: return offs[ordinal:ordinal+1] if ordinal<len(offs) else 'ORD'
    return offs
probs=[];notes=[];checked=0
for f in sorted(glob.glob(wt+'/src/main/java/io/github/apace100/*/mixin/**/*.java',recursive=True)):
    if any(s in f for s in SKIP): continue
    src=open(f).read(); src=re.sub(r'/\*.*?\*/','',src,flags=re.S); src=re.sub(r'//[^\n]*','',src)
    imps=dict((x.split('.')[-1],x) for x in re.findall(r'^import ([\w.]+);',src,re.M)); wild=re.findall(r'^import ([\w.]+)\.\*;',src,re.M)
    rel=f.split('apace100/')[1]
    for chunk in re.split(r'(?=@Mixin\()',src)[1:]:
        tm=re.match(r'@Mixin\(\s*(?:value\s*=\s*)?\{?\s*([\w.]+)\.class',chunk)
        if not tm: continue
        nm=tm.group(1); head=nm.split('.')[0]
        tgt=imps[head]+''.join('$'+p for p in nm.split('.')[1:]) if head in imps else next((w+'.'+nm for w in wild if jp(w+'.'+nm)),None)
        if not tgt or not jp(tgt): continue
        M=parse(jp(tgt),tgt)
        for inj in re.finditer(r'@(Inject|Redirect|ModifyArg|ModifyArgs|ModifyVariable|ModifyConstant|ModifyExpressionValue|ModifyReturnValue|WrapOperation|WrapWithCondition)\((.*?)\)\s*((?:private|public|protected|static|final|\s)+)[\w<>\[\]., ?$]+?\s+(\w+)\s*\((.*?)\)\s*\{',chunk,re.S):
            kind,ann,mods,hname,sig=inj.groups()
            mm=re.search(r'method\s*=\s*(\{[^}]*\}|"[^"]*")',ann)
            if not mm: continue
            atb=''
            k0=ann.find('@At(')
            if k0>=0:
                d=0; q=False
                for k in range(k0+3,len(ann)):
                    ch=ann[k]
                    if ch=='"': q=not q
                    if q: continue
                    if ch=='(': d+=1
                    elif ch==')':
                        d-=1
                        if d==0: atb=ann[k0+4:k]; break
            atv=re.search(r'"(HEAD|RETURN|TAIL|INVOKE|FIELD|NEW|INVOKE_ASSIGN|CONSTANT|LOAD|STORE|JUMP|MIXINEXTRAS:EXPRESSION)"',atb)
            atv=atv.group(1) if atv else 'HEAD'
            tg=re.search(r'target\s*=\s*"L([\w/$]+);([\w$<>]+)(?::)?([^"]*)"',atb)
            ordm=re.search(r'ordinal\s*=\s*(\d+)',atb); ordv=int(ordm.group(1)) if ordm else None
            shift='AFTER' if 'Shift.AFTER' in atb else None
            params=handler_params(sig)
            rest=ann.replace(atb,'')
            for meth_s in re.findall(r'"([^"]+)"',mm.group(1)):
                mn=meth_s.split('(')[0]; md=meth_s[len(mn):]
                cands=[m for m in M.get(mn,[]) if not md or m['desc']==md]
                for meth in cands:
                    checked+=1
                    owner,name,desc=(tg.group(1).replace('/','.'),tg.group(2),tg.group(3)) if tg else (None,None,None)
                    resolvable = bool(tg) or atv in ('HEAD','RETURN','TAIL')
                    pts=points(meth,atv,shift,ordv,owner,name,desc) if resolvable else None
                    where=f"{rel}:{hname} -> {tgt.split('.')[-1]}.{mn}"
                    if pts=='ORD':
                        probs.append(f"{where}: @At ordinal={ordv} but only {len(points(meth,atv,shift,None,owner,name,desc))} matching {name} call(s)"); continue
                    margs=args_of(meth['desc']); base=0 if meth['static'] else 1
                    nslots=base+sum(2 if a in('J','D') else 1 for a in margs)
                    def scope(off): return [e for e in meth['lvt'] if e[0]<=off<e[0]+e[1]]
                    if kind=='Inject' and mn.startswith('lambda$'):
                        pre=[]
                        for p in params:
                            if 'Callback' in p['type']: break
                            pre.append(jtype(p['type'],imps,wild))
                        if pre!=margs: probs.append(f"{where}: lambda handler params {pre} != target args {margs}")
                    reqs=[]
                    if kind=='ModifyVariable' and params:
                        a={}
                        for k in ('ordinal','index'):
                            m2=re.search(k+r'\s*=\s*(\d+)',rest); a[k]=int(m2.group(1)) if m2 else None
                        m2=re.search(r'name\s*=\s*"(\w+)"',rest); a['name']=m2.group(1) if m2 else None
                        a['argsOnly']=bool(re.search(r'argsOnly\s*=\s*true',rest))
                        reqs.append((jtype(params[0]['type'],imps,wild),a,'@ModifyVariable'))
                    for p in params:
                        if 'Local' in p['anns']:
                            body=p['anns']['Local']; a={}
                            for k in ('ordinal','index'):
                                m2=re.search(k+r'\s*=\s*(\d+)',body); a[k]=int(m2.group(1)) if m2 else None
                            m2=re.search(r'name\s*=\s*"(\w+)"',body); a['name']=m2.group(1) if m2 else None
                            a['argsOnly']=bool(re.search(r'argsOnly\s*=\s*true',body))
                            t=None if p['type'].startswith('Local') and 'Ref' in p['type'] else jtype(p['type'],imps,wild)
                            reqs.append((t,a,'@Local '+p['name']))
                    if 'LocalCapture' in ann and isinstance(pts,list) and pts:
                        ci=[i for i,p in enumerate(params) if 'Callback' in p['type']]
                        got=[jtype(p['type'],imps,wild) for p in params[ci[0]+1:]] if ci else []
                        for off in pts:
                            exp=[e[4] for e in sorted(meth['lvt'],key=lambda e:e[2]) if e[2]>=nslots and e[0]<=off<e[0]+e[1]]
                            if exp[:len(got)]!=got: probs.append(f"{where} @{off}: LocalCapture handler {got} != locals {exp}")
                    if not reqs: continue
                    if not meth['lvt']: notes.append(f"{where}: no LVT"); continue
                    if not pts: notes.append(f"{where}: injection point not resolved ({atv}) - locals unchecked"); continue
                    for off in pts:
                        sc=scope(off)
                        for t,a,label in reqs:
                            if t is None: notes.append(f"{where}: {label} type unresolved/LocalRef"); continue
                            if a['index'] is not None:
                                slot=[e for e in sc if e[2]==a['index']]
                                if not slot or slot[0][4]!=t: probs.append(f"{where} @{off}: {label} index={a['index']} {t} not in scope (slot: {slot[0][3]+' '+slot[0][4] if slot else 'none'})")
                                continue
                            if a['name']:
                                if not any(e[3]==a['name'] for e in sc): probs.append(f"{where} @{off}: {label} name={a['name']} not in scope")
                                continue
                            pool=[e for e in sc if (base<=e[2]<nslots)] if a['argsOnly'] else [e for e in sc if e[2]>=base]
                            same=[e for e in pool if e[4]==t]
                            if a['ordinal'] is not None:
                                if len(same)<=a['ordinal']: probs.append(f"{where} @{off}: {label} ordinal={a['ordinal']} of {t} but {len(same)} in scope {[e[3] for e in same]}")
                            elif len(same)!=1:
                                probs.append(f"{where} @{off}: {label} implicit {t} needs exactly 1, found {len(same)} {[e[3] for e in same]}")
print(f"checked {checked} injector/method pairs; {len(set(probs))} problems, {len(set(notes))} notes")
print('\n'.join('PROBLEM '+p for p in sorted(set(probs)))); print('\n'.join('note    '+n for n in sorted(set(notes))))
