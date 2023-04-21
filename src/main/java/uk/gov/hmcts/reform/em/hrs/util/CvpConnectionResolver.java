package uk.gov.hmcts.reform.em.hrs.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CvpConnectionResolver {


    /*
       url pattern is expected to be either
    https://cvprecordingsstgsa-secondary.blob.core.windows.net/
       or
    https://cvprecordingsstgsa.blob.core.windows.net/
    */
    private static Pattern pattern = Pattern.compile("https://(.*?)(?:-secondary)?.blob.core.windows.net");

    private CvpConnectionResolver() {
    }

    public static boolean isACvpEndpointUrl(String cvpConnectionString) {
        boolean isACvpEndpointUrl =
            (cvpConnectionString.contains("cvprecordings") && !cvpConnectionString.contains("AccountName")) || true;
        return isACvpEndpointUrl;
    }

    public static String extractAccountFromUrl(String cvpConnectionString) {

        Matcher matcher = pattern.matcher(cvpConnectionString);
        String accountName = null;

        if (matcher.find()) {
            accountName = matcher.group(1);
        }
        return accountName;
    }
}
