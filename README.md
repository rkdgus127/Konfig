# Konfig

kotlin 기반 config 파일 생성 api

[![Gradle](https://img.shields.io/badge/Gradle-9.8-blue?style=flat-square&logo=gradle&logoColor=blue)](https://gradle.org)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.5-orange?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-GPL--3.0-green?logo=gnu&logoColor=green)](https://www.gnu.org/licenses/gpl-3.0.html)
[![auto-listen](https://img.shields.io/badge/GitHub-auto-listen-white?logo=github)](https://github.com/rkdgus127/auto-listen)


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
    compileOnly(files("libs/AutoListen.jar"))
}
```

`plugin.yml`:

```yaml
name: MyPlugin
version: 1.0.0
main: example.myplugin.Main
api-version: '1.21'
depend: [Konfig,AutoListen]
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

#### auto-listen의 의존성 추가 필요
