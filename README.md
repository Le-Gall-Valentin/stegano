# Travail Pratique #3: Stéganographie

## Auteurs
- Code permanent, nom, prénom

## Compatibilité

Java - 21

## Utilisation

### Prérequis

Un JDK 21, par exemple [Eclipse Temurin 21 (Adoptium)](https://adoptium.net/temurin/releases/?version=21). Les commandes `java`, `javac` et `jar` doivent être dans le `PATH` ; `java -version` doit afficher une version 21.

Aucune autre dépendance : le programme n'utilise que la bibliothèque standard du JDK.

### Compilation

Depuis la racine du projet (dossier contenant `src/`), les deux commandes suivantes sont identiques sous Windows (cmd ou PowerShell), macOS et Linux :

```
javac --release 21 -encoding UTF-8 -Xlint:all -d out --source-path src src/stegano/Main.java
jar --create --file stegano.jar --main-class stegano.Main -C out .
```

`javac` compile `Main.java` et, grâce à `--source-path`, toutes les classes dont il dépend : aucun joker (`*.java`) n'est nécessaire.

### Lancement

```
java -jar stegano.jar hide -i <input.png> -o <output.png> -s "your secret string"
java -jar stegano.jar reveal <image.png>
java -jar stegano.jar help
```

Les options de `hide` peuvent apparaître dans n'importe quel ordre.

### Exemples

```
java -jar stegano.jar hide -i input/rgb.png -o output/rgb.png -s "Rendez-vous à 18 h"
--> secret successfully hidden in "output/rgb.png"

java -jar stegano.jar reveal output/rgb.png
--> secret message is: "Rendez-vous à 18 h"

java -jar stegano.jar hide -i input/tiny.png -o output/tiny.png -s "trop long"
--> /!\ The secret string exceeds the maximum length supported by the image.
```

Codes de sortie : `0` en cas de succès, `1` en cas d'erreur d'exécution, `2` en cas de mauvaise utilisation. Les erreurs et l'aide sont écrites sur la sortie d'erreur.
