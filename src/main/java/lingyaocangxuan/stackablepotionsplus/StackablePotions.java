// 模组入口：注册配置与（客户端专属的）配置界面。
package lingyaocangxuan.stackablepotionsplus;

import lingyaocangxuan.stackablepotionsplus.client.ClientSetup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(StackablePotions.MOD_ID)
public class StackablePotions {
    public static final String MOD_ID = "stackablepotionsplus";

    /**
     * 配置项 {@code general.potionStackSize} 的桥接字段。
     * <p>
     * <b>Mixin 里不能碰 {@code ForgeConfigSpec}</b>，而 {@code Items.<clinit>} 又发生在配置加载
     * 之前（那时读配置会直接抛异常），所以注册期先取这个静态初值 —— 它必须与配置默认值一致。
     * 配置就绪后 {@link StackSizeApplier} 会把它和每个药水物品实例的 {@code maxStackSize}
     * 一起改成玩家设定的值，因此运行时改配置也能生效。
     */
    public static volatile int potionStackSize = 64;

    public StackablePotions() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // 客户端专属：给「模组列表 → 配置」挂上中文界面。
        // 这里用 dist 判断 + 独立的客户端类，专用服务端不会加载任何客户端类。
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.registerConfigScreen();
        }
    }
}
