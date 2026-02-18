#!/usr/bin/env python3
"""Extract all image resources (PICT, ICON, ICN#) from Pharaoh 1.2 resource fork.
Converts to PNG, scaled 150% with bilinear smoothing. Uses only stdlib."""

import struct
import zlib
import os
import sys

# ── Resource fork parsing ────────────────────────────────────

def read_u16(data, off): return struct.unpack('>H', data[off:off+2])[0]
def read_s16(data, off): return struct.unpack('>h', data[off:off+2])[0]
def read_rect(data, off):
    return (read_s16(data, off), read_s16(data, off+2),
            read_s16(data, off+4), read_s16(data, off+6))

def extract_resources(rsrc_path, rtype_filter):
    """Parse resource fork and return dict of {id: (name, bytes)} for given type."""
    with open(rsrc_path, 'rb') as f:
        data = f.read()

    data_off = struct.unpack('>I', data[0:4])[0]
    map_off = struct.unpack('>I', data[4:8])[0]
    map_base = map_off
    type_list_off = read_u16(data, map_base + 24)
    name_list_off = read_u16(data, map_base + 26)
    type_list_base = map_base + type_list_off
    num_types = read_u16(data, type_list_base) + 1

    results = {}
    pos = type_list_base + 2
    for _ in range(num_types):
        rtype = data[pos:pos+4]
        count = read_u16(data, pos+4) + 1
        ref_off = read_u16(data, pos+6)
        if rtype == rtype_filter.encode('ascii').ljust(4):
            ref_base = type_list_base + ref_off
            for j in range(count):
                e = ref_base + j * 12
                rid = read_u16(data, e)
                name_off_val = read_u16(data, e+2)
                d_off_bytes = data[e+5:e+8]
                d_off = (d_off_bytes[0] << 16) | (d_off_bytes[1] << 8) | d_off_bytes[2]
                abs_off = data_off + d_off
                res_len = struct.unpack('>I', data[abs_off:abs_off+4])[0]
                res_data = data[abs_off+4:abs_off+4+res_len]
                name = ''
                if name_off_val != 0xFFFF:
                    npos = map_base + name_list_off + name_off_val
                    nlen = data[npos]
                    name = data[npos+1:npos+1+nlen].decode('ascii', errors='replace')
                results[rid] = (name, res_data)
        pos += 8
    return results

# ── PICT decoding ────────────────────────────────────────────

def decode_packbits_row(data, offset, row_bytes):
    result = bytearray()
    start = offset
    while len(result) < row_bytes:
        if offset >= len(data):
            break
        n = data[offset]; offset += 1
        if n < 128:
            count = n + 1
            result.extend(data[offset:offset+count]); offset += count
        elif n > 128:
            count = 257 - n
            result.extend([data[offset]] * count); offset += 1
    return bytes(result[:row_bytes]), offset - start

def decode_bitmap(data, offset, opcode):
    row_bytes = read_u16(data, offset); offset += 2
    bounds = read_rect(data, offset); offset += 8
    src_rect = read_rect(data, offset); offset += 8
    dst_rect = read_rect(data, offset); offset += 8
    offset += 2  # mode

    height = bounds[2] - bounds[0]
    rows = []
    if opcode == 0x90:
        for _ in range(height):
            rows.append(data[offset:offset+row_bytes]); offset += row_bytes
    elif opcode == 0x98:
        for _ in range(height):
            if row_bytes >= 8:
                if row_bytes >= 250:
                    cnt = read_u16(data, offset); offset += 2
                else:
                    cnt = data[offset]; offset += 1
                row, _ = decode_packbits_row(data, offset, row_bytes)
                rows.append(row); offset += cnt
            else:
                rows.append(data[offset:offset+row_bytes]); offset += row_bytes
    return bounds, src_rect, dst_rect, rows, offset

