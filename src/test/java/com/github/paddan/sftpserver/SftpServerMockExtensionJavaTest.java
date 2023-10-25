package com.github.paddan.sftpserver;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import net.schmizz.sshj.xfer.InMemoryDestFile;
import net.schmizz.sshj.xfer.InMemorySourceFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.*;
import java.security.PublicKey;
import java.util.Collections;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

public class SftpServerMockExtensionJavaTest {
    private static final String user = "user";
    private static final String pwd = "pwd";

    @RegisterExtension
    static SftpServerMockExtension sftpServer = new SftpServerMockExtension(user, pwd, 0);
    private final SSHClient sshClient = new SSHClient();

    @BeforeEach
    void setUp() throws IOException {
        var hostKeyVerifier = new HostKeyVerifier() {
            @Override
            public boolean verify(String hostname, int port, PublicKey key) {
                return true;
            }

            @Override
            public List<String> findExistingAlgorithms(String hostname, int port) {
                return Collections.emptyList();
            }
        };

        sshClient.addHostKeyVerifier(hostKeyVerifier);
        sshClient.connect("localhost", sftpServer.getPort());
        sshClient.authPassword(user, pwd);
    }

    @AfterEach
    void tearDown() {
        sftpServer.rm("/", true);
    }

    @Test
    void shouldUploadFile() throws IOException {
        // Given
        SFTPClient sftp = sshClient.newSFTPClient();
        String fileName = "new_file.txt";
        String data = "Hello world!";
        InputStream inputStream = new ByteArrayInputStream(data.getBytes());
        InMemorySourceFile inMemoryFile = new InMemorySourceFile() {
            @Override
            public String getName() {
                return fileName;
            }

            @Override
            public long getLength() {
                try {
                    return inputStream.available();
                } catch (IOException e) {
                    fail(e.getMessage());
                }
                return 0;
            }

            @Override
            public InputStream getInputStream() {
                return inputStream;
            }
        };

        // When
        sftp.put(inMemoryFile, "/");

        // Then
        assertTrue(sftpServer.existsFile("/" + fileName));
        assertEquals(data, sftpServer.getFileContent("/" + fileName, UTF_8));
    }

    @Test
    void shouldDownloadFile() throws IOException {
        // Given
        SFTPClient sftp = sshClient.newSFTPClient();
        String fileName = "/new_file.txt";
        String data = "Hello world!";
        sftpServer.putFile(fileName, new ByteArrayInputStream(data.getBytes()));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        InMemoryDestFile inMemoryFile = new InMemoryDestFile() {
            @Override
            public long getLength() {
                return data.length();
            }

            @Override
            public OutputStream getOutputStream() {
                return outputStream;
            }

            @Override
            public OutputStream getOutputStream(boolean append) {
                return outputStream;
            }
        };

        // When
        sftp.get(fileName, inMemoryFile);

        // Then
        assertEquals(data, outputStream.toString(UTF_8));
    }

    @Test
    void shouldDeleteFile() throws IOException {
        // Given
        SFTPClient sftp = sshClient.newSFTPClient();
        String fileName = "/new_file.txt";
        String data = "Hello world!";
        sftpServer.putFile(fileName, new ByteArrayInputStream(data.getBytes()));

        // When
        sftp.rm(fileName);

        // Then
        assertFalse(sftpServer.existsFile(fileName));
    }

    @Test
    void shouldDeleteDir() throws IOException {
        // Given
        SFTPClient sftp = sshClient.newSFTPClient();
        String dirName = "/new_dir";
        sftpServer.mkdir(dirName);

        // When
        sftp.rmdir(dirName);

        // Then
        assertFalse(sftpServer.existsDir(dirName));
        assertEquals(0, sftpServer.listAll(dirName).size());
    }

    @Test
    void shouldCreateDir() throws IOException {
        // Given
        SFTPClient sftp = sshClient.newSFTPClient();
        String dirName = "/new_dir";


        // When
        sftp.mkdir(dirName);

        // Then
        assertTrue(sftpServer.existsDir(dirName));
        assertEquals(1, sftpServer.listAll(dirName).size());
        assertEquals(dirName, sftpServer.listAll(dirName).get(0));
    }
}
