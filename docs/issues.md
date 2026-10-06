# MVE Issue Tracker（代号表）

> 反馈代号规则：ISn = Issue n。状态：OPEN / FIXED / WONTFIX / BY-DESIGN。
> 每条记录：代号、描述、状态、修复提交或说明。
>
> ## 架构关系命名（v1.1.0 起）
> **管线共栖（Pipeline Symbiosis）**——MVE 与互助/不掉落/墓碑/图腾类模组的关系：
> 各方只占用原版事件管线上的一个槽位（slot），互不引用对方代码，靠三个单方面契约咬合：
> ① 上游让位（fate diff：池中缺席即 DISABLED）；② 空池中止（matched=0 整体退出）；
> ③ 下游回灌（结算重放 LivingDropsEvent 供 LOWEST 槽位接管）。
> 可视化总纲：docs/死亡与掉落管线.html。

## v1.1.0 路线（管线共栖版）
1. IS14 Prepare 移至"确认未取消"分支
2. IS16 死亡界面 extends 原版 DeathScreen
3. IS15 drops 托管 LOWEST→LOW + 全托管取消事件（共栖硬约束）
4. IS17 DeathTx 记录死亡来源 + 断线发包防御
5. IS13/IS31 结算重放（下游回灌落地，GraveStone 实测）
6. IS20 最后 10 秒半透明脉动
7. 收尾：版本号 1.1.0、双装实测
（IS32 容器模式复制 bug 后置 v1.1.x）

## 代号表

