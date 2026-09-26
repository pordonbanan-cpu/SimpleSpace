package com.simplespace.client;

import com.simplespace.SimpleSpace;
import com.simplespace.tars.TarsEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TarsGeoModel extends GeoModel<TarsEntity> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(SimpleSpace.MOD_ID, "geo/entity/tars.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(SimpleSpace.MOD_ID, "textures/entity/tars.png");
    private static final ResourceLocation ANIM =
            ResourceLocation.fromNamespaceAndPath(SimpleSpace.MOD_ID, "animations/entity/tars.animation.json");

    @Override
    public ResourceLocation getModelResource(TarsEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(TarsEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(TarsEntity animatable) {
        return ANIM;
    }
}