def draw_line(canvas, x0, y0, x1, y1, w, h):
    """Bresenham's line algorithm. Sets pixels to 1 (black in Mac convention)."""
    dx = abs(x1 - x0); dy = abs(y1 - y0)
    sx = 1 if x0 < x1 else -1
    sy = 1 if y0 < y1 else -1
    err = dx - dy
    while True:
        if 0 <= y0 < h and 0 <= x0 < w:
            canvas[y0][x0] = 1
        if x0 == x1 and y0 == y1:
            break
        e2 = 2 * err
        if e2 > -dy: err -= dy; x0 += sx
        if e2 < dx:  err += dx; y0 += sy

def read_s8(data, off):
    v = data[off]
    return v - 256 if v >= 128 else v

def parse_pict_data(data):
    """Parse raw PICT resource data (no 512-byte file header)."""
    pos = 0
    pic_size = read_u16(data, pos); pos += 2
    pic_frame = read_rect(data, pos); pos += 8
    top, left, bottom, right = pic_frame
    h = bottom - top
    w = right - left
    if w <= 0 or h <= 0:
        return None, 0, 0
    canvas = [[0]*w for _ in range(h)]
    pen_v, pen_h = 0, 0  # current pen position (Mac coords)

    while pos < len(data):
        if pos >= len(data):
            break
        opcode = data[pos]; pos += 1
        if opcode == 0xFF: break
        elif opcode == 0x00: continue
        elif opcode == 0x11: pos += 1
        elif opcode == 0xA0: pos += 2
        elif opcode == 0xA1:
            pos += 2
            sz = read_u16(data, pos); pos += 2 + sz
        elif opcode == 0x01:
            rgn_sz = read_u16(data, pos); pos += rgn_sz
        elif opcode in (0x90, 0x98):
            bounds, src_rect, dst_rect, rows, pos = decode_bitmap(data, pos, opcode)
            src_t, src_l, src_b, src_r = src_rect
            dst_t, dst_l = dst_rect[0], dst_rect[1]
            bnd_t, bnd_l = bounds[0], bounds[1]
            for sy in range(src_t, src_b):
                bmp_row = sy - bnd_t
                if bmp_row < 0 or bmp_row >= len(rows): continue
                rd = rows[bmp_row]
                cy = (dst_t + (sy - src_t)) - top
                for sx in range(src_l, src_r):
                    bmp_x = sx - bnd_l
                    bi = bmp_x // 8
                    bt = 7 - (bmp_x % 8)
                    if bi < len(rd):
                        px = (rd[bi] >> bt) & 1
                        cx = (dst_l + (sx - src_l)) - left
                        if 0 <= cy < h and 0 <= cx < w:
                            canvas[cy][cx] = px
        elif opcode == 0x20:  # Line: pnLoc, newPt
            pen_v = read_s16(data, pos); pen_h = read_s16(data, pos+2)
            nv = read_s16(data, pos+4); nh = read_s16(data, pos+6)
            pos += 8
            draw_line(canvas, pen_h-left, pen_v-top, nh-left, nv-top, w, h)
            pen_v, pen_h = nv, nh
        elif opcode == 0x21:  # LineFrom: newPt
            nv = read_s16(data, pos); nh = read_s16(data, pos+2)
            pos += 4
            draw_line(canvas, pen_h-left, pen_v-top, nh-left, nv-top, w, h)
            pen_v, pen_h = nv, nh
        elif opcode == 0x22:  # ShortLine: pnLoc, dh, dv
            pen_v = read_s16(data, pos); pen_h = read_s16(data, pos+2)
            dh = read_s8(data, pos+4); dv = read_s8(data, pos+5)
            pos += 6
            nv, nh = pen_v + dv, pen_h + dh
            draw_line(canvas, pen_h-left, pen_v-top, nh-left, nv-top, w, h)
            pen_v, pen_h = nv, nh
        elif opcode == 0x23:  # ShortLineFrom: dh, dv
            dh = read_s8(data, pos); dv = read_s8(data, pos+1)
            pos += 2
            nv, nh = pen_v + dv, pen_h + dh
            draw_line(canvas, pen_h-left, pen_v-top, nh-left, nv-top, w, h)
            pen_v, pen_h = nv, nh
        elif opcode == 0x03:  # TxFont
            pos += 2
        elif opcode == 0x04:  # TxFace
            pos += 1
        elif opcode == 0x05:  # TxMode
            pos += 2
        elif opcode == 0x07:  # PnSize
            pos += 4
        elif opcode == 0x08:  # PnMode
            pos += 2
        elif opcode == 0x09:  # PnPat
            pos += 8
        elif opcode == 0x0A:  # FillPat
            pos += 8
        elif opcode == 0x0B:  # OvSize
            pos += 4
        elif opcode == 0x0D:  # TxSize
            pos += 2
        elif opcode == 0x0E:  # FgColor
            pos += 4
        elif opcode == 0x0F:  # BkColor
            pos += 4
        elif opcode == 0x10:  # TxRatio
            pos += 8
        elif opcode == 0x28:  # LongText
            pos += 4
            tlen = data[pos]; pos += 1 + tlen
        elif opcode == 0x29:  # DHText
            pos += 1
            tlen = data[pos]; pos += 1 + tlen
        elif opcode == 0x2A:  # DVText
            pos += 1
            tlen = data[pos]; pos += 1 + tlen
        elif opcode == 0x2B:  # DHDVText
            pos += 2
            tlen = data[pos]; pos += 1 + tlen
        elif opcode == 0x2C:  # fontName
            sz = read_u16(data, pos); pos += sz
        elif opcode in (0x30, 0x34, 0x38, 0x3C,  # Rect ops
                         0x40, 0x44, 0x48, 0x4C,  # RRect ops
                         0x50, 0x54, 0x58, 0x5C):  # Oval ops
            pos += 8
        elif opcode in (0x31, 0x35, 0x39, 0x3D,  # same rect
                         0x41, 0x45, 0x49, 0x4D,
                         0x51, 0x55, 0x59, 0x5D):
            pass  # zero data, uses last rect
        elif opcode in (0x60, 0x64, 0x68, 0x6C):  # Arc ops
            pos += 12
        elif opcode in (0x61, 0x65, 0x69, 0x6D):  # same arc
            pos += 4
        elif opcode in (0x70, 0x74, 0x78, 0x7C):  # Poly ops
            sz = read_u16(data, pos); pos += sz
        elif opcode in (0x71, 0x75, 0x79, 0x7D):  # same poly
            pass
        elif opcode in (0x80, 0x84, 0x88, 0x8C):  # Rgn ops
            sz = read_u16(data, pos); pos += sz
        elif opcode in (0x81, 0x85, 0x89, 0x8D):  # same rgn
            pass
        else:
            print(f"  Unknown opcode 0x{opcode:02X} at {pos-1}, stopping")
            break
    return canvas, w, h

