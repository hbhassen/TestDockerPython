#!/bin/bash
# ==============================================
# Script : scan_poms.sh
# Objectif :
#   - Lister les projets GitLab contenant un pom.xml
#   - Récupérer la branche la plus récente
#   - Extraire le parent <artifactId> ou version Java
# Exigences :
#   - bash 5+
#   - jq (pour JSON)
#   - curl
# ==============================================

# ---------- CONFIGURATION ----------
GITLAB_URL="https://gitlab.example.com"
API_TOKEN="YOUR_PERSONAL_ACCESS_TOKEN"
OUTPUT_FILE="resultats_poms.csv"

# ---------- EN-TÊTE ----------
echo "Projet;Branche;ParentArtifactId;JavaVersion" > "$OUTPUT_FILE"

# ---------- 1. Lister tous les projets ----------
projects=$(curl -s --header "PRIVATE-TOKEN: $API_TOKEN" "$GITLAB_URL/api/v4/projects?per_page=100" | jq -r '.[].path_with_namespace')

for project in $projects; do
  echo "🔍 Vérification du projet: $project"

  # ---------- 2. Vérifier s’il contient un pom.xml ----------
  file_url="$GITLAB_URL/api/v4/projects/$(urlencode "$project")/repository/tree?per_page=100"
  has_pom=$(curl -s --header "PRIVATE-TOKEN: $API_TOKEN" "$file_url" | jq -r '.[] | select(.path=="pom.xml") | .path')

  if [ -z "$has_pom" ]; then
    echo "   ➤ Aucun pom.xml trouvé, on passe."
    continue
  fi

  # ---------- 3. Trouver la branche la plus récente ----------
  branches=$(curl -s --header "PRIVATE-TOKEN: $API_TOKEN" "$GITLAB_URL/api/v4/projects/$(urlencode "$project")/repository/branches?per_page=100")
  latest_branch=$(echo "$branches" | jq -r 'sort_by(.commit.created_at) | reverse | .[0].name')

  if [ -z "$latest_branch" ]; then
    echo "   ⚠️ Aucune branche trouvée, on passe."
    continue
  fi

  echo "   ➤ Branche récente : $latest_branch"

  # ---------- 4. Télécharger le contenu du pom.xml ----------
  pom_content=$(curl -s --header "PRIVATE-TOKEN: $API_TOKEN" \
    "$GITLAB_URL/api/v4/projects/$(urlencode "$project")/repository/files/pom.xml/raw?ref=$latest_branch")

  if [ -z "$pom_content" ]; then
    echo "   ⚠️ pom.xml introuvable dans la branche $latest_branch"
    continue
  fi

  # ---------- 5. Extraire le parent ou la version Java ----------
  parent_artifact=$(echo "$pom_content" | grep -A3 "<parent>" | grep "<artifactId>" | sed -E 's/.*<artifactId>(.*)<\/artifactId>.*/\1/' | head -n 1)
  java_version=$(echo "$pom_content" | grep -E "<java\.version>|<maven\.compiler\.source>" | sed -E 's/.*>(.*)<.*/\1/' | head -n 1)

  # ---------- 6. Enregistrer résultat ----------
  echo "$project;$latest_branch;${parent_artifact:-N/A};${java_version:-N/A}" >> "$OUTPUT_FILE"

done

echo "✅ Résultats enregistrés dans $OUTPUT_FILE"

# ---------- Fonction d’encodage URL ----------
urlencode() {
  local length="${#1}"
  for (( i = 0; i < length; i++ )); do
    local c="${1:i:1}"
    case $c in
      [a-zA-Z0-9.~_-]) printf "%s" "$c" ;;
      *) printf "%%%02X" "'$c" ;;
    esac
  done
}