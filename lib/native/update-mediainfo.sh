#!/bin/sh -xu

MEDIAINFO_VERSION="26.01"
ZEN_VERSION="0.4.41"


# Download and extract archives
mkdir -p "Staging" && cd "Staging"

curl -O "https://mediaarea.net/download/binary/libmediainfo0/${MEDIAINFO_VERSION}/MediaInfo_DLL_${MEDIAINFO_VERSION}_Mac_x86_64+arm64.tar.bz2"
curl -O "https://mediaarea.net/download/binary/libmediainfo0/${MEDIAINFO_VERSION}/MediaInfo_DLL_${MEDIAINFO_VERSION}_Windows_x64_WithoutInstaller.7z"
curl -O "https://mediaarea.net/download/binary/libmediainfo0/${MEDIAINFO_VERSION}/libmediainfo0v5_${MEDIAINFO_VERSION}-1_amd64.Debian_12.deb"
curl -O "https://mediaarea.net/download/binary/libzen0/${ZEN_VERSION}/libzen0v5_${ZEN_VERSION}-1_amd64.Debian_12.deb"

# Linux i386 is intentionally not refreshed here as current upstream MediaInfo packages do not provide a matching i386 binary.

for FILE in *.tar.* *.deb *.7z
	do mkdir -p "${FILE%.*}" && 7z x "$FILE" -aoa -o"${FILE%.*}"
done

for FILE in */*.tar
	do mkdir -p "${FILE%.*}" && 7z x "$FILE" -aoa -o"${FILE%.*}"
done


# Copy native libraries into repository
cd ..

cp Staging/*Mac*x86_64+arm64*/*/*/libmediainfo.dylib mac-x86_64/libmediainfo.dylib
cp Staging/*Windows*x64*/MediaInfo.dll win32-x64/MediaInfo.dll
cp Staging/*/data/usr/lib/x86_64-linux-gnu/libmediainfo.so.0.0.0 linux-amd64/libmediainfo.so
cp Staging/*/data/usr/lib/x86_64-linux-gnu/libzen.so.${ZEN_VERSION} linux-amd64/libzen.so

rm -r Staging
