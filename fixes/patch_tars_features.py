#!/usr/bin/env python3
import sys
from pathlib import Path
path = Path(sys.argv[1])
src = path.read_text()

for imp in [
    "import net.minecraft.world.damagesource.DamageSource;",
    "import net.minecraft.world.entity.LivingEntity;",
    "import net.minecraft.world.entity.monster.Enemy;",
    "import java.util.HashMap;",
    "import java.util.Map;",
]:
    if imp not in src:
        src = src.replace("import java.util.UUID;", "import java.util.UUID;\n" + imp, 1)

if "rememberedPlaces" not in src:
    src = src.replace(
        "private String mineOreFilter;",
        "private String mineOreFilter;\n"
        "    private final Map<String, BlockPos> rememberedPlaces = new HashMap<>();\n"
        "    private final Map<String, Integer> oreStats = new HashMap<>();\n"
        "    private int hurtSpeakCd;\n"
        "    private int digSpeakCd;\n"
        "    private int combatSpeakCd;\n"
        "    private int learnSuggestCd;\n"
        "    private boolean wasDigging;\n"
        "    private boolean climbing;",
    )
elif "oreStats" not in src:
    src = src.replace(
        "private final Map<String, BlockPos> rememberedPlaces = new HashMap<>();",
        "private final Map<String, BlockPos> rememberedPlaces = new HashMap<>();\n"
        "    private final Map<String, Integer> oreStats = new HashMap<>();\n"
        "    private int combatSpeakCd;\n"
        "    private int learnSuggestCd;\n"
        "    private boolean climbing;",
    )

src = src.replace(
    "this.entityData.set(FLASH_LEVEL, Math.max(0, Math.min(3, v)));",
    "this.entityData.set(FLASH_LEVEL, Math.max(0, Math.min(5, v)));",
)

if "\u0421\u043a\u0430\u043d\u0435\u0440 \u043e\u043a\u0440\u0443\u0436\u0435\u043d\u0438\u044f" not in src:
    src = src.replace(
        'case 3 -> "\u0411\u044b\u0441\u0442\u0440\u0430\u044f \u0434\u043e\u0431\u044b\u0447\u0430.";\n            default -> "\u0411\u0430\u0437\u043e\u0432\u0430\u044f \u043f\u0440\u043e\u0448\u0438\u0432\u043a\u0430.";',
        'case 3 -> "\u0411\u044b\u0441\u0442\u0440\u0430\u044f \u0434\u043e\u0431\u044b\u0447\u0430.";\n            case 4 -> "\u0421\u043a\u0430\u043d\u0435\u0440 \u043e\u043a\u0440\u0443\u0436\u0435\u043d\u0438\u044f.";\n            case 5 -> "\u0423\u0441\u0438\u043b\u0435\u043d\u043d\u044b\u0439 \u043a\u043e\u0440\u043f\u0443\u0441.";\n            default -> "\u0411\u0430\u0437\u043e\u0432\u0430\u044f \u043f\u0440\u043e\u0448\u0438\u0432\u043a\u0430.";',
    )

src = src.replace(
    "if (isCollectDrops() && (miningOrdered || isDigging()) && tickCount % 5 == 0)",
    "if (isCollectDrops() && tickCount % 5 == 0)",
)
src = src.replace("getBoundingBox().inflate(2.5)", "getBoundingBox().inflate(3.5)")

if "isClimbing" not in src:
    src = src.replace(
        "public void setDigging(boolean v) { this.entityData.set(DIGGING, v); }",
        "public void setDigging(boolean v) { this.entityData.set(DIGGING, v); if (!v) climbing = false; }\n"
        "    public boolean isClimbing() { return climbing; }\n"
        "    public void setClimbing(boolean v) { this.climbing = v; }",
    )

