package nl.chimpgamer.mapartlock.lock;

import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.UUID;

/** Stores a UUID as its 16 raw bytes instead of a 36-character string. */
public final class UuidType implements PersistentDataType<byte[], UUID> {
    public static final UuidType INSTANCE = new UuidType();

    private static final int BYTES = 16;

    private UuidType() {
    }

    @Override
    public @NotNull Class<byte[]> getPrimitiveType() {
        return byte[].class;
    }

    @Override
    public @NotNull Class<UUID> getComplexType() {
        return UUID.class;
    }

    @Override
    public byte @NotNull [] toPrimitive(@NotNull UUID complex, @NotNull PersistentDataAdapterContext context) {
        return ByteBuffer.allocate(BYTES)
                .putLong(complex.getMostSignificantBits())
                .putLong(complex.getLeastSignificantBits())
                .array();
    }

    @Override
    public @NotNull UUID fromPrimitive(byte @NotNull [] primitive, @NotNull PersistentDataAdapterContext context) {
        if (primitive.length != BYTES) {
            throw new IllegalArgumentException("UUID must be " + BYTES + " bytes, was " + primitive.length);
        }

        ByteBuffer buffer = ByteBuffer.wrap(primitive);
        return new UUID(buffer.getLong(), buffer.getLong());
    }
}
