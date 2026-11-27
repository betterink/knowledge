#!/bin/sh
# Temporary systemd service: restore Xochitl independently of the SSH client.
set -eu
pen_input=${1:-none}
case "$pen_input" in
    none|evdevtablet) ;;
    *) echo 'Usage: run-on-tablet.sh [none|evdevtablet]' >&2; exit 1 ;;
esac
if test "$pen_input" = evdevtablet; then
    tablet_plugin=$(find /usr/lib -path '*/generic/*evdevtablet*.so' -print -quit)
    if test -z "$tablet_plugin"; then
        echo 'Qt evdevtablet plugin not installed; Xochitl left running. Collect device diagnostics.' >&2
        exit 1
    fi
fi
app=/home/root/rm-knowledge/current/rm-knowledge
test -x "$app" || { echo 'Deploy the tablet binary first.' >&2; exit 1; }
"$app" --probe
systemctl is-active --quiet xochitl || {
    echo 'Xochitl must be running before starting rm-knowledge.' >&2; exit 1;
}
if systemctl is-active --quiet rm-knowledge.service; then
    echo 'rm-knowledge is already running.' >&2; exit 1
fi
# Refuse display takeover if the required platform/scenegraph plugins are absent.
platform_plugin=$(find /usr/lib -path '*/platforms/*epaper*.so' -print -quit)
scenegraph_plugin=$(find /usr/lib -path '*/scenegraph/*epaper*.so' -print -quit)
if test -z "$platform_plugin" || test -z "$scenegraph_plugin"; then
    echo 'Standalone epaper platform or scenegraph plugin missing; Xochitl left running.' >&2
    exit 1
fi
systemctl_path=$(command -v systemctl)
systemctl reset-failed rm-knowledge.service 2>/dev/null || true
set --
if test -n "${RM_KNOWLEDGE_SERVER:-}"; then
    set -- --setenv=RM_KNOWLEDGE_SERVER="$RM_KNOWLEDGE_SERVER"
fi
if test "$pen_input" = evdevtablet; then
    set -- "$@" --setenv=QT_QPA_GENERIC_PLUGINS=evdevtablet --setenv=QT_LOGGING_RULES=qt.qpa.input=true
fi
systemd-run --collect --unit=rm-knowledge \
    --property=Type=exec \
    --property="ExecStartPre=$systemctl_path stop xochitl" \
    --property="ExecStopPost=$systemctl_path start xochitl" \
    --property=RuntimeMaxSec=600 \
    --property=TimeoutStopSec=10 \
    --property=TimeoutStartSec=30 \
    --property=KillMode=control-group \
    --setenv=QT_QUICK_BACKEND=epaper \
    --setenv=QT_QPA_PLATFORM=epaper \
    --setenv=QML_DISABLE_DISK_CACHE=1 \
    --setenv=LD_PRELOAD= \
    --setenv=HOME=/home/root \
    "$@" \
    "$app" --fullscreen -platform epaper
echo 'rm-knowledge started. Exit on-screen, or: systemctl stop rm-knowledge.service'
echo 'Automatic exit after 10 minutes. Logs: journalctl -u rm-knowledge.service'
