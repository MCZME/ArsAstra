package com.github.mczme.arsastra.registry;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.core.knowledge.PlayerKnowledge;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class AAAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ArsAstra.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerKnowledge>> PLAYER_KNOWLEDGE = ATTACHMENT_TYPES.register(
            "player_knowledge",
            () -> AttachmentType.builder(PlayerKnowledge::new)
                    .serialize(new IAttachmentSerializer<CompoundTag, PlayerKnowledge>() {
                        @Override
                        public PlayerKnowledge read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
                            PlayerKnowledge pk = new PlayerKnowledge();
                            pk.deserializeNBT(provider, tag);
                            return pk;
                        }

                        @Override
                        public CompoundTag write(PlayerKnowledge attachment, HolderLookup.Provider provider) {
                            return attachment.serializeNBT(provider);
                        }
                    })
                    .copyOnDeath() // 死亡时自动复制数据
                    .build()
    );

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
