# CB_Treadmill 宝可梦动画渲染笔记

> 目标：解释为什么"猫鼬少（Yungoos）/ 魅力喵（Glameow）这类宝可梦在跑步机上不会播放行走动画"，
> 以及如何在 `CB_TreadmillRenderer` 上渲染 行走 / 跑步 / 飞行 / 攻击 / 死亡 等动画。
> 代码依据：Cobblemon 1.6.x（curse.maven:cobblemon-687131 / 7553231）反编译 + NeoForge 1.21.1 源码。

---

## 1. 结论（本轮到此的修复）

- **根因**：Cobblemon 的行走动画分两套驱动：
  1. `q.anim_time` 驱动的骨骼动画（`q.bedrock('x', 'ground_walk')`）—— 由合成实体的动画年龄推进，跑步机没问题；
  2. 函数动画 `q.quadruped_walk / q.biped_walk / q.bimanual_swing` —— 直接使用原版的
     `limbSwing / limbSwingAmount`（`LivingEntityRenderer.render` 从实体 `walkAnimation` 状态取出），
     而 `walkAnimation` 只由 `LivingEntity#calculateEntityAnimation` 按实体**实际移动量**推进。
  跑步机上的合成 `PokemonEntity` 从不移动 → 这两个值恒为 0 → 顺腿动画的振幅被乘成 0，
  于是猫鼬少、魅力喵等以函数动画行走的宝可梦"姿势是行走、但腿完全不动"（看起来冻住了）。
  妙蛙种子这类用 `q.bedrock('x','ground_walk')` 的则正常，因为动画按 `q.anim_time` 播放。
- **修复**：在 `CB_TreadmillEntity.tick()`（客户端）每 tick 调
  `clientPokemonEntity.walkAnimation.update(target, 1.0F)`，代替实体推进步态相位；
  `target` 与跑步机实际转速（RPM）联动：`Mth.clamp(|getSpeed()| / 64F, 0.15F, 0.6F)`。
  渲染时 `MobRenderer` 自动以 `walkAnimation.speed(partialTicks)`（振幅）/ `position(partialTicks)`（相位）插值。

---

## 2. 动画管线的完整数据流（合成实体）

```
CB_TreadmillEntity.tick()（客户端，每 tick）
  ├─ delegate.tick(entity)            → PosableState.incrementAge → age+1（q.anim_time 的来源）
  └─ entity.walkAnimation.update(...) → 推进 limbSwing / limbSwingAmount（顺腿函数动画的来源）

CB_TreadmillRenderer.renderSafe（每帧）
  └─ delegate.updatePartialTicks(partialTick)                     → q.anim_time = (age + partialTick) / 20
     └─ EntityRenderDispatcher.render → PokemonRenderer.render
        └─ MobRenderer.render → entity.walkAnimation.speed/position(partialTick)
           └─ PosableEntityModel.setupAnim(limbSwing, limbSwingAmount, ...)
              └─ PosableModel.applyAnimations(entity, state, limbSwing, ...)
                 1. validatePose(entity, state)：拿 state.currentPose 与 entity.getCurrentPoseType() 匹配
                    └─ 不一致时 getFirstSuitablePose(state, poseType)：第一个 poseTypes 含 poseType 且 isSuitable 的姿势
                 2. pose.apply(...)：跑走路姿势里的全部动画（q.bedrock / q.quadruped_walk / q.look ...）
                 3. primaryAnimation / activeAnimations（一次性动作：攻击、惨叫、昏厥等）
```

要点：**姿势（Pose）**由 `PoseType` 决定，**动画播放**由 `q.anim_time`（年龄）与
`limbSwing`（步态相位）两条钟共同驱动。合成实体两个钟都要自己手动推。

### 关键 API 索引

