package com.github.paddan.sftpserver

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder.newLinux
import org.apache.sshd.common.file.FileSystemFactory
import org.apache.sshd.common.session.SessionContext
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.auth.password.PasswordAuthenticator
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.sftp.server.SftpSubsystemFactory
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
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.nio.file.WatchService
import java.nio.file.attribute.UserPrincipalLookupService
import java.nio.file.spi.FileSystemProvider
import java.util.UUID
import kotlin.random.Random

class SftpServerMockExtension(userName: String, password: String, port: Int = 0) : BeforeAllCallback, AfterAllCallback {

    private val fileSystem = newLinux().build("MockSftpServer:${UUID.randomUUID()}")
    private val sshServer = SshServer.setUpDefaultServer()

    val port: Int
        get() = sshServer.port

    init {
        sshServer.port = if (port == 0) Random.nextInt(2000, 65000) else port
        sshServer.keyPairProvider = SimpleGeneratorHostKeyProvider()
        sshServer.subsystemFactories = listOf(SftpSubsystemFactory())
        sshServer.fileSystemFactory = DoNotCloseFileSystemFactory(fileSystem)
        sshServer.passwordAuthenticator = PasswordAuthenticator { uname, pwd, _ ->
            uname == userName && pwd == password
        }
    }

    fun rm(path: String, recursive: Boolean = false) {
        if (recursive) {
            Files.walk(fileSystem.getPath(path))
                .sorted(reverseOrder())
                .toList()
                .filter { it.toString() != "/" } // Can't delete root
                .forEach(Files::delete)
        } else {
            Files.delete(fileSystem.getPath(path))
        }
    }

    override fun beforeAll(context: ExtensionContext?) = sshServer.start()

    override fun afterAll(context: ExtensionContext?) {
        sshServer.stop(true)
        fileSystem.close()
    }

    fun mkdir(vararg paths: String) =
        paths.forEach {
            Files.createDirectories(fileSystem.getPath(it))
        }

    fun listAll(path: String = "/") =
        try {
            Files.walk(fileSystem.getPath(path))
                .sorted(reverseOrder())
                .map { it.toString() }.toList()
        } catch (e: NoSuchFileException) {
            emptyList<String>()
        }

    fun getFileContent(path: String, encoding: Charset = UTF_8) = String(readAllBytes(fileSystem.getPath(path)), encoding)

    fun existsFile(path: String) = exists(fileSystem.getPath(path)) && !isDirectory(fileSystem.getPath(path))

    fun existsDir(path: String) = exists(fileSystem.getPath(path)) && isDirectory(fileSystem.getPath(path))

    fun putFile(path: String, inputStream: InputStream) = copy(inputStream, fileSystem.getPath(path))
}

internal class DoNotCloseFileSystemFactory(private val fileSystem: FileSystem) : FileSystemFactory {
    override fun getUserHomeDir(session: SessionContext?): Path = fileSystem.getPath("/")

    override fun createFileSystem(session: SessionContext?)  = DoNotCloseFileSystem(fileSystem)
}

internal class DoNotCloseFileSystem(private val fileSystem: FileSystem) : FileSystem() {
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