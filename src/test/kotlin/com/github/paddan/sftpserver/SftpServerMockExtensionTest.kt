package com.github.paddan.sftpserver

import io.kotlintest.shouldBe
import net.schmizz.sshj.SSHClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

internal class SftpServerMockExtensionTest {
    companion object {
        const val userName = "user"
        const val password = "pwd"
        @RegisterExtension
        @JvmField
        val sftpServer = SftpServerMockExtension(userName, password)
    }

    private val sshClient = SSHClient()

    @BeforeEach
    internal fun setUp() {
        sshClient.addHostKeyVerifier { _, _, _ -> true }
        sshClient.connect("localhost", sftpServer.port)
        sshClient.authPassword(userName, password)
    }

    @Test
    internal fun `should upload file`() {
        // Given
        val sftpClient = sshClient.newSFTPClient()
        val fileName = "new_file.txt"
        val fileContent = "Hello world!"

        // When
        sftpClient.put(fileContent, fileName)

        // Then
        sftpServer.getFileContent(fileName) shouldBe fileContent
    }
}