package pl.mythoscore.mixin;

import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DisplayEntity.TextDisplayEntity.class)
public interface TextDisplayMixin {

    @Invoker("setText")
    void mythos$setText(Text text);

    @Invoker("setDisplayFlags")
    void mythos$setDisplayFlags(byte flags);

    @Invoker("setBackground")
    void mythos$setBackground(int background);
}
