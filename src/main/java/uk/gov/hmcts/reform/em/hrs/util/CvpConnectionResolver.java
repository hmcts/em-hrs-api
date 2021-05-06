package uk.gov.hmcts.reform.em.hrs.util;

public class CvpConnectionResolver {

    public static boolean isACvpEndpointUrl(String cvpConnectionString) {
        boolean isACvpEndpointUrl =
            cvpConnectionString.contains("cvprecordings") && !cvpConnectionString.contains("AccountName");
        return isACvpEndpointUrl;
    }


}
