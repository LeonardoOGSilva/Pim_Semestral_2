"""
Reduz as fotos da pasta Imagens (largura máxima 800 px) mantendo os MESMOS nomes de arquivo,
para o site e o app carregarem rápido (de ~5 MB para ~100-300 KB por foto).
Os originais ficam guardados em Imagens_originais.

Como usar (na pasta BoxTechFront, onde está a pasta Imagens):
    pip install pillow
    python comprimir_imagens.py
"""
import shutil
from pathlib import Path
from PIL import Image

PASTA = Path("BoxTechFront\Imagens")
BACKUP = Path("Imagens_originais")
LARGURA_MAX = 800
EXTENSOES = {".png", ".jpg", ".jpeg"}

if not PASTA.is_dir():
    raise SystemExit("Pasta 'Imagens' não encontrada. Rode este script dentro da pasta BoxTechFront.")

BACKUP.mkdir(exist_ok=True)
total_antes = total_depois = 0

for arq in sorted(PASTA.rglob("*")):
    if arq.suffix.lower() not in EXTENSOES or not arq.is_file():
        continue
    antes = arq.stat().st_size
    destino_backup = BACKUP / arq.relative_to(PASTA)
    destino_backup.parent.mkdir(parents=True, exist_ok=True)
    if not destino_backup.exists():
        shutil.copy2(arq, destino_backup)          # guarda o original uma única vez

    img = Image.open(destino_backup)               # sempre parte do original
    if img.width > LARGURA_MAX:
        altura = round(img.height * LARGURA_MAX / img.width)
        img = img.resize((LARGURA_MAX, altura), Image.LANCZOS)

    if arq.suffix.lower() == ".png":
        img.save(arq, format="PNG", optimize=True)
    else:
        img.convert("RGB").save(arq, format="JPEG", quality=82, optimize=True)

    depois = arq.stat().st_size
    total_antes += antes
    total_depois += depois
    print(f"{arq.name}: {antes/1024:.0f} KB -> {depois/1024:.0f} KB")

print(f"\nTotal: {total_antes/1048576:.1f} MB -> {total_depois/1048576:.1f} MB")
