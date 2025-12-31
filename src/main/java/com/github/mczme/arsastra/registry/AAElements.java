package com.github.mczme.arsastra.registry;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.core.element.BasicElement;
import com.github.mczme.arsastra.core.element.Element;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.joml.Vector2f;

import java.util.function.Supplier;

public class AAElements {
    public static final DeferredRegister<Element> ELEMENTS = DeferredRegister.create(AARegistries.ELEMENT_REGISTRY_KEY, ArsAstra.MODID);

    // Cardinal
    public static final Supplier<Element> ORDER = register("order", () -> new BasicElement(new Vector2f(0, 1)));
    public static final Supplier<Element> DEATH = register("death", () -> new BasicElement(new Vector2f(0, -1)));
    public static final Supplier<Element> MATTER = register("matter", () -> new BasicElement(new Vector2f(1, 0)));
    public static final Supplier<Element> LIFE = register("life", () -> new BasicElement(new Vector2f(-1, 0)));

    // Intercardinal
    public static final Supplier<Element> STRUCTURE = register("structure", () -> new BasicElement(new Vector2f(1, 1).normalize()));
    public static final Supplier<Element> GROWTH = register("growth", () -> new BasicElement(new Vector2f(-1, 1).normalize()));
    public static final Supplier<Element> CORROSION = register("corrosion", () -> new BasicElement(new Vector2f(1, -1).normalize()));
    public static final Supplier<Element> DECAY = register("decay", () -> new BasicElement(new Vector2f(-1, -1).normalize()));


    private static Supplier<Element> register(String name, Supplier<Element> supplier) {
        return ELEMENTS.register(name, supplier);
    }

    public static void register(IEventBus eventBus) {
        ELEMENTS.register(eventBus);
    }
}
