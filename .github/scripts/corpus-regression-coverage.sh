#!/usr/bin/env bash
set -euo pipefail

baseline_classes="$1"
current_classes="$2"
fixture_root="decompilation-test/test-data"
report_root="target/corpus-regression"
agent_jar="$HOME/.m2/repository/org/jacoco/org.jacoco.agent/0.8.16/org.jacoco.agent-0.8.16-runtime.jar"
execution_data="$PWD/target/jacoco.exec"

if [[ ! -f "$agent_jar" ]]; then
    echo "JaCoCo agent not found: $agent_jar" >&2
    exit 1
fi

if [[ ! -d "$fixture_root/precompiled_tests" || ! -d "$fixture_root/hardcoded" ]]; then
    echo "The recursively checked-out CFR test fixtures are missing." >&2
    exit 1
fi

mkdir -p "$report_root"
total_classes=0
total_variants=0

compare_fixture_set() {
    local name="$1"
    local source_dir="$2"
    local baseline_output="$report_root/baseline/$name"
    local current_output="$report_root/current/$name"
    local baseline_log="$report_root/$name.baseline.log"
    local current_log="$report_root/$name.current.log"
    local comparison_log="$report_root/$name.diff"
    local class_file
    local -a class_files=()

    while IFS= read -r -d '' class_file; do
        class_files+=("$class_file")
    done < <(find "$source_dir" -type f -name '*.class' -print0 | sort -z)

    if (( ${#class_files[@]} == 0 )); then
        echo "No compiled classes found in $source_dir" >&2
        exit 1
    fi

    echo "Comparing $name: ${#class_files[@]} class files"
    mkdir -p "$baseline_output" "$current_output"

    java -Xmx2g -cp "$baseline_classes" org.benf.cfr.reader.Main \
        "${class_files[@]}" --showversion false --dumpexceptionstacktrace false \
        --renameillegalidents true --outputdir "$baseline_output" > "$baseline_log" 2>&1

    java -Xmx2g -javaagent:"$agent_jar=destfile=$execution_data,append=true" \
        -cp "$current_classes" org.benf.cfr.reader.Main \
        "${class_files[@]}" --showversion false --dumpexceptionstacktrace false \
        --renameillegalidents true --outputdir "$current_output" > "$current_log" 2>&1

    if ! find "$current_output" -type f -name '*.java' -print -quit | grep -q .; then
        echo "No decompilation output generated for $name" >&2
        exit 1
    fi

    if ! diff -qr "$baseline_output" "$current_output" > "$comparison_log"; then
        echo "Decompilation output differs from master for $name:" >&2
        head -n 40 "$comparison_log" >&2
        exit 1
    fi

    total_classes=$((total_classes + ${#class_files[@]}))
    total_variants=$((total_variants + 1))
}

for fixture_directory in "$fixture_root"/precompiled_tests/*; do
    if [[ -d "$fixture_directory" ]]; then
        compare_fixture_set "${fixture_directory##*/}" "$fixture_directory"
    fi
done

compare_fixture_set "hardcoded" "$fixture_root/hardcoded"

if (( total_variants < 2 || total_classes < 1000 )); then
    echo "Unexpectedly small decompilation corpus: $total_variants sets, $total_classes classes" >&2
    exit 1
fi

echo "Verified $total_classes compiled class files across $total_variants fixture sets against master."
