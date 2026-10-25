#!/usr/bin/env bash
# Builds one small libmpv for Windows with only what playing web video needs, each source pinned to a release and its hash or to a commit.

# It runs in the UCRT64 shell of MSYS2 with the mingw packages gcc, meson, ninja, pkgconf, nasm, python, python-jinja, dav1d and zlib, plus make, git, diffutils, tar and xz, takes a work folder, and leaves the file in its out folder.
set -euo pipefail

work="$(realpath -m "${1:-build/libmpv}")"
prefix="$work/prefix"
src="$work/src"
out="$work/out"
jobs="$(nproc)"
mkdir -p "$src" "$prefix" "$out"

# The libraries built here are found before those of the system, and each is linked in whole, not as a separate file.
export PKG_CONFIG_PATH="$prefix/lib/pkgconfig"
export CFLAGS="-O2 -I$prefix/include"
export CXXFLAGS="-O2 -I$prefix/include"
export LDFLAGS="-L$prefix/lib"

# Fetches one source archive and stops when it is not the one that was pinned.
fetch() {
    local name="$1" url="$2" sha="$3"
    if [ ! -f "$src/$name" ] || [ "$(sha256sum "$src/$name" | cut -d' ' -f1)" != "$sha" ]; then
        echo "Fetching $url"
        curl -sSLf -o "$src/$name" "$url"
    fi
    local found
    found="$(sha256sum "$src/$name" | cut -d' ' -f1)"
    if [ "$found" != "$sha" ]; then
        echo "$name has the hash $found, not the pinned $sha" >&2
        exit 1
    fi
}

# Unpacks a source archive into a fresh folder and gives the folder.
unpack() {
    local name="$1" folder="$2"
    rm -rf "${src:?}/$folder"
    tar -xf "$src/$name" -C "$src"
    echo "$src/$folder"
}

# A meson project, built as a static library into the prefix.
meson_static() {
    local folder="$1"
    shift
    meson setup "$folder/build" "$folder" --prefix="$prefix" --libdir=lib --buildtype=release --default-library=static --wrap-mode=nodownload "$@"
    meson install -C "$folder/build"
}

fetch freetype-2.14.3.tar.xz https://download.savannah.gnu.org/releases/freetype/freetype-2.14.3.tar.xz 36bc4f1cc413335368ee656c42afca65c5a3987e8768cc28cf11ba775e785a5f
fetch fribidi-1.0.17.tar.xz https://github.com/fribidi/fribidi/releases/download/v1.0.17/fribidi-1.0.17.tar.xz 6949dcde27d41cebad1fd741fcafc36d55a1020d2d872d4a6eb3914caabbada2
fetch harfbuzz-14.6.0.tar.xz https://github.com/harfbuzz/harfbuzz/releases/download/14.6.0/harfbuzz-14.6.0.tar.xz d07a007327277708a2a73ae437887cdbaf282937f6d03ca5467723e9099af586
fetch libass-0.17.5.tar.xz https://github.com/libass/libass/releases/download/0.17.5/libass-0.17.5.tar.xz 2dca25c0e0c837ddf00b52011b3f82cac1e4ddd3ad018227806b0c2288864acc
fetch ffmpeg-8.1.3.tar.xz https://ffmpeg.org/releases/ffmpeg-8.1.3.tar.xz 7138d28c96d9d3e3af4ee3d8cad72741f8ffb40da90c1112235dea3ecd3178a3

# The fonts of subtitles: FreeType draws the letters, FriBidi orders text that runs right to left, and HarfBuzz shapes the letters of scripts that join.
freetype="$(unpack freetype-2.14.3.tar.xz freetype-2.14.3)"
meson_static "$freetype" -Dbrotli=disabled -Dbzip2=disabled -Dharfbuzz=disabled -Dpng=disabled -Dzlib=disabled -Dtests=disabled

fribidi="$(unpack fribidi-1.0.17.tar.xz fribidi-1.0.17)"
meson_static "$fribidi" -Ddocs=false -Dbin=false -Dtests=false

harfbuzz="$(unpack harfbuzz-14.6.0.tar.xz harfbuzz-14.6.0)"
meson_static "$harfbuzz" -Dfreetype=enabled -Dglib=disabled -Dgobject=disabled -Dcairo=disabled -Dicu=disabled -Dgraphite2=disabled \
    -Dchafa=disabled -Dtests=disabled -Ddocs=disabled -Dintrospection=disabled -Dutilities=disabled -Dbenchmark=disabled

# libass draws the subtitles, with the fonts of Windows found through DirectWrite.
libass="$(unpack libass-0.17.5.tar.xz libass-0.17.5)"
(
    cd "$libass"
    ./configure --prefix="$prefix" --enable-static --disable-shared --disable-fontconfig --enable-directwrite --disable-require-system-font-provider
    make -j"$jobs"
    make install
)

# libplacebo is what mpv draws with. It is built without any GPU, as the app draws the frames itself.
placebo="$src/libplacebo"
if [ ! -d "$placebo/.git" ] || [ "$(git -C "$placebo" rev-parse HEAD)" != "cee9b076f2c63104ccfd497fa79c39a867293ec4" ]; then
    rm -rf "$placebo"
    git clone --depth 1 --branch v7.360.1 --recurse-submodules --shallow-submodules https://code.videolan.org/videolan/libplacebo.git "$placebo"
