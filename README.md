# Travail Pratique #3: Stéganographie

## Auteurs
 - LEGV17030400, Le Gall, Valentin
 - GIRB04080500, Girard, Benoit

## Compatibilité

Java - 21

## Utilisation

### Prérequis

- Un JDK 21, par exemple [Eclipse Temurin 21 (Adoptium)](https://adoptium.net/temurin/releases/?version=21).
- Les commandes `java`, `javac` et `jar` doivent être accessibles dans le `PATH` ; `java -version` doit afficher une version 21.

Le programme n'a aucune autre dépendance : il n'utilise que la bibliothèque standard du JDK (`javax.imageio`, `java.awt.image`, `javax.crypto`, `java.util.zip`).

### Compilation

Les commandes sont à lancer depuis la racine du projet, c'est-à-dire le dossier qui contient `src/`. Elles sont identiques sous Windows (cmd ou PowerShell), macOS et Linux (Terminal) :

```
javac --release 21 -encoding UTF-8 -Xlint:all -d out --source-path src src/stegano/Main.java
jar --create --file stegano.jar --main-class stegano.Main -C out .
```

`javac` compile `Main.java` et, grâce à `--source-path`, toutes les classes dont il dépend : aucun joker (`*.java`) n'est nécessaire, ce qui évite les différences entre shells. La compilation ne produit aucun avertissement.

### Lancement

Sous Windows, macOS et Linux :

```
java -jar stegano.jar hide -i <input.png> -o <output.png> -s "your secret string"
java -jar stegano.jar reveal <image.png>
```

Les options de `hide` peuvent apparaître dans n'importe quel ordre. Les commandes complètes, avec les bonus décrits plus bas, sont :

```
java -jar stegano.jar hide -i <input.png> -o <output.png> -s "secret" [-p <password>]
java -jar stegano.jar reveal <image.png> [-p <password>]
java -jar stegano.jar capacity <image.png>
java -jar stegano.jar help
```

Sous macOS et Linux, un secret contenant `!`, `$` ou `` ` `` doit être placé entre apostrophes (`-s 'Hello!'`) pour que le shell ne l'interprète pas.

### Exemples

Cacher un secret, puis le révéler :

```
java -jar stegano.jar hide -i input/rgb.png -o output/rgb.png -s "Rendez-vous à 18 h devant le pavillon principal."
--> secret successfully hidden in "output/rgb.png"

java -jar stegano.jar reveal output/rgb.png
--> secret message is: "Rendez-vous à 18 h devant le pavillon principal."
```

Secret trop long pour l'image :

```
java -jar stegano.jar hide -i input/tiny.png -o output/tiny.png -s "trop long"
--> /!\ The secret string exceeds the maximum length supported by the image.
```

Avec un mot de passe (bonus) :

```
java -jar stegano.jar hide -i input/rgb.png -o output/rgb-pw.png -s "Chiffré : Rendez-vous à 18 h devant le pavillon principal." -p uqac-8INF138
--> secret successfully hidden in "output/rgb-pw.png"

java -jar stegano.jar reveal output/rgb-pw.png -p uqac-8INF138
--> secret message is: "Chiffré : Rendez-vous à 18 h devant le pavillon principal."

java -jar stegano.jar reveal output/rgb-pw.png
--> /!\ This secret is password-protected: use -p <password> to reveal it.

java -jar stegano.jar reveal output/rgb-pw.png -p mauvais
--> /!\ Incorrect password or corrupted data.
```

Capacité d'une image (bonus) :

```
java -jar stegano.jar capacity input/rgb.png
--> maximum secret length without password: 28787 bytes (UTF-8)
--> maximum secret length with password: 28743 bytes (UTF-8)
```

Erreurs d'utilisation :

```
java -jar stegano.jar reveal
--> /!\ Missing argument: an image file is expected.
Usage:
  java -jar stegano.jar hide -i <input.png> -o <output.png> -s "secret" [-p <password>]
  ...

java -jar stegano.jar hide -i input/rgb.png -o output/rgb.jpg -s "secret"
--> /!\ The output file must have a .png extension: a lossy format would destroy the hidden bits.
```

### Codes de sortie et sorties

| Code | Signification |
|---|---|
| `0` | succès |
| `1` | erreur d'exécution : fichier introuvable ou qui n'est pas un PNG, dossier de sortie inexistant, secret trop long, aucun secret, données corrompues, mot de passe manquant ou incorrect… |
| `2` | mauvaise utilisation : commande ou option inconnue, argument manquant ou en double, secret ou mot de passe vide, sortie autre que `.png` |

Les résultats sont écrits sur la sortie standard ; les erreurs, les avertissements et l'aide, sur la sortie d'erreur. Aucune erreur n'affiche de trace de pile.

## Images fournies

Le dossier `input/` contient des images générées couvrant les différents types de PNG ; `output/` contient les images produites par le programme à partir de celles-ci.

| Image d'entrée (`input/`) | Type PNG |
|---|---|
| `rgb.png` | RGB 8 bits |
| `rgba.png` | RGBA 8 bits, dont une bande de pixels totalement transparents |
| `palette.png` | palette (256 couleurs) |
| `palette-transparent.png` | palette avec transparence (`tRNS`) |
| `gray.png` | niveaux de gris 8 bits |
| `gray16.png` | niveaux de gris 16 bits |
| `rgb16.png` | RGB 16 bits |
| `rgba16.png` | RGBA 16 bits |
| `small.png` | RGB 20 × 10 : capacité de 62 octets sans mot de passe, 18 avec (limites exactes) |
| `tiny.png` | RGB 4 × 4 : capacité nulle |

Secrets cachés dans `output/`, pour vérification :

| Image de sortie | Image source | Secret caché | Mot de passe |
|---|---|---|---|
| `output/rgb.png` | `input/rgb.png` | Rendez-vous à 18 h devant le pavillon principal. | — |
| `output/rgb-pw.png` | `input/rgb.png` | Chiffré : Rendez-vous à 18 h devant le pavillon principal. | `uqac-8INF138` |
| `output/rgba.png` | `input/rgba.png` | La transparence est préservée : l'alpha ne bouge pas ✅ | — |
| `output/rgba-pw.png` | `input/rgba.png` | Chiffré : La transparence est préservée : l'alpha ne bouge pas ✅ | `uqac-8INF138` |
| `output/palette.png` | `input/palette.png` | Image en palette convertie en RGB 🎨 | — |
| `output/palette-pw.png` | `input/palette.png` | Chiffré : Image en palette convertie en RGB 🎨 | `uqac-8INF138` |
| `output/palette-transparent.png` | `input/palette-transparent.png` | Palette avec transparence (tRNS) 👻 | — |
| `output/palette-transparent-pw.png` | `input/palette-transparent.png` | Chiffré : Palette avec transparence (tRNS) 👻 | `uqac-8INF138` |
| `output/gray.png` | `input/gray.png` | Niveaux de gris → couleur, sans perte du secret | — |
| `output/gray-pw.png` | `input/gray.png` | Chiffré : Niveaux de gris → couleur, sans perte du secret | `uqac-8INF138` |
| `output/gray16.png` | `input/gray16.png` | 16 bits ramenés à 8 bits par composante | — |
| `output/gray16-pw.png` | `input/gray16.png` | Chiffré : 16 bits ramenés à 8 bits par composante | `uqac-8INF138` |
| `output/rgb16.png` | `input/rgb16.png` | Hello, world! 🌍 Ça marche aussi en RGB 16 bits | — |
| `output/rgb16-pw.png` | `input/rgb16.png` | Chiffré : Hello, world! 🌍 Ça marche aussi en RGB 16 bits | `uqac-8INF138` |
| `output/rgba16.png` | `input/rgba16.png` | RGBA 16 bits : « ceci n'est pas un exercice » 🚀 | — |
| `output/rgba16-pw.png` | `input/rgba16.png` | Chiffré : RGBA 16 bits : « ceci n'est pas un exercice » 🚀 | `uqac-8INF138` |

Les images produites sont toujours des PNG 8 bits par composante, en RGB, ou en RGBA si l'image source a un canal alpha. Elles s'ouvrent dans n'importe quelle visionneuse et ont les mêmes dimensions que l'image source. Seuls les bits de poids faible de R, G et B diffèrent de l'image source normalisée ; l'alpha est identique.

## Bonus

Sans option supplémentaire, le comportement est exactement celui de l'énoncé. Les fonctionnalités suivantes ont été ajoutées.

### Chiffrement : option `-p <password>` de `hide` et `reveal`

```
java -jar stegano.jar hide -i <input.png> -o <output.png> -s "secret" -p <password>
java -jar stegano.jar reveal <image.png> -p <password>
```

- Le secret est chiffré et authentifié avec **AES-256-GCM** (nonce de 12 octets, tag de 16 octets).
- La clé est dérivée du mot de passe par **PBKDF2-HMAC-SHA256** avec **600 000 itérations** (recommandation OWASP) et un **sel aléatoire de 16 octets**. Le nombre d'itérations est une constante liée à la version 1 du format.
- PBKDF2 produit 64 octets : les 32 premiers forment la clé AES, les 32 suivants la graine de la dispersion.
- Le sel et le nonce sont tirés avec `SecureRandom` : cacher deux fois le même secret avec le même mot de passe produit deux images différentes.
- La signature, la version et les flags de l'en-tête sont authentifiés comme données associées (AAD) : les modifier fait échouer le déchiffrement.
- `reveal` sans `-p` sur une image chiffrée échoue avec un message demandant le mot de passe ; un mauvais mot de passe donne `Incorrect password or corrupted data.`
- `reveal -p` sur une image **non** chiffrée révèle quand même le secret et affiche un avertissement sur la sortie d'erreur.

### Dispersion : activée par `-p`

Avec un mot de passe, les bits de la charge utile (hormis le sel) ne sont pas écrits à la suite, mais répartis sur toute l'image dans un ordre pseudo-aléatoire qui dépend du mot de passe et du sel. Le détail de l'algorithme figure dans la section [Format](#format-de-stockage).

### Intégrité : CRC32

L'en-tête contient le CRC32 de la charge utile, vérifié à chaque `reveal` : une image abîmée (recompression, retouche…) est signalée par `Corrupted data: the checksum does not match.` au lieu d'afficher un secret erroné.

### Capacité : commande `capacity <image.png>`

```
java -jar stegano.jar capacity <image.png>
```

Affiche la taille maximale du secret, en octets après encodage UTF-8, sans et avec mot de passe. Un caractère accentué occupe 2 octets, un emoji 4.

### Autres ajouts

- Commande `help` (ou `-h`, `--help`) qui affiche l'aide.
- Prise en charge de tous les types de PNG en entrée (palette, niveaux de gris, 16 bits, avec ou sans alpha), par normalisation.
- Échappement des caractères de contrôle à l'affichage du secret (voir les notes de sécurité).

## Format de stockage

### Emplacements

Chaque pixel offre trois emplacements d'un bit : les bits de poids faible de R, G et B. L'emplacement `s` est la composante `s mod 3` (0 = R, 1 = G, 2 = B) du pixel `s div 3`, les pixels étant numérotés ligne par ligne à partir du coin supérieur gauche. Une image de L × H pixels offre donc `3 × L × H` bits. Chaque octet est écrit bit de poids fort d'abord. Le canal alpha n'est jamais modifié, et les emplacements inutilisés gardent leur valeur d'origine.

Avant toute lecture ou écriture, l'image est copiée en RGB 8 bits (`TYPE_INT_RGB`), ou en ARGB 8 bits (`TYPE_INT_ARGB`) si elle a un canal alpha. Les bits ne sont jamais écrits directement dans une image en palette ou en niveaux de gris, où ils seraient perdus. La copie conserve la couleur des pixels totalement transparents.

### En-tête (13 octets, toujours en clair et séquentiel)

| Champ | Taille | Contenu |
|---|---|---|
| Signature | 3 octets | `STG` |
| Version | 1 octet | `1` |
| Flags | 1 octet | bit 0 : chiffré, bit 1 : dispersé ; les autres bits sont réservés et valent 0 |
| Longueur | 4 octets, big-endian | taille de la charge utile en octets |
| CRC32 | 4 octets, big-endian | CRC32 de la charge utile |

### Charge utile

- **Sans mot de passe** (flags = `0`) : le secret encodé en UTF-8, écrit séquentiellement juste après l'en-tête.
- **Avec mot de passe** (flags = `3`, chiffré et dispersé) : sel (16 octets) + nonce (12 octets) + texte chiffré + tag GCM (16 octets).
  - Le sel est écrit en clair, séquentiellement, juste après l'en-tête (emplacements 104 à 231), car l'ordre de dispersion en dépend.
  - Le reste (nonce, texte chiffré, tag) est réparti sur les emplacements `[232, 3 × L × H)` dans l'ordre de dispersion.

La capacité est donc de `⌊3 × L × H / 8⌋ − 13` octets sans mot de passe, et de 44 octets de moins avec mot de passe.

### Ordre de dispersion

L'ordre est un mélange de **Fisher-Yates** des emplacements `[232, N)`, piloté par un générateur déterministe, identique sur toutes les plateformes et toutes les JVM :

1. **Générateur** : le flux d'octets est la concaténation des blocs `HMAC-SHA256(graine, uint64_be(k))` pour `k = 0, 1, 2…`, où la graine est formée des 32 derniers octets dérivés par PBKDF2.
2. **Tirage d'un entier uniforme dans `[0, n)`** : on lit les 8 octets suivants du flux comme un entier non signé big-endian `r`, et on retient `r mod n`. Le tirage est rejeté puis recommencé lorsque `r` tombe dans le dernier intervalle incomplet de taille `n` sous 2⁶⁴, ce qui supprime le biais du modulo.
3. **Mélange** : on considère le tableau virtuel `a = [232, 233, …, N − 1]` de taille `n = N − 232`. À l'étape `i = 0, 1, 2…`, on tire `j` uniforme dans `[i, n)` (soit `i` + un tirage dans `[0, n − i)`), on échange `a[i]` et `a[j]`, et `a[i]` est l'emplacement suivant.

Le mélange est **partiel et paresseux** : on ne calcule que les `k` emplacements nécessaires, et seules les cases déplacées du tableau virtuel sont mémorisées (table de hachage). La mémoire est en O(k), quelle que soit la taille de l'image. Comme le secret est un argument de la ligne de commande, `k` reste modeste. À titre indicatif, cacher un secret de 98 Ko avec mot de passe dans une image de 24 mégapixels prend environ 2 s.

### Validation au `reveal`

| Situation | Message |
|---|---|
| image trop petite ou signature absente | `No hidden secret was found in this image.` |
| version inconnue | `Unsupported format: unknown version N.` |
| flags réservés non nuls | `Unsupported format: reserved flags are set.` |
| dispersé sans être chiffré | `Unsupported format: a scattered secret must be encrypted.` |
| longueur incohérente avec la capacité de l'image | `Invalid data: the declared length is inconsistent with the image capacity.` |
| CRC incorrect (sans mot de passe) | `Corrupted data: the checksum does not match.` |
| secret chiffré mais pas de `-p` | `This secret is password-protected: use -p <password> to reveal it.` |
| CRC incorrect ou échec de l'authentification GCM (avec mot de passe) | `Incorrect password or corrupted data.` |

Avec un mot de passe, un CRC incorrect est signalé comme l'échec de GCM. En effet, un mauvais mot de passe produit un mauvais ordre de dispersion, donc des octets erronés et un CRC faux avant même le déchiffrement. Les deux causes sont alors indiscernables.

## Notes de sécurité

- **La signature trahit la présence d'un secret.** L'en-tête `STG` en clair permet à quiconque connaît le format de savoir qu'une image contient un secret. C'est un compromis assumé, en échange de messages d'erreur clairs (« aucun secret », « format non supporté », « données corrompues »).
- **La substitution LSB reste détectable.** Des méthodes de stéganalyse statistique (test du χ², analyse RS…) peuvent repérer la modification des bits de poids faible, même lorsque la charge utile est dispersée et chiffrée.
- **Le chiffrement protège le contenu, pas la présence du secret.** Sans le mot de passe, on ne peut ni lire ni modifier le secret sans que ce soit détecté, mais on sait qu'il existe.
- **Le mot de passe est visible.** Passé en argument (`-p`), il reste dans l'historique du shell et apparaît dans la liste des processus (`ps`) pendant l'exécution. L'énoncé interdisant toute saisie interactive, il n'y a pas d'autre moyen de le fournir. Pour éviter l'historique, on peut par exemple précéder la commande d'une espace sous bash (avec `HISTCONTROL=ignorespace`).
- **Affichage sûr.** Un secret lu dans une image non fiable pourrait contenir des séquences d'échappement capables de manipuler le terminal. Les caractères de contrôle (sauf saut de ligne et tabulation) sont donc affichés sous la forme `\uXXXX`.

## Guillemets et encodage

### Guillemets droits

L'énoncé montre des guillemets typographiques (`“…”`), issus de la correction automatique de Word. Le programme affiche des guillemets droits : `--> secret message is: "your secret string"`.

### UTF-8 sous Windows

Les secrets peuvent contenir des accents et des emojis : ils sont encodés en UTF-8 dans l'image. Sous macOS et Linux, la console est en UTF-8 et tout fonctionne directement. Sous Windows, la console utilise une page de code héritée :

- **Affichage.** Java 21 suit l'encodage de la console. Pour afficher correctement accents et emojis, passer la console en UTF-8 avant de lancer le programme :
  - dans cmd : `chcp 65001` ;
  - dans PowerShell : `[Console]::OutputEncoding = [System.Text.Encoding]::UTF8`.

  On peut aussi forcer l'encodage de sortie : `java -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -jar stegano.jar reveal output/rgba.png`. Une police comprenant les emojis (Windows Terminal) est nécessaire pour les voir.
- **Arguments.** Le lanceur `java.exe` convertit les arguments avec la page de code ANSI du système (Windows-1252 en français). Les accents usuels passent, mais les caractères absents de cette page de code, dont les emojis, deviennent `?`. Pour les passer en argument (`-s`, `-p`), il faut activer l'option Windows « Utiliser Unicode UTF-8 pour la prise en charge des langues du monde entier » (Paramètres régionaux → Administration → Modifier les paramètres régionaux du système), puis redémarrer. Révéler un secret contenant des emojis ne pose pas ce problème : seul l'affichage est concerné (point précédent).
