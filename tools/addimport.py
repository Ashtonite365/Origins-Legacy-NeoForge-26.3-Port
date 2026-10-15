#!/usr/bin/env python3
# usage: addimport.py <SimpleName> <fqn> files...   adds the import where the name is used but not imported
import re,sys
n,fq=sys.argv[1],sys.argv[2]; c=0
for p in sys.argv[3:]:
    s=open(p).read()
    if not re.search(r'\b'+n+r'\b',s) or re.search(r'^import [\w.]*\.'+n+';',s,re.M) or re.search(r'^import '+re.escape(fq.rsplit('.',1)[0])+r'\.\*;',s,re.M): continue
    s=re.sub(r'^(package [\w.]+;\n)',r'\1import '+fq+';\n',s,count=1,flags=re.M); open(p,'w').write(s); c+=1
print(n,c)
