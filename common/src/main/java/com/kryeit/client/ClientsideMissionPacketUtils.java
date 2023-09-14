package com.kryeit.client;

import com.kryeit.Main;
import com.kryeit.client.ClientMissionData.ClientsideActiveMission;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class ClientsideMissionPacketUtils {
    public static final ResourceLocation IDENTIFIER = new ResourceLocation("missions", "active_missions");
    public static final ResourceLocation PAYOUT_IDENTIFIER = new ResourceLocation(Main.MOD_ID, "payout");
    public static final ResourceLocation REROLL_IDENTIFIER = new ResourceLocation(Main.MOD_ID, "reroll");
    public static final ResourceLocation REQUEST_MISSIONS = new ResourceLocation(Main.MOD_ID, "request_missions");
    private static Consumer<ClientMissionData> updateHandler;

    public static void handlePacket(FriendlyByteBuf buf) {
        boolean rewardsAvailable = buf.readBoolean();
        ItemStack rerollPrice = buf.readItem();
        int freeRerollsLeft = buf.readInt();
        boolean canReroll = buf.readBoolean();

        List<ClientsideActiveMission> missions = buf.readList(ClientsideActiveMission::fromBuffer);
        updateHandler.accept(new ClientMissionData(rewardsAvailable, missions, rerollPrice, freeRerollsLeft, canReroll));
    }

    public static FriendlyByteBuf serialize(ClientMissionData data) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBoolean(data.hasUnclaimedRewards());
        buf.writeItem(data.rerollPrice());
        buf.writeInt(data.freeRerollsLeft());
        buf.writeBoolean(data.canReroll());

        buf.writeCollection(data.activeMissions(), (b, mission) -> {
            b.writeComponent(mission.titleString());
            b.writeEnum(mission.difficulty());
            b.writeInt(mission.requiredAmount());
            b.writeInt(mission.progress());
            b.writeItem(mission.previewItem());
            b.writeItem(mission.itemRequired());
            b.writeComponent(mission.missionString());
            b.writeBoolean(mission.isCompleted());
            mission.rewardAmount().writeToBuf(b);
            b.writeUtf(mission.rewardItemLocation());
        });
        return buf;
    }

    public static void requestPayout() {
        ServerboundCustomPayloadPacket packet = new ServerboundCustomPayloadPacket(
                PAYOUT_IDENTIFIER,
                new FriendlyByteBuf(Unpooled.buffer(0))
        );
        sendPacket(packet);
    }

    public static void requestReroll(int index) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(4));
        buf.writeInt(index);
        ServerboundCustomPayloadPacket packet = new ServerboundCustomPayloadPacket(
                REROLL_IDENTIFIER,
                buf
        );
        sendPacket(packet);
    }

    public static void requestMissions() {
        ServerboundCustomPayloadPacket packet = new ServerboundCustomPayloadPacket(
                ClientsideMissionPacketUtils.REQUEST_MISSIONS,
                new FriendlyByteBuf(Unpooled.buffer(0))
        );
        sendPacket(packet);
    }

    public static void setMissionUpdateHandler(Consumer<ClientMissionData> handler) {
        updateHandler = handler;
    }

    private static void sendPacket(ServerboundCustomPayloadPacket packet) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        Objects.requireNonNull(connection, "Connection may not be null").send(packet);
    }
}
