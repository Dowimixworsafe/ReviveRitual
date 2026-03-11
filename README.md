<div align="center">

# ReviveRitual

**A hardcore death & revival system for Paper servers**

![Minecraft](https://img.shields.io/badge/Minecraft-1.21+-62B47A?style=for-the-badge&logo=modrinth&logoColor=white)
![Paper](https://img.shields.io/badge/Paper%20API-1.21.10-blue?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![License](https://img.shields.io/badge/License-All%20Rights%20Reserved-red?style=for-the-badge)

*Transform death on your server into a dramatic, immersive experience.*
*Ghosts roam the world, graves mark the fallen, and arcane rituals bring the dead back to life.*

---

</div>

## Overview

**ReviveRitual** is a feature-rich Paper plugin that replaces the default death system with a fully configurable punishment & revival mechanic. When a player dies, they don't simply respawn — instead, they enter a **punishment state** (ghost, spectator, or temporary ban) and must wait for the timer to expire or be revived through a **dark cauldron ritual** performed by another player.

## Features

### Grave System
- Beautiful **3D graves** built from Display Entities
- Player's **skull** mounted on top of the tombstone
- **R.I.P.** text, player name, and death date carved into stone
- Stores the player's **entire inventory** and **XP** — recoverable upon interaction
- Optional **cross-player looting**
- **Coordinate notification** sent to the player upon death
- Admin command to **clear graves** by player, radius, or all at once

### Ghost Mode
- Dead players become **invisible spirits** that can fly and roam the world
- A configurable **companion mob** follows the ghost with optional **glowing outline**
- Ghosts **cannot** interact with the world — no block breaking/placing, no item pickup/drop, no combat
- Mobs ignore ghosts entirely
- **Haunting ability** — ghosts can scare living players with a spooky sound effect
- Action bar **countdown timer** showing remaining punishment time

### Spectator Mode
- Players enter Minecraft's built-in spectator mode
- Paginated **teleport GUI** to jump between living players
- Auto-kick if no living players are available

### Ban Mode
- Players are **temporarily kicked** from the server upon death
- Custom **kick screen** with death skull art, ban reason, and remaining time
- Blocked from rejoining until the timer expires or an admin revives them

### Revival Ritual
A dramatic, multi-step cauldron ritual to bring the dead back:

1. **Rename** a Potion of Strong Healing to the dead player's name
2. **Pour** the potion into a Cauldron — it accepts the "blood sacrifice" and fills with a crimson liquid
3. **Use** a Totem of Undying on the blood-filled cauldron
4. **Lightning strikes**, and the player is resurrected with a server-wide broadcast!

The ritual is protected against griefing — blood cauldrons survive explosions, pistons, and are immune to damage.

### Multi-Language Support
- Built-in **English** and **Polish** translations
- All messages are fully customizable via `messages_en.yml` / `messages_pl.yml`
- Switch language on-the-fly with `/rr set lang <en|pl>`

---

## Installation

1. Download the latest `ReviveRitual.jar`
2. Place it in your server's `plugins/` directory
3. Restart the server
4. Configure the plugin in `plugins/ReviveRitual/config.yml`

### Requirements
| Requirement      | Version |
|------------------|---------|
| Paper (or fork)  | 1.21+   |
| Java             | 21+     |

---

## Configuration

All settings are in `config.yml` and can be changed in-game using `/rr set`:

```yaml
# ReviveRitual Configuration File
# ---------------------------

# General Settings
# Language of the plugin's messages. Available: 'en', 'pl'
language: "en"

# Defines what happens when a player dies
# Available modes:
# - ghost: Player becomes an invisible spirit accompanied by a mob, with limited interactions.
# - spectator: Player enters Minecraft's built-in spectator mode and a teleport GUI opens.
# - ban: Player is temporarily banned from the server upon death.
punishment-mode: "ghost"

# Determines how long the punishment lasts before an automatic revive or unban
# You can use suffixes: s (seconds), m (minutes), h (hours), d (days). Example: 3h, 30m, 1d
punishment-time: "3h"

# ---------------------------
# Ghost Settings
# ---------------------------

# The mob that accompanies the ghost player. Only applies in 'ghost' mode.
# Can be any valid Bukkit EntityType name, e.g., "ALLAY", "VEX", "BAT", "PARROT"
ghost-mob: "ALLAY"

# Should ghosts be allowed to fly? (True/False)
ghost-fly: true

# Should the ghost's accompanying mob have a glowing outline? (True/False)
ghost-glow: true

# Should ghosts be able to use the haunting item to scare alive players? (True/False)
ghost-haunt-enabled: true

# Cooldown time (in seconds) between ghost haunting abilities.
ghost-haunt-delay: 45

# ---------------------------
# Grave Settings
# ---------------------------

# Should a grave be automatically generated when a player dies to store their items and experience?
# If false, items and experience will drop naturally.
graves-enabled: true

# Should the player receive a chat message with grave coordinates upon death?
grave-show-coordinates: true

# Should players be allowed to loot graves of other players?
# Default: false (only grave owner can loot their own grave)
grave-cross-loot: false
```

---

## Commands

All commands require **OP** or `reviveritual.admin` permission.

| Command | Description |
|---------|-------------|
| `/rr` | Show help menu |
| `/rr <nick>` | Revive a player (admin) |
| `/rr reload` | Reload config & messages |
| `/rr set lang <en\|pl>` | Change language |
| `/rr set mode <ghost\|ban\|spectator>` | Change punishment mode |
| `/rr set time <value>` | Change punishment duration (ex: 3h) |
| `/rr set mob <TYPE>` | Change ghost companion mob |
| `/rr set fly <true\|false>` | Toggle ghost flight |
| `/rr set glow <true\|false>` | Toggle companion glow |
| `/rr set haunt <true\|false>` | Toggle haunting ability |
| `/rr set haunt-delay <seconds>` | Change haunt cooldown |
| `/rr set graves <true\|false>` | Toggle grave generation |
| `/rr set grave-coords <true\|false>` | Toggle coordinate display |
| `/rr set cross-loot <true\|false>` | Toggle cross-player looting |
| `/rr cleargraves all` | Remove all graves |
| `/rr cleargraves <nick>` | Remove a player's graves |
| `/rr cleargraves <radius>` | Remove graves in radius |

> All `/rr set` commands support **tab completion** with intelligent suggestions.

---

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `reviveritual.admin` | Access to all admin commands | OP |

---

## Building from Source

```bash
git clone https://github.com/Dowimixworsafe/ReviveRitual.git
cd ReviveRitual
mvn clean package
```

---

<div align="center">

*Made by [Dowimixworsafe](https://github.com/Dowimixworsafe)*

</div>
