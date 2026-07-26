package cc.vanishclient.utils.other;

public class StringUtils {

    /**
     * Normalizes the given enum name to a readable string (e.g. "TEST_ENUM" becomes "Test enum"). This method is used to
     * generate a readable name for the enum. "$" will be removed, "_" will be replaced with " ",
     * and the first letter will be uppercase.
     *
     * @param string The name to normalize
     * @return The normalized name
     */
    public static String normalizeEnumName(String string) {
        if (string.length() < 2) return string;

        string = string.replace("_", " ");
        string = string.replace("$", "");

        return string.charAt(0) + string.substring(1).toLowerCase();
    }

    public static String toDottedLowerCase(String string) {
        if (string == null || string.isEmpty()) {
            return "";
        }

        String normalized = string
                .replaceAll("[^A-Za-z0-9]+", ".")
                .replaceAll("([a-z])([A-Z])", "$1.$2")
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1.$2")
                .replaceAll("\\.+", ".")
                .replaceAll("^\\.|\\.$", "");

        return normalized.toLowerCase();
    }
}

