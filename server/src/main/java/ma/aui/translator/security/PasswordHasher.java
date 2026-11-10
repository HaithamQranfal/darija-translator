package ma.aui.translator.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Salted, iterated SHA-256 password hashing. Stored format: {@code sha256:<iterations>:<base64 salt>:<base64 hash>}.
 * Generate a hash with: {@code java -cp target/classes ma.aui.translator.security.PasswordHasher mySecret}
 */
public final class PasswordHasher {

    private static final int ITERATIONS = 10_000;

    private PasswordHasher() {
    }

    public static String hash(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return "sha256:" + ITERATIONS + ":" + b64(salt) + ":" + b64(digest(password, salt, ITERATIONS));
    }

    /** Verifies a password against a stored value (a hash in the format above, or "plain:" + password). */
    public static boolean verify(String password, String stored) {
        if (password == null || stored == null) return false;
        if (stored.startsWith("plain:")) {
            return constantTimeEquals(password.getBytes(StandardCharsets.UTF_8),
                    stored.substring(6).getBytes(StandardCharsets.UTF_8));
        }
        String[] parts = stored.split(":");
        if (parts.length != 4 || !"sha256".equals(parts[0])) return false;
        int iterations = Integer.parseInt(parts[1]);
        byte[] salt = Base64.getDecoder().decode(parts[2]);
        byte[] expected = Base64.getDecoder().decode(parts[3]);
        return constantTimeEquals(digest(password, salt, iterations), expected);
    }

    private static byte[] digest(String password, byte[] salt, int iterations) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] h = md.digest(password.getBytes(StandardCharsets.UTF_8));
            for (int i = 1; i < iterations; i++) {
                md.reset();
                h = md.digest(h);
            }
            return h;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        return MessageDigest.isEqual(a, b);
    }

    private static String b64(byte[] b) {
        return Base64.getEncoder().encodeToString(b);
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: PasswordHasher <password>");
            System.exit(1);
        }
        System.out.println(hash(args[0]));
    }
}
