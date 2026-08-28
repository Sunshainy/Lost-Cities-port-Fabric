#!/usr/bin/env bash
# Прогон клиента по всем версиям подряд, с сохранением лога каждой отдельно.
#
# Скрипт запускает runClient для первой версии и ждёт. Вы играете: создаёте мир,
# смотрите генерацию, проверяете кнопку Cities. Закрываете игру — лог сохраняется,
# скрипт сразу поднимает следующую версию. В конце печатает сводку по всем логам.
#
# Использование:
#   tools/test-clients.sh                      # все версии по порядку
#   tools/test-clients.sh 1.21.8 1.21.11       # только указанные

set -uo pipefail

ALL_VERSIONS=(1.20.1 1.20.4 1.20.6 1.21.1 1.21.4 1.21.8 1.21.11)
VERSIONS=("$@")
if [ ${#VERSIONS[@]} -eq 0 ]; then
    VERSIONS=("${ALL_VERSIONS[@]}")
fi

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOGDIR="$ROOT/logs/clients-$(date +%Y%m%d-%H%M%S)"
SUMMARY="$LOGDIR/СВОДКА.txt"

if [ -z "${JAVA_HOME:-}" ]; then
    export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
fi

mkdir -p "$LOGDIR"
cd "$ROOT" || exit 1

echo "Логи: $LOGDIR"
echo "Версии: ${VERSIONS[*]}"
echo
echo "Для каждой версии: создайте мир, посмотрите генерацию, проверьте кнопку"
echo "Cities на экране создания мира. Потом закройте игру — запустится следующая."
echo

# Что ищем в логах. Первая колонка — метка для сводки, дальше grep-шаблон.
# Realms-строку исключаем: на offline-аккаунте она вылезает всегда, уровень INFO,
# к моду отношения не имеет. Без этого исключения счётчик падений врал.
declare -a CHECKS=(
    "миксины|Mixin apply for mod lostcities failed"
    "выход за границы|Requested chunk"
    "ошибки|/ERROR\]"
    "падения|^(java|net\.minecraft\.util\.crash|org\.spongepowered)\.[A-Za-z.]*(Exception|Error)"
    "лут не найден|Missing loot table|Couldn't find loot table"
    "клиент поднялся|\[CLIENT\] Initializing client-side components"
)

for version in "${VERSIONS[@]}"; do
    log="$LOGDIR/client-$version.log"
    echo "=============================================="
    echo " $version — запуск клиента"
    echo "=============================================="

    mkdir -p "$ROOT/versions/$version/run"

    start=$(date +%s)
    ./gradlew ":$version:runClient" --no-daemon --console=plain > "$log" 2>&1
    status=$?
    elapsed=$(( $(date +%s) - start ))

    echo " $version — закрыт, ${elapsed}с, код выхода $status"
    echo " лог: $log"
    echo
done

# Сводка по всем логам, чтобы не открывать каждый вручную.
{
    echo "Прогон клиентов, $(date '+%Y-%m-%d %H:%M')"
    echo "Версии: ${VERSIONS[*]}"
    echo

    for version in "${VERSIONS[@]}"; do
        log="$LOGDIR/client-$version.log"
        echo "=== $version ==="
        if [ ! -s "$log" ]; then
            echo "  лога нет или он пустой"
            echo
            continue
        fi

        echo "  строк в логе: $(wc -l < "$log")"
        mcver=$(grep -oE "for Minecraft [0-9.]+" "$log" | head -1)
        echo "  мод сообщает: ${mcver:-не нашёл строку инициализации}"
        echo "  чанков через фичу: $(grep -oE "generate\(\) called [0-9]+ times" "$log" | grep -oE "[0-9]+" | sort -n | tail -1 | sed 's/^$/0/')"

        for check in "${CHECKS[@]}"; do
            label="${check%%|*}"
            pattern="${check#*|}"
            count=$(grep -cE "$pattern" "$log")
            printf "  %-16s %s\n" "$label:" "$count"
        done

        echo "  --- первые проблемные строки ---"
        grep -nE "Mixin apply for mod lostcities failed|Requested chunk|/ERROR\]|Missing loot table" "$log" \
            | head -8 | sed 's/^/    /'
        echo
    done
} > "$SUMMARY"

echo "=============================================="
cat "$SUMMARY"
echo "=============================================="
echo "Сводка: $SUMMARY"
