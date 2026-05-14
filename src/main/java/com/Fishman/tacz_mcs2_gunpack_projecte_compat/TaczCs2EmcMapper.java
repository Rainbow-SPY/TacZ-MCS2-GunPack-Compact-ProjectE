package com.Fishman.tacz_mcs2_gunpack_projecte_compat;

import com.mojang.logging.LogUtils;
import moze_intel.projecte.api.mapper.EMCMapper;
import moze_intel.projecte.api.mapper.IEMCMapper;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.slf4j.Logger;

import com.Fishman.tacz_mcs2_gunpack_projecte_compat.config.ConfigLoader;
import com.Fishman.tacz_mcs2_gunpack_projecte_compat.config.GunRuleConfig;

import java.util.ArrayList;
import java.util.List;

@EMCMapper(priority = 1000, requiredMods = { "tacz" })
public final class TaczCs2EmcMapper implements IEMCMapper<NormalizedSimpleStack, Long> {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 保底规则：即使 JSON 没读到，至少 USP-S 还能工作，方便排错。
     */
    private static final List<GunRule> DEFAULT_RULES = List.of(
            new GunRule(
                    "tacz:modern_kinetic_gun",
                    "mcs2:cs_usp",
                    200,
                    1000,
                    50,
                    12,
                    false,
                    0,
                    "SEMI",
                    List.of(
                            new AttachmentRule(
                                    "AttachmentMUZZLE",
                                    "tacz:attachment",
                                    "mcs2:usp_silencer"))));

    @Override
    public String getName() {
        return "TacZ Gun EMC Mapper";
    }

    @Override
    public String getTranslationKey() {
        return "mapper." + TaczProjectECompat.MODID + ".tacz_gun_emc";
    }

    @Override
    public String getDescription() {
        return "Assigns ProjectE EMC values to TacZ guns from config/tacz_mcs2_gunpack_projecte_compat/guns.json.";
    }

