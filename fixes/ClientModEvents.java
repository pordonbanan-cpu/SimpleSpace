package com.simplespace.client;

import com.simplespace.SimpleSpace;
import com.simplespace.entity.ModEntities;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

@EventBusSubscriber(modid = SimpleSpace.MOD_ID, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SUN.get(), CelestialBodyRenderer::new);
        event.registerEntityRenderer(ModEntities.EARTH.get(), CelestialBodyRenderer::new);
        event.registerEntityRenderer(ModEntities.MOON.get(), CelestialBodyRenderer::new);
        event.registerEntityRenderer(ModEntities.TARS.get(), TarsGeoRenderer::new);
        SimpleSpace.LOGGER.info("Registered renderers (planets + GeckoLib TARS)");
    }

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(
            ResourceLocation.fromNamespaceAndPath(SimpleSpace.MOD_ID, "space"),
            new SpaceDimensionEffects()
        );
        SimpleSpace.LOGGER.info("Registered Space dimension special effects");
    }
}
