package com.github.paddan.sftpserver

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder.newLinux
import org.apache.sshd.common.file.FileSystemFactory
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.auth.password.PasswordAuthenticator
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.FileStore
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Files.copy
import java.nio.file.Files.exists
import java.nio.file.Files.isDirectory
import java.nio.file.Files.readAllBytes
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.nio.file.WatchService
import java.nio.file.attribute.UserPrincipalLookupService
import java.nio.file.spi.FileSystemProvider
import java.util.UUID
import kotlin.random.Random
import kotlin.streams.toList

class SftpServerMockExtension(userName: String, password: String, port: Int = 0) : BeforeAllCallback, AfterAllCallback {

    private val fs = newLinux().build("MockSftpServer:${UUID.randomUUID()}")
    private val sshd = SshServer.setUpDefaultServer()

    val port: Int
        get() = sshd.port

    init {
        sshd.port = if (port == 0) Random.nextInt(2000, 65000) else port
        sshd.keyPairProvider = SimpleGeneratorHostKeyProvider()
        sshd.subsystemFactories = listOf(SftpSubsystemFactory())
        sshd.fileSystemFactory = FileSystemFactory { DoNotCloseFileSystem(fs) }
        sshd.passwordAuthenticator = PasswordAuthenticator { uname, pwd, _ ->
            uname == userName && pwd == password
        }
    }

    fun deleteAll(vararg paths: String) {
        paths.forEach { path ->
            Files.walk(fs.getPath(path))
                .sorted(reverseOrder())
                .toList()
                .dropLast(1) // This is the root dir and cannot be deleted
                .forEach(Files::delete)
        }
    }

    override fun beforeAll(context: ExtensionContext?) = sshd.start()

    override fun afterAll(context: ExtensionContext?) {
        deleteAll("/")
        sshd.stop(true)
    }

    fun createDirs(vararg paths: String) {
        for (path in paths)
            createDir(path)
    }

    fun listAll(path: String = "/") = Files.walk(fs.getPath(path))
        .sorted(reverseOrder())
        .map { it.toString() }.toList()

    fun createDir(path: String): Path = Files.createDirectories(fs.getPath(path))

    fun getFileContent(path: String, encoding: Charset = UTF_8) = String(readAllBytes(fs.getPath(path)), encoding)

    fun existsFile(path: String) = exists(fs.getPath(path)) && !isDirectory(fs.getPath(path))

    fun putFile(path: String, inputStream: InputStream) = copy(inputStream, fs.getPath(path))
}

internal class DoNotCloseFileSystem(val fileSystem: FileSystem) : FileSystem() {
    override fun provider(): FileSystemProvider = fileSystem.provider()
    override fun close() = Unit
    override fun isOpen() = fileSystem.isOpen
    override fun isReadOnly() = fileSystem.isReadOnly
    override fun getSeparator(): String = fileSystem.separator
    override fun getRootDirectories(): MutableIterable<Path> = fileSystem.rootDirectories
    override fun getFileStores(): MutableIterable<FileStore> = fileSystem.fileStores
    override fun supportedFileAttributeViews(): MutableSet<String> = fileSystem.supportedFileAttributeViews()
    override fun getPath(first: String, vararg more: String): Path = fileSystem.getPath(first, *more)
    override fun getPathMatcher(syntaxAndPattern: String): PathMatcher = fileSystem.getPathMatcher(syntaxAndPattern)
    override fun getUserPrincipalLookupService(): UserPrincipalLookupService = fileSystem.userPrincipalLookupService
    override fun newWatchService(): WatchService = fileSystem.newWatchService()
}