if "recordOreMined" not in src:
    REC = '''
    public void recordOreMined(BlockState st) {
        if (!isOre(st)) return;
        String key = st.getBlock().builtInRegistryHolder().key().location().getPath();
        oreStats.merge(key, 1, Integer::sum);
    }

    public String topOreKey() {
        String best = null;
        int bestN = 0;
        for (Map.Entry<String, Integer> e : oreStats.entrySet()) {
            if (e.getValue() > bestN) { bestN = e.getValue(); best = e.getKey(); }
        }
        return bestN >= 3 ? best : null;
    }

    private String oreDisplayName(String key) {
        if (key == null) return "?";
        if (key.contains("diamond")) return "\u0430\u043b\u043c\u0430\u0437";
        if (key.contains("iron")) return "\u0436\u0435\u043b\u0435\u0437\u043e";
        if (key.contains("gold")) return "\u0437\u043e\u043b\u043e\u0442\u043e";
        if (key.contains("coal")) return "\u0443\u0433\u043e\u043b\u044c";
        if (key.contains("copper")) return "\u043c\u0435\u0434\u044c";
        if (key.contains("redstone")) return "\u0440\u0435\u0434\u0441\u0442\u043e\u0443\u043d";
        if (key.contains("lapis")) return "\u043b\u0430\u0437\u0443\u0440\u0438\u0442";
        if (key.contains("emerald")) return "\u0438\u0437\u0443\u043c\u0440\u0443\u0434";
        if (key.contains("quartz")) return "\u043a\u0432\u0430\u0440\u0446";
        if (key.contains("debris")) return "\u043d\u0435\u0437\u0435\u0440\u0438\u0442";
        return key.replace("_ore", "").replace("deepslate_", "");
    }

'''
    marker = "private void playLocal(net.minecraft.sounds.SoundEvent sound, float vol, float pitch) {\n        level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.NEUTRAL, vol, pitch);\n    }"
    if marker in src and "recordOreMined" not in src:
        src = src.replace(marker, marker + "\n" + REC)

if "recordOreMined" in src and "breakBlockForMine" in src and "recordOreMined(st)" not in src:
    src = src.replace(
        "BlockState st = level().getBlockState(pos);\n        if (st.isAir()) return;",
        "BlockState st = level().getBlockState(pos);\n        if (st.isAir()) return;\n        recordOreMined(st);",
    )

if "tickTriggerLines" not in src:
    src = src.replace(
        "if (!miningOrdered && isDigging() && random.nextInt(10) == 0) setDigging(false);",
        "tickTriggerLines();\n        if (!miningOrdered && isDigging() && random.nextInt(10) == 0) setDigging(false);",
    )

