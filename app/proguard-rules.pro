# Regras de ofuscação do app.
# Room, Hilt e Compose já trazem consumer rules; nada específico é necessário na v1.
# Manter nomes de classes de entidade é dispensável: Room resolve por anotação.

-dontwarn org.jetbrains.annotations.**
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
