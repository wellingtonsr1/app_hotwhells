# Quickstart — validar o editor de fotografias

Pré-requisitos: `JAVA_HOME` com JDK 17 e Android SDK em `.tools/android-sdk` (já configurado em `local.properties`).

```bash
export JAVA_HOME="$PWD/.tools/jdk-17"
./gradlew :app:assembleDebug            # compilação
./gradlew :core:data:testDebugUnitTest  # banco, repositório, backup
./gradlew :core:domain:test
./gradlew :app:testDebugUnitTest        # estado do editor
./gradlew :app:connectedDebugAndroidTest # UI (requer emulador/aparelho)
```

## Roteiro manual

1. Abra um exemplar já cadastrado → seção **Fotografias** → toque em **Câmera**.
2. Tire a foto. A revisão deve mostrar a imagem e quatro ações.
3. **Salvar sem editar**: a miniatura aparece na categoria selecionada; nenhum editor abre.
4. Repita e toque em **Editar fotografia**:
   - escolha 1:1, arraste a área de corte, gire 90°, espelhe, incline 5°;
   - ajuste brilho, contraste, saturação e nitidez;
   - segure **Comparar** para ver o original e solte;
   - use **Desfazer** duas vezes e **Refazer** uma;
   - toque em **Restaurar** e confirme que a prévia volta ao original;
   - aplique novamente um ajuste e toque em **Salvar**.
5. Confirme: miniatura atualizada, mesma categoria, mesma contagem de fotos, estrela de principal inalterada.
6. Toque na foto e escolha **Editar** de novo: o editor deve abrir a partir do **original** (sem ajustes aplicados) e mostrar que já existe uma versão editada.
7. Escolha **Restaurar original** e confirme: a imagem volta à original e a derivada é removida (`filesDir/images/<exemplarId>/` passa a ter apenas o arquivo original).
8. Selecione **Galeria**, escolha uma imagem e confirme a abertura automática do editor; verifique que o arquivo da galeria não foi alterado.
9. Abra o gerenciador de arquivos do app (`filesDir/images/<exemplarId>/`) e confirme no máximo dois arquivos por foto editada.
10. Faça **Backup** em Ajustes, edite uma foto, gere outro backup, restaure o segundo e confirme que original e editada voltam.
11. Gire a tela com o editor aberto; os ajustes devem permanecer.
12. Cancele o editor com alterações: deve pedir confirmação; ao confirmar, nenhum arquivo novo permanece.