| 代号 | 描述 | 状态 | 备注 |
| --- | --- | --- | --- |
| IS0 | 在新 UI 出现前会短暂出现旧 UI（原版死亡屏闪现） | FIXED | 根因：Opening 拦截后存在界面空窗（日志显示视图载荷晚 ~700ms 才被客户端处理）。修复：prepare 载荷直接打开占位死亡屏（MvePendingScreen），视图载荷到达后填充；abort 路径用 ClosePayload 回退到原版死亡屏 |
| IS1 | 经验条语义错误：应按设计显示"当前经验点数对应等级"与"重生后预计经验点数对应等级"（原版样式：条=等级内进度，绿色等级数字居中条上），而非经验点数/费用比例 | FIXED | 上栏=预计等级（k=0 时显示 0），下栏=当前等级；数字绘制复刻原版绿字黑影样式 |
| IS2 | 布局：标题与死亡消息重叠；两个区域下方大量空白却只用 2 行滚动；面板宽度未与快捷栏完全对齐 | FIXED | 标题 2x 绘制的 y 坐标未计入缩放导致重叠（修正）；行数公式改为按剩余高度求解（全屏下 4 行起）；面板宽度统一 182 与快捷栏一致 |
| IS3 | 颜色规范：标签应全部白色；倒计时剩余 ≤30s 变黄、≤10s 变红 | FIXED | |
| IS4 | 倒计时结束后物品未立即掉落（感知为重生/退出才掉落） | FIXED | 日志证实超时结算立即执行（死亡后整 30s），掉落发生在遗体位置；settle 日志补充掉落计数与坐标以便复核。若仍复现请提供当次日志 |
| IS5 | PVP 拾取页面反馈 | FIXED（阶段一） | 重做为**原版大箱子容器**：注册自定义 `MenuType`（pvp_pickup），服务端开真菜单并同步槽位；上半部分 6×9 拾取格沿用 generic_54 贴图、点击走容器点击包（左键/Shift 均拾取，物品从遗体位置抛向拾取者）；下半部分为**真实槽位**绑定的玩家物品栏（与打开容器行为一致，整理类模组可识别），可整理论玩家自己的物品但不影响遗体物品。多观察者同步：任一拾取会清空所有已打开菜单的对应槽位并 broadcastChanges。旧 payload 式 PVP 界面（TakePayload/LeavePayload/PvpScreen）已移除 |
| IS11 | 右键遗体/死亡玩家无法打开 PVP 页面 | FIXED | 根因：服务端菜单已打开并下发 `ClientboundOpenScreenPacket`，但客户端未为自定义 MenuType 注册 Screen，`MenuScreens` 每次尝试都失败（日志: Failed to create screen for menu type: millionvalueexfil:pvp_pickup）。修复：客户端通过 `RegisterMenuScreensEvent` 注册 `PvpPickupScreen` |
| IS12 | 遗体实体名称应常显且显示归属玩家与剩余时间 | FIXED | `setCustomNameVisible(true)` + 每秒刷新名称为"归属玩家 (剩余秒s)" |
| IS13 | MVE 托管结束后未保留物品应可交由墓碑类模组托管（而非原地爆出） | FIXED（v1.1.0） | 结算重放已落地：settle 在"玩家仍死亡且未断线且维度一致"时以 `CommonHooks.onLivingDrops(owner, 记录的死亡来源, drops, false)` 回灌管线（DeathTx/ Pending 新增 DamageType ResourceKey 字段），corelib 在 LOWEST 命中死亡快照建墓（位置=死亡点、经验=0）；无人接手/被否决时剩余物照常落地防丢失。重生路径与登出路径自动回退直接掉落（玩家实体状态门控）。GraveStone 双装待实测 |
| IS14 | Prepare 占位屏过早发送：死亡事件被取消（互助/复活类模组倒地）时占位屏已发出，盖住倒地 UI 5 秒 | FIXED（v1.1.0） | PreparePayload 从 HIGHEST 处理器移至 onDeathSettled 的"确认未取消"分支（仍在 die() 栈内、早于 CombatKill 包，客户端时序不变） |
| IS15 | LivingDropsEvent 托管优先级与 corelib 同为 LOWEST：注册序竞态；MVE 先行时 corelib 生成空墓 | FIXED（v1.1.0） | 托管上移 LOW（位于 NORMAL 保留类之后、LOWEST 墓碑之前——管线共栖硬约束）；托管后池空即 `event.setCanceled(true)`（压制空墓与低位消费者，corelib 死亡快照保留为结算重放数据源）；池中有剩余（他人物品）则不取消，照常让位 |
| IS16 | MVE 死亡界面应以类型语义成为"死亡屏"：GUI 锁定类模组（如 PlayerRevive disableAllGUIAccess）按 `instanceof DeathScreen` 放行白名单拦截非原版屏幕 | FIXED（v1.1.0） | MveDeathScreen / MvePendingScreen extends 原版 DeathScreen（init 不调 super 以免原版按钮；isPauseScreen/shouldCloseOnEsc 继承）；ClientEvents.onScreenOpening 的取消守卫排除 MVE 自有屏幕（否则会自我拦截） |
| IS17 | DeathTx 未记录死亡来源（结算重放 `CommonHooks.onLivingDrops` 需要 source 参数）；断线玩家发包缺连接检查（互助模组登出即死场景） | FIXED（v1.1.0） | Pending/DeathTx 新增 `damageType`（DamageType 的 ResourceKey，NBT 持久化，缺省 null → generic）；重放时经 registryAccess 还原 DamageSource。`MveNet.sendToPlayer` 统一加 `hasDisconnected()` 防御 |
| IS18 | 启动即崩：`mvedebug` 注册渲染器时 NPE（Trying to access unbound value: mvedebug:debug_corpse） | FIXED | 根因：调试实体迁移进 mvedebug 时留下两个同名 DeferredRegister——`MveDebug.ENTITY_TYPES`（空壳，挂了总线）与真正持有条目的 `MveDebugEntities.TYPES`（从未挂总线）。修复：删除空壳，构造器改挂 `MveDebugEntities.TYPES` |
| IS19 | 倒计时未结束时把待选区物品全部选至保留区后，待选区面板整体消失（计时/经验条一并不可见） | FIXED | 根因：渲染侧 `!available.isEmpty()` 门闸把整个面板跳过。修复：待选区面板常驻（空网格 + 标题/倒计时/经验条仍在），GridView 空列表路径已确认安全 |
| IS20 | 最后 10 秒待选区警示效果方向错误：预期"半透明脉动"（面板周期性变透），实际是深色遮罩 alpha 递增 + 正弦振荡 = "闪烁变深" | FIXED（v1.1.0） | 深色遮罩已删除；改为待选区面板背景 alpha 在 0x30↔0x90 间正弦调制（周期约 1.25s），红色警示保留在倒计时文字 |
| IS21 | 打开实体 PVP 拾取菜单时客户端断线（打开带有高级数据的屏幕失败: No value with id 15012） | FIXED | 根因：菜单附加数据读写顺序不对称——服务端写 [Boolean][UUID][ItemStack(OPTIONAL_CODEC)]，客户端却先 decode 物品再读 UUID，物品编解码器把 UUID 字节当注册表 id 解析（15012 即 UUID 前几个字节）。修复：客户端读取顺序对齐为 UUID 先于 decode |
| IS22 | PVP 拾取页面上半区采用默认容器槽位外观（空槽显示、无滚动条），与设计图"物品"语义不符 | FIXED | 上半区改为自绘：不再 blit generic_54 上段贴图，改画死亡选择页风格面板（深色半透明底 + 555555 边）+ #373737 槽位格（仅占用格渲染，空槽经 `Slot.isActive()=!hasItem()` 隐藏，同时门控悬停/点击）+ 右侧常驻滚动条（超过 6 行时显示滑块）；拾取容器本身已顺序压实故无空洞。下半区保留原版贴图与真实槽位（整理模组兼容不变） |
| IS23 | 快速连点选择时部分物品"没选上" | FIXED | 根因：`handleSelect` 对版本号做严格相等校验，服务端应用上一次点击后版本 +1，客户端在收到新 view 前的后续点击携带旧版本被静默丢弃（日志：一次死亡内 17 次 view 重发、17 件只保住 16 件）。修复：版本改为"仅拒绝客户端超前"（`txn.version < payload.version()`），过期版本按幂等意图处理——目标已是期望状态则视为成功空操作，真实状态转移仍走 toggle 预算校验，双击误撤销由"已 RESERVED 则跳过"天然防住 |
| IS24 | 死亡点在熔岩/火中时，结算放弃的物品在原位生成即被烧毁（表现为"放弃的物品没有掉落"） | WONTFIX（已回退） | 曾加安全点搜索，经管理员现场核实报告案例发生在无岩浆的安全位置、非烧毁，且防御性搜索与调试风格不符，按用户决定回退为**原位掉落为最终语义**（与原版死亡掉落一致，熔岩死亡时物品同原版一样会烧毁）。附日志备注：Xiejc1015 第一次死亡（23:01，35 件）的物品已被本人捡回——其第二次死亡（23:05）的物品栏与第一次掉落清单几乎一致；真正报告丢失的是第二次掉落（265,66,-33，30 件），日志确认生成过、代码路径与第一次完全相同，成因未定（不排除现场被他人拾取），复发时再以实体级调试日志排查 |
| IS25 | PVP 拾取页面上半区（自绘深色面板）与下半区（原版灰底）视觉割裂 | FIXED（IS27 修订） | 首版方案（保留原版箱框+内部深色区）被用户否决（比割裂版更难看），已回滚为原版大箱子完整外观；`pvpShowOwnInventory` 语义改为"整页模式开关"，见 IS27 |
| IS26 | 调试模组缺少事后追溯能力：交易结算/移除后物品去向无从查证，丢失时无法补偿 | FIXED | 新增 `mvedebug_history` 存档（世界目录 mvedebug_history.nbt，每秒轮询玩家挂载的交易快照，最近 100 条，跨重启）：记录归属、时间、维度、坐标、阶段、物品清单。新指令：`/mvedebug history [player]` 列出全部/指定玩家的掉落历史；`/mvedebug redrop <txnId>` 由管理员在记录坐标重新掉落一次全部物品（标记 redropped 防重复操作）——开发阶段的安全网，杜绝不可挽回的生产事故 |
| IS27 | PVP 页面兼容模式：`pvpShowOwnInventory=false` 时不再走注册的自定义 MenuType，整页只渲染"物品" | FIXED | 服务端 openPickupMenu 按配置分支：false 时发送 PvpOpenPayload（S2C，含 txnId/ownerId/剩余 ticks/条目清单），客户端打开 PvpCompatScreen——死亡选择页同款单面板（深色底、182 宽与快捷栏对齐、行数自适应、滚动条、空槽不显示、tooltip），页头"点击以掉落该物品"+ 倒计时（30s 黄/10s 红）；点击物品发 TakePayload（C2S）→ takeFromMenu 抛落，全部观察者即时刷新（每秒定时同步倒计时）；E/Esc/点击空白处退出（LeavePayload 注销观察者）。settle/超时经 ClosePayload 关闭兼容页。顺带修复 closeAll/onContainerClose 用 menu.txnId 对比 ownerId 的既有错配（观察者注销从未生效） |
| IS28 | 依据用户提供的批注设计图对两个 PVP 页面做视觉与交互修订 | FIXED | 兼容模式页：拾取区整体垂直居中、页头改"拾取物品"、叠加层由红色渐变改为中性暗色（0xC0101010）、面板按物品行数自适应消除空隙、`/mvedebug corpse` 的模拟交易 ticksLeft 补为 seconds*20（修复倒计时恒 00:00）。容器模式页：拾取区槽位背全部抹除改为整体黑色凹栏（空位留空）、顶部右侧新增倒计时（菜单附加数据携带 ticksLeft，客户端经 ClientTickEvent 递减，因 AbstractContainerScreen.tick 为 final）、拾取后服务端对容器整体重排（refill）实现物品自动前移而非留空洞、右侧滑动条。注：滑动条滑块仅在物品超过 6 行时出现（玩家物品上限 41 件 ≤ 6 行，轨道常驻） |
| IS29 | 调试指令：运行时切换 PVP 拾取页面模式（玩家物品栏显示 t/f） | FIXED | `/mvedebug pvp <true|false>`：直接 set `pvpShowOwnInventory` 配置值（写入 serverconfig toml 并生效），模式判定在服务端 openPickupMenu 分支，对下一次打开的拾取页立即生效，无需重启/重登；反馈消息注明当前模式与生效时机。注：IS32 下线容器模式后该指令暂时失效（配置已不被读取） |
| IS33 | 调试观测不足：DeathSpy 仅覆盖 2+4 个优先级，管线共栖后出现问题时日志无法定位"谁在哪一级动了池子" | FIXED | DeathSpy 升级为 **RulerOfDeath**（`[RULER]` 前缀）：① 死亡宣告与掉落事件**全部五个优先级**（HIGHEST/HIGH/NORMAL/LOW/LOWEST，均 receiveCanceled）逐级留痕，池大小逐级对比即可看出哪一级消耗了物品；② 伤害管线仅记录**致死一击**（`LivingDamageEvent.Pre` newDamage ≥ 当前生命，避免刷屏）；③ 图腾生效（`LivingUseTotemEvent`）；④ 经验掉落（含 canceled）；⑤ Clone（wasDeath/新旧物品栏/PERSISTED_NBT 键）与 Respawn；⑥ **死亡序号 seq**：从 death@HIGHEST 起算、respawn 清零，一次死亡的整条日志链可用 seq 关联（/kill 等无伤害路径也覆盖） |
| IS34 | 超时结算把保留经验发给死亡实体：`ServerPlayer.restoreFrom` 仅在 keepEverything=true 时拷贝经验（死亡重生为 false）→ 点重生后 keep 经验全部丢失 | FIXED（v1.1.0） | `DeathTx.keepXpPending`（随 NBT 持久化）：结算时玩家仍死亡则 keep 经验**暂存交易**；重生结算（玩家已复活）照旧直发。补发点：onRespawn（点重生）、onLogin（结算后重连）；onLogout（放弃）折成经验球落死亡点防丢失。经验全流程：宣告@HIGHEST 冻结三字段 → 原版经验球=0 → 结算时 `fee=C(n)`、`remain=XpTotal−fee`、`keep=k×remain`、`lost` 按原版公式 min(等级×7,100) 折球落死亡点 |
| IS30 | 容器页凹陷与对齐仍不符合预期：空槽悬浮灰块回归、滑动条漏做且超出凹槽、程序化黑底观感差 | FIXED | ① 拾取槽恢复 `isActive()=有物品`，空槽不再出现悬浮高亮/可点击；② 放弃程序化黑底：把原版 generic_54 复制为模组材质 `assets/millionvalueexfil/textures/gui/pvp_54.png` 并让页面改用之，整体凹陷由用户手动编辑该贴图实现；③ 拾取区改为 8 列×6 行（48 格 ≥ 玩家身上最多 41 件），物品格（8..152px）+ 滑动条（166..170px）整体落在玩家物品栏槽位的左右边缘（8..170px）内，滑动条贴右缘对齐；用户编辑贴图时把凹槽画满 8..170 即可 |
| IS31 | 用户询问滑动条精确位置，并希望有脱离手绘贴图的更健壮方案 | FIXED | 滑动条位置（GUI 原点坐标）：轨道 (166,18)-(170,126)，右缘与玩家物品栏最右列右缘对齐。纯代码方案已设为默认：凹槽以原版 bevel 内嵌语言程序化绘制（近黑底 + 上/左 1px 纯黑阴影 + 下/右 1px 亮边），盖满原贴图 9 列烤槽区（8..170px），滑动条绘于凹槽内右缘；贴图回退为原版 generic_54。用户手绘的 `pvp_54.png` 原样保留未引用，需要切回手绘版时把 `BG` 改回模组命名空间并删除 bevel 绘制段即可（各一行） |
| IS32 | 容器模式 PVP 页（带玩家物品栏）存在复制物品的恶性 bug | OPEN（已下线） | 按用户决定，容器模式整条路径暂时下线：`openPickupMenu` 强制走兼容模式（PvpOpenPayload + PvpCompatScreen），`pvpShowOwnInventory` 配置与 `/mvedebug pvp` 指令均不再生效（指令已移除），真容器菜单不再被触达。根因排查待办（怀疑方向：真实容器槽位与服务端 refill 重排/点击拦截的交互，或整理类模组对容器的批量操作与 takeFromMenu 叠加）；复现时用 `/mvedebug history` 存档 + 日志审计物品流向。IS30 的容器页代码保留在仓库中未删除 |
| IS6 | 经验条填充宽度与网格未对齐 | FIXED | 条宽改为与网格同宽（162），面板 182 对齐快捷栏 |
| IS7 | 单人世界死亡后选择退出至标题崩溃（NPE: gameMode null） | FIXED | 根因：`disconnect(null)` 使断开过程中的中间 tick 以 pause=false 运行，`gameMode.tick()` 在 `gameMode` 已被置空而 `level` 尚未清空的窗口内 NPE。原版传 `GenericMessageScreen`（isPauseScreen=true → pause=true 跳过守卫）故不崩。修复：完全复刻原版退出流程 `level.disconnect()` → `disconnect(GenericMessageScreen("menu.savingLevel"))` → `setScreen(TitleScreen)` |
| IS8 | 经验条/面板被"遮蔽"、颜色发糊 | FIXED | 根因：`Screen.render` 开头会再次调用 `renderBackground`，红色渐变在自绘内容之后被二次叠加（按钮在其后绘制所以清晰）。修复：render 不再调 super.render，改为显式调用一次 renderBackground + 手动遍历 renderables |
| IS9 | 等级数字应与经验条轻微重叠（原版样式） | FIXED | 数字改为条绘制之后绘制，底边压住条顶 2px |
| IS10 | PVP 拾取页面与设计书示例图不符 | FIXED | 按示例图重做：中性深色底、"拾取物品"居中标题、浅色半透明槽位（白 15% + #8b8b8b 边）、"物品栏"标签 + 视角者物品栏只读展示、常驻滚动条 |

## 与设计书的有意偏离（备案）

- 经验条 A/B 语义由"费用 C(n) / 剩余 Xe"改为"预计重生等级 / 当前等级"（用户澄清，IS1）。
- 经验条 A 条形填充由 C(n)/X₀ 改为预计等级内的进度（原公式在大经验下不可见，IS1 前身问题）。
- 倒计时归零只结算不强制重生（用户澄清，设计书"重生玩家"步骤改为玩家手动重生触发）。
