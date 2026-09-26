# Tasks : remove-screensaver

## 1. Retrait

- [x] 1.1 Vérifier qu'aucun code ni aucune autre spec ne référence le screensaver (`git grep -i -E 'screensaver|économiseur|dream'`)
- [x] 1.2 Retirer la ligne P3 de la roadmap du README
- [x] 1.3 `openspec validate --all --strict` vert, puis `openspec archive remove-screensaver`
