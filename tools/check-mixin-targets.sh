#!/usr/bin/env bash
# Проверка целей миксинов в маппингах конкретной версии Minecraft.
#
# Миксины не проверяются компилятором: несуществующий метод обнаружится только
# в рантайме, а клиентские миксины серверный прогон вообще не затрагивает.
# Этот скрипт смотрит прямо в remapped-джарник Minecraft, который Loom положил
# в кэш, и проверяет, что цель на месте.
#
# Использование: tools/check-mixin-targets.sh 1.20.1 [1.20.4 ...]
# Джарники появляются в кэше после первой сборки соответствующей версии.

set -uo pipefail

CACHE="$HOME/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft"

# Цели в формате "класс метод". Список должен совпадать с миксинами в
# src/main/resources/lostcity.mixins.json и src/client/resources/*.mixins.json
TARGETS=(
    "net/minecraft/client/gui/screen/world/CreateWorldScreen init"
    "net/minecraft/client/gui/screen/Screen addDrawableChild"
)

fail=0

for version in "$@"; do
    jar=$(find "$CACHE" -name "minecraft-clientonly-$version-*.jar" 2>/dev/null | head -1)
    merged=$(find "$CACHE" -name "minecraft-merged-$version-*.jar" -o -name "minecraft-common-$version-*.jar" 2>/dev/null | head -1)

    if [ -z "$jar" ] && [ -z "$merged" ]; then
        echo "[$version] ПРОПУСК: remapped-джарник не найден, сначала соберите эту версию"
        continue
    fi

    cp="${jar}:${merged}"
    echo "[$version]"

    for target in "${TARGETS[@]}"; do
        class="${target% *}"
        method="${target#* }"
        if javap -cp "$cp" "${class//\//.}" 2>/dev/null | grep -q "[ .]${method}("; then
            echo "  OK      ${class##*/}.${method}"
        else
            echo "  ОШИБКА  ${class##*/}.${method} — не найден"
            fail=1
        fi
    done
done

if [ "$fail" -ne 0 ]; then
    echo
    echo "Часть миксинов не найдёт свою цель в рантайме. Правьте миксин под эту версию."
    exit 1
fi
