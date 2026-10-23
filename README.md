# Million Value Exfil / 百万价值撤离

**English** | [中文](#中文)

## What is this?

A Minecraft 1.21.1 NeoForge mod that turns death into a negotiation.

When you die, your items are not scattered on the ground — they are seized into a
death transaction. A selection screen opens: the server locks in your seized loot,
and a countdown starts. You choose which items to ransom back with **XP**; anything
you leave behind (or fail to pay for) is dropped for **other players** to race to
and loot. Then you respawn — empty-handed, level zero — and must fight your way
back to the corpse site to win your goods back.

## Features

- **Death transaction** — on death (survival/adventure, keepInventory off), your
  whole inventory is captured into a tamper-proof transaction; nothing hits the
  ground until it settles.
- **Ransom with XP** — each item costs XP according to how many items you keep.
  Pay on the death screen or after respawn, per item.
- **PvP loot race** — unclaimed items drop near your corpse. Nearby living
  players (configurable) get a live pickup screen and can grab your stuff while
  you are still deciding.
- **Grace-timer expiry** — run out of time and everything unkept is dropped.
- **Cross-dimension recovery** — the transaction remembers where you died; your
  reserved items come back to you there.
- **In-game debug toolkit** (`mvedebug` build): `/mvedebug die|state|xp` plus
  pipeline spy logging.

## Configuration (server, `config/mve-server.toml`)

| Option | Default | Description |
|---|---|---|
| `selectionSeconds` | `90` | Countdown before the transaction settles itself (5–3600). |
| `xpKeepRatio` | `1.0` | Fraction of your remaining XP you are allowed to keep (0–1). |
| `allowHardcore` | `false` | Whether the mod intercepts hardcore deaths at all. |
| `allowGenericKill` | `false` | Whether `/kill` (generic_kill) triggers the transaction. |
| `pvpShowOwnInventory` | `true` | Whether your own loot race screen can also show your own drops. |

## Installation

1. Install [NeoForge](https://neoforged.net/) 21.1.x for Minecraft 1.21.1.
2. Drop `millionvalueexfil-*.jar` into your `mods/` folder (client **and** server).
3. Optional: tweak `config/mve-server.toml` on the server.

## License

[MIT](LICENSE)

---

<a id="中文"></a>

# Million Value Exfil / 百万价值撤离

## 这是什么？

一个 Minecraft 1.21.1 NeoForge 模组：把死亡变成一场谈判。

你死亡时，物品不会散落一地——它们会被"查封"进一笔死亡交易。选择界面随即打开：
服务器锁定你被查封的战利品并开始倒计时。你可以用**经验**赎回物品；没赎回的
（或来不及付钱的）会掉落在尸体附近，供**其他玩家**竞速拾取。之后你重生——
空手、0 级——必须杀回尸体现场赢回你的东西。

## 特性

- **死亡交易**——死亡时（生存/冒险、keepInventory 关闭）整个背包被冻结进一笔
  防篡改交易；结算前任何东西都不会落地。
- **经验赎回**——保留的物品越多，单件赎金越高。可在死亡界面或重生后逐件支付。
- **PvP 拾取竞速**——无人认领的物品掉落在尸体附近，附近的存活玩家（可配置）
  会收到实时拾取界面，趁你犹豫时抢走你的东西。
- **宽限倒计时**——超时后所有未赎回物品自动掉落。
- **跨维度找回**——交易记录死亡地点，保留物品会在那里归还给你。
- **游戏内调试工具**（`mvedebug` 构建）：`/mvedebug die|state|xp` 及管线探针日志。

## 配置项（服务端 `config/mve-server.toml`）

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `selectionSeconds` | `90` | 交易自动结算的倒计时（5–3600 秒）。 |
| `xpKeepRatio` | `1.0` | 结算时允许保留的剩余经验比例（0–1）。 |
| `allowHardcore` | `false` | 是否拦截极限模式死亡。 |
| `allowGenericKill` | `false` | `/kill`（generic_kill）是否触发交易。 |
| `pvpShowOwnInventory` | `true` | 拾取竞速界面是否也显示自己的掉落。 |

## 安装

1. 安装 Minecraft 1.21.1 对应的 [NeoForge](https://neoforged.net/) 21.1.x。
2. 把 `millionvalueexfil-*.jar` 放进 `mods/` 目录（**客户端和服务端都要**）。
3. 可选：在服务端调整 `config/mve-server.toml`。

## 许可证

[MIT](LICENSE)
