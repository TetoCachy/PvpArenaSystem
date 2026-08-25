package com.tetocachy.pvparenasystem.network;

import com.tetocachy.pvparenasystem.PvpArenaSystem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record C2SActionPayload(String action, String param1, String param2, int intParam1, int intParam2, int intParam3, int intParam4) implements CustomPacketPayload {
    public static final Type<C2SActionPayload> TYPE = new Type<>(PvpArenaSystem.id("c2s_action"));

    public C2SActionPayload(String action, String param1, String param2, int intParam1, int intParam2) {
        this(action, param1, param2, intParam1, intParam2, 0, 0);
    }

    public C2SActionPayload(String action, String param1, String param2, int intParam1, int intParam2, int intParam3) {
        this(action, param1, param2, intParam1, intParam2, intParam3, 0);
    }

    public static final StreamCodec<FriendlyByteBuf, C2SActionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeUtf(p.action());
                buf.writeUtf(p.param1());
                buf.writeUtf(p.param2());
                buf.writeInt(p.intParam1());
                buf.writeInt(p.intParam2());
                buf.writeInt(p.intParam3());
                buf.writeInt(p.intParam4());
            },
            buf -> new C2SActionPayload(
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}