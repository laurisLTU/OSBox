# OSBox — Simple Operating System Builder

OSBox is a lightweight, beginner‑friendly IDE for building small operating systems from a single `kernel.c` file.  
It provides a clean editor, automatic build pipeline, and one‑click ISO generation — all without Makefiles, CMake, or complex toolchains.

OSBox is written entirely in **Java (Swing)** and runs on any Linux system with Java installed.

---

## 📦 Features

### ✔ Build an OS from one C file  
Write your entire kernel in `kernel.c` and OSBox handles the rest.

### ✔ Automatic bootloader + linker generation  
OSBox generates:
- `boot.s` — assembly entry point  
- `linker.ld` — linker script  
- `grub.cfg` — GRUB boot menu  

### ✔ One‑click ISO creation  
OSBox compiles your kernel into One Click and makes the iso very quickly.


Bootable in:
- QEMU  
- VirtualBox  
- Real hardware  

### ✔ Built‑in editor  
- VSCode‑style line numbers  
- Save / Save As  
- Build output panel  

### ✔ Zero external build systems  
No Makefile.  
No CMake.  
No Meson.  
No Gradle.  
No Maven.  

Just **OSBox + system tools**.

---

## 🧰 What OSBox Uses to Build an OS

Your OS is written in:

### **C + Assembly**
- `kernel.c` — your kernel  
- `boot.s` — assembly entry point  
- `linker.ld` — linker script  
- GRUB loads your kernel using the Multiboot standard  

OSBox uses standard Linux tools:

| Purpose | Tool |
|--------|------|
| Assemble boot.s | `as` |
| Compile kernel.c | `gcc -m32` |
| Link kernel | `ld -m elf_i386` |
| Create ISO | `grub-mkrescue` |
| Required by GRUB | `xorriso`, `mtools` |

---

## ❌ What OSBox Does NOT Do

OSBox is intentionally simple. It **does not**:

### ✗ Support VESA / VBE graphics automatically  
You *can* write a graphics kernel yourself, but OSBox does not generate VESA code for you.

### ✗ Provide a full OS framework  
No scheduler, no filesystem, no drivers — you write everything.

### ✗ Support multiple source files  
OSBox is designed for **single‑file kernels**.

### ✗ Replace a real OS development environment  
It’s a learning tool, not a full OSDev suite.

---

## 🖥️ Supported Operating Systems

OSBox runs on:

- Ubuntu / Debian  
- Linux Mint  
- Pop!\_OS  
- Kali Linux  
- Any Linux with Java + GRUB tools  

Windows and macOS are **not supported**.

---

## 🔧 Installation

### 1. Install required tools

Run:

```bash
sudo apt update
sudo apt install default-jre gcc grub-pc-bin xorriso mtools binutils