fi
if [ "$(git -C "$placebo" rev-parse HEAD)" != "cee9b076f2c63104ccfd497fa79c39a867293ec4" ]; then
    echo "libplacebo is not at the pinned commit" >&2
    exit 1
fi
rm -rf "$placebo/build"
meson_static "$placebo" -Dvulkan=disabled -Dopengl=disabled -Dd3d11=disabled -Dglslang=disabled -Dshaderc=disabled -Dlcms=disabled \
    -Ddovi=disabled -Dlibdovi=disabled -Ddemos=false -Dtests=false -Dbench=false -Dfuzz=false -Dxxhash=disabled -Dunwind=disabled
# Part of libplacebo is C++, but its file for pkg-config does not say so, and mpv is C, so the C++ library is named here to be linked in too.
sed -i '/^Libs:/ s/$/ -lstdc++/' "$prefix/lib/pkgconfig/libplacebo.pc"

# FFmpeg with only what web video uses. Video: H.264, HEVC, VP8, VP9 and AV1. Sound: AAC, MP3, Opus, Vorbis, FLAC and AC3. Containers: HLS, MP4, Matroska and WebM, MPEG-TS and FLV. Subtitles: ASS, SRT, WebVTT and those inside MP4. Network: HTTP and HTTPS through Windows' own TLS.
ffmpeg="$(unpack ffmpeg-8.1.3.tar.xz ffmpeg-8.1.3)"
(
    cd "$ffmpeg"
    ./configure --prefix="$prefix" --enable-static --disable-shared --pkg-config-flags=--static \
        --disable-programs --disable-doc --disable-debug --disable-autodetect --disable-everything \
        --enable-network --enable-schannel --enable-zlib --enable-libdav1d \
        --enable-avcodec --enable-avformat --enable-avfilter --enable-swscale --enable-swresample \
        --enable-protocol=file,http,https,tcp,tls,crypto,httpproxy,data,hls \
        --enable-demuxer=hls,mov,matroska,mpegts,flv,aac,mp3,ogg,flac,wav,ac3,eac3,webvtt,ass,srt,concat,data \
        --enable-decoder=h264,hevc,vp8,vp9,libdav1d,aac,aac_latm,mp3,mp3float,opus,vorbis,flac,ac3,eac3,pcm_s16le,pcm_f32le \
        --enable-decoder=ass,ssa,subrip,srt,webvtt,mov_text,text \
        --enable-parser=h264,hevc,vp8,vp9,av1,aac,aac_latm,mpegaudio,opus,vorbis,flac,ac3 \
        --enable-bsf=h264_mp4toannexb,hevc_mp4toannexb,aac_adtstoasc,vp9_superframe_split,extract_extradata,av1_frame_split,null \
        --enable-filter=aformat,anull,aresample,format,null,scale,buffer,buffersink,abuffer,abuffersink,hflip,vflip,transpose,crop,pad,setpts,asetpts
    make -j"$jobs"
    make install
)

# mpv itself, as libmpv alone, with every part it can do without left out, and the libraries above built into it. Nothing here needs the GPL, so it is built as LGPL. It is a commit after 0.41.0: 0.41.0 lists the sound of every quality of an HLS stream as its own track, and this one only those of the quality that plays.
mpv="$src/mpv"
mpv_commit=b2c255c13e8e37952dbac6c34da56a690560378b
if [ ! -d "$mpv/.git" ] || [ "$(git -C "$mpv" rev-parse HEAD)" != "$mpv_commit" ]; then
    rm -rf "$mpv"
    git init -q "$mpv"
    git -C "$mpv" fetch -q --depth 1 https://github.com/mpv-player/mpv.git "$mpv_commit"
    git -C "$mpv" checkout -q FETCH_HEAD
fi
if [ "$(git -C "$mpv" rev-parse HEAD)" != "$mpv_commit" ]; then
    echo "mpv is not at the pinned commit" >&2
    exit 1
fi
rm -rf "$mpv/build"
meson setup "$mpv/build" "$mpv" --prefix="$prefix" --libdir=lib --buildtype=release --default-library=shared --prefer-static --wrap-mode=nodownload \
    -Dlibmpv=true -Dcplayer=false -Dgpl=false -Dbuild-date=false -Dauto_features=disabled -Dgl=disabled -Dwin32-threads=enabled -Dwasapi=enabled -Dzlib=enabled \
    -Dc_link_args="-static -static-libgcc -static-libstdc++" -Dcpp_link_args="-static -static-libgcc -static-libstdc++"
meson compile -C "$mpv/build"
cp "$mpv/build/libmpv-2.dll" "$out/libmpv-2.dll"
strip --strip-unneeded "$out/libmpv-2.dll"

# The file may lean only on the DLLs every Windows has.
echo "libmpv-2.dll is $(stat -c %s "$out/libmpv-2.dll") bytes and needs:"
objdump -p "$out/libmpv-2.dll" | grep "DLL Name" | sort -u
