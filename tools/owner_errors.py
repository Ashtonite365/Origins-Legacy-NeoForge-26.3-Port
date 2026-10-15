#!/usr/bin/env python3
"""Count / filter javac errors per owner using OWNERS.tsv (globs ending in /** allowed; exact paths win, else the longest glob).
usage: owner_errors.py <compile.log> [OWNER]   -> counts per owner, or the error lines for OWNER."""
import re,sys,os,collections
root=os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
rules=[l.rstrip('\n').split('\t') for l in open(os.path.join(root,'OWNERS.tsv')) if l.strip() and not l.startswith('#')]
def own(p):
    best=None
    for o,g in rules:
        if g==p: return o
        if g.endswith('/**') and p.startswith(g[:-2]) and (best is None or len(g)>len(best[1])): best=(o,g)
    return best[0] if best else '?'
pat=re.compile(r'^\s*(/\S+?\.java):\d+: error:')
seen=set(); c=collections.Counter(); want=sys.argv[2] if len(sys.argv)>2 else None
for line in open(sys.argv[1],errors='replace'):
    m=pat.match(line)
    if not m: continue
    k=line.strip()
    if k in seen: continue
    seen.add(k)
    p=m.group(1)
    i=p.find('/src/main/'); rel=p[i+1:] if i>=0 else p
    o=own(rel); c[o]+=1
    if want==o: print(line.rstrip())
if not want:
    for o,n in sorted(c.items()): print(o,n)
    print('TOTAL',sum(c.values()))
