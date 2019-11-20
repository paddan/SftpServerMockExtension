package com.github.paddan.sftpserver

import io.kotlintest.shouldBe
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.xfer.InMemoryDestFile
import net.schmizz.sshj.xfer.InMemorySourceFile
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.ByteArrayOutputStream

class SftpServerMockExtensionTest {
    companion object {
        const val userName = "user"
        const val password = "pwd"
        @RegisterExtension
        @JvmField
        val sftpServer = SftpServerMockExtension(userName, password)
    }

    private val sshClient = SSHClient()

    @BeforeEach
    fun setUp() {
        sshClient.addHostKeyVerifier { _, _, _ -> true }
        sshClient.connect("localhost", sftpServer.port)
        sshClient.authPassword(userName, password)
    }

    @AfterEach
    internal fun tearDown() {
        sftpServer.deleteAll("/")
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
}