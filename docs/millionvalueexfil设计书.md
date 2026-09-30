# Million Value Exfil（MVE）模组设计书

## 模组内容

当生存模式或冒险模式玩家在 `keepInventory=false` 的世界中死亡时，MVE 会在死亡结算完成前打开选择界面，并替换原版死亡界面。

玩家可以在服务端限定的时间 \(t\) 内，从死亡时携带的物品中选择 \(n\) 个**物品**保留。这里的“物品”指**一个非空槽位（Slot）及其存储的物品堆叠**；两个槽位中即使存在完全相同\可以合并的物品堆叠，也视为两个独立物品。

未保留物品继续按照原有死亡掉落规则处理；保留物品在玩家重生后返还。已经被原版特殊死亡规则或其他模组接管的物品不由 MVE 重新处理。

MVE 不取消已经成立的死亡，不模拟额外的“濒死”生命状态，也不通过修改全局 `keepInventory` 规则实现物品保留。

## 核心特性

### 触发时机与行为

MVE 只在玩家**实际死亡已经成立**后触发,MVE触发后会替换原版死亡页面并且采取一系列措施。

不死图腾、复活类模组及其他能够阻止或延迟真正死亡的机制优先处理；只有这些机制最终没有阻止死亡时,实际死亡才已经成立，MVE 进入选择流程。

触发规则：

- 生存模式、冒险模式：允许；
- 创造模式、旁观模式：不触发；
- `keepInventory=true`：不触发；
- 没有可由 MVE 处理的死亡物品：不触发；
- `doImmediateRespawn=true`：不触发；
- 极限模式：默认不触发(可配置)；
- 指令杀`minecraft:generic_kill`：默认不触发(可配置)；

允许和禁止触发的伤害类型应支持服务端配置。

### 物品状态

进入选择流程后,玩家的存储与经验将会被冻结，死亡时存在的物品分为三种状态：

**默认状态**

-  正常进入死亡掉落流程且当前仍可由 MVE 安全处理的物品。

-  倒下玩家可以点击该物品，使其转变为**保留**状态。

**保留状态**

- 已被倒下玩家选择的物品。

- 保留物品不会作为本次死亡掉落生成，而是在重生后返至玩家原本槽位。

- 再次点击可取消保留，使其恢复为默认状态。

**禁用状态**

- 死亡时存在，但根据原版规则、物品自身规则或其他模组规则，MVE 无法安全取得处理权的物品。

- 例如具有消失诅咒或因持有者实际死亡触发特定行为的物品。

- 禁用物品可以显示其物品信息，但无法选择，也无法通过 PVP 选择界面取得。

所有物品均以完整 `ItemStack` 为最小处理单位，与容器槽位不同的是,物品不允许：

- 主动丢弃；
- 移动或交换槽位；
- 拆分或合并堆叠；
- 快速移动；
- 数字键换位；
- 双击收集；
- 修改物品数量；
- 操作收纳袋等物品内部库存。

等背包管理操作.物品只是槽位的映射与以玩家熟悉的方式具象化.

MVE 不进入潜影盒、收纳袋、背包等物品堆叠的内部库存；这些容器本身始终作为一个完整物品堆叠处理。

### 物品保留数 n

玩家消耗经验点数，增加死亡后可保留的物品数量。

保留 n 个物品的总经验费用：

$$
\boxed{
C(n)=
\left\lfloor
\frac{1395n(n+1)}{1332}
\right\rfloor
}
$$

其中 C(0)=0，C(36)=1395（即原版 0→30 级的 1395 点经验）。

第 n 个保留名额的边际费用：

$$
\boxed{
\Delta C(n)=C(n)-C(n-1)
}
$$

近似为 2.095n，边际成本线性递增。

典型费用：

```text
n = 1   ->    2 XP
n = 5   ->   31 XP
n = 10  ->  115 XP
n = 20  ->  439 XP
n = 30  ->  973 XP
n = 36  -> 1395 XP
```

36 仅为费用基准点，非上限。若其他模组增加额外装备槽位，n 可超过 36，沿用同一费用曲线,曲线的各个参数,甚至曲线本身的公式都应可以配置。