    @Override
    public void addMappings(
            IMappingCollector<NormalizedSimpleStack, Long> mapper,
            ReloadableServerResources serverResources,
            RegistryAccess registryAccess,
            ResourceManager resourceManager) {
        List<GunRule> rules = loadRules();

        int generated = 0;

        for (GunRule rule : rules) {
            ResourceLocation itemId = ResourceLocation.tryParse(rule.itemId());

            if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
                LOGGER.warn("TacZ ProjectE Compat: item id not found: {}", rule.itemId());
                continue;
            }

            Item item = BuiltInRegistries.ITEM.get(itemId);

            if (item == Items.AIR) {
                LOGGER.warn("TacZ ProjectE Compat: item resolved to AIR: {}", rule.itemId());
                continue;
            }

            if (rule.maxAmmo() == null) {
                long emc = calculateEmc(rule, null, false);

                ItemStack stack = new ItemStack(item);
                CompoundTag customData = createGunCustomData(rule, null, false);

                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
                mapper.setValueBefore(NSSItem.createItem(stack), emc);

                generated++;

            } else {
                for (int ammo = 0; ammo <= rule.maxAmmo(); ammo++) {
                    for (boolean hasBulletInBarrel : new boolean[] { false, true }) {
                        long emc = calculateEmc(rule, ammo, hasBulletInBarrel);

                        ItemStack stack = new ItemStack(item);
                        CompoundTag customData = createGunCustomData(rule, ammo, hasBulletInBarrel);

                        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));

                        mapper.setValueBefore(NSSItem.createItem(stack), emc);
                        generated++;
                    }
                }
            }
        }

        LOGGER.info("TacZ ProjectE Compat: generated {} TacZ EMC mappings from {} rules", generated, rules.size());
    }

    private static List<GunRule> loadRules() {
        List<GunRule> rules = new ArrayList<>(DEFAULT_RULES);

        GunRuleConfig config = ConfigLoader.loadOrCreate();

        if (config == null || config.guns == null) {
            LOGGER.warn("TacZ ProjectE Compat: config is empty, using hardcoded default rules only");
            return rules;
        }

        int loaded = 0;

        for (GunRuleConfig.GunEntry entry : config.guns) {
            GunRule rule = createRuleFromConfig(entry);

            if (rule == null) {
                continue;
            }

            rules.add(rule);
            loaded++;
        }

        LOGGER.info("TacZ ProjectE Compat: loaded {} gun rules from config", loaded);

        return rules;
    }

    private static GunRule createRuleFromConfig(GunRuleConfig.GunEntry entry) {
        if (entry == null) {
            return null;
        }

        if (isBlank(entry.itemId)) {
            LOGGER.warn("TacZ ProjectE Compat: skipped config entry because itemId is missing");
            return null;
        }

        if (isBlank(entry.gunId)) {
            LOGGER.warn("TacZ ProjectE Compat: skipped config entry because gunId is missing");
            return null;
        }

        if (isBlank(entry.fireMode)) {
            LOGGER.warn("TacZ ProjectE Compat: skipped {} because fireMode is missing", entry.gunId);
            return null;
        }

        List<AttachmentRule> attachments = new ArrayList<>();

        if (entry.attachments != null) {
            for (GunRuleConfig.AttachmentEntry attachment : entry.attachments) {
                if (attachment == null) {
                    continue;
                }

                if (isBlank(attachment.slotName)
                        || isBlank(attachment.attachmentItemId)
                        || isBlank(attachment.attachmentId)) {
                    LOGGER.warn("TacZ ProjectE Compat: skipped invalid attachment in {}", entry.gunId);
                    continue;
                }

                attachments.add(new AttachmentRule(
                        attachment.slotName,
                        attachment.attachmentItemId,
                        attachment.attachmentId));
            }
        }

        return new GunRule(
                entry.itemId,
                entry.gunId,
                entry.basePrice,
                entry.weaponScale,
                entry.ammoEmc,
                entry.maxAmmo,
                entry.countBarrelBullet,
                entry.attachmentsPrice,
                entry.fireMode,
                attachments);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static long calculateEmc(
            GunRule rule,
            Integer currentAmmo,
            boolean hasBulletInBarrel) {
        long emc = (long) rule.basePrice() * rule.weaponScale()
                + rule.attachmentsPrice();

        if (currentAmmo != null && rule.ammoEmc() != null) {
            emc += (long) currentAmmo * rule.ammoEmc();

            if (rule.countBarrelBullet() && hasBulletInBarrel) {
                emc += rule.ammoEmc();
            }
        }

        return emc;
    }

    private static CompoundTag createGunCustomData(
            GunRule rule,
            Integer ammo,
            boolean hasBulletInBarrel) {
        CompoundTag gun = new CompoundTag();

        gun.putString("GunId", rule.gunId());

        if (!isBlank(rule.fireMode())) {
            gun.putString("GunFireMode", rule.fireMode());
        }

        if (ammo != null) {
            gun.putInt("GunCurrentAmmoCount", ammo);
            gun.putBoolean("HasBulletInBarrel", hasBulletInBarrel);
        }

        for (AttachmentRule attachment : rule.attachments()) {
            gun.put(
                    attachment.slotName(),
                    createAttachmentStackTag(
                            attachment.attachmentItemId(),
                            attachment.attachmentId()));
        }

        return gun;
    }

    private static CompoundTag createAttachmentStackTag(String itemId, String attachmentId) {
        CompoundTag attachmentStack = new CompoundTag();
        attachmentStack.putString("id", itemId);
        attachmentStack.putInt("count", 1);

        CompoundTag attachmentCustomData = new CompoundTag();
        attachmentCustomData.putString("AttachmentId", attachmentId);

        CompoundTag components = new CompoundTag();
        components.put("minecraft:custom_data", attachmentCustomData);

        attachmentStack.put("components", components);

        return attachmentStack;
    }

    private record GunRule(
            String itemId,
            String gunId,

            int basePrice,
            int weaponScale,

            Integer ammoEmc,
            Integer maxAmmo,

            boolean countBarrelBullet,

            int attachmentsPrice,

            String fireMode,

            List<AttachmentRule> attachments) {
    }

    private record AttachmentRule(
            String slotName,
            String attachmentItemId,
            String attachmentId) {
    }
}