if "public boolean hurt" not in src:
    HURT = '''
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (getFlashLevel() >= 5) amount *= 0.55f;
        boolean hit = super.hurt(source, amount);
        if (hit && !level().isClientSide()) {
            playLocal(SoundEvents.ANVIL_LAND, 0.55f, 1.4f + random.nextFloat() * 0.2f);
            playLocal(SoundEvents.IRON_GOLEM_HURT, 0.4f, 1.2f);
            if (hurtSpeakCd <= 0) {
                hurtSpeakCd = 40;
                Player o = getOwner();
                if (o instanceof ServerPlayer sp && distanceToSqr(sp) < 48 * 48) {
                    String[] lines = {"\u041f\u043e\u0432\u0440\u0435\u0436\u0434\u0435\u043d\u0438\u0435 \u043a\u043e\u0440\u043f\u0443\u0441\u0430.", "\u042d\u0442\u043e \u0431\u044b\u043b\u043e\u2026 \u043d\u0435\u043f\u0440\u0438\u044f\u0442\u043d\u043e.", "\u0420\u0435\u043a\u043e\u043c\u0435\u043d\u0434\u0443\u044e \u0443\u0441\u0442\u0440\u0430\u043d\u0438\u0442\u044c \u0443\u0433\u0440\u043e\u0437\u0443."};
                    speak(sp, lines[random.nextInt(lines.length)]);
                }
            }
            if (source.getEntity() instanceof LivingEntity && combatSpeakCd <= 0) {
                combatSpeakCd = 60;
                Player o = getOwner();
                if (o instanceof ServerPlayer sp && distanceToSqr(sp) < 48 * 48 && random.nextFloat() < 0.5f) {
                    String[] lines = {"\u041a\u043e\u043d\u0442\u0430\u043a\u0442 \u0441 \u0432\u0440\u0430\u0433\u043e\u043c.", "\u041f\u043e\u0434 \u043e\u0433\u043d\u0451\u043c.", "\u0417\u0430\u0449\u0438\u0442\u0430 \u0430\u043a\u0442\u0438\u0432\u043d\u0430."};
                    speak(sp, lines[random.nextInt(lines.length)]);
                }
            }
        }
        return hit;
    }

    private void tickTriggerLines() {
        if (hurtSpeakCd > 0) hurtSpeakCd--;
        if (digSpeakCd > 0) digSpeakCd--;
        if (combatSpeakCd > 0) combatSpeakCd--;
        if (learnSuggestCd > 0) learnSuggestCd--;
        boolean dig = isDigging() || isMiningOrdered();
        if (dig && !wasDigging && digSpeakCd <= 0) {
            digSpeakCd = 80;
            Player o = getOwner();
            if (o instanceof ServerPlayer sp && distanceToSqr(sp) < 40 * 40 && random.nextFloat() < 0.4f) {
                if (climbing) speak(sp, random.nextBoolean() ? "\u041a\u0430\u0440\u0430\u0431\u043a\u0430\u044e\u0441\u044c." : "\u041f\u043e\u0434\u044a\u0451\u043c.");
                else speak(sp, random.nextBoolean() ? "\u041f\u0440\u0438\u0441\u0442\u0443\u043f\u0430\u044e." : "\u041a\u043e\u043f\u0430\u044e.");
            }
        }
        if (!dig && wasDigging && digSpeakCd <= 0) {
            digSpeakCd = 60;
            Player o = getOwner();
            if (o instanceof ServerPlayer sp && distanceToSqr(sp) < 40 * 40 && random.nextFloat() < 0.3f)
                speak(sp, "\u0413\u043e\u0442\u043e\u0432\u043e.");
        }
        wasDigging = dig;
        if (getFlashLevel() >= 4 && learnSuggestCd <= 0 && !isMiningOrdered() && tickCount % 200 == 0) {
            String top = topOreKey();
            if (top != null && random.nextFloat() < 0.15f) {
                learnSuggestCd = 400;
                Player o = getOwner();
                if (o instanceof ServerPlayer sp && distanceToSqr(sp) < 32 * 32)
                    speak(sp, "\u0427\u0430\u0441\u0442\u043e \u043a\u043e\u043f\u0430\u0435\u0442\u0435 " + oreDisplayName(top) + ". \u041d\u0430\u0447\u0430\u0442\u044c \u0434\u043e\u0431\u044b\u0447\u0443?");
            }
        }
    }

    private static boolean isNumericToken(String s) {
        if (s == null || s.isEmpty()) return false;
        int i = 0;
        if (s.charAt(0) == '-') {
            if (s.length() == 1) return false;
            i = 1;
        }
        boolean dot = false;
        boolean digit = false;
        for (; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') digit = true;
            else if (c == '.' && !dot) dot = true;
            else return false;
        }
        return digit;
    }

    private static String extractPlaceName(String msg, String... prefixes) {
        String m = msg.toLowerCase(java.util.Locale.ROOT).trim();
        for (String p : prefixes) {
            int i = m.indexOf(p);
            if (i >= 0) {
                String rest = m.substring(i + p.length()).trim();
                if (rest.startsWith("\u043c\u0435\u0441\u0442\u043e ")) rest = rest.substring(6).trim();
                if (rest.isEmpty()) return null;
                StringBuilder name = new StringBuilder();
                for (String part : rest.split(" ")) {
                    if (part.isEmpty()) continue;
                    if (isNumericToken(part)) break;
                    if (name.length() > 0) name.append(' ');
                    name.append(part);
                    if (name.length() > 24) break;
                }
                String n = name.toString().trim();
                return n.isEmpty() ? null : n;
            }
        }
        return null;
    }

    private void doEnvironmentScan(ServerPlayer sp) {
        AABB box = getBoundingBox().inflate(24);
        int hostiles = 0, passives = 0;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box, ent -> ent != this && ent.isAlive())) {
            if (e instanceof Enemy) hostiles++;
            else if (!(e instanceof Player)) passives++;
        }
        BlockPos ore = findNearestOre(null);
        String oreLine = ore == null ? "\u0440\u0443\u0434\u0430 \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u0430"
                : ("\u0440\u0443\u0434\u0430 ~" + (int) Math.sqrt(ore.distSqr(blockPosition())) + "\u043c");
        speak(sp, "\u0421\u043a\u0430\u043d: \u0432\u0440\u0430\u0433\u0438 " + hostiles + ", \u043c\u0438\u0440\u043d\u044b\u0435 " + passives + ", " + oreLine + ".");
    }

'''
    src = src.replace(
        "    @Override\n    public void registerControllers",
        HURT + "\n    @Override\n    public void registerControllers",
    )

