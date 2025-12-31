package com.github.mczme.arsastra.core.environment;

import com.github.mczme.arsastra.registry.AARegistries;

import net.minecraft.Util;

public interface EnvironmentType {

    default String getDescriptionId() {
        return Util.makeDescriptionId("environmentType", AARegistries.ENVIRONMENT_TYPE_REGISTRY.getKey(this));
    }
    
}
