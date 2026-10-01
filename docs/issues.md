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
| IS6 | 经验条填充宽度与网格未对齐 | FIXED | 条宽改为与网格同宽（162），面板 182 对齐快捷栏 |
| IS7 | 单人世界死亡后选择退出至标题崩溃（NPE: gameMode null） | FIXED | 根因：`disconnect(null)` 使断开过程中的中间 tick 以 pause=false 运行，`gameMode.tick()` 在 `gameMode` 已被置空而 `level` 尚未清空的窗口内 NPE。原版传 `GenericMessageScreen`（isPauseScreen=true → pause=true 跳过守卫）故不崩。修复：完全复刻原版退出流程 `level.disconnect()` → `disconnect(GenericMessageScreen("menu.savingLevel"))` → `setScreen(TitleScreen)` |
| IS8 | 经验条/面板被"遮蔽"、颜色发糊 | FIXED | 根因：`Screen.render` 开头会再次调用 `renderBackground`，红色渐变在自绘内容之后被二次叠加（按钮在其后绘制所以清晰）。修复：render 不再调 super.render，改为显式调用一次 renderBackground + 手动遍历 renderables |
| IS9 | 等级数字应与经验条轻微重叠（原版样式） | FIXED | 数字改为条绘制之后绘制，底边压住条顶 2px |
| IS10 | PVP 拾取页面与设计书示例图不符 | FIXED | 按示例图重做：中性深色底、"拾取物品"居中标题、浅色半透明槽位（白 15% + #8b8b8b 边）、"物品栏"标签 + 视角者物品栏只读展示、常驻滚动条 |

## 与设计书的有意偏离（备案）

- 经验条 A/B 语义由"费用 C(n) / 剩余 Xe"改为"预计重生等级 / 当前等级"（用户澄清，IS1）。
- 经验条 A 条形填充由 C(n)/X₀ 改为预计等级内的进度（原公式在大经验下不可见，IS1 前身问题）。
- 倒计时归零只结算不强制重生（用户澄清，设计书"重生玩家"步骤改为玩家手动重生触发）。
