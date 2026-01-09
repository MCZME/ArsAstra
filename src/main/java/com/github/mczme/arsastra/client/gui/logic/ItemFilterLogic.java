package com.github.mczme.arsastra.client.gui.logic;

import com.github.mczme.arsastra.core.element.Element;
import com.github.mczme.arsastra.core.element.profile.ElementProfile;
import com.github.mczme.arsastra.core.element.profile.ElementProfileManager;
import com.github.mczme.arsastra.registry.AARegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public class ItemFilterLogic {

    public static Predicate<ItemStack> create(String elementFilterRaw, String tagFilterRaw) {
        String elementFilter = elementFilterRaw == null ? "" : elementFilterRaw.toLowerCase();
        String tagFilter = tagFilterRaw == null ? "" : tagFilterRaw.toLowerCase();
        
        return stack -> {
            // 要素筛选 (AND 逻辑)
            if (!elementFilter.isBlank()) {
                String[] requiredElements = elementFilter.split(",");
                ElementProfile profile = ElementProfileManager.getInstance().getElementProfile(stack.getItem()).orElse(null);
                if (profile == null) return false;

                for (String req : requiredElements) {
                    String q = req.trim();
                    if (q.isEmpty()) continue;
                    boolean hasElement = profile.elements().keySet().stream().anyMatch(key -> {
                        Element e = AARegistries.ELEMENT_REGISTRY.get(key);
                        if (e == null) return false;
                        if (key.getPath().contains(q)) return true;
                        if (Component.translatable(e.getDescriptionId()).getString().toLowerCase().contains(q)) return true;
                        return false;
                    });
                    if (!hasElement) return false; 
                }
            }

            // 标签筛选 (AND 逻辑)
            if (!tagFilter.isBlank()) {
                String[] requiredTags = tagFilter.split(",");
                for (String req : requiredTags) {
                    String q = req.trim();
                    if (q.isEmpty()) continue;
                    boolean hasTag = stack.getTags().anyMatch(tag -> {
                        String tagLoc = tag.location().toString().toLowerCase();
                        String tagPath = tag.location().getPath().toLowerCase();
                        return tagLoc.contains(q) || tagPath.contains(q);
                    });
                    if (!hasTag) return false;
                }
            }
            
            return true;
        };
    }
}
