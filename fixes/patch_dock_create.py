#!/usr/bin/env python3
"""Add dock mode + Create bridge hooks to TarsEntity."""
import sys
from pathlib import Path

path = Path(sys.argv[1])
src = path.read_text()

if "dockOrdered" not in src:
    src = src.replace(
        "private boolean climbing;",
        "private boolean climbing;\n    private boolean dockOrdered;\n    private boolean docked;",
    )

if "isDockOrdered" not in src:
    src = src.replace(
        "public void setClimbing(boolean v) { this.climbing = v; }",
        "public void setClimbing(boolean v) { this.climbing = v; }\n"
        "    public boolean isDockOrdered() { return dockOrdered; }\n"
        "    public void setDockOrdered(boolean v) {\n"
        "        dockOrdered = v;\n"
        "        if (!v) docked = false;\n"
        "        if (v) { setFollowing(false); setSprintMode(false); setMiningOrdered(false); clearGoToTarget(false); }\n"
        "    }\n"
        "    public boolean isDocked() { return docked; }\n"
        "    public void setDocked(boolean v) { this.docked = v; }\n"
        "    public ItemStack inventoryGet(int i) {\n"
        "        if (i < 0 || i >= INV_SIZE) return ItemStack.EMPTY;\n"
        "        return inventory.get(i);\n"
        "    }\n"
        "    public void inventorySet(int i, ItemStack s) {\n"
        "        if (i < 0 || i >= INV_SIZE) return;\n"
        "        inventory.set(i, s == null ? ItemStack.EMPTY : s);\n"
        "    }",
    )

if "TarsDockGoal" not in src:
    src = src.replace(
        "this.goalSelector.addGoal(1, new TarsMineGoal(this));",
        "this.goalSelector.addGoal(1, new TarsMineGoal(this));\n"
        "        this.goalSelector.addGoal(1, new TarsDockGoal(this));",
    )

if '"sit"' in src or "SIT" not in src:
    if "SIT" not in src:
        src = src.replace(
            'private static final RawAnimation DIG = RawAnimation.begin().thenLoop("dig");',
            'private static final RawAnimation DIG = RawAnimation.begin().thenLoop("dig");\n'
            '    private static final RawAnimation SIT = RawAnimation.begin().thenPlayAndHold("sit");',
        )
    old = 'if (isClimbing()) return event.setAndContinue(CLIMB);'
    if old in src and "isDocked()" not in src:
        src = src.replace(
            old,
            'if (isDocked()) return event.setAndContinue(SIT);\n            ' + old,
        )
    elif 'if (isDigging()) return event.setAndContinue(DIG);' in src and "isDocked()" not in src:
        src = src.replace(
            'if (isDigging()) return event.setAndContinue(DIG);',
            'if (isDocked()) return event.setAndContinue(SIT);\n            if (isDigging()) return event.setAndContinue(DIG);',
        )

DOCK_VOICE = '''
        if (containsAny(msg, "док", "порт", "ложись", "в порт", "на порт", "отстой", "dock")) {
            setDockOrdered(true);
            speak(sp, "Иду в порт.");
            return true;
        }
        if (containsAny(msg, "выйди", "встань", "с дока", "покинь порт")) {
            setDockOrdered(false);
            setDocked(false);
            speak(sp, "Покинул порт.");
            return true;
        }
        if (containsAny(msg, "разгруз", "выгруз", "unload", "в сундук", "конвеер", "конвейер", "depot",
                "дирижабл", "аэронавт", "aeronaut", "борт", "пилот", "create")) {
            com.simplespace.tars.TarsCreateBridge.handleVoice(this, sp, msg);
            return true;
        }
'''

if "setDockOrdered(true)" not in src:
    marker = 'if (containsAny(msg, "за мной", "следуй", "follow", "ко мне", "за мно"))'
    if marker in src:
        src = src.replace(marker, DOCK_VOICE + "\n        " + marker, 1)

# clear dock when follow/stop
for pair in [
    ('setFollowing(true); setSprintMode(false); clearGoToTarget(false);\n            speak(sp, "Иду за вами.");',
     'setDockOrdered(false); setFollowing(true); setSprintMode(false); clearGoToTarget(false);\n            speak(sp, "Иду за вами.");'),
    ('setFollowing(false); setSprintMode(false); clearGoToTarget(false);\n            getNavigation().stop(); speak(sp, "Стою.");',
     'setDockOrdered(false); setFollowing(false); setSprintMode(false); clearGoToTarget(false);\n            getNavigation().stop(); speak(sp, "Стою.");'),
]:
    if pair[0] in src:
        src = src.replace(pair[0], pair[1])

path.write_text(src)
print("dock+create patch ok", len(src))
