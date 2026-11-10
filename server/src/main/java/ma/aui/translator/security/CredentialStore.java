package ma.aui.translator.security;

import ma.aui.translator.config.AppConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Identity store used by the authentication module.
 * Users come from {@code /users.properties} ({@code username = <hash>}) and can be overridden/extended with the
 * {@code TRANSLATOR_USERS} environment variable: {@code "alice:secret1,bob:secret2"} (plain passwords).
 * Every valid user receives the {@value #ROLE} group, mapped to the role of the same name.
 */
public class CredentialStore {

    public static final String ROLE = "translator";

    private final Map<String, String> users;

    public CredentialStore(Map<String, String> users) {
        this.users = Map.copyOf(users);
    }

    public static CredentialStore fromConfig() {
        Map<String, String> users = new HashMap<>();
        Properties p = new Properties();
        try (InputStream in = CredentialStore.class.getResourceAsStream("/users.properties")) {
            if (in != null) p.load(in);
        } catch (IOException ignored) {
            // no file users
        }
        p.stringPropertyNames().forEach(u -> users.put(u, p.getProperty(u).trim()));

        String env = AppConfig.get().get(AppConfig.TRANSLATOR_USERS, "");
        for (String entry : env.split(",")) {
            int i = entry.indexOf(':');
            if (i > 0) users.put(entry.substring(0, i).trim(), "plain:" + entry.substring(i + 1).trim());
        }
        return new CredentialStore(users);
    }

    /** Returns the caller's groups when the credentials are valid. */
    public Optional<List<String>> validate(String username, String password) {
        String stored = users.get(username);
        if (stored != null && PasswordHasher.verify(password, stored)) {
            return Optional.of(List.of(ROLE));
        }
        return Optional.empty();
    }

    public int size() {
        return users.size();
    }
}
