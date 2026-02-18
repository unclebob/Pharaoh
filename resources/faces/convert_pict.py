#!/usr/bin/env python3
"""Convert classic Mac PICT v1 files to PNG.
Uses only stdlib (struct, zlib) - no Pillow needed."""

import struct
import zlib
import os

def read_u16(data, offset):
    return struct.unpack('>H', data[offset:offset+2])[0]

def read_s16(data, offset):
    return struct.unpack('>h', data[offset:offset+2])[0]

def read_rect(data, offset):
    top = read_s16(data, offset)
    left = read_s16(data, offset + 2)
    bottom = read_s16(data, offset + 4)
    right = read_s16(data, offset + 6)
    return (top, left, bottom, right)

def decode_packbits_row(data, offset, row_bytes):
    """Decode one PackBits-compressed row. Returns (decoded_bytes, bytes_consumed)."""
    result = bytearray()
    start = offset
    while len(result) < row_bytes:
        if offset >= len(data):
            break
        n = data[offset]
        offset += 1
        if n < 128:  # 0..127: literal run of n+1 bytes
            count = n + 1
            result.extend(data[offset:offset + count])
            offset += count
        elif n > 128:  # 129..255: replicate next byte (257-n) times
            count = 257 - n
            result.extend([data[offset]] * count)
            offset += 1
        # n == 128: NOP
    return bytes(result[:row_bytes]), offset - start

def decode_bitmap(data, offset, opcode):
    """Decode a BitsRect (0x90) or PackBitsRect (0x98) operation.
    Returns (bounds, src_rect, dst_rect, bitmap_rows, new_offset)."""
    row_bytes = read_u16(data, offset)
    offset += 2
    bounds = read_rect(data, offset)
    offset += 8
    src_rect = read_rect(data, offset)
    offset += 8
    dst_rect = read_rect(data, offset)
    offset += 8
    mode = read_u16(data, offset)
    offset += 2

    height = bounds[2] - bounds[0]
    rows = []

    if opcode == 0x90:  # BitsRect - uncompressed
        for _ in range(height):
            row = data[offset:offset + row_bytes]
            rows.append(row)
            offset += row_bytes
    elif opcode == 0x98:  # PackBitsRect - compressed
        for _ in range(height):
            if row_bytes >= 8:
                # Each row has a byte count prefix
                if row_bytes >= 250:
                    count = read_u16(data, offset)
                    offset += 2
                else:
                    count = data[offset]
                    offset += 1
                row, _ = decode_packbits_row(data, offset, row_bytes)
                rows.append(row)
                offset += count
            else:
                row = data[offset:offset + row_bytes]
                rows.append(row)
                offset += row_bytes

    return bounds, src_rect, dst_rect, rows, offset

def parse_pict(filepath):
    """Parse a PICT v1 file and return the face bitmap as a 2D pixel array."""
    with open(filepath, 'rb') as f:
        data = f.read()

    # Skip 512-byte header
    pos = 512
    pic_size = read_u16(data, pos)
    pos += 2
    pic_frame = read_rect(data, pos)
    pos += 8

    frame_top, frame_left, frame_bottom, frame_right = pic_frame
    frame_h = frame_bottom - frame_top
    frame_w = frame_right - frame_left

    # Canvas: 0 = white in Mac terms, we'll store Mac convention (1=black)
    canvas = [[0] * frame_w for _ in range(frame_h)]

    # Parse opcodes
    while pos < len(data):
        opcode = data[pos]
        pos += 1

        if opcode == 0xFF:  # EndOfPicture
            break
        elif opcode == 0x00:  # NOP
            continue
        elif opcode == 0x11:  # Version
            pos += 1  # skip version byte
        elif opcode == 0xA0:  # ShortComment
            pos += 2  # skip comment kind
        elif opcode == 0xA1:  # LongComment
            pos += 2  # skip comment kind
            size = read_u16(data, pos)
            pos += 2 + size
        elif opcode == 0x01:  # Clip
            rgn_size = read_u16(data, pos)
            pos += rgn_size  # skip entire region
        elif opcode == 0x90 or opcode == 0x98:  # BitsRect / PackBitsRect
            bounds, src_rect, dst_rect, rows, pos = decode_bitmap(data, pos, opcode)

            # Place bitmap on canvas
            # src_rect defines which part of the bitmap to copy
            # dst_rect defines where to place it (same coords in these files)
            src_top, src_left, src_bottom, src_right = src_rect
            dst_top, dst_left = dst_rect[0], dst_rect[1]
            bnd_top, bnd_left = bounds[0], bounds[1]

            for src_y in range(src_top, src_bottom):
                bmp_row = src_y - bnd_top
                if bmp_row < 0 or bmp_row >= len(rows):
                    continue
                row_data = rows[bmp_row]
                canvas_y = (dst_top + (src_y - src_top)) - frame_top

                for src_x in range(src_left, src_right):
                    bmp_x = src_x - bnd_left
                    byte_idx = bmp_x // 8
                    bit_idx = 7 - (bmp_x % 8)
                    if byte_idx < len(row_data):
                        pixel = (row_data[byte_idx] >> bit_idx) & 1
                        canvas_x = (dst_left + (src_x - src_left)) - frame_left
                        if 0 <= canvas_y < frame_h and 0 <= canvas_x < frame_w:
                            canvas[canvas_y][canvas_x] = pixel
        else:
            print(f"  Unknown opcode 0x{opcode:02X} at offset {pos-1}")
            break

    return canvas, frame_w, frame_h

