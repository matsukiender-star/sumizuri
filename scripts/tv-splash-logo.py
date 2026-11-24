# Draws app/android/src/main/res/drawable/tv_splash_logo_{light,dark}.xml: the splash medallion with "Sumizuri" in Inter Bold beside it, for the wide splash of the TV app. Run from app/ with fontTools installed: python3 ../scripts/tv-splash-logo.py
import re, sys
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
font = TTFont('shared/src/commonMain/composeResources/font/inter_bold.ttf')
gs = font.getGlyphSet(); cmap = font.getBestCmap(); upm = font['head'].unitsPerEm
cap = font['OS/2'].sCapHeight
size = 112.0                      # font size in viewport units
k = size / upm
spacing = -0.023 * size           # Akari's banner: letter-spacing -2 at 86
H = 180.0                         # viewport height; the medallion sits in a 180 square
x = 180 + 34                      # the name starts after the medallion and a gap
base = H / 2 + cap * k / 2        # caps centered on the medallion
paths = []
for ch in "Sumizuri":
    g = cmap[ord(ch)]
    pen = SVGPathPen(gs)
    gs[g].draw(TransformPen(pen, (k, 0, 0, -k, x, base)))
    paths.append(pen.getCommands())
    x += gs[g].width * k + spacing
width = x - spacing + 6
text = ' '.join(paths)
def mark(which):
    s = open(f'android/src/main/res/drawable/splash_mark_{which}.xml').read()
    inner = s[s.index('>', s.index('<vector')) + 1 : s.rindex('</vector>')]
    return inner
for which, ink in (('light', '#1A1D24'), ('dark', '#E7E7E8')):
    out = f'''<?xml version="1.0" encoding="utf-8"?>
<!-- The splash of the TV app: the Sumizuri medallion with the name beside it, wide like the screen. The name is Inter Bold, as in the banner of the project, drawn as paths so it is sharp on any TV. Made by scripts/tv-splash-logo.py from splash_mark_{which}.xml and inter_bold.ttf. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="{width * 0.6:.0f}dp"
    android:height="{H * 0.6:.0f}dp"
    android:viewportWidth="{width:.1f}"
    android:viewportHeight="{H:.0f}">
    <group android:translateX="-54" android:translateY="-54">{mark(which)}    </group>
    <path android:fillColor="{ink}" android:pathData="{text}" />
</vector>
'''
    open(f'android/src/main/res/drawable/tv_splash_logo_{which}.xml', 'w').write(out)
print('ancho', round(width), 'dp', round(width * 0.6), 'x', round(H * 0.6))
