#!/bin/sh

# Open-BSD Start-Script fuer regulaeren Standalone-Betrieb.
# Jameica wird hierbei mit GUI gestartet.

cd `dirname $(readlink -f $0)`

java -Xmx256m -jar jameica-openbsd.jar $@
