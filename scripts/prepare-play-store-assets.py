"""Create an original, flat brand banner for the Play listing; no app screenshots are edited."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'build' / 'play-store'
OUT.mkdir(parents=True, exist_ok=True)
image = Image.new('RGB', (1024, 500), '#063e3d')
draw = ImageDraw.Draw(image)
bold = 'C:/Windows/Fonts/segoeuib.ttf'
regular = 'C:/Windows/Fonts/segoeui.ttf'
draw.rounded_rectangle((650, -100, 1140, 595), 170, fill='#076966')
draw.line([(726, 390), (726, 180), (895, 180), (895, 85)], fill='#36c4b1', width=24, joint='curve')
draw.ellipse((702, 366, 750, 414), fill='#ffffff')
draw.ellipse((868, 56, 922, 110), fill='#efb11b')
draw.text((68, 62), 'intu', font=ImageFont.truetype(bold, 116), fill='white')
draw.text((74, 230), 'Tu mototaxi en Satipo', font=ImageFont.truetype(bold, 42), fill='white')
draw.text((76, 310), 'Elige tu destino. Sigue tu viaje.', font=ImageFont.truetype(regular, 27), fill='#bde4df')
image.save(OUT / 'feature-graphic.png')
print(OUT / 'feature-graphic.png')