# ── Scaling & PNG output ─────────────────────────────────────

def scale_and_smooth(canvas, src_w, src_h, scale):
    dst_w = round(src_w * scale)
    dst_h = round(src_h * scale)
    out = [[255]*dst_w for _ in range(dst_h)]
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
            c00 = (1 - canvas[sy0][sx0]) * 255
            c10 = (1 - canvas[sy0][sx1]) * 255
            c01 = (1 - canvas[sy1][sx0]) * 255
            c11 = (1 - canvas[sy1][sx1]) * 255
            v = (c00*(1-fx)*(1-fy) + c10*fx*(1-fy) +
                 c01*(1-fx)*fy + c11*fx*fy)
            out[oy][ox] = max(0, min(255, round(v)))
    return out, dst_w, dst_h

def write_png(filepath, pixels, width, height):
    raw = bytearray()
    for y in range(height):
        raw.append(0)
        for x in range(width):
            raw.append(pixels[y][x])
    compressed = zlib.compress(bytes(raw))
    def chunk(ctype, cdata):
        c = ctype + cdata
        crc = zlib.crc32(c) & 0xFFFFFFFF
        return struct.pack('>I', len(cdata)) + c + struct.pack('>I', crc)
    with open(filepath, 'wb') as f:
        f.write(b'\x89PNG\r\n\x1a\n')
        f.write(chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 0, 0, 0, 0)))
        f.write(chunk(b'IDAT', compressed))
        f.write(chunk(b'IEND', b''))

