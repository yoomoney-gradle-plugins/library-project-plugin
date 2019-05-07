# library-project-plugin
Плагин создан для упрощения сборки существующих библиотек и разработки новых.

## Подключение
Для подключения в проект этого плагина, нужно добавить файл ```project.gradle```:
```groovy
System.setProperty("platformLibraryProjectVersion", "2.+")
System.setProperty("platformDependenciesVersion", "3.+")
System.setProperty("kotlinVersion", "1.2.61")

repositories {
    maven { url 'https://nexus.yamoney.ru/content/repositories/thirdparty/' }
    maven { url 'https://nexus.yamoney.ru/content/repositories/central/' }
    maven { url 'https://nexus.yamoney.ru/content/repositories/releases/' }
    maven { url 'https://nexus.yamoney.ru/content/repositories/jcenter.bintray.com/' }

    dependencies {
        classpath 'ru.yandex.money.gradle.plugins:yamoney-library-project-plugin:' + 
                System.getProperty("platformLibraryProjectVersion")
        classpath group: 'ru.yandex.money.platform', name: 'yamoney-libraries-dependencies', 
                version: System.getProperty("platformDependenciesVersion"), ext: 'zip'
        classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:' + 
                        System.getProperty('kotlinVersion')
    }
}
```
А в `build.gradle` добавить соответствующую секцию, чтобы конфигурационный файл выглядел подобным образом:
```groovy
buildscript {
    apply from: 'project.gradle', to: buildscript
    copy {
        from zipTree(buildscript.configurations.classpath.files.find{ it.name.contains('library-project-plugin')})
        into 'tmp'
        include 'gradle-scripts/**'
    }
}
apply from: 'tmp/gradle-scripts/_root.gradle'
/////////////////////////////////////////////

groupIdSuffix = "common"
artifactID = "yamoney-json-utils"

dependencies {
    compile 'com.fasterxml.jackson.core:jackson-annotations:2.9.0'
}
```


## Конфигурация
Задачи плагина можно настраивать.
Для этого, в `build.gradle` нужно указать имя задачи и параметры, которые вы хотите поменять. Для каждого плагина нужно указывать свою,
отдельную конфигурацию в build.gradle

Возможные конфигурации со значениями по умолчанию приведены в секции описания плагинов.

## Устройство library-plugin
В качестве агрегатора функционала выступает LibraryPlugin - в нём делаются все настройки, необходимые для нормальной работы проекта
и подключаются все дополнительные модули.

Дополнительные модули - тоже плагины, которые не видны посторонним. 
Их реализация в виде плагинов сделана для удобства, поскольку каждый модуль может настраивать корневой проект, добавляя задачи (gradle tasks) 
и возможность конфигурировать модуль из `build.gradle`