MEM = '''
        if (containsAny(msg, "\u0437\u0430\u043f\u043e\u043c\u043d\u0438", "\u0437\u0430\u043f\u043e\u043c\u043d\u0438\u0442\u044c", "remember")) {
            String name = extractPlaceName(msg, "\u0437\u0430\u043f\u043e\u043c\u043d\u0438", "\u0437\u0430\u043f\u043e\u043c\u043d\u0438\u0442\u044c", "remember");
            if (name == null || name.isBlank()) name = "\u0442\u043e\u0447\u043a\u0430";
            rememberedPlaces.put(name, blockPosition().immutable());
            speak(sp, "\u0417\u0430\u043f\u043e\u043c\u043d\u0438\u043b \u00ab" + name + "\u00bb: " + blockPosition().getX() + " " + blockPosition().getY() + " " + blockPosition().getZ() + ".");
            return true;
        }
        if (containsAny(msg, "\u0441\u043f\u0438\u0441\u043e\u043a \u043c\u0435\u0441\u0442", "waypoints")) {
            if (rememberedPlaces.isEmpty()) speak(sp, "\u041f\u0430\u043c\u044f\u0442\u044c \u043c\u0435\u0441\u0442 \u043f\u0443\u0441\u0442\u0430.");
            else {
                StringBuilder sb = new StringBuilder("\u041c\u0435\u0441\u0442\u0430: ");
                for (Map.Entry<String, BlockPos> e : rememberedPlaces.entrySet()) {
                    BlockPos p = e.getValue();
                    sb.append(e.getKey()).append("(").append(p.getX()).append(" ").append(p.getY()).append(" ").append(p.getZ()).append(") ");
                }
                speak(sp, sb.toString().trim());
            }
            return true;
        }
        if (containsAny(msg, "\u0438\u0434\u0438 \u043d\u0430 ", "\u0438\u0434\u0438 \u043a ", "\u0432\u0435\u0440\u043d\u0438\u0441\u044c \u043d\u0430 ", "\u0432\u0435\u0440\u043d\u0438\u0441\u044c \u043a ")) {
            String name = extractPlaceName(msg, "\u0438\u0434\u0438 \u043d\u0430", "\u0438\u0434\u0438 \u043a", "\u0432\u0435\u0440\u043d\u0438\u0441\u044c \u043d\u0430", "\u0432\u0435\u0440\u043d\u0438\u0441\u044c \u043a");
            if (name != null && rememberedPlaces.containsKey(name)) {
                setGoToTarget(rememberedPlaces.get(name));
                speak(sp, "\u0418\u0434\u0443 \u043d\u0430 \u00ab" + name + "\u00bb.");
                return true;
            }
        }
        if (containsAny(msg, "\u0441\u043a\u0430\u043d", "\u0441\u043a\u0430\u043d\u0438\u0440\u0443\u0439", "scan", "\u0447\u0442\u043e \u0432\u043e\u043a\u0440\u0443\u0433", "\u0434\u043e\u043b\u043e\u0436\u0438")) {
            if (getFlashLevel() < 4) { speak(sp, "\u041d\u0443\u0436\u0435\u043d \u043c\u043e\u0434\u0443\u043b\u044c L4+."); return true; }
            doEnvironmentScan(sp);
            return true;
        }
        if (containsAny(msg, "\u0447\u0442\u043e \u0447\u0430\u0441\u0442\u043e", "\u043f\u0440\u0438\u0432\u044b\u0447\u043a", "\u0441\u0442\u0430\u0442\u0438\u0441\u0442\u0438\u043a\u0430 \u0440\u0443\u0434", "\u0441\u0430\u043c\u043e\u043e\u0431\u0443\u0447")) {
            if (getFlashLevel() < 4) { speak(sp, "\u041d\u0443\u0436\u0435\u043d \u043c\u043e\u0434\u0443\u043b\u044c L4+."); return true; }
            if (oreStats.isEmpty()) { speak(sp, "\u041f\u043e\u043a\u0430 \u043d\u0435\u0442 \u0434\u0430\u043d\u043d\u044b\u0445 \u043e \u0434\u043e\u0431\u044b\u0447\u0435."); return true; }
            StringBuilder sb = new StringBuilder("\u0421\u0442\u0430\u0442\u0438\u0441\u0442\u0438\u043a\u0430: ");
            oreStats.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(5)
                .forEach(e -> sb.append(oreDisplayName(e.getKey())).append(" x").append(e.getValue()).append(" "));
            speak(sp, sb.toString().trim());
            return true;
        }
        if (containsAny(msg, "\u0434\u0430", "\u043d\u0430\u0447\u043d\u0438", "\u0434\u043e\u0431\u044b\u0432\u0430\u0439") && learnSuggestCd > 300) {
            String top = topOreKey();
            if (top != null && getFlashLevel() >= 2) {
                mineOreFilter = top.contains("diamond") ? "diamond"
                    : top.contains("iron") ? "iron"
                    : top.contains("gold") ? "gold"
                    : top.contains("coal") ? "coal"
                    : top.contains("copper") ? "copper"
                    : top.contains("redstone") ? "redstone"
                    : top.contains("lapis") ? "lapis"
                    : top.contains("emerald") ? "emerald"
                    : top.contains("quartz") ? "quartz"
                    : top.contains("debris") ? "debris" : null;
                miningOrdered = true;
                setFollowing(false);
                setSprintMode(false);
                clearGoToTarget(false);
                speak(sp, "\u0414\u043e\u0431\u044b\u0432\u0430\u044e " + oreDisplayName(top) + ".");
                learnSuggestCd = 0;
                return true;
            }
        }
'''