| 用途 | API |
| --- | --- |
| 强制姿势 | `entity.getEntityData().set(PokemonEntity.getPOSE_TYPE(), PoseType.X)` + `entity.setEnablePoseTypeRecalculation(false)` |
| 推进年龄 | `entity.getDelegate().tick(entity)`（每 tick）；`delegate.updatePartialTicks(pt)`（每帧） |
| 推进步态 | `entity.walkAnimation.update(speed, 1.0F)`（每 tick，speed ≈ 振幅，也是相位增量） |
| 姿势可用值 | PoseType：STAND, WALK, SLEEP, HOVER, FLY, FLOAT, SWIM, GLIDE, SHOULDER_LEFT, SHOULDER_RIGHT, PROFILE, PORTRAIT, OPEN, NONE |
| 取动画 | `model.getAnimation(state, name, runtime)`；名字 = `"species:动画key"`（bedrock 文件）或 poser JSON 顶层的命名动画（如 `faint`/`cry`/`recoil`） |
| 播放一次性动画 | `state.addActiveAnimation(anim, onDone)`（叠加）；`state.addPrimaryAnimation(primary)`（压过姿势动画） |
| 姿势弹道 | `q.quadruped_walk / q.biped_walk / q.bimanual_swing / q.sine_wing_flap / q.look / q.punch / q.bedrock / q.bedrock_stateful / q.bedrock_primary`（定义于 `ClientMoLangFunctions.animationFunctions`） |

---

## 3. 行走 / 跑步

- 姿势：`PoseType.WALK`（部分物种两个姿势都吃 WALK/SWIM）。
- 行走动画来源（每个物种的 poser JSON，如 `bedrock/pokemon/posers/0734_yungoos/yungoos.json`）：
  - `"walking": {"poseTypes":["WALK","SWIM"], "animations":["q.look('head')","q.bedrock('x','ground_idle')","q.quadruped_walk(...)"]}`
  - 或用 bedrock 行走动画（妙蛙种子）：`q.bedrock('x','ground_walk')`。
- 跑步 = 同一个 WALK 姿势，只是加快步态相位（提高 `walkAnimation` 的 target）并可选加大振幅：
  - `target = Mth.clamp(|getSpeed()| / 64F, 0.15F, 0.6F)`（现状：转速联动，天然"越跑越快"）。
  - 想要更强的"跑步"感：`target` 上限与每秒步频（`walkAnimation.position` 增量）再乘一个系数即可。
- 注意不要给合成实体加 `deltaMovement`：渲染位移由 `CB_TreadmillRenderer` 固定翻译，移动只是欺骗。
- **先决条件**：合成实体的 `PoseType` 必须被强制为 WALK，且 `setEnablePoseTypeRecalculation(false)`，
  否则服务端/客户端姿态重算会把静止实体判回 STAND。

---

## 4. 飞行

- 姿势：`PoseType.FLY`（拍翼）、`PoseType.HOVER`（浮空不动）、`PoseType.GLIDE`（滑翔，如坠落）。
- 姿势数据：物种 poser JSON 里有 `poseTypes` 含 FLY/HOVER 的姿势（如 `q.bedrock('x','fly')`、`q.sine_wing_flap(...)` 拍翼）时才有飞行姿态；
  没有飞行姿势的物种会回退到第一个姿势（stand），属于 Cobblemon 数据限制。
- 渲染步骤：
  1. `entity.getEntityData().set(PokemonEntity.getPOSE_TYPE(), PoseType.FLY)`（或 HOVER/GLIDE）；
  2. `entity.setNoGravity(true)`，并把合成实体抬到空中（`setPos` y+若干 / 渲染时 `poseStack.translate`），
     否则模型会站在跑道上；
  3. 年龄照常 `delegate.tick` 推进 —— 飞行动画基本按 `q.anim_time` 播放（`q.sine_wing_flap` 的 WaveFunction 也按时间驱动），
     不需要额外推进 `walkAnimation`（除非该物种飞行姿态用了顺腿类函数动画）。
  - 若想要"绕圈飞行"，还需每 tick 更新朝向（`yBodyRot`）与 `setPos` 的平移。

---

## 5. 攻击

一次性动作，不走姿势系统，而是往 `PosableState.activeAnimations / primaryAnimation` 里塞动画：

