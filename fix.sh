#!/bin/bash

find . -type f -name "Kconfig*" | while read -r kconfig_file; do
    grep -E '^[[:space:]]*source[[:space:]]+"[^"]+"' "$kconfig_file" | \
    sed -E 's/^[[:space:]]*source[[:space:]]+"([^"]+)".*/\1/' | \
    while read -r source_path; do
        if [ -f "$source_path" ]; then
            dir_path=$(dirname "$source_path")
            rm -rf "$source_path"
            mkdir -p "$dir_path"
            touch "$dir_path/Kconfig"
            echo "Converted: $source_path -> $dir_path/Kconfig"
        fi
    done
done