---

### 最大可选数量

$$
\boxed{
n_{\max}
=
\max
\left\{
n\in\mathbb Z
\mid
0\le n\le m,\;
C(n)\le X_0
\right\}
}
$$

其中 X₀ 为死亡时总经验，m 为当前可选物品总数。

选择后剩余经验：

$$
\boxed{
X_r=X_0-C(n)
}
$$

---

### 剩余经验处理

服务端配置项 k 决定 Xᵣ 的处理方式：

```text
k = num[0,1]
重生后保留 Xe=Xr*k 点经验，剩余 Xr*(1-k) 按以下逻辑掉落
```

当 k≠1 时，由 Xᵣ 反查对应经验等级 Lᵣ：

$$
E(L)=
\begin{cases}
L^2+6L,&0\le L\le16\$$4pt]
2.5L^2-40.5L+360,&17\le L\le31\$$4pt]
4.5L^2-162.5L+2220,&L\ge32
\end{cases}
$$

$$
L_r=
\max
\left\{
L\in\mathbb Z_{\ge0}
\mid E(L)\le X_r
\right\}
$$

死亡实际掉落经验：

$$
\boxed{
D(X_r)=\min(7L_r,\,100)
}
$$

遵循原版"等级×7，上限100"规则，不直接将全部剩余经验Xr*(1-k)变为经验球。

### 选择时间t

选择阶段持续：

$$
\boxed{
T=20t
}
$$

个有效服务端游戏刻。

其中t为单位为秒选择时间

服务器关闭期间不消耗剩余选择时间；服务器恢复后继续未完成的选择流程。

选择期间，其他存活玩家可以右击死亡位置的 MVE 交互目标(倒下的玩家)，打开 PVP 选择界面。

PVP 玩家只能看见/取得处于**默认与保留**状态的物品。

当 PVP 玩家取得一个物品时：

- 该物品立即从 MVE 的死亡事务中移除；
- 该物品从死亡位置朝操作玩家方向以物品实体形式抛出(以原版丢弃的方式)；
- 该物品不再参与后续死亡掉落结算；
- 该物品同时从倒下玩家和其他 PVP 玩家的选择界面中消失。

倒下玩家与 PVP 玩家同时操作同一物品时，以**服务端实际接受操作的先后顺序**为唯一结果：

```text
PVP 玩家取得物品
-> 更新死亡玩家的选择页面,从保留/备选区域移除该物品,返回对应的经验
-> 物品离开死亡事务
-> 倒下玩家无法再选择。
```

客户端时间戳、网络延迟和本地显示结果均不能决定所有权。

倒计时结束或玩家点击“重生”时，立即以服务端当前确认的选择状态完成结算：

- 保留已选择物品；
- 掉落所有未保留物品；
- 扣除 \(C(n)\)；
- 按配置 \(k\) 处理剩余经验；
- 重生玩家；
- 返还保留物品。

玩家在倒计时结束前点击“退出至标题”时，视为**放弃本次 MVE 选择**：

```text
n = 0
不收取 MVE 经验费用
所有仍由 MVE 持有的物品按死亡掉落处理
经验按原版死亡规则处理
```

选择期间发生客户端掉线、客户端崩溃或强制退出时，同样默认视为放弃选择，防止通过断线规避 PVP 抢取或死亡代价。

服务端正常关闭或服务端崩溃不视为玩家主动放弃；未完成事务应在服务器恢复后继续。

## 选择界面

### 倒下玩家选择界面

界面基于原版死亡屏幕修改，玩家死亡后弹出，用于在有限时间内选择保留部分掉落物品。布局自上而下分为四个区域：

1. **顶部状态区(原版组件)**：居中显示死亡标题（如"你死了！"）、死亡消息及分数。

2. 已保留物品区

   ：位于上方，显示当前已选择保留的物品槽位。

   - **左侧**：显示黄色文本"已保留物品"。
   - **右侧**：显示物品总数限制 $(n/n_{\max})$。
   - **下方**：显示玩家死亡时的经验值的经验条（经验条 A）。

