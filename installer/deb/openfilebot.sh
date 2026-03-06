#!/bin/sh
APP_ROOT=/usr/share/openfilebot

if [ -z "$HOME" ]; then
	echo '$HOME must be set'
	exit 1
fi

# add APP_ROOT to LD_LIBRARY_PATH
if [ ! -z "$LD_LIBRARY_PATH" ]; then
	export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$APP_ROOT"
else
	export LD_LIBRARY_PATH="$APP_ROOT"
fi

# select application data folder
APP_DATA="$HOME/.openfilebot"

get_java_major() {
	java_cmd="$1"
	version="$($java_cmd -version 2>&1 | sed -n '1s/.* version "\([^"]*\)".*/\1/p')"
	version="${version%%-*}"
	major="$(printf '%s' "$version" | sed 's/\..*$//')"

	case "$major" in
		''|*[!0-9]*)
			return 1
			;;
		*)
			printf '%s\n' "$major"
			return 0
			;;
	esac
}

pick_java() {
	best_java=''
	best_major=0

	for java_cmd in "$JAVA_HOME/bin/java" /usr/lib/jvm/*/bin/java /usr/java/*/bin/java "$(command -v java 2>/dev/null)"; do
		if [ -x "$java_cmd" ]; then
			major="$(get_java_major "$java_cmd")" || continue

			if [ "$major" -ge 21 ] && [ "$major" -le 25 ] && [ "$major" -gt "$best_major" ]; then
				best_java="$java_cmd"
				best_major="$major"
			fi
		fi
	done

	if [ -n "$best_java" ]; then
		printf '%s\n' "$best_java"
		return 0
	fi

	return 1
}

JAVA_BIN="$(pick_java)" || {
	echo 'No compatible Java runtime found. Please install OpenJDK 21-25 or set JAVA_HOME accordingly.'
	exit 1
}

"$JAVA_BIN" -Dunixfs=false -DuseGVFS=true -DuseExtendedFileAttributes=true -DuseCreationDate=false -Djava.net.useSystemProxies=true -Djna.nosys=false -Djna.nounpack=true -Dapplication.deployment=deb -Dorg.openfilebot.gio.GVFS="$XDG_RUNTIME_DIR/gvfs" -Dapplication.dir="$APP_DATA" -Djava.io.tmpdir="$APP_DATA/temp" -Dorg.openfilebot.AcoustID.fpcalc="$APP_ROOT/fpcalc" $JAVA_OPTS $OPENFILEBOT_OPTS $FILEBOT_OPTS -jar "$APP_ROOT/openfilebot.jar" "$@"