Для того, чтобы понять, как пишутся плагины, можно посмотреть [официальную документацию](https://docs.gradle.org/current/userguide/custom_plugins.html)
или книгу Gradle Beyond The Basics.

Несколько правил и рекомензаций для написания плагинов:
1. Для удобства чтения и поддержки, все плагины пишутся на Java.
1. Плагины конфигурируются с помощью `*Extension` классов - все поля этих классов должны быть `public` и не `final`.
Это позволяет переопределять эти поля в `build.gradle` так, как показано в секции выше.
1. Все классы, которые должны использоваться в gradle (`*Task`, `*Extension`, `*Plugin`) должны быть `public`, иначе возникнут ошибки на этапе выполнения задач в проекте.
1. Как можно большее число настроек должно быть со значениями по умолчанию, чтобы не заставлять пользователей плагинов указывать эти настройки вручную.

## Известные проблемы
На данный момент, для плагинов используется версия gradle 2.10, из-за чего тесты могут не проходить, если они запущены из Идеи.
Подробности:
* [https://discuss.gradle.org/t/nosuchmethoderror-in-testkit-after-2-9-2-10-transition/13505](https://discuss.gradle.org/t/nosuchmethoderror-in-testkit-after-2-9-2-10-transition/13505)
* [https://github.com/palantir/gradle-idea-test-fix](https://github.com/palantir/gradle-idea-test-fix)

# Описание плагинов

## LibraryProjectPlugin
Плагин для объединения всех остальных плагинов, не нуждается в конфигурировании.
Единственный плагин из всех в этом проекте, который виден внешним компонентам, используется для подключения всех остальных плагинов.

## ReadmePlugin
Плагин для работы с readme файлами библиотек. На данный момент плагин отвечает за публикацию readme файла на confluence.

Конфигурация и значения по умолчанию: 
```groovy
readme {
    authentication = System.getenv("CONFLUENCE_AUTH")
    confluenceRestApiUrl = "https://wiki.yamoney.ru:443/rest/api/"
    confluenceSpace = "WebPortal"
    confluenceAncestorPageId = 128657081 //https://wiki.yamoney.ru/display/WebPortal/Libraries
    prefix = "lib."
    pathToDocument = project.getProjectDir().getPath() + "/README.md"
    pageTitle = project.getName()
}
```
Описание параметров конфигурации можно найти в javadoc'ах класса ReadmePluginExtension.

## CheckChangelogPlugin
Плагин проверяет наличие описания изменений в текущем релизе в файле CHANGELOG.md.
При отсутствии вышеназванного файла проверка пропускается.

Никаких дополнительных настроек данный плагин не добавляет.

## CheckDependenciesPlugin

Плагин проверяет легитимность изменения версий используемых библиотек (как прямо, так и по транзитивным зависимостям) в проекте.

Зачастую проект может содержать большое количество повторно используемых библоиотек разных версий, найденных по транзитивным
зависимостям. Однако, при запуске приложения может быть использована только одна версия одной и той же библиотеки.
Чтобы гарантировать согласованность этой библиотеки с другими, Gradle имеет встроенный механизм решения конфликтов версий.
По умолчанию Gradle из всех версий одной и той же библиотеки выбирает самую последнюю. При таком подходе нет гарантии, что самая
новая версия библиотеки будет обратно совместима с предыдущей версией. А значит нельзя гарантировать, что такое повышение
не сломает проект.

Для того, чтобы избежать не контролируемое изменение версий, используется подход с фиксацией набор версий бибилиотек, на которых
гарантируется работа приложения.

Для фиксации используется сторонний плагин <b>IO Spring Dependency Management plugin</b>. Список фиксируемых библиотек с
версиями хранится в maven xml.pom файле. Плагин предоставляет программный доступ к этому списку.

Обратной стороной фиксации служит неконтролируемое понижение версии библиотек. Чтобы сделать этот процесс изменения версий
библиотек контролируемым сделан этот плагин.

После того, как все зависимости и версии библиотек определены, плагин выполняет проверку. Правила проверки следующие:

<ol>
<li>Если изменение версии библиотеки связано с фиксацией версии, плагин остановит билд с ошибкой.</li>
<li>Если изменение версии библиотеки не связано с фиксацией версии, то билд допускается к выполнению.</li>
</ol>

В большинстве случаев не возможно подобрать такой набор версий библиотек, который бы удовлетворил всем подключаемым библиотекам 
прямо и по транзититивным зависимостям. Поэтому плагин поддерживает введение исключение из правил. Удостоверившись, 
что более новая версия библиотеки полностью обратно совместима со старой версии, можно разрешить обновление с одной версии 
библиоетки до другой.

### Настройка разрешающих правил изменения версий библиотек

Правила исключения описываются в property файле. По умолчанию используется файл с названием <b>libraries-versions-exclusions.properties</b>
расположенный в корне проекта. Однако, плагин позволяет переопределить название и место расположение такого файла. Для этого
используется расширение плагина:

```groovy
checkDependencies {
   exclusionsRulesSources = ["Путь к файлу", "Maven артефакт"]
}
```

Помимо чтения правил из файла, плагин поддерживает способ чтения правил из файла мавен артефакта. При этом можно указывать 
полное название артефакта (группа, id артефакта и версия), так и опускать версию:

```groovy
checkDependencies {
   exclusionsRulesSources = ["ru.yandex.money.platform:platform-dependencies:",
                             "ru.yandex.money.platform:libraries-dependencies:1.0.2"]
}
```

Так же плагин разрешает использовать несколько источников файлов с правилами:

```groovy
checkDependencies {
   exclusionsRulesSources = ["my_libraries_versions_exclusions.properties", 
                             "settings/additional_libraries_versions_exclusions.properties", 
                             "ru.yandex.money.platform:platform-dependencies:"]
}
```

Для описания самих правил может использоваться одна из следующих форм записи:

```properties
org.slf4j.jul-to-slf4j = 1.7.15 -> 1.7.16
org.slf4j.jcl-over-slf4j = 1.7.7, 1.7.15 -> 1.7.16
org.slf4j.slf4j-api = 1.6.3, 1.6.4, 1.7.0, 1.7.6, 1.7.7, 1.7.10, 1.7.12, 1.7.13 -> 1.7.16
```

### Отключение конфигураций из проверки

Помимо этой настройки плагин позволяет исключить из проверки конфигурации. Для этого используется список исключения 
<b>excludedConfigurations</b>:

```groovy
checkDependencies {
    excludedConfigurations = ["testCompile", "testRuntime"]
}
```

Все перечисленные конфигурации будут исключены из проверки.

Подробнее о настройки IO Spring Dependency Management plugin описано на [официальной странице проекта](https://github.com/spring-gradle-plugins/dependency-management-plugin)

## ReleasePlugin
Делает релиз
[Подробнее](https://bitbucket.yamoney.ru/projects/BACKEND-GRADLE-PLUGINS/repos/artifact-release-plugin/browse/README.md)

## JavaModulePlugin
[Подробнее](https://bitbucket-public.yamoney.ru/projects/BACKEND-GRADLE-PLUGINS/repos/java-module-plugin/browse/README.md)