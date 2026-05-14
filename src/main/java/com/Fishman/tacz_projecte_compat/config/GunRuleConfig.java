package com.Fishman.tacz_projecte_compat.config;

import java.util.ArrayList;
import java.util.List;

public class GunRuleConfig {

    public List<GunEntry> guns = new ArrayList<>();

    public static class GunEntry {
        public String itemId;
        public String gunId;

        public int basePrice;
        public int weaponScale;

        // null means this item has no ammo state, for example knives.
        public Integer ammoEmc;
        public Integer maxAmmo;

        public boolean countBarrelBullet;
        public int attachmentsPrice;

        public String fireMode;

        public List<AttachmentEntry> attachments = new ArrayList<>();
    }

    public static class AttachmentEntry {
        public String slotName;
        public String attachmentItemId;
        public String attachmentId;
    }
}
