#!/bin/bash

INPUT_CSV="input.csv"
OUTPUT_CSV="output.csv"
TMP_DIR="temp_git_project"

# Nettoyage de sortie
echo "git_url,tag,base_image" > "$OUTPUT_CSV"

# Lire ligne par ligne en sautant l’en-tête
tail -n +2 "$INPUT_CSV" | while IFS=',' read -r git_url tag
do
    echo "➡️ Traitement : $git_url [tag: $tag]"

    rm -rf "$TMP_DIR"

    # Cloner le dépôt (sans checkout pour éviter erreur si le tag n'est pas dans HEAD)
    git clone --quiet --depth 1 --no-checkout "$git_url" "$TMP_DIR" 2>/dev/null
    if [ $? -ne 0 ]; then
        echo "❌ Échec du clonage : $git_url"
        continue
    fi

    cd "$TMP_DIR"

    # Récupérer le tag spécifié
    git fetch --tags --quiet
    git checkout --quiet "tags/$tag"
    if [ $? -ne 0 ]; then
        echo "❌ Tag $tag introuvable dans $git_url"
        cd ..
        rm -rf "$TMP_DIR"
        continue
    fi

    # Trouver un Dockerfile
    dockerfile_path=$(find . -type f -iname "Dockerfile" | head -n 1)
    if [ -z "$dockerfile_path" ]; then
        echo "⚠️ Aucun Dockerfile trouvé pour $git_url [$tag]"
        cd ..
        rm -rf "$TMP_DIR"
        continue
    fi

    # Extraire la première image de base
    base_image=$(grep -i '^FROM' "$dockerfile_path" | head -n 1 | awk '{print $2}')
    if [ -z "$base_image" ]; then
        base_image="UNKNOWN"
    fi

    cd ..
    echo "$git_url,$tag,$base_image" >> "$OUTPUT_CSV"
    echo "✅ $git_url [$tag] → $base_image"

    rm -rf "$TMP_DIR"

done

echo ""
echo "🎯 Script terminé. Résultats enregistrés dans : $OUTPUT_CSV"