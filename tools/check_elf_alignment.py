from pathlib import Path
import struct
import sys

for filename in sys.argv[1:]:
    data = Path(filename).read_bytes()
    if data[:4] != b"\x7fELF":
        print(filename, "not-elf")
        continue
    elf_class = data[4]
    endian = "<" if data[5] == 1 else ">"
    if elf_class == 2:
        header = struct.unpack_from(endian + "16sHHIQQQIHHHHHH", data, 0)
        phoff, phentsize, phnum = header[5], header[9], header[10]
        loads = []
        for i in range(phnum):
            off = phoff + i * phentsize
            p_type, p_flags, p_offset, p_vaddr, p_paddr, p_filesz, p_memsz, p_align = struct.unpack_from(endian + "IIQQQQQQ", data, off)
            if p_type == 1:
                loads.append((hex(p_offset), hex(p_vaddr), p_align))
    elif elf_class == 1:
        header = struct.unpack_from(endian + "16sHHIIIIIHHHHHH", data, 0)
        phoff, phentsize, phnum = header[5], header[9], header[10]
        loads = []
        for i in range(phnum):
            off = phoff + i * phentsize
            p_type, p_offset, p_vaddr, p_paddr, p_filesz, p_memsz, p_flags, p_align = struct.unpack_from(endian + "IIIIIIII", data, off)
            if p_type == 1:
                loads.append((hex(p_offset), hex(p_vaddr), p_align))
    else:
        print(filename, "unknown-class")
        continue
    print(filename)
    for offset, vaddr, align in loads:
        print("  LOAD", offset, vaddr, "align", align, "OK16K" if align >= 16384 and align % 16384 == 0 else "NOT16K")
