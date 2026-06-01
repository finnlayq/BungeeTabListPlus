package codecrafter47.bungeetablistplus.util;

import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.geyser.api.GeyserApi;

import java.util.UUID;
import java.util.function.Function;

public class GeyserCompat {

    private static Function<UUID, Boolean> geyserHook = uuid -> false;;
    private static Function<UUID, Boolean> floodgateHook = uuid -> false;;

    public static void init() {

        // Geyser
        try {
            Class.forName("org.geysermc.geyser.api.GeyserApi");
            geyserHook = new Function<UUID, Boolean>() {
                @Override
                public Boolean apply(UUID uuid) {

                    // Geyser documentation says, this will return null when not initialized.
                    // In reality, it throws an exception
                    try {

                        GeyserApi instance = GeyserApi.api();
                        if (instance == null) {
                            return false;
                        }
                        return instance.connectionByUuid(uuid) != null;
                    } catch (Throwable ignored) {

                    }

                    return false;
                }
            };
        } catch (Throwable ignored) {
        }

        // Floodgate
        try {
            Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            floodgateHook = new Function<UUID, Boolean>() {
                @Override
                public Boolean apply(UUID uuid) {
                    FloodgateApi api = FloodgateApi.getInstance();
                    if (api == null) {
                        return false;
                    }
                    return api.isFloodgatePlayer(uuid);
                }
            };
        } catch (Throwable ignored) {
        }
    }

    public static boolean isBedrockPlayer(UUID uuid) {
        return geyserHook.apply(uuid) || floodgateHook.apply(uuid);
    }
}
