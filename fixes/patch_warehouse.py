#!/usr/bin/env python3
"""Patch TarsEntity.java to add warehouse/inventory system."""
import sys
from pathlib import Path

path = Path(sys.argv[1] if len(sys.argv) > 1 else "src/main/java/com/simplespace/tars/TarsEntity.java")
src = path.read_text()

imports = [
    "import net.minecraft.core.NonNullList;",
    "import net.minecraft.nbt.CompoundTag;",
    "import net.minecraft.nbt.ListTag;",
    "import net.minecraft.nbt.Tag;",
    "import net.minecraft.server.level.ServerLevel;",
    "import net.minecraft.world.Containers;",
    "import net.minecraft.world.entity.item.ItemEntity;",
    "import net.minecraft.world.item.ItemStack;",
    "import net.minecraft.world.phys.AABB;",
]
for imp in imports:
    if imp not in src:
        src = src.replace(
            "import net.minecraft.core.BlockPos;",
            "import net.minecraft.core.BlockPos;\n" + imp,
            1,
        )

if "COLLECT_DROPS" not in src:
    src = src.replace(
        """private static final EntityDataAccessor<Boolean> DIGGING =
            SynchedEntityData.defineId(TarsEntity.class, EntityDataSerializers.BOOLEAN);""",
        """private static final EntityDataAccessor<Boolean> DIGGING =
            SynchedEntityData.defineId(TarsEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> COLLECT_DROPS =
            SynchedEntityData.defineId(TarsEntity.class, EntityDataSerializers.BOOLEAN);""",
    )

if "INV_SIZE" not in src:
    src = src.replace(
        "public static final int ORE_SEARCH_RADIUS = 24;",
        "public static final int ORE_SEARCH_RADIUS = 24;\n    public static final int INV_SIZE = 27;",
    )
if "NonNullList<ItemStack> inventory" not in src:
    src = src.replace(
        "private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);",
        "private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);\n    private final NonNullList<ItemStack> inventory = NonNullList.withSize(INV_SIZE, ItemStack.EMPTY);",
    )

if "COLLECT_DROPS, true" not in src and "COLLECT_DROPS, false" not in src:
    src = src.replace(
        "builder.define(DIGGING, false);",
        "builder.define(DIGGING, false);\n        builder.define(COLLECT_DROPS, true);",
    )

if "isCollectDrops" not in src:
    src = src.replace(
        "public void setDigging(boolean v) { this.entityData.set(DIGGING, v); }",
        """public void setDigging(boolean v) { this.entityData.set(DIGGING, v); }
    public boolean isCollectDrops() { return this.entityData.get(COLLECT_DROPS); }
    public void setCollectDrops(boolean v) { this.entityData.set(COLLECT_DROPS, v); }""",
    )

WAREHOUSE = r'''
    public ItemStack insertItem(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack remaining = stack.copy();
        for (int i = 0; i < INV_SIZE && !remaining.isEmpty(); i++) {
            ItemStack slot = inventory.get(i);
            if (slot.isEmpty()) {
                int put = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                inventory.set(i, remaining.copyWithCount(put));
                remaining.shrink(put);
            } else if (ItemStack.isSameItemSameComponents(slot, remaining)
                    && slot.getCount() < slot.getMaxStackSize()) {
                int space = slot.getMaxStackSize() - slot.getCount();
                int put = Math.min(space, remaining.getCount());
                slot.grow(put);
                remaining.shrink(put);
            }
        }
        return remaining;
    }

    public int countItems() {
        int n = 0;
        for (ItemStack s : inventory) if (!s.isEmpty()) n += s.getCount();
        return n;
    }

    public int giveToPlayer(ServerPlayer player, boolean oresOnly) {
        int given = 0;
        for (int i = 0; i < INV_SIZE; i++) {
            ItemStack s = inventory.get(i);
            if (s.isEmpty()) continue;
            if (oresOnly) {
                String k = s.getItem().builtInRegistryHolder().key().location().getPath();
                boolean ore = k.contains("ore") || k.contains("raw_") || k.contains("coal")
                        || k.contains("diamond") || k.contains("emerald") || k.contains("quartz")
                        || k.contains("debris") || k.contains("ingot") || k.contains("lapis")
                        || k.contains("redstone") || k.contains("copper");
                if (!ore) continue;
            }
            ItemStack copy = s.copy();
            if (!player.getInventory().add(copy)) player.drop(copy, false);
            given += s.getCount();
            inventory.set(i, ItemStack.EMPTY);
        }
        return given;
    }

    public void breakBlockForMine(BlockPos pos) {
        if (!(level() instanceof ServerLevel sl)) return;
        BlockState st = level().getBlockState(pos);
        if (st.isAir()) return;
        if (isCollectDrops()) {
            java.util.List<ItemStack> drops = Block.getDrops(st, sl, pos, null, this, ItemStack.EMPTY);
            level().destroyBlock(pos, false, this);
            for (ItemStack d : drops) {
                ItemStack left = insertItem(d);
                if (!left.isEmpty())
                    Containers.dropItemStack(level(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, left);
            }
        } else {
            level().destroyBlock(pos, true, this);
        }
        level().playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.7f, 1.0f);
    }

    public void scoopNearbyItems() {
        if (!isCollectDrops() || level().isClientSide()) return;
        AABB box = getBoundingBox().inflate(2.5);
        for (ItemEntity ie : level().getEntitiesOfClass(ItemEntity.class, box)) {
            if (!ie.isAlive() || ie.hasPickUpDelay()) continue;
            ItemStack left = insertItem(ie.getItem());
            if (left.isEmpty()) ie.discard();
            else ie.setItem(left);
        }
    }

'''