3. 待选物品区

   ：位于下方，显示死亡掉落的所有物品槽位。

   - **左侧**：显示黄色文本"请选择需要保留的物品"。
   - **右侧**：显示倒计时"剩余 MM:SS"。
   - **下方**：显示预计玩家重生后保留的经验值经验条（经验条 B）。
   - 距离倒计时结束前$t_e$秒,待选物品区所有控件会透明度闪烁并且逐渐变得透明,当倒计时结束时消失并且禁用.

4. **底部按钮区(原版组件)**：包含"标题屏幕"和"重生"两个按钮。与原版不同的,是点击"标题屏幕"后弹出页面的两个选项从原版的"标题画面"和"重生"改为"标题画面"和"返回",点击返回后退回选择页面.

界面根据窗口大小动态调整，物品过多时采用滚动区域显示，不缩放槽位。

#### 信息反馈

- **经验条 A（上）**：可视化当前已选物品的总费用 $C(n)$。
- **经验条 B（下）**：可视化选择完成后的剩余经验 $X_e = k(X_0 - C(n))$。
- 数值显示：
  - 上方左侧直接显示当前已选数量 $n$。
  - 上方右侧显示容量上限 $(n/n_{\max})$。
  - 下方左侧显示实时计算的剩余经验值。
  - 下方右侧显示剩余选择时间。

> 客户端显示的数值与经验条仅作为实时反馈，最终结算以服务端状态为准!!!

#### 物品交互逻辑

物品以原版物品栏槽位形式渲染，保留图标、堆叠数、耐久度、附魔光效及 Tooltip。

##### 渲染规则

- **稀疏渲染**：仅渲染包含物品堆叠的槽位，跳过所有物品堆叠为的空槽位,显示的槽位 ID 可能不连续。
- **滚动容器**：两个物品网格区域均为固定高度、纵向可滚动容器，最多显示约两行槽位。

##### 排序规则

槽位按 Minecraft 内部槽位 ID 升序排列，**以开发时的实际值为准**，不同版本可能存在差异：

| 区域     | 示例 ID | 说明                                   |
| -------- | ------- | -------------------------------------- |
| 快捷栏   | 0–8     | 9 个快捷栏槽位                         |
| 主物品栏 | 9–35    | 27 个背包槽位                          |
| 主手     | 98      | 快捷栏当前选中格的动态引用，非独立槽位 |
| 副手     | 99      | 副手槽位                               |
| 盔甲栏   | 100–103 | 依次为靴子、护腿、胸甲、头盔           |

> **关于主手**：快捷栏当前选中格的动态引用（指针），并非独立存储槽位。其物品已包含在快捷栏 (ID: 0–8) 中，因此不单独渲染，避免同一物品被重复显示与提交。

##### 操作行为

- **点击待选区物品**：移入上方"已保留物品区"，扣除对应经验。
- **点击已保留区物品**：移回下方"待选物品区"，返还对应经验。
- **禁用物品**：此类物品槽位背景呈红色，**不可点击,只能存在于备选区**,无论其槽位 ID 为何，被标记为"禁用"的物品始终排列在所有正常物品的末尾。
- **经验不足反馈**：当剩余经验不足以支付费用时，仅阻止操作或给予提示，**不改变**槽位的禁用状态。

除上述保留/取消操作外，不支持拖拽整理或其他物品栏操作。

#### 组件及操作行为参考

以下 H5仅用于传达界面的组件结构和交互逻辑设计思路。**设计规则以上述文档正文为准**，代码注释仅补充文档中未覆盖的实现细节（如 CSS 类名约定、JS 状态流转方式）。

