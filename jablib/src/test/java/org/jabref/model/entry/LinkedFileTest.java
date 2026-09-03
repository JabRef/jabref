package org.jabref.model.entry;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LinkedFileTest {

    @ParameterizedTest
    @CsvSource({
            "http://example.com/paper.pdf, true",
            "https://example.com/paper.pdf, true",
            "ftp://ftp.example.com/paper.pdf, true",
            "ftps://ftp.example.com/paper.pdf, true",
            "www.example.com/paper.pdf, true",
            "HTTP://EXAMPLE.COM/paper.pdf, true",
            "FTP://FTP.EXAMPLE.COM/paper.pdf, true",
            "FTPS://FTP.EXAMPLE.COM/paper.pdf, true",
            "HTTPS://EXAMPLE.COM/paper.pdf, true",
            "/local/path/file.pdf, false",
            "C:\\Users\\file.pdf, false",
            "file.txt, false",
            "'', false"
    })
    void isOnlineLinkRecognizesFtp(String link, boolean expected) {
        assertEquals(expected, LinkedFile.isOnlineLink(link));
    }
}
