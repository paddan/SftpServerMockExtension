package com.github.paddan.sftpserver

import io.kotlintest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.shouldBe
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.xfer.InMemoryDestFile
import net.schmizz.sshj.xfer.InMemorySourceFile
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.ByteArrayOutputStream

class SftpServerMockExtensionKotlinTest {
    companion object {
        const val user = "user"
        const val pwd = "pwd"

        @JvmField
        @RegisterExtension
        val sftpServer = SftpServerMockExtension(user, pwd)
    }

    private val sshClient = SSHClient()

    @BeforeEach
    fun setUp() {
        sshClient.addHostKeyVerifier { _, _, _ -> true }
        sshClient.connect("localhost", sftpServer.port)
        sshClient.authPassword(user, pwd)
    }

    @AfterEach
    internal fun tearDown() {
        sftpServer.rm("/", true)
    }

    @Test
    fun `should upload file`() {
        // Given
        val sftp = sshClient.newSFTPClient()
        val fileName = "new_file.txt"
        val data = "Hello world!"
        val inputStream = data.byteInputStream()
        val inMemoryFile = object : InMemorySourceFile() {
            override fun getLength() = inputStream.available().toLong()
            override fun getName() = fileName
            override fun getInputStream() = inputStream
        }

        // When
        sftp.put(inMemoryFile, "/")

        // Then
        sftpServer.existsFile("/$fileName") shouldBe true
        sftpServer.getFileContent("/$fileName") shouldBe data
    }

    @Test
    fun `should download file`() {
        // Given
        val sftp = sshClient.newSFTPClient()
        val fileName = "/new_file.txt"
        val data = "Hello world!"
        sftpServer.putFile(fileName, data.byteInputStream())
        val outputStream = ByteArrayOutputStream()
        val inMemoryFile = object : InMemoryDestFile() {
            override fun getOutputStream() = outputStream
        }

        // When
        sftp.get(fileName, inMemoryFile)

        // Then
        String(outputStream.toByteArray(), Charsets.UTF_8) shouldBe data
    }

    @Test
    fun `should delete file`() {
        // Given
        val sftp = sshClient.newSFTPClient()
        val fileName = "/new_file.txt"
        val data = "Hello world!"
        sftpServer.putFile(fileName, data.byteInputStream())

        // When
        sftp.rm(fileName)

        // Then
        sftpServer.existsFile(fileName) shouldBe false
    }

    @Test
    fun `should delete dir`() {
        // Given
        val sftp = sshClient.newSFTPClient()
        val dirName = "/new_dir"
        sftpServer.mkdir(dirName)

        // When
        sftp.rmdir(dirName)

        // Then
        sftpServer.existsDir(dirName) shouldBe false
        sftpServer.listAll(dirName) shouldHaveSize 0
    }

    @Test
    fun `should create dir`() {
        // Given
        val sftp = sshClient.newSFTPClient()
        val dirName = "/new_dir"

        // When
        sftp.mkdir(dirName)

        // Then
        sftpServer.existsDir(dirName) shouldBe true
        sftpServer.listAll(dirName) shouldContainExactlyInAnyOrder listOf(dirName)
    }
}