```html
<div id="death-retention-screen" class="fullscreen-overlay">

  <!-- 1. 顶部状态区 -->
  <div id="header-status" class="text-center">
    <h1 id="death-title">你死了！</h1>
    <p id="death-message">Xieyc从高处摔了下来</p>
    <span id="score-display">分数：<b>0</b></span>
  </div>

  <!-- 2. 已保留物品区 -->
  <section id="selected-inventory-section" class="inventory-panel">
    <div class="panel-info-bar">
      <div class="info-left">
        <span>已保留物品</span>
        <span class="highlight-green" id="val-n">12</span>
      </div>
      <div class="info-right">
        <!-- 动态值：由 JS state.n / state.n_max 计算 -->
        <span id="val-capacity">(12/23)</span>
      </div>
    </div>

    <!-- 经验条 A：宽度 = C(n) / X0 * 100% -->
    <div class="xp-bar-container cost-bar">
      <div id="bar-cost" class="xp-fill"></div>
    </div>

    <!-- 滚动容器：固定高度，overflow-y: scroll -->
    <div id="grid-selected" class="item-grid scrollable">
      <!--
        槽位示例（随机缺失模拟空槽位跳过）：
        实际 ID 以运行时为准，此处仅为演示排序效果。
        data-mc-id 用于排序，data-state 用于交互判断。
      -->
      <div class="slot" data-state="selected" data-mc-id="0"></div>
      <div class="slot" data-state="selected" data-mc-id="2"></div>
      <div class="slot" data-state="selected" data-mc-id="5"></div>
      <div class="slot" data-state="selected" data-mc-id="9"></div>
      <div class="slot" data-state="selected" data-mc-id="14"></div>
      <div class="slot" data-state="selected" data-mc-id="20"></div>
      <div class="slot" data-state="selected" data-mc-id="25"></div>
      <div class="slot" data-state="selected" data-mc-id="31"></div>
      <div class="slot" data-state="selected" data-mc-id="35"></div>
      <div class="slot" data-state="selected" data-mc-id="99"></div>  <!-- 副手 -->
      <div class="slot" data-state="selected" data-mc-id="100"></div> <!-- 靴子 -->
      <div class="slot" data-state="selected" data-mc-id="102"></div> <!-- 胸甲 -->
      <div class="slot" data-state="selected" data-mc-id="103"></div> <!-- 头盔 -->
      <div class="slot" data-state="selected" data-mc-id="3"></div>
      <div class="slot" data-state="selected" data-mc-id="7"></div>
      <div class="slot" data-state="selected" data-mc-id="12"></div>
      <div class="slot" data-state="selected" data-mc-id="18"></div>
      <div class="slot" data-state="selected" data-mc-id="23"></div>
    </div>
  </section>

  <!-- 3. 待选物品区 -->
  <section id="available-inventory-section" class="inventory-panel">
    <div class="panel-info-bar">
      <div class="info-left">
        <span>请选择需要保留的物品</span>
        <span class="highlight-green" id="val-xr">36</span>
      </div>
      <div class="info-right">
        <!-- 动态值：由state.timer 格式化为 MM:SS -->
        <span id="val-timer">剩余01:29</span>
      </div>
    </div>

    <div class="xp-bar-container remaining-bar">
      <div id="bar-remaining" class="xp-fill"></div>
    </div>

    <!-- 滚动容器 -->
    <div id="grid-available" class="item-grid scrollable">
      <!-- 正常可选槽位（随机缺失模拟空槽位跳过） -->
      <div class="slot" data-state="available" data-mc-id="1"></div>
      <div class="slot" data-state="available" data-mc-id="4"></div>
      <div class="slot" data-state="available" data-mc-id="8"></div>
      <div class="slot" data-state="available" data-mc-id="11"></div>
      <div class="slot" data-state="available" data-mc-id="16"></div>
      <div class="slot" data-state="available" data-mc-id="22"></div>
      <div class="slot" data-state="available" data-mc-id="27"></div>
      <div class="slot" data-state="available" data-mc-id="33"></div>
      <div class="slot" data-state="available" data-mc-id="6"></div>
      <div class="slot" data-state="available" data-mc-id="13"></div>
      <div class="slot" data-state="available" data-mc-id="19"></div>
      <div class="slot" data-state="available" data-mc-id="24"></div>
      <div class="slot" data-state="available" data-mc-id="29"></div>
      <div class="slot" data-state="available" data-mc-id="34"></div>

      <!-- 禁用槽位：始终排在末尾，.disabled 类控制样式与 pointer-events -->
      <div class="slot disabled" data-state="disabled" data-mc-id="101"></div> <!-- 护腿(消失诅咒) -->
      <div class="slot disabled" data-state="disabled" data-mc-id="15"></div>  <!-- 某个模组中会因死亡执行特定行为的物品 -->
    </div>
  </section>

  <!-- 4. 底部按钮区 -->
  <footer id="action-buttons" class="button-row">
    <button id="btn-title-screen">标题屏幕</button>
    <button id="btn-respawn">重生</button>
  </footer>
</div>

<script>
/**
 * 状态管理与交互逻辑
 * 仅传达数据流转方式，实际实现以MC与模组加载器逻辑为准。
 */

const state = {
  X0: 1234,   // 死亡时总经验
  Xr: 12,     // 剩余经验
  n: 12,      // 当前已选数量
  n_max: 34,  // 最大可选数量
  timer: 12   // 剩余秒数
};

const ui = {
  n: document.getElementById('val-n'),
  capacity: document.getElementById('val-capacity'),
  xr: document.getElementById('val-xr'),
  timer: document.getElementById('val-timer'),
  gridSelected: document.getElementById('grid-selected'),
  gridAvailable: document.getElementById('grid-available')
};

function updateUI() {
  ui.capacity.innerText = `(${state.n}/${state.n_max})`;

  const m = Math.floor(state.timer / 60).toString().padStart(2, '0');
  const s = (state.timer % 60).toString().padStart(2, '0');
  ui.timer.innerText = `剩余${m}:${s}`;
}

function handleSlotClick(slot) {
  const currentState = slot.dataset.state;
  if (currentState === 'disabled') return;

  if (currentState === 'available') {
    if (state.n >= state.n_max) return;
    // 扣除经验（伪代码）：state.Xr -= getItemCost(mcId);
    slot.dataset.state = 'selected';
    ui.gridSelected.appendChild(slot);
    state.n++;
  } else if (currentState === 'selected') {
    // 返还经验（伪代码）：state.Xr += getItemCost(mcId);
    slot.dataset.state = 'available';
    ui.gridAvailable.appendChild(slot);
    state.n--;
  }

  sortGrid(ui.gridSelected);
  sortGrid(ui.gridAvailable);
  updateUI();
}

/**
 * 排序：正常物品按 槽位id 升序，禁用物品始终置底（内部也按 槽位id 升序）
 */
function sortGrid(gridElement) {
  const slots = Array.from(gridElement.children);
  slots.sort((a, b) => {
    const isDisA = a.classList.contains('disabled');
    const isDisB = b.classList.contains('disabled');
    if (isDisA !== isDisB) return isDisA ? 1 : -1;
    return parseInt(a.dataset.mcId) - parseInt(b.dataset.mcId);
  });
  slots.forEach(slot => gridElement.appendChild(slot));
}

[ui.gridSelected, ui.gridAvailable].forEach(grid => {
  grid.addEventListener('click', (e) => {
    const slot = e.target.closest('.slot');
    if (slot) handleSlotClick(slot);
  });
});

updateUI();
sortGrid(ui.gridSelected);
sortGrid(ui.gridAvailable);
</script>
```

