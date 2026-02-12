#!/bin/bash
set -e

APPNAME="Osbox"
VERSION="1.0"
ARCH="amd64"

echo "=== Building OSBox ==="

rm -rf build
mkdir -p build/DEBIAN
mkdir -p build/usr/bin
mkdir -p build/usr/share/osbox
mkdir -p build/usr/share/applications

# Control file
cat > build/DEBIAN/control <<EOF
Package: ${APPNAME}
Version: ${VERSION}
Section: utils
Priority: optional
Architecture: ${ARCH}
Depends: default-jre, gcc, grub-pc-bin, xorriso, mtools
Maintainer: Laurynas
Description: OSBox - Build your own OS from one C file
 A simple IDE for building bootable OS kernels.
EOF

# Copy Java source
cp OSBox.java build/usr/share/osbox/

# Compile Java
echo "=== Compiling Java ==="
javac build/usr/share/osbox/OSBox.java

# Create runnable JAR
echo "=== Creating JAR ==="
jar cfe build/usr/share/osbox/osbox.jar OSBox -C build/usr/share/osbox .

# Launcher
cat > build/usr/bin/osbox <<EOF
#!/bin/bash
java -jar /usr/share/osbox/osbox.jar
EOF
chmod +x build/usr/bin/osbox

# Desktop entry
cat > build/usr/share/applications/osbox.desktop <<EOF
[Desktop Entry]
Name=OSBox
Exec=/usr/bin/osbox
Type=Application
Terminal=false
Categories=Development;
EOF

# Build .deb
echo "=== Building .deb ==="
dpkg-deb --build build ${APPNAME}_${VERSION}_${ARCH}.deb

echo "=== Done ==="
echo "Created: ${APPNAME}_${VERSION}_${ARCH}.deb"
