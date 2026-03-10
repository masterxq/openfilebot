#!/usr/bin/env bash
set -euo pipefail

MEDIAINFO_VERSION="26.01"
ZEN_VERSION="0.4.41"

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
WORK_DIR="$REPO_ROOT/tmp-mediainfo-minimal"
PREFIX="$WORK_DIR/install"

DEPLOY=0
if [[ "${1:-}" == "--deploy-linux-amd64" ]]; then
  DEPLOY=1
fi

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

for cmd in curl tar make gcc g++ autoreconf pkg-config readelf nm; do
  need_cmd "$cmd"
done

echo "==> Using work dir: $WORK_DIR"
rm -rf "$WORK_DIR"
mkdir -p "$WORK_DIR"
cd "$WORK_DIR"

echo "==> Downloading sources"
curl -fsSLO "https://mediaarea.net/download/source/libzen/${ZEN_VERSION}/libzen_${ZEN_VERSION}.tar.bz2"
curl -fsSLO "https://mediaarea.net/download/source/libmediainfo/${MEDIAINFO_VERSION}/libmediainfo_${MEDIAINFO_VERSION}.tar.bz2"

echo "==> Extracting sources"
tar -xjf "libzen_${ZEN_VERSION}.tar.bz2"
tar -xjf "libmediainfo_${MEDIAINFO_VERSION}.tar.bz2"

echo "==> Building ZenLib"
cd "$WORK_DIR/ZenLib/Project/GNU/Library"
./autogen.sh >/tmp/zen-autogen.log 2>&1
./configure --prefix="$PREFIX" --enable-shared --disable-static >/tmp/zen-configure.log 2>&1
make -j"$(nproc)" >/tmp/zen-make.log 2>&1
make install >/tmp/zen-install.log 2>&1

echo "==> Building MediaInfoLib (minimal deps)"
cd "$WORK_DIR/MediaInfoLib/Project/GNU/Library"
./autogen.sh >/tmp/mi-autogen.log 2>&1
./configure \
  --prefix="$PREFIX" \
  --enable-shared \
  --disable-static \
  --with-libmms=no \
  --with-libcurl=runtime \
  PKG_CONFIG_PATH="$PREFIX/lib/pkgconfig:${PKG_CONFIG_PATH:-}" \
  CPPFLAGS="-I$PREFIX/include" \
  LDFLAGS="-L$PREFIX/lib" \
  >/tmp/mi-configure-min.log 2>&1
make -j"$(nproc)" >/tmp/mi-make-min.log 2>&1
make install >/tmp/mi-install-min.log 2>&1

NEW_LIB="$PREFIX/lib/libmediainfo.so.0.0.0"
CUR_LIB="$REPO_ROOT/lib/native/linux-amd64/libmediainfo.so"

echo
printf '%s\n' "==> Build summary"
grep -E 'Using libcurl\?|Using libmms\?' /tmp/mi-configure-min.log || true

echo
printf '%s\n' "==> NEEDED (new)"
readelf -d "$NEW_LIB" | grep NEEDED || true

echo
printf '%s\n' "==> NEEDED (current repo linux-amd64)"
readelf -d "$CUR_LIB" | grep NEEDED || true

echo
printf '%s\n' "==> API symbol check"
for s in \
  MediaInfo_New MediaInfo_Open MediaInfo_Option MediaInfo_Inform \
  MediaInfo_Get MediaInfo_GetI MediaInfo_Count_Get MediaInfo_Close \
  MediaInfo_Delete MediaInfo_Open_Buffer_Init MediaInfo_Open_Buffer_Continue \
  MediaInfo_Open_Buffer_Continue_GoTo_Get MediaInfo_Open_Buffer_Finalize; do
  if nm -D "$NEW_LIB" | grep -Eq "[[:space:]]$s$"; then
    echo "OK $s"
  else
    echo "MISSING $s"
    exit 1
  fi
done

echo
printf '%s\n' "==> Output files"
ls -l "$PREFIX/lib/libmediainfo.so"* "$PREFIX/lib/libzen.so"*

if [[ "$DEPLOY" -eq 1 ]]; then
  echo
  printf '%s\n' "==> Deploying linux-amd64 libs into repository"
  cp "$PREFIX/lib/libmediainfo.so.0.0.0" "$REPO_ROOT/lib/native/linux-amd64/libmediainfo.so"
  cp "$PREFIX/lib/libzen.so.0.0.0" "$REPO_ROOT/lib/native/linux-amd64/libzen.so"
  echo "Updated: lib/native/linux-amd64/libmediainfo.so"
  echo "Updated: lib/native/linux-amd64/libzen.so"
fi

echo
printf '%s\n' "Done. Candidate libs are in: $PREFIX/lib"
printf '%s\n' "Use --deploy-linux-amd64 to copy the result into the repository."