```java
PosableModel model = VaryingModelRepository.INSTANCE.getPoser(species, delegate);
ActiveAnimation anim = model.getAnimation(delegate, "yungoos:hurt", delegate.getRuntime()); // 名字 = "species:key"
if (anim instanceof PrimaryAnimation primary) {
    delegate.addPrimaryAnimation(primary);            // 覆盖行走姿势短暂播完，再回到姿势
} else {
    delegate.addActiveAnimation(anim, state -> {});   // 叠加播放，播完自动移除
}
```

- 可用 key：该物种 bedrock 动画文件里的任意动画（如 `hurt`/`recoil`/`shock`/`angry`/`attack` 等，按物种存在性各异），
  或 poser JSON 顶层的命名动画（`"animations":{"faint":..., "cry":..., "recoil":...}`）。
- `q.punch('head','body','leftArm','rightArm', swingRight)`（`PunchAnimation`）是适合人形的函数攻击动画，
  一般写在 poser JSON 的战斗姿势里，Java 侧同样可以 `model.getAnimation(delegate, "q.punch...", ...)` 不支持直接传表达式串——
  正确做法是用 `state.getRuntime()` 解析 MoLang：`MoLangExtensionsKt.resolve(runtime, "q.punch('head',...)")`。
- 更贴近 Cobblemon 战斗的做法：`state.addPrimaryAnimation(...)` 播完自动回到姿势（PrimaryAnimation 到期后 `afterAction` 被调用）。
- 想让攻击带音效：动画文件的 `sound_effects` 或播放物种叫声 `delegate.cry()`（`getCryAnimation`）。

---

## 6. 死亡（昏厥）

Cobblemon 的死亡动画 = `faint`（bedrock 动画，通常是 `primary` 类型）：

- 最省事的路径（与正式游戏完全一致）：
  ```java
  entity.getEntityData().set(PokemonEntity.getDYING_EFFECTS_STARTED(), true);
  ```
  `PokemonClientDelegate.onSyncedDataUpdated` 检测到该数据变化后会自动
  `model.getAnimation(state, "faint", runtime)` → `addPrimaryAnimation`，并在 3 秒后触发回调。
- 手动路径：
  ```java
  ActiveAnimation faint = model.getAnimation(delegate, "faint", delegate.getRuntime()); // poser JSON 命名动画
  delegate.addPrimaryAnimation((PrimaryAnimation) faint);
  ```
- 倒地带红闪：`entity.hurtTime` / `entity.deathTime > 0` 时 `PosableEntityModel.getOverlayTexture`
  会输出红色 overlay（`OverlayTexture.pack(u, v=1)`）；合成实体的 `deathTime` 需手动递增
  （`delegate.updatePostDeath()` 每次 `deathTime++`，见 `PokemonClientDelegate.updatePostDeath`）。
- 昏厥后通常还要 `entity.setHealth(0)`（影响 `LivingEntityRenderer` 的躺倒姿态）与停止 `walkAnimation` 推进，
  并把姿势换成 `STAND`（或物种的 `sleep` 姿势）避免"走着死"。

---

## 7. 通用注意事项

1. 合成实体从不进世界：所有"实体自己会做的事"都要手动模拟 —— 年龄、步态、姿态数据、朝向。
2. `setEnablePoseTypeRecalculation(false)` 是前置条件；否则每帧姿态重算会把 WALK 打回 STAND。
3. 姿势回退：`getFirstSuitablePose` 找不到匹配时会用**第一个姿势**，所以"不动的动物/无飞行姿态"表现正常但"跳不出动画"属数据缺失。
4. 部分物种的 walking 姿势把 WALK 和 SWIM 共用；把宝可梦放到水景/下界等场景时二选一即可。
5. 修改 `PoseType` 后建议清掉 `delegate.currentPose`（`setPoseToFirstSuitable(poseType)` 会重新选，
   或直接 `setPose(null)` 兼容字段）避免旧姿势残留 —— `getAnimation`/`validatePose` 都以 `currentPose` 为缓存。
