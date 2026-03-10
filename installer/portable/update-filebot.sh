#!/bin/sh
PRG="$0"

# resolve relative symlinks
while [ -h "$PRG" ]; do
	ls=`ls -ld "$PRG"`
	link=`expr "$ls" : '.*-> \(.*\)$'`
	if expr "$link" : '/.*' > /dev/null; then
		PRG="$link"
	else
		PRG="`dirname "$PRG"`/$link"
	fi
done

# make it fully qualified
WORKING_DIR=`pwd`
PRG_DIR=`dirname "$PRG"`
APP_ROOT=`cd "$PRG_DIR" && pwd`

# restore original working dir
cd "$WORKING_DIR"


# update core application files
PACKAGE_NAME="openfilebot.jar.xz.gpg"
PACKAGE_FILE="$APP_ROOT/$PACKAGE_NAME"
PACKAGE_URL="https://github.com/masterxq/openfilebot/releases/latest/download/$PACKAGE_NAME"

echo "Update $PACKAGE_FILE"
HTTP_CODE=`curl -L -o "$PACKAGE_FILE" -z "$PACKAGE_FILE" --retry 5 "$PACKAGE_URL" -w "%{http_code}"`

if [ $HTTP_CODE -ne 200 ]; then
	echo "$HTTP_CODE NO UPDATE"
	exit 1
fi


# initialize gpg
GPG_HOME="$APP_ROOT/data/.gpg"
JAR_XZ_FILE="$APP_ROOT/openfilebot.jar.xz"

if [ ! -d "$GPG_HOME" ]; then
	mkdir -p "$GPG_HOME" && chmod 700 "$GPG_HOME" && gpg --homedir "$GPG_HOME" --import "$APP_ROOT/maintainer.pub"
fi

# verify signature and extract jar
gpg --batch --yes --homedir "$GPG_HOME" --trusted-key "C0D3530B9A5417B5" --output "$JAR_XZ_FILE" --decrypt "$PACKAGE_FILE" && xz --decompress --force "$JAR_XZ_FILE"
