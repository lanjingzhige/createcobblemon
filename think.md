# CB_Treadmill 传动杆渲染为黑色的排查笔记

> 目标：解释"CB_Treadmill 渲染的传动杆是黑色的，而不是原版类似方块的颜色"，并给出修复方向。
> 已确认：机械动力源码 `D:\Github\MCMOD\Create-mc1.21.1-6.0.10`；运行环境 Flywheel 1.0.6 已加载（`run/logs/latest.log` 有 "Flywheel 1.0.6"、"Loaded 77 shader sources"、"Started 6 worker threads"）。

---

## 1. 两条传动轴渲染路径（Create）

- **Flywheel 实例化路径（用户环境中激活）**：
  - 注册：`ModBlockEntity.java` `.visual(() -> ShaftVisual::new, false)`（renderNormally=false → 原版 BE 渲染器被跳过，只画传动轴，不画宝可梦；注意：summary 里之前记的是 true，当前代码是 false）。
  - `ShaftVisual` extends `SingleAxisRotatingVisual` → 构造时 `instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT)).createInstance().rotateToFace(UP, rotationAxis()).setup(be).setPosition(...)`，创建 `RotatingInstance`。
  - `SingleAxisRotatingVisual.updateLight(pt)` → `relight(rotatingModel)` → `AbstractBlockEntityVisual.relight(FlatLit...)` → `relight(pos, instances)`，其中 `pos = blockEntity.getBlockPos()` → `FlatLit.relight(LevelRenderer.getLightColor(level, pos), instances)` → `instance.light(packedLight).handle().setChanged()`。
  - 光何时写入：`Storage.setup()`（Flywheel 源码）在 visual 创建时调用 `lightUpdated.updateLight(partialTick)` **一次**；之后仅当 `onLightUpdate(section)` 事件发生时（`LightUpdatedVisualStorage.plan()`），才会再次 updateLight。`ClientChunkCacheMixin` 注入 `ClientChunkCache.onLightUpdate` → `manager.onLightUpdate(pos, layer)`。
  - `RotatingInstance`（Create 类）继承 `ColoredLitInstance`，默认 `light = 0`（黑色）、color 白。
  - GPU 写入：`AllInstanceTypes.ROTATING` layout 含 `vector("light", SHORT, 2)`，writer 用 `put2x16(ptr+4, instance.light)`。
  - 顶点着色器 `rotating.vert`：`flw_vertexLight = max(vec2(instance.light)/256., flw_vertexLight)` → **最终光 = max(实例光, 模型烘焙光)**。

- **原版渲染器路径（Flywheel 不活动时兜底）**：
  - `KineticBlockEntityRenderer.renderSafe`：`if (VisualizationManager.supportsVisualization(be.getLevel())) return;` 否则 `standardKineticRotationTransform(be, ...)` 里 `buffer.light(light)`，light = `BlockEntityRenderDispatcher.setupAndRender` 传入的 `LevelRenderer.getLightColor(level, blockEntity.getBlockPos())`。
  - 结论：**两条路径采样的都是"方块自身体素"的光**，所以黑色与路径无关，只与方块体素处的光照值有关。

---

## 2. 光照值怎么来（已逐段读 NeoForge 21.1.248 源码确认）

`LevelRenderer.getLightColor(level, state, pos)`（`build/moddev/artifacts/neoforge-21.1.248-sources.jar`）：

```java
int i = level.getBrightness(LightLayer.SKY, pos);     // 方块自身体素
int j = level.getBrightness(LightLayer.BLOCK, pos);
int k = state.getLightEmission(level, pos);
if (j < k) j = k;
return i << 20 | j << 4;
```

- `Level` 未重写 `getBrightness`；`BlockAndTintGetter.getBrightness` 默认 = `getLightEngine().getLayerListener(type).getLightValue(pos)`。
- **1.21.1 的 `getLightColor` 没有 1.20 里 `isSolidRender → pos.above()` 的分支**（1.20 才有"实体方块采样上方"）。`setupAndRender` 也是 `getLightColor(level, be.getBlockPos())`。

### 方块体素存的是什么光

- `BlockBehaviour.getLightBlock(state, level, pos)`：
  ```java
  if (state.isSolidRender(level, pos)) return level.getMaxLightLevel(); // 15
  else return state.propagatesSkylightDown(level, pos) ? 0 : 1;
  ```
- `isSolidRender = canOcclude() ? Block.isShapeFullBlock(getOcclusionShape) : false`。
- `propagatesSkylightDown = !Block.isShapeFullBlock(state.getShape(level,pos)) && fluid为空`。
- **关键：`Properties.noOcclusion()` 只设置 `canOcclude = false`。** 跑步机用了 `.noOcclusion()` → `canOcclude=false` → `isSolidRender=false` → `lightBlock` 不可能是 15。
  - 跑步机 `getShape` 默认 `Shapes.block()`（完整立方体）→ `propagatesSkylightDown=false` → **`getLightBlock = 1`**。
- 光照引擎：
  - `getOpacity(state) = max(1, state.getLightBlock(...))` → 跑步机 opacity = 1。
  - `BlockLightEngine.propagateIncrease`：进入相邻体素的光 = `lightLevel - getOpacity`；只有结果 `> 已存值` 才写入。
  - 跑步机 `useShapeForLightOcclusion=false` → `isEmptyShape=true` → 光照引擎把它当"空形状"，光可以穿入 → 相邻 15 级空气时，跑步机体素应存 **14**。
  - `SkyLightSectionStorage.getLightValue`：体素所在 section 在"顶部 section"之下 → 返回该体素**存储值**；处于/高于顶部 section → 返回 15。
  - `ChunkSkyLightSources.isEdgeOccluded`：`state2.getLightBlock != 0` 即视为遮挡天空 → 跑步机 `lightBlock=1` 会挡住天空（算作该列天空源的最低点）→ 上方无遮挡时，跑步机体素由传播得到 ~14（15-1）。

