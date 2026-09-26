package com.simplespace.client;

import com.simplespace.tars.TarsEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class TarsGeoRenderer extends GeoEntityRenderer<TarsEntity> {

    public TarsGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new TarsGeoModel());
        this.shadowRadius = 0.4f;
    }
}
