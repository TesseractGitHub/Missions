package com.kryeit.client;

import com.kryeit.Main;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class ClientsideMissionPacketUtils {
    public static final ResourceLocation IDENTIFIER = new ResourceLocation("missions", "active_missions");
    public static final ResourceLocation PAYOUT_IDENTIFIER = new ResourceLocation(Main.MOD_ID, "payout");
    public static final ResourceLocation REQUEST_MISSIONS = new ResourceLocation(Main.MOD_ID, "request_missions");
    private static Consumer<List<ClientsideActiveMission>> updateHandler;

    public static void handlePacket(FriendlyByteBuf buf) {
        List<ClientsideActiveMission> missions = buf.readList(b -> new ClientsideActiveMission(buf.readInt(), buf.readItem(), buf.readComponent(), buf.readBoolean()));
        updateHandler.accept(missions);
    }

    public static FriendlyByteBuf serialize(List<ClientsideActiveMission> missions) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        buf.writeCollection(missions, (b, mission) -> {
            buf.writeInt(mission.requiredAmount());
            buf.writeItem(mission.itemStack());
            buf.writeComponent(mission.missionString());
            buf.writeBoolean(mission.isCompleted());
        });
        return buf;
    }

    public static void requestPayout() {
        ServerboundCustomPayloadPacket packet = new ServerboundCustomPayloadPacket(
                PAYOUT_IDENTIFIER,
                new FriendlyByteBuf(Unpooled.buffer(0))
        );
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        Objects.requireNonNull(connection, "Connection may not be null").send(packet);
    }

    public static void requestMissions() {
        ServerboundCustomPayloadPacket packet = new ServerboundCustomPayloadPacket(
                ClientsideMissionPacketUtils.REQUEST_MISSIONS,
                new FriendlyByteBuf(Unpooled.buffer(0))
        );
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        Objects.requireNonNull(connection, "Connection may not be null").send(packet);
    }

    public static void setMissionUpdateHandler(Consumer<List<ClientsideActiveMission>> handler) {
        updateHandler = handler;
    }
}