if "insertItem(" not in src:
    marker = "private void playLocal(net.minecraft.sounds.SoundEvent sound, float vol, float pitch) {\n        level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.NEUTRAL, vol, pitch);\n    }"
    if marker in src:
        src = src.replace(marker, marker + "\n" + WAREHOUSE)
    else:
        marker2 = "level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.NEUTRAL, vol, pitch);\n    }"
        src = src.replace(marker2, marker2 + "\n" + WAREHOUSE, 1)

VOICE = '''
        if (containsAny(msg, "отдай", "отдать", "принеси", "дай мне", "give", "выгрузи")) {
            boolean ores = containsAny(msg, "руд", "ore");
            int n = giveToPlayer(sp, ores);
            if (n <= 0) speak(sp, "Склад пуст.");
            else speak(sp, "Перенёс " + n + " шт.");
            return true;
        }
        if (containsAny(msg, "склад", "что в складе", "инвентарь")) {
            speak(sp, "На складе: " + countItems() + " шт. Сбор: " + (isCollectDrops() ? "вкл" : "выкл"));
            return true;
        }
        if (containsAny(msg, "собирай в инвентарь", "сбор вкл", "подбирай")) {
            setCollectDrops(true);
            speak(sp, "Сбор в склад: вкл.");
            return true;
        }
        if (containsAny(msg, "не подбирай", "оставь на земле", "сбор выкл")) {
            setCollectDrops(false);
            speak(sp, "Сбор в склад: выкл.");
            return true;
        }
'''

if 'speak(sp, "Склад пуст.")' not in src:
    old2 = 'if (msg.startsWith("тарс ")) msg = msg.substring(5).trim();'
    if old2 in src:
        src = src.replace(old2, old2 + "\n" + VOICE, 1)

if "scoopNearbyItems();" not in src:
    src = src.replace(
        "tickJokes();\n        if (!miningOrdered && isDigging()",
        "tickJokes();\n        if (isCollectDrops() && (miningOrdered || isDigging()) && tickCount % 5 == 0) {\n            scoopNearbyItems();\n        }\n        if (!miningOrdered && isDigging()",
    )

if "TarsInv" not in src:
    NBT_SAVE = '''
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUUID != null) tag.putUUID("Owner", ownerUUID);
        tag.putInt("Humor", getHumor());
        tag.putBoolean("Following", isFollowing());
        tag.putBoolean("Sprint", isSprintMode());
        tag.putInt("FlashLevel", getFlashLevel());
        tag.putBoolean("CollectDrops", isCollectDrops());
        tag.putBoolean("MiningOrdered", miningOrdered);
        if (mineOreFilter != null) tag.putString("MineFilter", mineOreFilter);
        ListTag inv = new ListTag();
        for (int i = 0; i < INV_SIZE; i++) {
            if (!inventory.get(i).isEmpty()) {
                CompoundTag slot = new CompoundTag();
                slot.putByte("Slot", (byte) i);
                slot.put("Item", inventory.get(i).save(level().registryAccess()));
                inv.add(slot);
            }
        }
        tag.put("TarsInv", inv);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) ownerUUID = tag.getUUID("Owner");
        if (tag.contains("Humor")) setHumor(tag.getInt("Humor"));
        if (tag.contains("Following")) this.entityData.set(FOLLOWING, tag.getBoolean("Following"));
        if (tag.contains("Sprint")) this.entityData.set(SPRINT_MODE, tag.getBoolean("Sprint"));
        if (tag.contains("FlashLevel")) setFlashLevel(tag.getInt("FlashLevel"));
        if (tag.contains("CollectDrops")) setCollectDrops(tag.getBoolean("CollectDrops"));
        miningOrdered = tag.getBoolean("MiningOrdered");
        if (tag.contains("MineFilter")) mineOreFilter = tag.getString("MineFilter");
        inventory.clear();
        ListTag inv = tag.getList("TarsInv", Tag.TAG_COMPOUND);
        for (int i = 0; i < inv.size(); i++) {
            CompoundTag slot = inv.getCompound(i);
            int idx = slot.getByte("Slot") & 255;
            if (idx >= 0 && idx < INV_SIZE && slot.contains("Item")) {
                ItemStack stack = ItemStack.parse(level().registryAccess(), slot.get("Item")).orElse(ItemStack.EMPTY);
                inventory.set(idx, stack);
            }
        }
    }
'''
    if "public void registerControllers" in src and "addAdditionalSaveData" not in src:
        src = src.replace(
            "    @Override\n    public void registerControllers",
            NBT_SAVE + "\n    @Override\n    public void registerControllers",
        )

path.write_text(src)
print("patched", path, "len", len(src))
