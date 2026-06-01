/*
 *     Copyright (C) 2020 Florian Stober
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package codecrafter47.bungeetablistplus.handler;

import codecrafter47.bungeetablistplus.BungeeTabListPlus;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.packet.Team;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

final class TeamPacketCompat {

    private static final int DEFAULT_COLOR = 21;
    private static final Accessor COLOR_ACCESSOR = findColorAccessor();
    private static final AtomicBoolean SET_COLOR_WARNING_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean GET_COLOR_WARNING_LOGGED = new AtomicBoolean();

    private TeamPacketCompat() {
    }

    static void setColor(Team team, int color) {
        if (COLOR_ACCESSOR == null) {
            logSetColorWarning(null);
            return;
        }
        try {
            COLOR_ACCESSOR.set(team, color);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            logSetColorWarning(ex);
        }
        ensureColor(team);
    }

    static void setDefaultColor(Team team) {
        setColor(team, DEFAULT_COLOR);
    }

    static void ensureColor(DefinedPacket packet) {
        if (packet instanceof Team) {
            ensureColor((Team) packet);
        }
    }

    private static void ensureColor(Team team) {
        if (COLOR_ACCESSOR == null) {
            logSetColorWarning(null);
            return;
        }
        try {
            Object rawColor = COLOR_ACCESSOR.getRaw(team);
            if (rawColor == null || (rawColor instanceof Optional && !((Optional<?>) rawColor).isPresent())) {
                COLOR_ACCESSOR.set(team, DEFAULT_COLOR);
            }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            logSetColorWarning(ex);
        }
    }

    static int getColor(Team team) {
        if (COLOR_ACCESSOR == null) {
            logGetColorWarning(null);
            return DEFAULT_COLOR;
        }
        try {
            Object color = COLOR_ACCESSOR.get(team);
            return toColorId(color);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            logGetColorWarning(ex);
            return DEFAULT_COLOR;
        }
    }

    private static Accessor findColorAccessor() {
        Method getter = findGetter();
        Accessor fieldAccessor = findFieldAccessor(getter);
        if (fieldAccessor != null) {
            return fieldAccessor;
        }
        Accessor methodAccessor = findMethodAccessor(getter, int.class);
        if (methodAccessor != null) {
            return methodAccessor;
        }
        methodAccessor = findMethodAccessor(getter, byte.class);
        if (methodAccessor != null) {
            return methodAccessor;
        }
        methodAccessor = findMethodAccessor(getter, Integer.class);
        if (methodAccessor != null) {
            return methodAccessor;
        }
        methodAccessor = findMethodAccessor(getter, ChatColor.class);
        if (methodAccessor != null) {
            return methodAccessor;
        }
        methodAccessor = findMethodAccessor(getter, String.class);
        if (methodAccessor != null) {
            return methodAccessor;
        }
        methodAccessor = findMethodAccessor(getter, Optional.class);
        if (methodAccessor != null) {
            return methodAccessor;
        }
        return null;
    }

    private static Method findGetter() {
        try {
            Method method = Team.class.getMethod("getColor");
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Accessor findMethodAccessor(Method getter, Class<?> parameterType) {
        try {
            Method setter = Team.class.getMethod("setColor", parameterType);
            setter.setAccessible(true);
            return new MethodAccessor(setter, getter, parameterType, setter.getGenericParameterTypes()[0]);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Accessor findFieldAccessor(Method getter) {
        try {
            Field field = Team.class.getDeclaredField("color");
            field.setAccessible(true);
            return new FieldAccessor(field, getter);
        } catch (NoSuchFieldException ignored) {
            return null;
        }
    }

    private static int toColorId(Object color) {
        if (color instanceof Number) {
            return ((Number) color).intValue();
        }
        if (color instanceof ChatColor) {
            return toColorId((ChatColor) color);
        }
        if (color instanceof String) {
            return toColorId((String) color);
        }
        if (color instanceof Optional) {
            Optional<?> optionalColor = (Optional<?>) color;
            return optionalColor.isPresent() ? toColorId(optionalColor.get()) : DEFAULT_COLOR;
        }
        return DEFAULT_COLOR;
    }

    private static Object fromColorId(int color, Class<?> targetType) {
        return fromColorId(color, targetType, targetType);
    }

    private static Object fromColorId(int color, Class<?> targetType, Type genericType) {
        if (targetType == int.class || targetType == Integer.class) {
            return color;
        }
        if (targetType == byte.class || targetType == Byte.class) {
            return (byte) color;
        }
        if (targetType == ChatColor.class) {
            return toChatColor(color);
        }
        if (targetType == String.class) {
            return toColorName(color);
        }
        if (Optional.class.isAssignableFrom(targetType)) {
            return Optional.of(fromColorId(color, getOptionalValueType(genericType)));
        }
        return color;
    }

    private static Class<?> getOptionalValueType(Type optionalType) {
        if (optionalType instanceof ParameterizedType) {
            Type[] typeArguments = ((ParameterizedType) optionalType).getActualTypeArguments();
            if (typeArguments.length == 1 && typeArguments[0] instanceof Class<?>) {
                return (Class<?>) typeArguments[0];
            }
        }
        return Integer.class;
    }

    private static int toColorId(String color) {
        String name = color.toLowerCase(Locale.ROOT);
        if (name.length() == 2 && name.charAt(0) == ChatColor.COLOR_CHAR) {
            switch (name.charAt(1)) {
                case '0':
                    return 0;
                case '1':
                    return 1;
                case '2':
                    return 2;
                case '3':
                    return 3;
                case '4':
                    return 4;
                case '5':
                    return 5;
                case '6':
                    return 6;
                case '7':
                    return 7;
                case '8':
                    return 8;
                case '9':
                    return 9;
                case 'a':
                    return 10;
                case 'b':
                    return 11;
                case 'c':
                    return 12;
                case 'd':
                    return 13;
                case 'e':
                    return 14;
                case 'f':
                    return 15;
                case 'k':
                    return 16;
                case 'l':
                    return 17;
                case 'm':
                    return 18;
                case 'n':
                    return 19;
                case 'o':
                    return 20;
                default:
                    return DEFAULT_COLOR;
            }
        }
        if ("black".equals(name)) {
            return 0;
        } else if ("dark_blue".equals(name)) {
            return 1;
        } else if ("dark_green".equals(name)) {
            return 2;
        } else if ("dark_aqua".equals(name)) {
            return 3;
        } else if ("dark_red".equals(name)) {
            return 4;
        } else if ("dark_purple".equals(name)) {
            return 5;
        } else if ("gold".equals(name)) {
            return 6;
        } else if ("gray".equals(name)) {
            return 7;
        } else if ("dark_gray".equals(name)) {
            return 8;
        } else if ("blue".equals(name)) {
            return 9;
        } else if ("green".equals(name)) {
            return 10;
        } else if ("aqua".equals(name)) {
            return 11;
        } else if ("red".equals(name)) {
            return 12;
        } else if ("light_purple".equals(name)) {
            return 13;
        } else if ("yellow".equals(name)) {
            return 14;
        } else if ("white".equals(name)) {
            return 15;
        } else if ("obfuscated".equals(name)) {
            return 16;
        } else if ("bold".equals(name)) {
            return 17;
        } else if ("strikethrough".equals(name)) {
            return 18;
        } else if ("underlined".equals(name)) {
            return 19;
        } else if ("italic".equals(name)) {
            return 20;
        } else {
            return DEFAULT_COLOR;
        }
    }

    private static int toColorId(ChatColor color) {
        if (color == ChatColor.BLACK) {
            return 0;
        } else if (color == ChatColor.DARK_BLUE) {
            return 1;
        } else if (color == ChatColor.DARK_GREEN) {
            return 2;
        } else if (color == ChatColor.DARK_AQUA) {
            return 3;
        } else if (color == ChatColor.DARK_RED) {
            return 4;
        } else if (color == ChatColor.DARK_PURPLE) {
            return 5;
        } else if (color == ChatColor.GOLD) {
            return 6;
        } else if (color == ChatColor.GRAY) {
            return 7;
        } else if (color == ChatColor.DARK_GRAY) {
            return 8;
        } else if (color == ChatColor.BLUE) {
            return 9;
        } else if (color == ChatColor.GREEN) {
            return 10;
        } else if (color == ChatColor.AQUA) {
            return 11;
        } else if (color == ChatColor.RED) {
            return 12;
        } else if (color == ChatColor.LIGHT_PURPLE) {
            return 13;
        } else if (color == ChatColor.YELLOW) {
            return 14;
        } else if (color == ChatColor.WHITE) {
            return 15;
        } else if (color == ChatColor.MAGIC) {
            return 16;
        } else if (color == ChatColor.BOLD) {
            return 17;
        } else if (color == ChatColor.STRIKETHROUGH) {
            return 18;
        } else if (color == ChatColor.UNDERLINE) {
            return 19;
        } else if (color == ChatColor.ITALIC) {
            return 20;
        } else {
            return DEFAULT_COLOR;
        }
    }

    private static String toColorName(int color) {
        switch (color) {
            case 0:
                return "black";
            case 1:
                return "dark_blue";
            case 2:
                return "dark_green";
            case 3:
                return "dark_aqua";
            case 4:
                return "dark_red";
            case 5:
                return "dark_purple";
            case 6:
                return "gold";
            case 7:
                return "gray";
            case 8:
                return "dark_gray";
            case 9:
                return "blue";
            case 10:
                return "green";
            case 11:
                return "aqua";
            case 12:
                return "red";
            case 13:
                return "light_purple";
            case 14:
                return "yellow";
            case 15:
                return "white";
            case 16:
                return "obfuscated";
            case 17:
                return "bold";
            case 18:
                return "strikethrough";
            case 19:
                return "underlined";
            case 20:
                return "italic";
            default:
                return "reset";
        }
    }

    private static ChatColor toChatColor(int color) {
        switch (color) {
            case 0:
                return ChatColor.BLACK;
            case 1:
                return ChatColor.DARK_BLUE;
            case 2:
                return ChatColor.DARK_GREEN;
            case 3:
                return ChatColor.DARK_AQUA;
            case 4:
                return ChatColor.DARK_RED;
            case 5:
                return ChatColor.DARK_PURPLE;
            case 6:
                return ChatColor.GOLD;
            case 7:
                return ChatColor.GRAY;
            case 8:
                return ChatColor.DARK_GRAY;
            case 9:
                return ChatColor.BLUE;
            case 10:
                return ChatColor.GREEN;
            case 11:
                return ChatColor.AQUA;
            case 12:
                return ChatColor.RED;
            case 13:
                return ChatColor.LIGHT_PURPLE;
            case 14:
                return ChatColor.YELLOW;
            case 15:
                return ChatColor.WHITE;
            case 16:
                return ChatColor.MAGIC;
            case 17:
                return ChatColor.BOLD;
            case 18:
                return ChatColor.STRIKETHROUGH;
            case 19:
                return ChatColor.UNDERLINE;
            case 20:
                return ChatColor.ITALIC;
            default:
                return ChatColor.RESET;
        }
    }

    private static void logSetColorWarning(Exception ex) {
        if (SET_COLOR_WARNING_LOGGED.compareAndSet(false, true)) {
            log(Level.WARNING, "Unable to initialize BungeeCord Team packet color using the current runtime API. Team packets may fail to encode on this BungeeCord version.", ex);
        }
    }

    private static void logGetColorWarning(Exception ex) {
        if (GET_COLOR_WARNING_LOGGED.compareAndSet(false, true)) {
            log(Level.WARNING, "Unable to read BungeeCord Team packet color using the current runtime API. A default tab list team color will be used to avoid crashing.", ex);
        }
    }

    private static void log(Level level, String message, Exception ex) {
        Logger logger;
        BungeeTabListPlus instance = BungeeTabListPlus.getInstance();
        if (instance != null) {
            logger = instance.getLogger();
        } else {
            logger = Logger.getLogger(TeamPacketCompat.class.getName());
        }
        if (ex != null) {
            logger.log(level, message, ex);
        } else {
            logger.log(level, message);
        }
    }

    private interface Accessor {

        void set(Team team, int color) throws ReflectiveOperationException;

        Object get(Team team) throws ReflectiveOperationException;

        Object getRaw(Team team) throws ReflectiveOperationException;
    }

    private static final class MethodAccessor implements Accessor {

        private final Method setter;
        private final Method getter;
        private final Class<?> colorType;
        private final Type genericColorType;

        private MethodAccessor(Method setter, Method getter, Class<?> colorType, Type genericColorType) {
            this.setter = setter;
            this.getter = getter;
            this.colorType = colorType;
            this.genericColorType = genericColorType;
        }

        @Override
        public void set(Team team, int color) throws ReflectiveOperationException {
            setter.invoke(team, fromColorId(color, colorType, genericColorType));
        }

        @Override
        public Object get(Team team) throws ReflectiveOperationException {
            if (getter == null) {
                return DEFAULT_COLOR;
            }
            return getter.invoke(team);
        }

        @Override
        public Object getRaw(Team team) throws ReflectiveOperationException {
            return get(team);
        }
    }

    private static final class FieldAccessor implements Accessor {

        private final Field field;
        private final Method getter;
        private final Type genericColorType;

        private FieldAccessor(Field field, Method getter) {
            this.field = field;
            this.getter = getter;
            this.genericColorType = field.getGenericType();
        }

        @Override
        public void set(Team team, int color) throws ReflectiveOperationException {
            field.set(team, fromColorId(color, field.getType(), genericColorType));
        }

        @Override
        public Object get(Team team) throws ReflectiveOperationException {
            if (getter != null) {
                return getter.invoke(team);
            }
            return field.get(team);
        }

        @Override
        public Object getRaw(Team team) throws ReflectiveOperationException {
            return field.get(team);
        }
    }
}
