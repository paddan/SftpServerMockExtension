# SftpServerMockExtension

## Description
A jUnit 5 mock extension for a in-memory sft server written in kotlin.

## Maven and gradle usage

### Maven
```
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependency>
  <groupId>com.github.paddan</groupId>
  <artifactId>SftpServerMockExtension</artifactId>
  <version>1.0</version>
</dependency>
```

### Gradle
```
repositories {
    maven { url "https://jitpack.io" }
}

dependencies {
    testCompile 'com.github.paddan:SftpServerMockExtension:1.0'
}
```

## SftpServerMockExtension

For a jUnit 5 test in kotlin see [SftpServerMockExtensionTest](src/test/kotlin/com/github/paddan/sftpserver/SftpServerMockExtensionTest.kt)