### PVP 拾取界面

PVP 模式下采用类似原版容器的双栏布局：

*   **上半部分**：显示倒下玩家当前可被拾取的物品(默认\保留)。
*   **下半部分**：显示操作玩家自身的物品栏（仅用于视觉展示，不可转移物品到上半）。

**交互逻辑**：
点击上半部分物品后，由服务端校验是否仍可拾取。校验通过后，物品立即从所有打开PVP 拾取界面的文件的界面中移除，并从死亡位置向操作玩家方向掉落实体。

#### 组件及操作行为参考

以下 H5仅用于传达界面的组件结构和交互逻辑设计思路。**设计规则以上述文档正文为准**，代码注释仅补充文档中未覆盖的实现细节（如 CSS 类名约定、JS 状态流转方式）。

```html
<div id="pvp-pickup-screen" class="fullscreen-overlay">
    
    <!-- 1. 顶部标题区 -->
    <div id="header-title" class="text-center">
            <div class="info-left">
                <span>拾取物品</span>
            </div>
    </div>

    <!-- 2. 待拾取物品区 (上半部分) -->
    <!-- 规则: 仅渲染禁用状态以外的物品, 并按照槽位id 排序 -->
    <section id="pickupable-inventory-section" class="inventory-panel">
        <!-- 固定高度的滚动容器, 强制显示右侧滑动条 -->
        <div id="grid-pickupable" class="item-grid scrollable">
            
            <!-- 
              槽位示例 (空槽位已跳过):
              保持基础 .slot 格式, 使用 槽位id 进行排序和伪代码参数绑定。
              禁用物品在服务端渲染阶段已被过滤, 此处不显示。
            -->
            <div class="slot" data-mc-id="0"></div>
            <div class="slot" data-mc-id="1"></div>
            <div class="slot" data-mc-id="2"></div>
            <div class="slot" data-mc-id="5"></div>
            <div class="slot" data-mc-id="9"></div>
            <div class="slot" data-mc-id="15"></div>
            <div class="slot" data-mc-id="22"></div>
            <div class="slot" data-mc-id="30"></div>
            <div class="slot" data-mc-id="35"></div>

        </div>
    </section>

    <!-- 3. 拾取者物品栏区 (下半部分) -->
    <section id="player-inventory-section" class="inventory-panel">
        <div class="panel-info-bar">
            <div class="info-left">
                <span>物品栏</span>
            </div>
        </div>

        <div id="grid-player" class="item-grid">
            <div class="vanilla-slot"></div>
            <div class="vanilla-slot"></div>
            <div class="vanilla-slot"></div>
            <div class="vanilla-slot"></div>
            <div class="vanilla-slot"></div>
            <!-- ... (此处省略主物品栏及快捷栏的空 vanilla-slot) ... -->
            <div class="vanilla-slot"></div>
            <div class="vanilla-slot"></div>
            <div class="vanilla-slot"></div>
        </div>
    </section>

</div>

<style>
.fullscreen-overlay {
    position: fixed; top: 0; left: 0; width: 100%; height: 100%;
    display: flex; flex-direction: column; align-items: center; justify-content: center;
    background: rgba(0, 0, 0, 0.75); color: white;
}
.text-center { text-align: center; margin-bottom: 15px; }

.inventory-panel { width: 420px; margin-bottom: 20px; }
.panel-info-bar { display: flex; justify-content: space-between; margin-bottom: 10px; font-size: 14px;}

.item-grid { display: grid; grid-template-columns: repeat(9, 1fr); gap: 4px; }

/* 上半部分：带滚动条的拾取区 */
.scrollable {
    height: 180px;
    overflow-y: scroll; /* 强制显示右侧垂直滑动条轨道 */
    padding-right: 10px; /* 防止滚动条遮挡最后一列 */
}

/* 基础槽位 (与死亡选择页面保持视觉一致) */
.slot {
    width: 36px; height: 36px; background: rgba(255, 255, 255, 0.15);
    border: 1px solid #8b8b8b; cursor: pointer;
    transition: background 0.2s;
}
.slot:hover { background: rgba(255, 255, 255, 0.3); border-color: #fff; }

/* 下半部分：纯视觉展示的原版槽位 */
.vanilla-slot {
    width: 36px; height: 36px; background: rgba(200, 200, 200, 0.4);
    border: 1px solid #555;
}
</style>

<script>
/**
 * PVP 拾取界面核心交互逻辑
 */

// 辅助：按 data-mc-id 对上半部分的物品槽位进行升序排列
function sortPickupableGrid() {
    const grid = document.getElementById('grid-pickupable');
    const slots = Array.from(grid.children);
    slots.sort((a, b) => {
        return parseInt(a.dataset.mcId) - parseInt(b.dataset.mcId);
    });
    slots.forEach(slot => grid.appendChild(slot));
}

// 初始化：加载时执行排序
document.addEventListener('DOMContentLoaded', () => {
    sortPickupableGrid();
});

// 事件委托：监听上半部分的所有点击事件
document.getElementById('grid-pickupable').addEventListener('click', (e) => {
    // 确保点击的是带有 .slot 类的子元素
    const slot = e.target.closest('.slot');
    if (!slot) return;

    const mcId = slot.dataset.mcId;

    // 【客户端视觉更新】点击后从 DOM 中立即移除该物品槽位
    slot.remove();

    // 【通知服务端】
    // 在实际的模组后端中，你需要将请求发送给服务器以进行最终校验。
    
    console.log(`[PVP Pickup] 玩家点击拾取，已移除槽位 ID: ${mcId}`);
});
</script>
```