### 与"方块外观"对比（为什么方块盒子亮而轴黑，才叫 bug）

- 方块本体（block model）的光是**面片光**，取自相邻空气体素（亮）。
- 传动轴（BE visual / renderer）的光取自**方块自身体素**。
- 因此只要"自身体素"比"相邻空气"暗，就会出现：盒子亮、轴黑。

---

## 3. 尚未解决的矛盾（核心）

- **矛盾 A（跑步机应亮）**：按上面推导，暴露在露天/亮处时，跑步机体素 sky≈14、block≈14 → `getLightColor` 明亮 → 传动轴应显示为浅灰色，不应纯黑。
- **矛盾 B（压机先例）**：Create 的 `MechanicalPress` 是全不透明实体方块，`PressVisual.updateLight` 也是 `relight` 到压机自身体素，压机机头却正常受光显示——说明**不透明方块的自身体素在实际游戏里并非全黑**（原因：`useShapeForLightOcclusion=false` → 光能穿入 → 存储值 = 相邻光 − opacity）。这与 A 一致。
- **矛盾 C（宝箱类比）**：箱子形状非满立方 → `isSolidRender=false` → lightBlock=0/1 → 体素也亮。此"宝箱悖论"已由 `noOcclusion → lightBlock=1` 的分析解决：**满立方 + canOcclude=false → lightBlock=1（不是15）→ 体素不黑**。

结论：按源码推演，**室外亮处跑步机传动轴不应黑**。但用户看到黑轴。所以要么：

1. 用户测试环境里跑步机体素确实暗（室内/屋顶/洞穴/被遮挡 → sky 0 + block 0）——此时普通传动轴也会黑，不算 bug；但用户说"原版类似方块"不黑，需要现场对比确认。
2. 我的某一步推导有误（最可疑：sky 的 `getLightValue` 顶部 section 判定、或跑步机并非"该列顶部遮挡块"）。
3. 实例光值正确但被别处覆盖：`rotating.vert` 里 `max(instance.light/256, flw_vertexLight)`——若 **模型烘焙光 `flw_vertexLight` 为 0** 且实例光也为 0 → 黑。**实例光为什么是 0？** `Storage.setup` 只在 visual 创建时调用一次 `updateLight`；若此时跑步机体素光还未算好（刚放置/区块刚加载），读到 0，之后靠 `onLightUpdate` 事件补救。若该事件没触发（例如客户端与服务端光同步时序、或该 section 光已是最新不再有更新事件）→ **一直黑**。

---

## 4. 最可疑根因（按可能性排序）

1. **`updateLight` 初始读到的光为 0，且此后该 section 没有再触发 `onLightUpdate` → 传动轴永远以 `light=0` 渲染**。普通方块（压机）可能因为其 section 光变化频繁/创建时序不同而正常。这是时序性/竞态问题，与方块自身是否不透明无关——这能解释"为什么只有我的方块黑"。
2. **测试场景本身暗**（跑步机在暗处），需要用户确认对比场景。
3. 方块模型里 east/west 用 `andesite_funnel_frame`（透明窗），透过窗看到黑轴；若箱体本身足够亮而轴全黑，观感差异明显。

---

## 5. 待验证 / 下一步

- 读 `Flywheel` 的 `ClientChunkCache.onLightUpdate` 触发条件，确认"放置新方块后该 section 是否必然触发一次 light update"。
- 确认用户测试时跑步机是否露天/受光。
- 若确认是"初始光 0 且不再刷新"，修复方向：
  - **方案1（渲染层，最贴近目标）**：自定义 visual，把 `updateLight` 改为采样更亮的体素，例如 `relight(pos.above(), rotatingModel)`（模拟旧版 `isSolidRender → pos.above()` 行为），只影响轴的亮度。
  - **方案2（方块层）**：让跑步机体素不挡光，使体素光更接近相邻空气。例如覆写 `getLightBlock` 返回 0（→opacity=1）或覆写 `getOcclusionShape` 返回空形状（→lightBlock 更小），副作用：光会更"漏"到下方/周围方块。
  - **方案3（视觉层）**：`updateLight` 里手动用 `FlatLit.relight(LightTexture.pack(15,15))` 之类强制给轴满光（最粗暴，忽略环境）。

> 注：修改代码前需征得用户同意（内存约束 ask-only-before-modifying-code）。

---

## 6. 已实施的修复

- **渲染层方案1**：新增 `CB_TreadmillShaftVisual`，继承 Create 的 `SingleAxisRotatingVisual`，将 `updateLight` 改为采样 `pos.above()` 的光照。
  - 在 `ModBlockEntity.CB_TREADMILL_ENTITY` 注册时改用 `CB_TreadmillShaftVisual::new`。
  - `renderNormally` 改为 `true`，避免 Flywheel 激活时跳过 `CB_TreadmillRenderer` 导致宝可梦不渲染。
- **兜底路径同步修复**：`CB_TreadmillRenderer.renderSafe` 在无 Flywheel 时也改用上方体素光渲染传动轴，避免两条路径表现不一致。

