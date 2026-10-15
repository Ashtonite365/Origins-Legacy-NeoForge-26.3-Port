#!/usr/bin/env python3
"""1.21.1 -> 26.3 GUI codemod (GuiGraphics -> GuiGraphicsExtractor). Mechanical; review results.
usage: gui_codemod.py <file-or-dir>...    Prints a per-rule count."""
import os,re,sys,collections
st=collections.Counter()

def split_args(s,i):
    """s[i]=='(' ; return (args:list[str], end_index_of_closing_paren)"""
    depth=0; args=[]; cur=''; j=i; instr=None
    while j<len(s):
        c=s[j]
        if instr:
            cur+=c
            if c=='\\': cur+=s[j+1]; j+=2; continue
            if c==instr: instr=None
        elif c in '"\'': instr=c; cur+=c
        elif c in '([{': depth+=1; cur+=c if depth>1 else ''
        elif c in ')]}':
            depth-=1
            if depth==0: args.append(cur); return [a.strip() for a in args if a.strip()!='' or len(args)>1], j
            cur+=c
        elif c==',' and depth==1: args.append(cur); cur=''
        else: cur+=c
        j+=1
    return None,None

def rewrite_calls(s,name,fn,recv=r'[\w\)\]]'):
    out=[]; pos=0
    for m in re.finditer(r'(?<=%s)\.%s\(' % (recv,name), s):
        if m.start()<pos: continue
        args,end=split_args(s,m.end()-1)
        if args is None: continue
        r=fn(args)
        if r is None: continue
        out.append(s[pos:m.start()]); out.append(r); pos=end+1; st[name]+=1
    out.append(s[pos:]); return ''.join(out)

P='RenderPipelines.GUI_TEXTURED'
def numeric(a): return re.fullmatch(r'[-\d.fF+*/ ()]+|\w*[xXyY]\w*|\w*(width|height|Width|Height|offset|Offset|left|top)\w*',a) is not None
def blit(a):
    if a and a[0].startswith('RenderPipelines'): return None
    if not a or numeric(a[0]) or 'prite' in a[0]: return None
    n=len(a)
    if n==7: return f'.blit({P}, {a[0]}, {a[1]}, {a[2]}, {a[3]}, {a[4]}, {a[5]}, {a[6]}, 256, 256)'
    if n==9: return f'.blit({P}, '+', '.join(a)+')'
    if n==10: return f'.blit({P}, {a[0]}, {a[1]}, {a[2]}, {a[4]}, {a[5]}, {a[6]}, {a[7]}, {a[8]}, {a[9]})'  # drop blitOffset
    if n==11: # (tex,x,y,w,h,u,v,uW,vH,texW,texH) -> (pipe,tex,x,y,u,v,w,h,uW,vH,texW,texH)
        return f'.blit({P}, {a[0]}, {a[1]}, {a[2]}, {a[5]}, {a[6]}, {a[3]}, {a[4]}, {a[7]}, {a[8]}, {a[9]}, {a[10]})'
    return None
def blitSprite(a):
    if a and a[0].startswith('RenderPipelines'): return None
    return f'.blitSprite({P}, '+', '.join(a)+')'
def drop3(name):
    def f(a):
        if len(a)==3: return f'.{name}({a[0]}, {a[1]})'
        return None
    return f
def opaque(c):
    m=re.fullmatch(r'0x([0-9a-fA-F]{6})',c)
    return f'0xFF{m.group(1).upper()}' if m else c
def text(newname,ci):
    def f(a):
        if len(a)>ci: a=a[:ci]+[opaque(a[ci])]+a[ci+1:]
        return f'.{newname}('+', '.join(a)+')'
    return f

SIMPLE=[(r'\bGuiGraphics\b','GuiGraphicsExtractor'),
 (r'\bimport net\.minecraft\.client\.gui\.GuiGraphicsExtractor;','import net.minecraft.client.gui.GuiGraphicsExtractor;\nimport net.minecraft.client.renderer.RenderPipelines;'),
 (r'\bvoid render\(\s*(final\s+)?GuiGraphicsExtractor','void extractRenderState(GuiGraphicsExtractor'),
 (r'\bvoid renderWidget\(\s*(final\s+)?GuiGraphicsExtractor','void extractWidgetRenderState(GuiGraphicsExtractor'),
 (r'\bvoid renderBackground\(\s*(final\s+)?GuiGraphicsExtractor','void extractBackground(GuiGraphicsExtractor'),
 (r'\bsuper\.render\(','super.extractRenderState('),(r'\bsuper\.renderWidget\(','super.extractWidgetRenderState('),
 (r'\bsuper\.renderBackground\(','super.extractBackground('),
 (r'\.renderItem\(','.item('),(r'\.renderFakeItem\(','.fakeItem('),(r'\.renderItemDecorations\(','.itemDecorations('),
 (r'\.renderTooltip\(','.setTooltipForNextFrame('),(r'\.renderComponentTooltip\(','.setComponentTooltipForNextFrame('),
 (r'\.drawWordWrap\(','.textWithWordWrap('),
]
def run(p):
    s=open(p,encoding='utf-8').read()
    if 'GuiGraphics' not in s: return
    o=s
    for a,b in SIMPLE:
        s,n=re.subn(a,b,s); st[a[:30]]+=n
    # pose push/pop only on GUI pose (Matrix3x2fStack), never on 3D PoseStack code
    if 'PoseStack' not in s:
        s=re.sub(r'(\.pose\(\))\.pushPose\(\)',r'\1.pushMatrix()',s); s=re.sub(r'(\.pose\(\))\.popPose\(\)',r'\1.popMatrix()',s)
    s=s.replace('import net.minecraft.client.renderer.RenderPipelines;\nimport net.minecraft.client.renderer.RenderPipelines;','import net.minecraft.client.renderer.RenderPipelines;')
    s=rewrite_calls(s,'blit',blit)
    s=rewrite_calls(s,'blitSprite',blitSprite)
    s=rewrite_calls(s,'drawString',text('text',4))
    s=rewrite_calls(s,'drawCenteredString',text('centeredText',4))
    for nm in ('translate','scale'):
        s=re.sub(r'(pose\(\)|poseStack|matrixStack|stack)\.'+nm+r'\(', lambda m: m.group(0), s)
    s=rewrite_calls(s,'translate',drop3('translate'),recv=r'pose\(\)')
    s=rewrite_calls(s,'scale',drop3('scale'),recv=r'pose\(\)')
    if s!=o: open(p,'w',encoding='utf-8').write(s); st['files']+=1
for root in sys.argv[1:]:
    if os.path.isfile(root): run(root); continue
    for dp,_,fs in os.walk(root):
        for f in fs:
            if f.endswith('.java'): run(os.path.join(dp,f))
print(dict(st))
