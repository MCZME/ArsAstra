package com.github.mczme.arsastra.registry;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.core.element.BasicElement;
import com.github.mczme.arsastra.core.element.Element;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.joml.Vector2f;

import java.util.function.Supplier;

public class AAElements {
    public static final DeferredRegister<Element> ELEMENTS = DeferredRegister.create(AARegistries.ELEMENT_REGISTRY_KEY, ArsAstra.MODID);

    // Cardinal
    public static final DeferredHolder<Element, BasicElement> ORDER = register("order", () -> new BasicElement(new Vector2f(0, 1)));
    public static final DeferredHolder<Element, BasicElement> DEATH = register("death", () -> new BasicElement(new Vector2f(0, -1)));
    public static final DeferredHolder<Element, BasicElement> MATTER = register("matter", () -> new BasicElement(new Vector2f(-1, 0)));
    public static final DeferredHolder<Element, BasicElement> LIFE = register("life", () -> new BasicElement(new Vector2f(1, 0)));

    // Intercardinal
    public static final DeferredHolder<Element, BasicElement> STRUCTURE = register("structure", () -> new BasicElement(new Vector2f(-1, 1).normalize()));
    public static final DeferredHolder<Element, BasicElement> GROWTH = register("growth", () -> new BasicElement(new Vector2f(1, 1).normalize()));
    public static final DeferredHolder<Element, BasicElement> CORROSION = register("corrosion", () -> new BasicElement(new Vector2f(-1, -1).normalize()));
    public static final DeferredHolder<Element, BasicElement> DECAY = register("decay", () -> new BasicElement(new Vector2f(1, -1).normalize()));


    private static <T extends Element> DeferredHolder<Element, T> register(String name, Supplier<T> supplier) {
        return ELEMENTS.register(name, supplier);
    }

    public static void register(IEventBus eventBus) {
        ELEMENTS.register(eventBus);
    }
}