def scale_and_smooth(canvas, src_w, src_h, scale):
    """Scale canvas by factor with bilinear interpolation.
    Input canvas: 1=black, 0=white (Mac convention).
    Returns 8-bit grayscale array (0=black, 255=white)."""
    dst_w = round(src_w * scale)
    dst_h = round(src_h * scale)
    out = [[255] * dst_w for _ in range(dst_h)]

    for oy in range(dst_h):
        sy = oy / scale - 0.5
        sy0 = max(0, int(sy))
        sy1 = min(src_h - 1, sy0 + 1)
        fy = sy - sy0
        for ox in range(dst_w):
            sx = ox / scale - 0.5
            sx0 = max(0, int(sx))
            sx1 = min(src_w - 1, sx0 + 1)
            fx = sx - sx0

            # Sample 4 neighbors (convert: Mac 1=black → 0, Mac 0=white → 255)
            c00 = (1 - canvas[sy0][sx0]) * 255
            c10 = (1 - canvas[sy0][sx1]) * 255
            c01 = (1 - canvas[sy1][sx0]) * 255
            c11 = (1 - canvas[sy1][sx1]) * 255

            # Bilinear interpolation
            v = (c00 * (1 - fx) * (1 - fy) +
                 c10 * fx * (1 - fy) +
                 c01 * (1 - fx) * fy +
                 c11 * fx * fy)
            out[oy][ox] = max(0, min(255, round(v)))

    return out, dst_w, dst_h

def write_png(filepath, pixels, width, height):
    """Write an 8-bit grayscale PNG using only struct and zlib."""
    raw = bytearray()
    for y in range(height):
        raw.append(0)  # filter: None
        for x in range(width):
            raw.append(pixels[y][x])

    compressed = zlib.compress(bytes(raw))

    def make_chunk(chunk_type, chunk_data):
        chunk = chunk_type + chunk_data
        crc = zlib.crc32(chunk) & 0xFFFFFFFF
        return struct.pack('>I', len(chunk_data)) + chunk + struct.pack('>I', crc)

    with open(filepath, 'wb') as f:
        f.write(b'\x89PNG\r\n\x1a\n')
        # IHDR: bit_depth=8, color_type=0 (grayscale)
        ihdr_data = struct.pack('>IIBBBBB', width, height, 8, 0, 0, 0, 0)
        f.write(make_chunk(b'IHDR', ihdr_data))
        f.write(make_chunk(b'IDAT', compressed))
        f.write(make_chunk(b'IEND', b''))

def main():
    faces_dir = os.path.dirname(os.path.abspath(__file__))
    for name in ['man1', 'man2', 'man3', 'man4']:
        pict_path = os.path.join(faces_dir, f'{name}.pict')
        png_path = os.path.join(faces_dir, f'{name}.png')
        print(f"Converting {name}.pict...")
        canvas, w, h = parse_pict(pict_path)
        pixels, sw, sh = scale_and_smooth(canvas, w, h, 1.5)
        print(f"  {w}x{h} -> {sw}x{sh}")
        write_png(png_path, pixels, sw, sh)
        print(f"  Wrote {png_path}")

if __name__ == '__main__':
    main()
