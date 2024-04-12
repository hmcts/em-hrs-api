package uk.gov.hmcts.reform.em.hrs.util;

public class FileNameCoder {

    private static String SLASH_CODE = "__*__";

    private FileNameCoder() {
    }

    public static String decodeFileName(String fileNameEncoded) {
        return fileNameEncoded.replace(SLASH_CODE, "/");
    }

    public static String encodeFileName(String fileNameEncoded) {
        return fileNameEncoded.replace("/", SLASH_CODE);
    }
}
