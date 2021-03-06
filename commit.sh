#!/usr/bin/env bash
set -e # fail script on error

# Script to commit & push
# It also lint the proiect and build it before commit

if [[ -z "$1" ]]
then
  echo "commit message mut be provided"
  exit -1;
fi

mvn clean

clocResult=$(cloc common core microservices frontend/atriangle-frontend/src testing --quiet)

mvn clean install

rm -f README.md
readmeTemplate=$(cat docs/DYNAMIC_README_TEMPLATE.md)
readme="$readmeTemplate \n \`\`\` \n $clocResult \n \`\`\`"
echo "$readme" >> README.md

echo "* $1" >> RELEASE_NOTE.md

git add .
git commit -m "$1"
git push

