#!/usr/bin/env bash
# Прогон генерации на конкретной версии: поднимает сервер, принудительно
# генерирует фиксированную область на фиксированном сиде и проверяет лог.
#
# Одинаковая область и сид на всех версиях дают сравнимые прогоны: разметка
# городов (isCity/cityFactor) обязана совпадать, потому что зависит только от
# координат чанка. Рельеф под ними — нет, ванильный шум между версиями менялся.
#
# Использование: tools/gen-test.sh 1.20.1 [радиус_в_чанках]

set -uo pipefail

VERSION="${1:?укажите версию, например 1.20.1}"
RADIUS="${2:-10}"
SEED=12345
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RUN="$ROOT/versions/$VERSION/run"
LOG="/tmp/gentest-$VERSION.log"
FIFO="/tmp/gentest-$VERSION.fifo"

if [ -z "${JAVA_HOME:-}" ]; then
    export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
fi

mkdir -p "$RUN"
echo "eula=true" > "$RUN/eula.txt"
cat > "$RUN/server.properties" <<EOF
level-seed=$SEED
online-mode=false
max-tick-time=-1
view-distance=6
spawn-protection=0
EOF

# Чистый мир: иначе уже сгенерированные чанки не будут проверены заново.
rm -rf "$RUN/world"
rm -f "$LOG" "$FIFO"
mkfifo "$FIFO"

echo "[$VERSION] генерация области $((RADIUS * 2 + 1))x$((RADIUS * 2 + 1)) чанков, сид $SEED"

cd "$ROOT" || exit 1
# Держим FIFO открытым на запись, иначе сервер увидит EOF и сразу выключится.
exec 3>"$FIFO"
timeout 1200 ./gradlew ":$VERSION:runServer" --no-daemon --console=plain < "$FIFO" > "$LOG" 2>&1 &
gradle_pid=$!

wait_for() {
    local pattern="$1" limit="$2" waited=0
    while [ "$waited" -lt "$limit" ]; do
        if grep -qE "$pattern" "$LOG" 2>/dev/null; then return 0; fi
        if ! kill -0 "$gradle_pid" 2>/dev/null; then return 1; fi
        sleep 2
        waited=$((waited + 2))
    done
    return 1
}

cleanup() {
    exec 3>&- 2>/dev/null
    kill "$gradle_pid" 2>/dev/null
    rm -f "$FIFO"
}
trap cleanup EXIT

if ! wait_for 'Done \(' 300; then
    echo "[$VERSION] сервер не поднялся, смотрите $LOG"
    grep -iE " ERROR |Exception" "$LOG" | head -10
    exit 1
fi

# forceload грузит максимум 256 чанков за раз, поэтому идём полосами по X.
for ((x = -RADIUS; x <= RADIUS; x++)); do
    echo "forceload add $((x * 16)) $((-RADIUS * 16)) $((x * 16)) $((RADIUS * 16))" >&3
    sleep 1
done

# Даём генерации догнать очередь.
sleep 25
echo "forceload remove all" >&3
sleep 2
echo "stop" >&3

wait "$gradle_pid" 2>/dev/null

# "generate() called N times" печатается не на каждый чанк — берём максимум N.
chunks=$(grep -oE "generate\(\) called [0-9]+ times" "$LOG" | grep -oE "[0-9]+" | sort -n | tail -1)
requested=$(grep -cE "Requested chunk" "$LOG")
errors=$(grep -cE " ERROR |Exception" "$LOG")
city=$(grep -cE "isCity: true" "$LOG")
nocity=$(grep -cE "isCity: false" "$LOG")

echo "[$VERSION] лог: $LOG"
echo "  чанков через фичу : ${chunks:-0}"
echo "  isCity true/false : $city / $nocity"
echo "  Requested chunk   : $requested"
echo "  ошибок/исключений : $errors"

if [ "$requested" -ne 0 ] || [ "$errors" -ne 0 ]; then
    echo
    echo "[$VERSION] ПРОВАЛ — первые проблемы:"
    grep -E "Requested chunk| ERROR |Exception" "$LOG" | head -10
    exit 1
fi

if [ "${chunks:-0}" -lt 100 ]; then
    echo
    echo "[$VERSION] ПРОВАЛ — сгенерировано слишком мало чанков, прогон не показателен"
    exit 1
fi

echo "[$VERSION] чисто"