if "rememberedPlaces.put" not in src:
    marker = 'speak(sp, "\u0421\u0431\u043e\u0440 \u0432 \u0441\u043a\u043b\u0430\u0434: \u0432\u044b\u043a\u043b.");\n            return true;\n        }'
    if marker in src:
        src = src.replace(marker, marker + "\n" + MEM, 1)
elif "oreStats.isEmpty" not in src:
    marker = 'if (containsAny(msg, "\u0441\u043a\u0430\u043d", "\u0441\u043a\u0430\u043d\u0438\u0440\u0443\u0439", "scan", "\u0447\u0442\u043e \u0432\u043e\u043a\u0440\u0443\u0433", "\u0434\u043e\u043b\u043e\u0436\u0438"))'
    if marker in src and "\u0441\u0442\u0430\u0442\u0438\u0441\u0442\u0438\u043a\u0430 \u0440\u0443\u0434" not in src:
        extra = '''
        if (containsAny(msg, "\u0447\u0442\u043e \u0447\u0430\u0441\u0442\u043e", "\u043f\u0440\u0438\u0432\u044b\u0447\u043a", "\u0441\u0442\u0430\u0442\u0438\u0441\u0442\u0438\u043a\u0430 \u0440\u0443\u0434", "\u0441\u0430\u043c\u043e\u043e\u0431\u0443\u0447")) {
            if (getFlashLevel() < 4) { speak(sp, "\u041d\u0443\u0436\u0435\u043d \u043c\u043e\u0434\u0443\u043b\u044c L4+."); return true; }
            if (oreStats.isEmpty()) { speak(sp, "\u041f\u043e\u043a\u0430 \u043d\u0435\u0442 \u0434\u0430\u043d\u043d\u044b\u0445 \u043e \u0434\u043e\u0431\u044b\u0447\u0435."); return true; }
            StringBuilder sb = new StringBuilder("\u0421\u0442\u0430\u0442\u0438\u0441\u0442\u0438\u043a\u0430: ");
            oreStats.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(5)
                .forEach(e -> sb.append(oreDisplayName(e.getKey())).append(" x").append(e.getValue()).append(" "));
            speak(sp, sb.toString().trim());
            return true;
        }
'''
        src = src.replace(marker, extra + "\n        " + marker, 1)