## 安全与一致性

- **客户端权限**：客户端只能提交：

```text
  死亡事务标识
  当前事务版本
  物品条目标识
  操作类型
```

客户端不得提交或决定：

```text
  ItemStack
  物品种类
  数量
  数据组件
  NBT
  经验费用
  剩余经验
  最终所有权
```

  所有操作均由服务端重新验证和执行。

- **崩溃恢复**：未完成的死亡事务必须持久保存。

  服务端恢复后应能够继续：

  ```text
  未完成的选择
  未完成的死亡掉落
  未完成的物品返还
  未完成的经验结算
  ```

  同一结算重复执行时必须具有幂等性，不得因重试产生重复物品。

  MVE 的一致性目标为：

  > 在自身能够确认所有权的范围内，不静默删除物品、不重复返还物品、不重复生成掉落；无法确认当前所有权时停止接管并采用安全退化。

  不对磁盘损坏、其他模组强制删除数据等超出 MVE 控制范围的情况承诺数学意义上的绝对 exactly-once。

- **兼容**：MVE 只处理**能够确认仍属于正常死亡掉落流程，并且尚未被其他机制取得所有权的物品**。

  已经被其他模组移动、保存、销毁、绑定或接管的物品不重新创建。

重点兼容类别包括：

-  墓碑 / 尸体：
   Corpse
   [GraveStone Mod](../refer/gravestone-1.21.1.zip)
   Corail Tombstone
   You're in Grave Danger

