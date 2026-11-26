package pl.mythoscore.mixin;

import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.util.math.AffineTransformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DisplayEntity.class)
public interface DisplayEntityMixin {

    @Invoker("setBillboardMode")
    void mythos$setBillboardMode(DisplayEntity.BillboardMode billboardMode);

    @Invoker("setTransformation")
    void mythos$setTransformation(AffineTransformation transformation);
}
