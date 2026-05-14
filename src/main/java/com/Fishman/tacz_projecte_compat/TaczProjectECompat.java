package com.Fishman.tacz_projecte_compat;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

@Mod(TaczProjectECompat.MODID)
public final class TaczProjectECompat {
    public static final String MODID = "tacz_projecte_compat";

    public TaczProjectECompat(IEventBus modBus, ModContainer container) {
        // 这里可以先空着；ProjectE Mapper 用 @EMCMapper 注解加载
    }
}
