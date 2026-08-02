#!/usr/bin/env bash
set -euo pipefail

# Renderiza un .puml a SVG. La fuente "Monaspace Neon Medium" (definida en
# utils/theme.puml) esta instalada en ~/.local/share/fonts -- asi PlantUML/
# Java mide el texto con las metricas reales (cajas bien dimensionadas).
# Ademas inyectamos un @font-face con la version online, para que el SVG
# tambien se vea bien en maquinas/navegadores que no tengan la fuente
# instalada (ej. el resto del equipo).

FONT_URL="https://github.com/githubnext/monaspace/raw/refs/heads/main/fonts/Static%20Fonts/Monaspace%20Neon/MonaspaceNeon-Medium.otf"
FONT_FAMILY="Monaspace Neon Medium"

if [ $# -ne 1 ]; then
    echo "Uso: $0 <archivo.puml>" >&2
    exit 1
fi

PUML_FILE="$1"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="$(dirname "$PUML_FILE")"
[ -n "$SRC_DIR" ] && [ "$SRC_DIR" != "." ] || SRC_DIR="$SCRIPT_DIR/.."

BASENAME="$(basename "$PUML_FILE" .puml)"
IMAGES_DIR="$(cd "$SRC_DIR/.." && pwd)/images"
SVG_FILE="$IMAGES_DIR/$BASENAME.svg"

plantuml -tsvg -o "$IMAGES_DIR" "$PUML_FILE"

if grep -q "$FONT_FAMILY" "$SVG_FILE"; then
    # font-weight: 100 900 -- un solo archivo (Medium) cubre cualquier peso
    # pedido (ej. los headers en 700). Sin este rango, el navegador sintetiza
    # un "faux bold" engrosando artificialmente el Medium, que se ve
    # pegado/apelmazado en los encabezados.
    FONT_FACE="<style>@font-face{font-family:'${FONT_FAMILY}';src:url('${FONT_URL}') format('opentype');font-weight:100 900;font-style:normal;}</style>"
    sed -i "s#<defs>#<defs>${FONT_FACE}#; s#<defs/>#<defs>${FONT_FACE}</defs>#" "$SVG_FILE"
    echo "Fuente '${FONT_FAMILY}' referenciada via @font-face online en $SVG_FILE"
fi
