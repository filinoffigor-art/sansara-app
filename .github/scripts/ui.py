#!/usr/bin/env python3
"""Мини-помощник для adb: найти элемент по тексту в дампе uiautomator и нажать на него."""
import re, subprocess, sys, time

def dump():
    subprocess.run(["adb", "shell", "uiautomator", "dump", "/sdcard/ui.xml"], capture_output=True)
    return subprocess.run(["adb", "shell", "cat", "/sdcard/ui.xml"], capture_output=True, text=True).stdout

def find(text, timeout=20):
    end = time.time() + timeout
    while time.time() < end:
        xml = dump()
        for m in re.finditer(r'<node [^>]*>', xml):
            node = m.group(0)
            t = re.search(r' text="([^"]*)"', node)
            d = re.search(r' content-desc="([^"]*)"', node)
            if (t and text in t.group(1)) or (d and text in d.group(1)):
                b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', node)
                if b:
                    x1, y1, x2, y2 = map(int, b.groups())
                    return (x1 + x2) // 2, (y1 + y2) // 2
        time.sleep(1)
    return None

if __name__ == "__main__":
    cmd, text = sys.argv[1], sys.argv[2]
    pos = find(text, int(sys.argv[3]) if len(sys.argv) > 3 else 20)
    if pos is None:
        print(f"НЕ НАЙДЕНО: {text}")
        sys.exit(1)
    if cmd == "tap":
        subprocess.run(["adb", "shell", "input", "tap", str(pos[0]), str(pos[1])])
    print(f"ok {cmd} {text} {pos}")
