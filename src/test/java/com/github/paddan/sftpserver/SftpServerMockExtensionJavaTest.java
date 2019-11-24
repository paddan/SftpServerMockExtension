package com.github.paddan.sftpserver;

import com.github.paddan.sftpserver.SftpServerMockExtension;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.xfer.InMemoryDestFile;
import net.schmizz.sshj.xfer.InMemorySourceFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

public class SftpServerMockExtensionJavaTest {
    private static String user = "user";
    private static String pwd = "pwd";

    @RegisterExtension
    static SftpServerMockExtension sftpServer = new SftpServerMockExtension(user, pwd, 0);
    private SSHClient sshClient = new SSHClient();

    @BeforeEach
    void setUp() throws IOException {
        sshClient.addHostKeyVerifier((hostname, port, key) -> true);
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
                    e.printStackTrace();
                }
                return 0;
            }

            @Override
            public InputStream getInputStream() throws IOException {
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
            public OutputStream getOutputStream() throws IOException {
                return outputStream;
            }
        };

        // When
        sftp.get(fileName, inMemoryFile);

        // Then
        assertEquals(data, new String(outputStream.toByteArray(), UTF_8));
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
