package xyz.wagyourtail.jsmacros.client.mixins.events;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.wagyourtail.jsmacros.client.note2midi.CaptureSource;
import xyz.wagyourtail.jsmacros.client.note2midi.Note2MidiRecorder;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinNote2MidiSoundPackets {
    @Inject(method = "onPlaySound", at = @At("TAIL"))
    private void note2midi$observeSoundPacket(PlaySoundS2CPacket packet, CallbackInfo callbackInfo) {
        Note2MidiRecorder.controller().capture(CaptureSource.STRICT_PACKET,
                packet.getSound().value().getId().toString(), packet.getPitch(), packet.getVolume(),
                packet.getX(), packet.getY(), packet.getZ());
    }

    @Inject(method = "onPlaySoundFromEntity", at = @At("TAIL"))
    private void note2midi$observeEntitySoundPacket(PlaySoundFromEntityS2CPacket packet, CallbackInfo callbackInfo) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        Entity entity = client.world.getEntityById(packet.getEntityId());
        if (entity == null) {
            Note2MidiRecorder.controller().incrementMissingEntity();
            return;
        }
        Note2MidiRecorder.controller().capture(CaptureSource.STRICT_PACKET,
                packet.getSound().value().getId().toString(), packet.getPitch(), packet.getVolume(),
                entity.getX(), entity.getY(), entity.getZ());
    }
}
