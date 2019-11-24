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
  <version>1.1</version>
</dependency>
```

### Gradle
```
repositories {
    maven { url "https://jitpack.io" }
}

dependencies {
    testCompile 'com.github.paddan:SftpServerMockExtension:1.1'
}
```

## SftpServerMockExtension

For jUnit 5 tests in kotlin see [SftpServerMockExtensionKotlinTest](src/test/kotlin/com/github/paddan/sftpserver/SftpServerMockExtensionKotlinTest.kt)

For jUnit 5 tests in java see [SftpServerMockExtensionJavaTest](src/test/java/com/github/paddan/sftpserver/SftpServerMockExtensionJavaTest.java)
