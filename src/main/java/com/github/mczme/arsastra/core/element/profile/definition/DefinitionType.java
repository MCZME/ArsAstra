package com.github.mczme.arsastra.core.element.profile.definition;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum DefinitionType implements StringRepresentable {
    BASIC("basic"),
    TEMPLATE("template"),
    RANDOM("random");

    public static final Codec<DefinitionType> CODEC = StringRepresentable.fromEnum(DefinitionType::values);

    private final String name;

    DefinitionType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
