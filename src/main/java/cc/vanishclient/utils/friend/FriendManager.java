package cc.vanishclient.utils.friend;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.TreeSet;

public class FriendManager {
    private static final Set<UUID> friends = new HashSet<>();
    private static final Set<String> friendNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    public static void addFriend(UUID uuid) {
        friends.add(uuid);
    }

    public static void removeFriend(UUID uuid) {
        friends.remove(uuid);
    }

    public static void addFriendName(String name) {
        if (name != null && !name.isBlank()) {
            friendNames.add(name.trim());
        }
    }

    public static void removeFriendName(String name) {
        if (name != null) {
            friendNames.remove(name.trim());
        }
    }

    public static boolean isFriendName(String name) {
        return name != null && friendNames.contains(name.trim());
    }

    public static boolean isFriend(UUID uuid) {
        return friends.contains(uuid);
    }

    public static void toggleFriend(UUID uuid) {
        if (isFriend(uuid)) {
            removeFriend(uuid);
        } else {
            addFriend(uuid);
        }
    }

    public static Set<UUID> getFriends() {
        return new HashSet<>(friends);
    }

    public static Set<String> getFriendNames() {
        return new TreeSet<>(friendNames);
    }

    public static void clearFriends() {
        friends.clear();
        friendNames.clear();
    }
}
