"""Generate identical raw PNG assets and AAPT2 NinePatch reference resources.

Run from the repository root using Python 3; no third-party packages required.
All ranges refer to the image interior, with exclusive ends.
"""
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FIXTURES = {
    'asymmetric': (8, 6, [(2, 5)], [(1, 4)], (1, 6), (2, 5), False),
    'multiple': (10, 8, [(0, 2), (5, 7), (9, 10)], [(1, 3), (6, 8)], (0, 10), (0, 8), False),
    'full': (4, 5, [(0, 4)], [(0, 5)], (0, 4), (0, 5), False),
    'implicit_padding': (8, 6, [(2, 5)], [(1, 4)], None, None, False),
    'white_border': (8, 6, [(2, 5)], [(1, 4)], (1, 6), (2, 5), True),
}

def chunk(kind, data):
    return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data))

for name, (width, height, horizontal, vertical, bottom, right, white) in FIXTURES.items():
    neutral = bytes([255, 255, 255, 255] if white else [0, 0, 0, 0])
    pixels = [[neutral for _ in range(width + 2)] for _ in range(height + 2)]
    for y in range(height):
        for x in range(width):
            pixels[y + 1][x + 1] = bytes([(x * 37) % 256, (y * 53) % 256,
                                         (x * 17 + y * 29) % 256, 255])
    black = bytes([0, 0, 0, 255])
    for start, end in horizontal:
        for x in range(start, end): pixels[0][x + 1] = black
    for start, end in vertical:
        for y in range(start, end): pixels[y + 1][0] = black
    if bottom:
        for x in range(*bottom): pixels[height + 1][x + 1] = black
    if right:
        for y in range(*right): pixels[y + 1][width + 1] = black
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width + 2, height + 2, 8, 6, 0, 0, 0))
    data += chunk(b'IDAT', zlib.compress(b''.join(b'\0' + b''.join(row) for row in pixels)))
    data += chunk(b'IEND', b'')
    for path in [ROOT/'assets/ninepatch'/f'{name}.9.png',
                 ROOT/'res/drawable-nodpi'/f'f06_{name}.9.png']:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    print(name, len(data))
