package com.legoatoom.develop.client.render.entity;

import com.legoatoom.develop.client.render.entity.model.ChainKnotEntityModel;
import com.legoatoom.develop.enitity.ChainKnotEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.world.LightType;

/**
 * @see net.minecraft.client.render.entity.LeashKnotEntityRenderer
 */
@Environment(EnvType.CLIENT)
public class ChainKnotEntityRenderer extends EntityRenderer<ChainKnotEntity> {
    private static final Identifier TEXTURE = new Identifier("minecraft:textures/entity/lead_knot.png");
    private final ChainKnotEntityModel<ChainKnotEntity> model = new ChainKnotEntityModel<>();

    public ChainKnotEntityRenderer(EntityRenderDispatcher entityRenderDispatcher) {
        super(entityRenderDispatcher);
    }

    public void render(ChainKnotEntity chainKnotEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        super.render(chainKnotEntity, f, g, matrixStack, vertexConsumerProvider, i);Entity entity = chainKnotEntity.getHoldingEntity();
        matrixStack.push();
        matrixStack.scale(-1.0F, -1.0F, 1.0F);
        this.model.setAngles(chainKnotEntity, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(this.model.getLayer(TEXTURE));
        this.model.render(matrixStack, vertexConsumer, i, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
        matrixStack.pop();
        if (entity != null){
            this.method_4073(chainKnotEntity, g, matrixStack, vertexConsumerProvider, entity);
        }
    }

    public Identifier getTexture(ChainKnotEntity chainKnotEntity) {
        return TEXTURE;
    }

    private void method_4073(ChainKnotEntity fromEntity, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, Entity chainOrPlayerEntity) {
        matrixStack.push();
        double d = MathHelper.lerp(f * 0.5F, chainOrPlayerEntity.yaw, chainOrPlayerEntity.prevYaw) * 0.017453292F;
        double e = MathHelper.lerp(f * 0.5F, chainOrPlayerEntity.pitch, chainOrPlayerEntity.prevPitch) * 0.017453292F;
        double g = Math.cos(d);
        double h = Math.sin(d);
        double i = Math.sin(e);
        float t;
        float r;
        float s;
        if (chainOrPlayerEntity instanceof AbstractDecorationEntity) {
            g = 0.0D;
            h = 0.0D;
            double k = MathHelper.lerp(f, chainOrPlayerEntity.prevX, chainOrPlayerEntity.getX());
            double l = MathHelper.lerp(f, chainOrPlayerEntity.prevY, chainOrPlayerEntity.getY());
            double m = MathHelper.lerp(f, chainOrPlayerEntity.prevZ, chainOrPlayerEntity.getZ());
            double o = MathHelper.lerp(f, fromEntity.prevX, fromEntity.getX());
            double p = MathHelper.lerp(f, fromEntity.prevY, fromEntity.getY());
            double q = MathHelper.lerp(f, fromEntity.prevZ, fromEntity.getZ());
            matrixStack.translate(g, 0.3F, h);
            r = (float) (k - o);
            s = (float) (l - p);
            t = (float) (m - q);
        } else {
            double j = Math.cos(e);
            double k = MathHelper.lerp(f, chainOrPlayerEntity.prevX, chainOrPlayerEntity.getX()) - g * 0.7D - h * 0.5D * j;
            double l = MathHelper.lerp(f, chainOrPlayerEntity.prevY + (double) chainOrPlayerEntity.getStandingEyeHeight() * 0.7D, chainOrPlayerEntity.getY() + (double) chainOrPlayerEntity.getStandingEyeHeight() * 0.7D) - i * 0.5D - 0.5D;
            double m = MathHelper.lerp(f, chainOrPlayerEntity.prevZ, chainOrPlayerEntity.getZ()) - h * 0.7D + g * 0.5D * j;
            double o = MathHelper.lerp(f, fromEntity.prevX, fromEntity.getX());
            double p = MathHelper.lerp(f, fromEntity.prevY, fromEntity.getY()) + 0.3F;
            double q = MathHelper.lerp(f, fromEntity.prevZ, fromEntity.getZ());
            matrixStack.translate(0,0.3F, 0);
            r = (float) (k - o);
            s = (float) (l - p);
            t = (float) (m - q);
        }
        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayer.getLeash());
        Matrix4f matrix4f = matrixStack.peek().getModel();
        float v = MathHelper.fastInverseSqrt(r * r + t * t) * 0.025F / 2.0F;
        float w = t * v;
        float x = r * v;
        int y = this.getBlockLight(fromEntity, f);
        int z = chainOrPlayerEntity.isOnFire() ? 15 : chainOrPlayerEntity.world.getLightLevel(LightType.BLOCK, new BlockPos(chainOrPlayerEntity.getCameraPosVec(f)));
        int aa = fromEntity.world.getLightLevel(LightType.SKY, new BlockPos(fromEntity.getCameraPosVec(f)));
        int ab = fromEntity.world.getLightLevel(LightType.SKY, new BlockPos(chainOrPlayerEntity.getCameraPosVec(f)));
        method_23186(vertexConsumer, matrix4f, r, s, t, y, z, aa, ab, 0.025F, 0.025F, w, x);
        method_23186(vertexConsumer, matrix4f, r, s, t, y, z, aa, ab, 0.025F, 0.0F, w, x);
        matrixStack.pop();
    }

    public static void method_23186(VertexConsumer vertexConsumer, Matrix4f matrix4f, float f, float g, float h, int i, int j, int k, int l, float m, float n, float o, float p) {
        for(int r = 0; r < 24; ++r) {
            float s = (float)r / 23.0F;
            int t = (int)MathHelper.lerp(s, (float)i, (float)j);
            int u = (int)MathHelper.lerp(s, (float)k, (float)l);
            int v = LightmapTextureManager.pack(t, u);
            method_23187(vertexConsumer, matrix4f, v, f, g, h, m, n, 24, r, false, o, p, r);
            method_23187(vertexConsumer, matrix4f, v, f, g, h, m, n, 24, r + 1, true, o, p, r + 1);
        }

    }

    public static void method_23187(VertexConsumer vertexConsumer, Matrix4f matrix4f, int i, float f, float g, float h, float j, float k, int l, int m, boolean bl, float n, float o, int dip) {
        float p = 0.5F;
        float q = 0.4F;
        float r = 0.3F;
        if (m % 2 == 0) {
            p *= 0.7F;
            q *= 0.7F;
            r *= 0.7F;
        }

        double x = (0.0023D * (dip*dip) - 0.0529D * dip);
        float s = ((float) m / (float) l);
        float t = f * s;
        float u = g * s;
        float v = h * s;
        if (!bl) {
            vertexConsumer.vertex(matrix4f, t + n, (float) (u + j - k + x), v - o).color(p, q, r, 1.0F).light(i).next();
        }

        vertexConsumer.vertex(matrix4f, t - n, (float) (u + k + x), v + o).color(p, q, r, 1.0F).light(i).next();
        if (bl) {
            vertexConsumer.vertex(matrix4f, t + n, (float) (u + j - k + x), v - o).color(p, q, r, 1.0F).light(i).next();
        }
    }

    @Override
    public boolean shouldRender(ChainKnotEntity entity, Frustum frustum, double x, double y, double z) {
        if (super.shouldRender(entity, frustum, x, y, z)){
            return true;
        } else {
            Entity entity1 = entity.getHoldingEntity();
            return entity1 != null && frustum.isVisible(entity1.getVisibilityBoundingBox());
        }
    }

}
