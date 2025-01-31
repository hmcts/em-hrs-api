package uk.gov.hmcts.reform.em.hrs.config.security;

public class UserContext {
    private static final ThreadLocal<UserDetails> userContext = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(UserDetails userDetails) {
        userContext.set(userDetails);
    }

    public static UserDetails get() {
        return userContext.get();
    }

    public static void clear() {
        userContext.remove();
    }

    public static class UserDetails {
        private final String userId;
        private final String email;

        public UserDetails(String userId, String email) {
            this.userId = userId;
            this.email = email;
        }

        public String getUserId() {
            return userId;
        }

        public String getEmail() {
            return email;
        }
    }
}
