#!/usr/bin/env sh
#
set -e

MYAPP=svgpinout

if [ ! -x "$MYAPP" ]; then
  echo "binary $MYAPP not found, exiting"
fi

# see https://github.com/AppImage/AppImageKit/wiki/AppDir
TMPD=$(mktemp -d)

trap 'rm -rf -- "$TMPD"' INT EXIT QUIT TERM

APPDIR=${MYAPP}.AppDir

install -d "${TMPD}/$APPDIR"

echo "Building appimage structure in '${TMPD}/$APPDIR' folder using executable $MYAPP"
echo ""

mkdir -p "${TMPD}/${APPDIR}/usr/bin"
install -m 0755 target/$MYAPP "${TMPD}/${APPDIR}/usr/bin"
install -m 0644 target/*.so "${TMPD}/${APPDIR}/usr/bin"

for i in javajpeg jsound lcms
do
  rm -f "${TMPD}/${APPDIR}/usr/bin/lib${i}.so"
done

touch "${TMPD}/${APPDIR}/${MYAPP}.svg"

cat << EOF > "${TMPD}/${APPDIR}/${MYAPP}.desktop"
[Desktop Entry]
Version=1.0
Terminal=false
NoDisplay=true
Name=$MYAPP
Comment=Creates a chip pinout svg image from a CSV description file
Exec=$MYAPP %U
Icon=${MYAPP}
Type=Application
Categories=Utility;
EOF

if command -v tree >/dev/null 2>&1 ; then
  tree -a "${TMPD}/${APPDIR}/"
fi

cat << EOF > "${TMPD}/${APPDIR}/AppRun"
#!/bin/sh

THISFILE=\$(readlink -f "\$0")
BASENAME=\$(dirname "\$THISFILE")

# set PATH
# run Exec app. from ${MYEXEC}.desktop
# see https://github.com/AppImage/AppImageKit/blob/appimagetool/master/src/AppRun.c
# or use directly the binary from https://github.com/AppImage/AppImageKit/releases
"\${BASENAME}"/usr/bin/$MYAPP "\$@"
EOF

chmod 755 "${TMPD}/${APPDIR}/AppRun"

if command -v appimagetool >/dev/null 2>&1 ; then
  appimagetool -v "${TMPD}/${APPDIR}"
else
  echo "Please also add a $MYAPP.svg file at the ${APPDIR} root"
fi