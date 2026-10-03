# MVE Issue Tracker（代号表）

> 反馈代号规则：ISn = Issue n。状态：OPEN / FIXED / WONTFIX / BY-DESIGN。
> 每条记录：代号、描述、状态、修复提交或说明。

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
| IS13 | MVE 托管结束后未保留物品应可交由墓碑类模组托管（而非原地爆出） | OPEN（调查完成） | 见 docs/研究-墓碑托管交接.md。结论：管线内存在接管窗口（LivingDropsEvent）；管线后无标准入口，但可在结算时用 `CommonHooks.onLivingDrops` 重放掉落事件实现通用交接（corelib 源码已证实 GraveStone 会接住）。另发现 corelib 与 MVE 同为 LOWEST 的竞态与空墓问题。待实施 |
| IS14 | Prepare 占位屏过早发送：死亡事件被取消（互助/复活类模组倒地）时占位屏已发出，盖住倒地 UI 5 秒 | OPEN（调查完成） | 修正：PreparePayload 发送从 HIGHEST 处理器移到"确认未取消"分支（同调用栈内微秒级延迟，客户端时序不变）。见 docs/研究-互助托管墓碑三方管线.md 修正点1 |
| IS15 | LivingDropsEvent 托管优先级与 corelib 同为 LOWEST：注册序竞态；MVE 先行时 corelib 生成空墓 | OPEN（调查完成） | 修正：托管优先级上移 LOW（位于 NORMAL 调整者之后、LOWEST 墓碑之前），全托管时取消事件压制空墓与低位消费者；被取消后 corelib 的 deathMap 快照保留为结算重放的数据源（闭环无泄漏风险见三方管线报告）。见三方管线报告修正点2 |
| IS16 | MVE 死亡界面应以类型语义成为"死亡屏"：GUI 锁定类模组（如 PlayerRevive disableAllGUIAccess）按 `instanceof DeathScreen` 放行白名单拦截非原版屏幕 | OPEN（调查完成） | 修正：MveDeathScreen / MvePendingScreen extends 原版 DeathScreen（已验证 public 构造器、非 final）。见三方管线报告修正点4 |
| IS17 | DeathTx 未记录死亡来源（结算重放 `CommonHooks.onLivingDrops` 需要 source 参数）；断线玩家发包缺连接检查（互助模组登出即死场景） | OPEN（调查完成） | 修正：存 DamageType 的 ResourceKey；发包前检查连接。见三方管线报告修正点5 |
| IS18 | 启动即崩：`mvedebug` 注册渲染器时 NPE（Trying to access unbound value: mvedebug:debug_corpse） | FIXED | 根因：调试实体迁移进 mvedebug 时留下两个同名 DeferredRegister——`MveDebug.ENTITY_TYPES`（空壳，挂了总线）与真正持有条目的 `MveDebugEntities.TYPES`（从未挂总线）。修复：删除空壳，构造器改挂 `MveDebugEntities.TYPES` |
| IS19 | 倒计时未结束时把待选区物品全部选至保留区后，待选区面板整体消失（计时/经验条一并不可见） | FIXED | 根因：渲染侧 `!available.isEmpty()` 门闸把整个面板跳过。修复：待选区面板常驻（空网格 + 标题/倒计时/经验条仍在），GridView 空列表路径已确认安全 |
| IS20 | 最后 10 秒待选区警示效果方向错误：预期"半透明脉动"（面板周期性变透），实际是深色遮罩 alpha 递增 + 正弦振荡 = "闪烁变深" | OPEN（随 IS14-IS17 批量实施） | 修法：去掉黑色遮罩叠加，改为调制面板背景 alpha（0x90 ↔ 0x30 脉动），红色警示保留在倒计时文字（IS3 已实现） |
| IS21 | 打开实体 PVP 拾取菜单时客户端断线（打开带有高级数据的屏幕失败: No value with id 15012） | FIXED | 根因：菜单附加数据读写顺序不对称——服务端写 [Boolean][UUID][ItemStack(OPTIONAL_CODEC)]，客户端却先 decode 物品再读 UUID，物品编解码器把 UUID 字节当注册表 id 解析（15012 即 UUID 前几个字节）。修复：客户端读取顺序对齐为 UUID 先于 decode |
| IS22 | PVP 拾取页面上半区采用默认容器槽位外观（空槽显示、无滚动条），与设计图"物品"语义不符 | FIXED | 上半区改为自绘：不再 blit generic_54 上段贴图，改画死亡选择页风格面板（深色半透明底 + 555555 边）+ #373737 槽位格（仅占用格渲染，空槽经 `Slot.isActive()=!hasItem()` 隐藏，同时门控悬停/点击）+ 右侧常驻滚动条（超过 6 行时显示滑块）；拾取容器本身已顺序压实故无空洞。下半区保留原版贴图与真实槽位（整理模组兼容不变） |
| IS23 | 快速连点选择时部分物品"没选上" | FIXED | 根因：`handleSelect` 对版本号做严格相等校验，服务端应用上一次点击后版本 +1，客户端在收到新 view 前的后续点击携带旧版本被静默丢弃（日志：一次死亡内 17 次 view 重发、17 件只保住 16 件）。修复：版本改为"仅拒绝客户端超前"（`txn.version < payload.version()`），过期版本按幂等意图处理——目标已是期望状态则视为成功空操作，真实状态转移仍走 toggle 预算校验，双击误撤销由"已 RESERVED 则跳过"天然防住 |
| IS24 | 死亡点在熔岩/火中时，结算放弃的物品在原位生成即被烧毁（表现为"放弃的物品没有掉落"） | WONTFIX（已回退） | 曾加安全点搜索，经管理员现场核实报告案例发生在无岩浆的安全位置、非烧毁，且防御性搜索与调试风格不符，按用户决定回退为**原位掉落为最终语义**（与原版死亡掉落一致，熔岩死亡时物品同原版一样会烧毁）。附日志备注：Xiejc1015 第一次死亡（23:01，35 件）的物品已被本人捡回——其第二次死亡（23:05）的物品栏与第一次掉落清单几乎一致；真正报告丢失的是第二次掉落（265,66,-33，30 件），日志确认生成过、代码路径与第一次完全相同，成因未定（不排除现场被他人拾取），复发时再以实体级调试日志排查 |
| IS25 | PVP 拾取页面上半区（自绘深色面板）与下半区（原版灰底）视觉割裂 | FIXED（IS27 修订） | 首版方案（保留原版箱框+内部深色区）被用户否决（比割裂版更难看），已回滚为原版大箱子完整外观；`pvpShowOwnInventory` 语义改为"整页模式开关"，见 IS27 |
| IS26 | 调试模组缺少事后追溯能力：交易结算/移除后物品去向无从查证，丢失时无法补偿 | FIXED | 新增 `mvedebug_history` 存档（世界目录 mvedebug_history.nbt，每秒轮询玩家挂载的交易快照，最近 100 条，跨重启）：记录归属、时间、维度、坐标、阶段、物品清单。新指令：`/mvedebug history [player]` 列出全部/指定玩家的掉落历史；`/mvedebug redrop <txnId>` 由管理员在记录坐标重新掉落一次全部物品（标记 redropped 防重复操作）——开发阶段的安全网，杜绝不可挽回的生产事故 |
| IS27 | PVP 页面兼容模式：`pvpShowOwnInventory=false` 时不再走注册的自定义 MenuType，整页只渲染"物品" | FIXED | 服务端 openPickupMenu 按配置分支：false 时发送 PvpOpenPayload（S2C，含 txnId/ownerId/剩余 ticks/条目清单），客户端打开 PvpCompatScreen——死亡选择页同款单面板（深色底、182 宽与快捷栏对齐、行数自适应、滚动条、空槽不显示、tooltip），页头"点击以掉落该物品"+ 倒计时（30s 黄/10s 红）；点击物品发 TakePayload（C2S）→ takeFromMenu 抛落，全部观察者即时刷新（每秒定时同步倒计时）；E/Esc/点击空白处退出（LeavePayload 注销观察者）。settle/超时经 ClosePayload 关闭兼容页。顺带修复 closeAll/onContainerClose 用 menu.txnId 对比 ownerId 的既有错配（观察者注销从未生效） |
| IS28 | 依据用户提供的批注设计图对两个 PVP 页面做视觉与交互修订 | FIXED | 兼容模式页：拾取区整体垂直居中、页头改"拾取物品"、叠加层由红色渐变改为中性暗色（0xC0101010）、面板按物品行数自适应消除空隙、`/mvedebug corpse` 的模拟交易 ticksLeft 补为 seconds*20（修复倒计时恒 00:00）。容器模式页：拾取区槽位背全部抹除改为整体黑色凹栏（空位留空）、顶部右侧新增倒计时（菜单附加数据携带 ticksLeft，客户端经 ClientTickEvent 递减，因 AbstractContainerScreen.tick 为 final）、拾取后服务端对容器整体重排（refill）实现物品自动前移而非留空洞、右侧滑动条。注：滑动条滑块仅在物品超过 6 行时出现（玩家物品上限 41 件 ≤ 6 行，轨道常驻） |
| IS29 | 调试指令：运行时切换 PVP 拾取页面模式（玩家物品栏显示 t/f） | FIXED | `/mvedebug pvp <true|false>`：直接 set `pvpShowOwnInventory` 配置值（写入 serverconfig toml 并生效），模式判定在服务端 openPickupMenu 分支，对下一次打开的拾取页立即生效，无需重启/重登；反馈消息注明当前模式与生效时机 |
| IS30 | 容器页凹陷与对齐仍不符合预期：空槽悬浮灰块回归、滑动条漏做且超出凹槽、程序化黑底观感差 | FIXED | ① 拾取槽恢复 `isActive()=有物品`，空槽不再出现悬浮高亮/可点击；② 放弃程序化黑底：把原版 generic_54 复制为模组材质 `assets/millionvalueexfil/textures/gui/pvp_54.png` 并让页面改用之，整体凹陷由用户手动编辑该贴图实现；③ 拾取区改为 8 列×6 行（48 格 ≥ 玩家身上最多 41 件），物品格（8..152px）+ 滑动条（166..170px）整体落在玩家物品栏槽位的左右边缘（8..170px）内，滑动条贴右缘对齐；用户编辑贴图时把凹槽画满 8..170 即可 |
| IS6 | 经验条填充宽度与网格未对齐 | FIXED | 条宽改为与网格同宽（162），面板 182 对齐快捷栏 |
| IS7 | 单人世界死亡后选择退出至标题崩溃（NPE: gameMode null） | FIXED | 根因：`disconnect(null)` 使断开过程中的中间 tick 以 pause=false 运行，`gameMode.tick()` 在 `gameMode` 已被置空而 `level` 尚未清空的窗口内 NPE。原版传 `GenericMessageScreen`（isPauseScreen=true → pause=true 跳过守卫）故不崩。修复：完全复刻原版退出流程 `level.disconnect()` → `disconnect(GenericMessageScreen("menu.savingLevel"))` → `setScreen(TitleScreen)` |
| IS8 | 经验条/面板被"遮蔽"、颜色发糊 | FIXED | 根因：`Screen.render` 开头会再次调用 `renderBackground`，红色渐变在自绘内容之后被二次叠加（按钮在其后绘制所以清晰）。修复：render 不再调 super.render，改为显式调用一次 renderBackground + 手动遍历 renderables |
| IS9 | 等级数字应与经验条轻微重叠（原版样式） | FIXED | 数字改为条绘制之后绘制，底边压住条顶 2px |
| IS10 | PVP 拾取页面与设计书示例图不符 | FIXED | 按示例图重做：中性深色底、"拾取物品"居中标题、浅色半透明槽位（白 15% + #8b8b8b 边）、"物品栏"标签 + 视角者物品栏只读展示、常驻滚动条 |

## 与设计书的有意偏离（备案）

- 经验条 A/B 语义由"费用 C(n) / 剩余 Xe"改为"预计重生等级 / 当前等级"（用户澄清，IS1）。
- 经验条 A 条形填充由 C(n)/X₀ 改为预计等级内的进度（原公式在大经验下不可见，IS1 前身问题）。
- 倒计时归零只结算不强制重生（用户澄清，设计书"重生玩家"步骤改为玩家手动重生触发）。
