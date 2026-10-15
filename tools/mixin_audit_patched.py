import json,re,subprocess,sys,os,functools
wt,cfg=sys.argv[1],sys.argv[2]
JAR=sys.argv[3]; CP=sys.argv[4] if len(sys.argv)>4 else JAR
d=json.load(open(f'{wt}/src/main/resources/{cfg}'))
pkg=d['package']; src=f'{wt}/src/main/java/'+pkg.replace('.','/')
@functools.lru_cache(None)
def jp(cls):
    r=subprocess.run(['/home/box/jdk25/bin/javap','-cp',CP,'-p','-c','-s',cls],capture_output=True,text=True)
    return r.stdout if r.returncode==0 else None
def members(txt):
    ms=[];fs=[]
    lines=txt.splitlines()
    for i,l in enumerate(lines):
        if l.startswith('  ') and not l.startswith('   ') and i+1<len(lines) and 'descriptor:' in lines[i+1]:
            desc=lines[i+1].split('descriptor:')[1].strip()
            m=re.search(r'([\w$<>]+)\(',l)
            if m and '(' in l: ms.append((m.group(1) if m.group(1)!=txt_cls(txt) else '<init>',desc))
            else:
                n=re.search(r'([\w$]+);$',l); 
                if n: fs.append((n.group(1),desc))
    return ms,fs
def txt_cls(txt):
    m=re.search(r'(?:class|interface) ([\w.$]+)',txt); return m.group(1) if m else ''
rows=[]
for side in ('mixins','client','server'):
  for m in d.get(side,[]):
    outer,_,inner=m.partition('$')
    f=f'{src}/{outer.replace(".","/")}.java'; s=open(f).read()
    if inner:
        i=s.find('class '+inner); hdr=s.rfind('@Mixin',0,i); s=s[:s.find('\n')]+'\n'+'\n'.join(re.findall(r'^import [^\n]*',s,re.M))+'\n'+s[hdr:]
    imps=dict((x.split('.')[-1],x) for x in re.findall(r'^import ([\w.]+);',s,re.M))
    tm=re.search(r'@Mixin\(\s*(?:value\s*=\s*)?\{?\s*([\w.]+)\.class',s) ; tgt=None
    if tm:
        n=tm.group(1); tgt=imps.get(n.split('.')[0],n) if '.' not in n or n.split('.')[0] in imps else n
        if '.' in n and n.split('.')[0] in imps: tgt=imps[n.split('.')[0]]+'$'+'$'.join(n.split('.')[1:])
    else:
        t2=re.search(r'targets\s*=\s*\{?\s*"([^"]+)"',s); tgt=t2.group(1).replace('/','.') if t2 else None
    probs=[]
    if tgt and '.' not in tgt:
        for wp in re.findall(r'^import ([\w.]+)\.\*;',s,re.M):
            if jp(wp+'.'+tgt): tgt=wp+'.'+tgt; break
    if inner and s.find('@Mixin',s.find('@Mixin')+1)>0: s=s[:s.find('@Mixin',s.find('@Mixin')+1)]
    txt=jp(tgt) if tgt else None
    if not txt: rows.append((side,m,tgt,'FAIL','target class not found')); continue
    full=txt_cls(txt); simple=full.split('.')[-1]
    ms=[];fs=[]
    lines=txt.splitlines()
    for i,l in enumerate(lines):
        if l.startswith('  ') and not l.startswith('   ') and i+1<len(lines) and 'descriptor:' in lines[i+1]:
            desc=lines[i+1].split('descriptor:')[1].strip()
            if '(' in l:
                nm=re.search(r'([\w$.]+)\(',l).group(1).split('.')[-1]
                if nm==simple or l.strip().startswith(full+'('): nm='<init>'
                if 'static {}' in l: nm='<clinit>'
                ms.append((nm,desc))
            else:
                fm=re.search(r"([\w$]+);$",l)
                if fm: fs.append((fm.group(1),desc))
    if 'static {}' in txt: ms.append(('<clinit>','()V'))
    mnames={a for a,b in ms}; fnames={a for a,b in fs}
    # shadows
    for sm in re.finditer(r'@Shadow[^;{]*?\s([\w$]+)\s*(\(|;|=)',s):
        n=sm.group(1)
        if sm.group(2)=='(' : 
            if n not in mnames: probs.append(f'@Shadow method {n}')
        elif n not in fnames: probs.append(f'@Shadow field {n}')
    for am in re.finditer(r'@(Accessor|Invoker)(?:\(\s*(?:value\s*=\s*)?"([^"]+)"[^)]*\))?\s*(?:@\w+\s*)*[\w<>\[\]., ?$]+\s+(\w+)\s*\(',s):
        kind,expl,meth=am.groups()
        if expl: n=expl
        else:
            n=re.sub(r'^(get|set|is|call|invoke)','',meth); n=n[:1].lower()+n[1:]
        if kind=='Accessor' and n not in fnames: probs.append(f'@Accessor {n}')
        if kind=='Invoker' and n not in mnames: probs.append(f'@Invoker {n}')
    for mm in re.finditer(r'method\s*=\s*(\{[^}]*\}|"[^"]*")',s):
        for v in re.findall(r'"([^"]+)"',mm.group(1)):
            nm=v.split('(')[0]; desc=v[len(nm):]
            if nm not in mnames: probs.append(f'method {v}'); continue
            if desc and not any(a==nm and b==desc for a,b in ms): probs.append(f'method desc {v}')
    for tm2 in re.finditer(r'target\s*=\s*"(L[^"]+)"',s):
        t=tm2.group(1)
        mm=re.match(r'L([\w/$]+);([\w$<>]+)(?::)?(.*)',t)
        if not mm: continue
        owner=mm.group(1).replace('/','.'); n=mm.group(2); desc=mm.group(3)
        ot=jp(owner)
        if not ot: probs.append(f'target owner {owner}'); continue
        if ('('+desc if not desc.startswith('(') else desc).startswith('(') and '(' in t:
            ok = re.search(r'descriptor: '+re.escape(desc), ot) and (n in ot)
            # check pair precisely
            ow=owner.replace('.','/'); nn=('"<init>"' if n=='<init>' else n)
            ok = (f'Method {ow}.{nn}:{desc}' in txt) or (f'Method {nn}:{desc}' in txt and ow==tgt.replace('.','/')) or (f'Method {nn}:{desc}' in txt)
            lines2=[] if ok else ot.splitlines()
            for i,l in enumerate(lines2[:-1]):
                if 'descriptor: '+desc==lines2[i+1].strip() and (re.search(r'[ .]'+re.escape(n)+r'\(',l) or (n=='<init>' and owner.split('.')[-1].split('$')[-1]+'(' in l.split('.')[-1])): ok=True;break
            if not ok: probs.append(f'INVOKE target {t}')
        else:
            if not re.search(r'[ ]'+re.escape(n)+r';\n\s+descriptor: '+re.escape(desc),ot): probs.append(f'FIELD target {t}')
    rows.append((side,m,tgt,'FAIL' if probs else 'PASS','; '.join(probs)))
for r in rows: print('\t'.join(x or '' for x in r))
