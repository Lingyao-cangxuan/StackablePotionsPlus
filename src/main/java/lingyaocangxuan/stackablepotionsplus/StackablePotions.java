// 模组入口：注册配置。
package lingyaocangxuan.stackablepotionsplus;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(StackablePotions.MOD_ID)
public class StackablePotions {
    public static final String MOD_ID = "stackablepotionsplus";

    public StackablePotions() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
