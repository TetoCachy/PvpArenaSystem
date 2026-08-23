# ⚔️ PvpArenaSystem

A modular, competitive PvP arena and matchmaking mod built for **Fabric (Minecraft 26.2 / Java 25)**. 

PvpArenaSystem allows server administrators to select structures directly from the overworld, clone them into an isolated void **Arena Dimension**, configure team spawn points, save custom kits, and allow players to challenge each other to isolated duels (1v1, party battles, and multi-team gamemodes) with zero inventory risk.

---

## ✨ Features

- 🔒 **Zero-Leak Player Isolation Guarantee**:
  - Full player state (inventory slots, armor, offhand, XP, health, food, active potion effects, and exact coordinates) is snapshotted upon entering any arena or setup session.
  - Snapshots are saved to disk (`world/pvparenasystem/snapshots/<UUID>.dat`)—if a player disconnects or the server restarts mid-match, their survival state is automatically restored upon reconnect.
  - Arena kit items and drops are completely wiped prior to restoration.

- 🌌 **Dedicated Void Dimension (`pvparenasystem:arena_dimension`)**:
  - Arenas are stored in spatial isolation within a custom void world type, keeping your survival world pristine and lag-free.

- 🪄 **Admin Selection Wand & Setup Mode**:
  - Use a selection tool (**Wooden Pickaxe** or toggle via `/arena wand`) to select corners with Left-Click (Pos 1) and Right-Click (Pos 2).
  - `/arena create <name>` automatically clones the selected volume into the Arena Dimension and teleports the admin into interactive **Setup Mode**.
  - Configure team spawns (`/arena setspawn <team>`) and spectator viewing areas (`/arena setspectator`) in real time.

- 🎒 **Custom Kit Engine**:
  - Build kits directly from your currently equipped armor and inventory via `/kit save <name>`.
  - Serializes all items, components, custom enchantments, durability, and lore using modern `ItemStack` codecs.

- ⚔️ **Interactive Match & Duel Engine**:
  - Challenge players via `/duel <player> [kit] [arena] [rounds]`.
  - Interactive clickable chat prompts (**`[ACCEPT]`** / **`[DECLINE]`**).
  - Configurable Best-of-X round systems, pre-match countdowns, title announcements, and sound effects.
  - **Death Interception**: Prevents vanilla "You Died" respawn screens and item dropping—eliminated players seamlessly transition to Spectator Mode.

- 🔄 **Arena Auto-Rollback**:
  - Captures block state snapshots of arenas and rolls back placed/broken blocks, clearing projectiles and dropped items after every match.

- 👥 **Party System**:
  - Form teams (`/party create`, `/party invite <player>`) and challenge opposing party leaders to team duels (2v2, 3v3, XvX).

---

## 📜 Commands

### 🛡️ Player Commands

| Command | Description |
| :--- | :--- |
| `/duel <player> [kit] [arena] [rounds]` | Send a duel challenge to another player. |
| `/duel accept <player>` | Accept a pending duel challenge. |
| `/duel decline <player>` | Decline a pending duel challenge. |
| `/duel leave` | Forfeit and exit the current match. |
| `/party create` | Create a new party. |
| `/party invite <player>` | Invite a player to your party. |
| `/party leave` | Leave your current party. |
| `/party duel <leader> <kit>` | Challenge another party to a team duel. |
| `/kit list` | View all available PvP kits. |
| `/arena list` | View all registered arenas and their status. |

### 👑 Admin Commands (Permission Level 2 / OP)

| Command | Description |
| :--- | :--- |
| `/arena wand` | Toggle Wand Selection mode on/off. |
| `/arena create <name>` | Clone wand selection to the Arena Dimension & enter Setup Mode. |
| `/arena edit <name>` | Re-enter Setup Mode for an existing arena. |
| `/arena setspawn <team>` | Set a spawn point for Team # (1, 2, 3, etc.) at your position. |
| `/arena setspectator` | Set the spectator spawn point at your position. |
| `/arena save` | Save arena configuration and commit map rollback snapshot. |
| `/arena leave` | Exit Setup Mode and restore previous survival state. |
| `/kit save <name>` | Save your current inventory, armor, and offhand as a kit. |
| `/kit load <name>` | Load a kit into your current inventory. |
| `/kit delete <name>` | Delete a custom kit. |

---
