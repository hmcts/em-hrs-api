package uk.gov.hmcts.reform.em.hrs.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FileNameCoderTest {

    @Test
    public void testEncodeFileName() {
        assertEquals(
            "file__*__name.txt",
            FileNameCoder.encodeFileName("file__*__name.txt")
        );
        assertEquals(
            "directory__*__with__*__subdirectory",
            FileNameCoder.encodeFileName("directory/with/subdirectory")
        );
        assertEquals(
            "another__*__file",
            FileNameCoder.encodeFileName("another/file")
        );
    }

    @Test
    public void testDecodeFileName() {
        assertEquals(
            "file__*__name.txt",
            FileNameCoder.decodeFileName("file__*__name.txt")
        );
        assertEquals(
            "directory/with/subdirectory",
            FileNameCoder.decodeFileName("directory__*__with__*__subdirectory")
        );
        assertEquals(
            "another/file",
            FileNameCoder.decodeFileName("another__*__file")
        );
    }
}

