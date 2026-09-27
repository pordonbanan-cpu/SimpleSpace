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

if "Сканер окружения" not in src:
    src = src.replace(
        'case 3 -> "Быстрая добыча.";\n            default -> "Базовая прошивка.";',
        'case 3 -> "Быстрая добыча.";\n            case 4 -> "Сканер окружения.";\n            case 5 -> "Усиленный корпус.";\n            default -> "Базовая прошивка.";',
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
        if (key.contains("diamond")) return "алмаз";
        if (key.contains("iron")) return "железо";
        if (key.contains("gold")) return "золото";
        if (key.contains("coal")) return "уголь";
        if (key.contains("copper")) return "медь";
        if (key.contains("redstone")) return "редстоун";
        if (key.contains("lapis")) return "лазурит";
        if (key.contains("emerald")) return "изумруд";
        if (key.contains("quartz")) return "кварц";
        if (key.contains("debris")) return "незерит";
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
                    String[] lines = {"Повреждение корпуса.", "Это было… неприятно.", "Рекомендую устранить угрозу."};
                    speak(sp, lines[random.nextInt(lines.length)]);
                }
            }
            if (source.getEntity() instanceof LivingEntity && combatSpeakCd <= 0) {
                combatSpeakCd = 60;
                Player o = getOwner();
                if (o instanceof ServerPlayer sp && distanceToSqr(sp) < 48 * 48 && random.nextFloat() < 0.5f) {
                    String[] lines = {"Контакт с врагом.", "Под огнём.", "Защита активна."};
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
            if (o instanceof ServerPlayer sp && distanceToSqr(sp) < 40 * 40 && random.nextFloat() < 0.25f) {
                if (climbing) speak(sp, random.nextBoolean() ? "Карабкаюсь." : "Подъём.");
                else speak(sp, random.nextBoolean() ? "Приступаю." : "Копаю.");
            }
        }
        if (!dig && wasDigging && digSpeakCd <= 0) {
            digSpeakCd = 60;
            Player o = getOwner();
            if (o instanceof ServerPlayer sp && distanceToSqr(sp) < 40 * 40 && random.nextFloat() < 0.2f)
                speak(sp, "Готово.");
        }
        wasDigging = dig;
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
                if (rest.startsWith("место ")) rest = rest.substring(6).trim();
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
        String oreLine = ore == null ? "руда не найдена"
                : ("руда ~" + (int) Math.sqrt(ore.distSqr(blockPosition())) + "м");
        speak(sp, "Скан: враги " + hostiles + ", мирные " + passives + ", " + oreLine + ".");
    }

'''
    src = src.replace(
        "    @Override\n    public void registerControllers",
        HURT + "\n    @Override\n    public void registerControllers",
    )

MEM = '''
        if (containsAny(msg, "запомни", "запомнить", "remember")) {
            String name = extractPlaceName(msg, "запомни", "запомнить", "remember");
            if (name == null || name.isBlank()) name = "точка";
            rememberedPlaces.put(name, blockPosition().immutable());
            speak(sp, "Запомнил «" + name + "»: " + blockPosition().getX() + " " + blockPosition().getY() + " " + blockPosition().getZ() + ".");
            return true;
        }
        if (containsAny(msg, "список мест", "waypoints")) {
            if (rememberedPlaces.isEmpty()) speak(sp, "Память мест пуста.");
            else {
                StringBuilder sb = new StringBuilder("Места: ");
                for (Map.Entry<String, BlockPos> e : rememberedPlaces.entrySet()) {
                    BlockPos bp = e.getValue();
                    sb.append(e.getKey()).append("(").append(bp.getX()).append(" ").append(bp.getY()).append(" ").append(bp.getZ()).append(") ");
                }
                speak(sp, sb.toString().trim());
            }
            return true;
        }
        if (containsAny(msg, "иди на ", "иди к ", "вернись на ", "вернись к ")) {
            String name = extractPlaceName(msg, "иди на", "иди к", "вернись на", "вернись к");
            if (name != null && rememberedPlaces.containsKey(name)) {
                setGoToTarget(rememberedPlaces.get(name));
                speak(sp, "Иду на «" + name + "».");
                return true;
            }
        }
        if (containsAny(msg, "скан", "сканируй", "scan", "что вокруг", "доложи")) {
            if (getFlashLevel() < 4) { speak(sp, "Нужен модуль L4+."); return true; }
            doEnvironmentScan(sp);
            return true;
        }
        if (containsAny(msg, "что часто", "привычк", "статистика руд", "самообуч")) {
            if (getFlashLevel() < 4) { speak(sp, "Нужен модуль L4+."); return true; }
            if (oreStats.isEmpty()) { speak(sp, "Пока нет данных о добыче."); return true; }
            StringBuilder sb = new StringBuilder("Статистика: ");
            oreStats.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(5)
                .forEach(e -> sb.append(oreDisplayName(e.getKey())).append(" x").append(e.getValue()).append(" "));
            speak(sp, sb.toString().trim());
            return true;
        }
'''

if "rememberedPlaces.put" not in src:
    marker = 'speak(sp, "Сбор в склад: выкл.");\n            return true;\n        }'
    if marker in src:
        src = src.replace(marker, marker + "\n" + MEM, 1)
elif "oreStats.isEmpty" not in src:
    marker = 'if (containsAny(msg, "скан", "сканируй", "scan", "что вокруг", "доложи"))'
    if marker in src and "статистика руд" not in src:
        extra = '''
        if (containsAny(msg, "что часто", "привычк", "статистика руд", "самообуч")) {
            if (getFlashLevel() < 4) { speak(sp, "Нужен модуль L4+."); return true; }
            if (oreStats.isEmpty()) { speak(sp, "Пока нет данных о добыче."); return true; }
            StringBuilder sb = new StringBuilder("Статистика: ");
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
