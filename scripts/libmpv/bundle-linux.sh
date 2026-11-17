#!/usr/bin/env bash
# Copies the libmpv of this Linux computer and the libraries it needs into one folder, so the AppImage plays video without libmpv installed on the computer it runs on.

# It takes the folder to fill, and runs on the machine that builds the AppImage, with libmpv2 and patchelf installed. Each library copied is set to find the others in its own folder first.
set -euo pipefail

dest="$(realpath -m "${1:?the folder to fill}")"
lib="$(ldconfig -p | awk '/libmpv\.so\.2 .*x86-64/ { print $NF; exit }')"
if [ -z "$lib" ]; then
    echo "libmpv.so.2 is not installed here; install the libmpv2 package first" >&2
    exit 1
fi
mkdir -p "$dest"

# What every Linux desktop has and must come from the computer itself: the C library and the loader, the drivers of the screen and of the sound, the window system and the fonts. A copy of these breaks on a computer whose drivers or system differ.
skip='^(ld-linux.*|libc|libm|libdl|libpthread|librt|libresolv|libutil|libanl|libnsl|libmvec|libBrokenLocale|libgcc_s|libstdc\+\+|libGL|libGLX|libGLdispatch|libEGL|libGLESv2|libOpenGL|libdrm|libgbm|libvulkan|libxcb.*|libX11.*|libXext|libXrender|libXau|libXdmcp|libXss|libXpresent|libXrandr|libXinerama|libXfixes|libXcursor|libXi|libXv|libxkbcommon.*|libwayland-.*|libasound|libjack|libpipewire-.*|libfontconfig|libfreetype|libharfbuzz|libexpat|libz|libuuid|libdbus-1|libsystemd|libudev|libselinux|libcap|libgcrypt|libgpg-error|libcom_err)\.so'

cp -L "$lib" "$dest/libmpv.so.2"
# ldd lists every library libmpv reaches, also through the libraries it needs.
ldd "$lib" | awk '/=> \// { print $1, $3 }' | while read -r name path; do
    if [[ "$name" =~ $skip ]]; then
        continue
    fi
    cp -L "$path" "$dest/$name"
done

for file in "$dest"/*.so*; do
    chmod 644 "$file"
    patchelf --set-rpath '$ORIGIN' "$file"
done
# The app asks for libmpv by its plain name.
ln -sf libmpv.so.2 "$dest/libmpv.so"

echo "libmpv and $(($(ls "$dest" | wc -l) - 2)) libraries it needs are in $dest ($(du -sh "$dest" | cut -f1))"