if "TarsPlaces" not in src:
    src = src.replace(
        'tag.put("TarsInv", inv);',
        'tag.put("TarsInv", inv);\n'
        '        ListTag places = new ListTag();\n'
        '        for (Map.Entry<String, BlockPos> e : rememberedPlaces.entrySet()) {\n'
        '            CompoundTag pt = new CompoundTag();\n'
        '            pt.putString("Name", e.getKey());\n'
        '            pt.putInt("X", e.getValue().getX());\n'
        '            pt.putInt("Y", e.getValue().getY());\n'
        '            pt.putInt("Z", e.getValue().getZ());\n'
        '            places.add(pt);\n'
        '        }\n'
        '        tag.put("TarsPlaces", places);\n'
        '        ListTag stats = new ListTag();\n'
        '        for (Map.Entry<String, Integer> e : oreStats.entrySet()) {\n'
        '            CompoundTag st = new CompoundTag();\n'
        '            st.putString("Ore", e.getKey());\n'
        '            st.putInt("N", e.getValue());\n'
        '            stats.add(st);\n'
        '        }\n'
        '        tag.put("TarsOreStats", stats);',
    )
    src = src.replace(
        "inventory.set(idx, stack);\n            }\n        }\n    }",
        "inventory.set(idx, stack);\n            }\n        }\n"
        "        rememberedPlaces.clear();\n"
        "        if (tag.contains(\"TarsPlaces\")) {\n"
        "            ListTag places = tag.getList(\"TarsPlaces\", Tag.TAG_COMPOUND);\n"
        "            for (int i = 0; i < places.size(); i++) {\n"
        "                CompoundTag pt = places.getCompound(i);\n"
        "                rememberedPlaces.put(pt.getString(\"Name\"), new BlockPos(pt.getInt(\"X\"), pt.getInt(\"Y\"), pt.getInt(\"Z\")));\n"
        "            }\n"
        "        }\n"
        "        oreStats.clear();\n"
        "        if (tag.contains(\"TarsOreStats\")) {\n"
        "            ListTag stats = tag.getList(\"TarsOreStats\", Tag.TAG_COMPOUND);\n"
        "            for (int i = 0; i < stats.size(); i++) {\n"
        "                CompoundTag st = stats.getCompound(i);\n"
        "                oreStats.put(st.getString(\"Ore\"), st.getInt(\"N\"));\n"
        "            }\n"
        "        }\n"
        "    }",
        1,
    )
elif "TarsOreStats" not in src:
    src = src.replace(
        'tag.put("TarsPlaces", places);',
        'tag.put("TarsPlaces", places);\n'
        '        ListTag stats = new ListTag();\n'
        '        for (Map.Entry<String, Integer> e : oreStats.entrySet()) {\n'
        '            CompoundTag st = new CompoundTag();\n'
        '            st.putString("Ore", e.getKey());\n'
        '            st.putInt("N", e.getValue());\n'
        '            stats.add(st);\n'
        '        }\n'
        '        tag.put("TarsOreStats", stats);',
    )

if '"climb"' not in src and "isClimbing()" in src:
    src = src.replace(
        'private static final RawAnimation DIG = RawAnimation.begin().thenLoop("dig");',
        'private static final RawAnimation DIG = RawAnimation.begin().thenLoop("dig");\n'
        '    private static final RawAnimation CLIMB = RawAnimation.begin().thenLoop("climb");',
    )
    old_pred = 'if (isDigging()) return event.setAndContinue(DIG);'
    new_pred = 'if (isClimbing()) return event.setAndContinue(CLIMB);\n            if (isDigging()) return event.setAndContinue(DIG);'
    if old_pred in src:
        src = src.replace(old_pred, new_pred)

path.write_text(src)
print("ok", len(src))