-  玩家互助 / 濒死复活：
   [PlayerRevive](../refer/PlayerRevive-1.21.zip)
   Revive Me!

-  扩展装备槽：
   [Curios](../refer/Curios-1.21.1.zip)
   Accessories
   Cosmetic Armor Reworked

-  容器 / 背包：
   原版收纳袋
   原版潜影盒
   Sophisticated Backpacks

-  灵魂绑定 / 死亡保留类：
   [inventory-protection](../refer/inventory-protection-1.21.1-neo.zip)
   [Soulbound](../refer/Soulbound-neoforge-1.21.1.zip)
   任何主动使指定物品不进入正常死亡掉落的机制


  上述主要项目均存在对应 1.21.1/NeoForge 版本基线，可作为实际兼容测试对象。值得注意的是,我们的开发工作是**兼容而非适配**,这些模组不是本模组的前置模组.本模组有自己的逻辑设计与底蕴,而不是在其它模组的夹缝中生存!!!

  对扩展装备槽采用显式支持方式，不假定可以自动发现所有其他模组增加的玩家槽位。

  对墓碑、尸体和死亡物品整体接管类模组，如果目标模组已经完整取得死亡物品，MVE 应静默退出。

  对濒死和玩家互助复活类模组，MVE 只在玩家最终真正死亡后介入。

  最终兼容原则：

  > **无法证明物品仍属于 MVE 可以安全处理的死亡掉落时，不取得该物品所有权。**

- **容错**：保留物品在玩家重生后按照以下顺序返还：

  1. 原始槽位仍为空且允许放置 → 放回原槽位；
  2. 否则使用对应槽位系统提供的安全插入方式；
  3. 否则尝试合并至玩家已有堆叠或放入空物品栏槽位；
  4. 仍无法放入 → 在重生玩家脚下生成物品实体。

  任何情况下禁止：

  ```text
  覆盖已有物品
  静默删除物品
  静默缩减堆叠数量
  根据客户端内容重建物品
  根据旧快照复制已被其他机制接管的物品
  ```

  单个物品发生异常时，只让该物品退化为安全掉落，不回滚已经正确完成的其他独立结算。

  MVE 整体遵循 **fail-open**：

  > 当无法确认主动接管比继续原版或其他模组的死亡处理更加安全时，不接管。