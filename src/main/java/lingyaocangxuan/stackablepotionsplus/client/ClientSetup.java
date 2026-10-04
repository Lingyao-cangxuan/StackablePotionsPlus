// 客户端初始化：把配置界面挂到「模组列表 → 配置」。
package lingyaocangxuan.stackablepotionsplus.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * 只在客户端被加载（主类里用 {@code FMLEnvironment.dist} 判断后才会调用），
 * 因此专用服务端不会碰到任何客户端类。
 */
@OnlyIn(Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {
    }

    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> new PotionConfigScreen(parent)));
    }
}
