# Konfig

Kotlin으로 작성된 Paper 설정 관리 플러그인

[![Gradle](https://img.shields.io/badge/Gradle-9.8-blue?style=flat-square&logo=gradle&logoColor=blue)](https://gradle.org)

[![Kotlin](https://img.shields.io/badge/Kotlin-2.5-orange?style=flat-square&logo=kotlin)](https://kotlinlang.org)

[![License](https://img.shields.io/badge/License-GPL--3.0-green?logo=gnu&logoColor=green)](https://www.gnu.org/licenses/gpl-3.0.html)


Java:

```java
public class Settings {
    @Config
    public int cooldown = 30;

    @Config
    private double damage = 10.0;

    public boolean ignored = true;
}
```

Kotlin:

```kotlin
class Settings {
    @Config
    var cooldown = 30

    @Config
    private var damage = 10.0

    var ignored = true
}
```



`build.gradle.kts`:

```kotlin
dependencies {
    compileOnly(files("libs/Konfig.jar"))
}
```

`plugin.yml`:

```yaml
name: MyPlugin
version: 1.0.0
main: example.myplugin.Main
api-version: '1.21'
depend: [Konfig]
```


## 지원 타입

- String
- int / Integer
- long / Long
- double / Double
- float / Float
- short / Short
- byte / Byte
- boolean / Boolean
- Enum
- UUID
- List
- Set
- Map