# ── ICON decoding ────────────────────────────────────────────

def parse_icon(data):
    """Parse a 32x32 1-bit Mac ICON (128 bytes). Returns canvas, 32, 32."""
    canvas = [[0]*32 for _ in range(32)]
    for row in range(32):
        for col in range(32):
            byte_idx = row * 4 + col // 8
            bit_idx = 7 - (col % 8)
            if byte_idx < len(data):
                canvas[row][col] = (data[byte_idx] >> bit_idx) & 1
    return canvas, 32, 32

# ── Main ─────────────────────────────────────────────────────

PICT_NAMES = {
    128: 'opening',
    512: 'logo',
    12022: 'license',
    31196: 'man1',
    25883: 'man2',
    16599: 'man3',
    32536: 'man4',
}

ICON_NAMES = {
    308: 'icon_buysell',
    20526: 'icon_feed',
    4138: 'icon_overseers',
    29504: 'icon_plant',
    772: 'icon_manure',
    24928: 'icon_loan',
    31570: 'icon_pyramid',
    6081: 'icon_event',
}

ICN_NAMES = {
    128: 'finder_app',
    129: 'finder_doc',
}

def convert_and_write(canvas, w, h, label, png_path):
    print(f"  Original: {w}x{h}")
    pixels, sw, sh = scale_and_smooth(canvas, w, h, 1.5)
    print(f"  Scaled:   {sw}x{sh}")
    write_png(png_path, pixels, sw, sh)
    print(f"  Wrote {png_path}\n")

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    rsrc_path = os.path.join(script_dir, '..', '..',
        'Pharaoh Media', 'Pharaoh Distribution', 'Pharaoh 1.2.rsrc')

    # ── PICTs ──
    print("Extracting PICT resources...")
    picts = extract_resources(rsrc_path, 'PICT')
    print(f"Found {len(picts)} PICT resources\n")

    for rid, (rname, rdata) in sorted(picts.items()):
        fname = PICT_NAMES.get(rid, f'pict_{rid}')
        label = f'PICT {rid} "{rname}"' if rname else f'PICT {rid}'
        print(f"{label} ({len(rdata)} bytes) -> {fname}.png")
        canvas, w, h = parse_pict_data(rdata)
        if canvas is None:
            print(f"  SKIP: empty frame\n"); continue
        convert_and_write(canvas, w, h, label, os.path.join(script_dir, f'{fname}.png'))

    # ── ICONs ──
    print("Extracting ICON resources...")
    icons = extract_resources(rsrc_path, 'ICON')
    print(f"Found {len(icons)} ICON resources\n")

    for rid, (rname, rdata) in sorted(icons.items()):
        fname = ICON_NAMES.get(rid, f'icon_{rid}')
        label = f'ICON {rid} "{rname}"' if rname else f'ICON {rid}'
        print(f"{label} ({len(rdata)} bytes) -> {fname}.png")
        canvas, w, h = parse_icon(rdata)
        convert_and_write(canvas, w, h, label, os.path.join(script_dir, f'{fname}.png'))

    # ── ICN# (Finder icons: icon + mask) ──
    print("Extracting ICN# resources...")
    icns = extract_resources(rsrc_path, 'ICN#')
    print(f"Found {len(icns)} ICN# resources\n")

    for rid, (rname, rdata) in sorted(icns.items()):
        fname = ICN_NAMES.get(rid, f'icn_{rid}')
        label = f'ICN# {rid} "{rname}"' if rname else f'ICN# {rid}'
        print(f"{label} ({len(rdata)} bytes) -> {fname}.png")
        # ICN# = 128 bytes icon + 128 bytes mask; just extract the icon part
        canvas, w, h = parse_icon(rdata[:128])
        convert_and_write(canvas, w, h, label, os.path.join(script_dir, f'{fname}.png'))

if __name__ == '__main__':
    main()
