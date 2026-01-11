package com.github.mczme.arsastra.client.gui.logic;

import com.github.mczme.arsastra.core.starchart.engine.AlchemyInput;
import com.github.mczme.arsastra.core.starchart.engine.DeductionResult;
import com.github.mczme.arsastra.network.payload.RequestDeductionPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 推演工坊会话类，负责管理推演的输入（AlchemyInput 序列）和输出（DeductionResult）。
 * 它是工坊 UI 的逻辑核心，负责与服务端同步数据。
 */
public class WorkshopSession {
    private final List<AlchemyInput> inputs = new ArrayList<>();
    private final int MAX_INPUTS = 16;
    
    private DeductionResult deductionResult;
    private Runnable onUpdateListener;
    private Consumer<ItemStack> onFirstItemAddedListener;
    private ResourceLocation currentStarChartId;
    
    // 当前选中的步骤索引 (-1 表示未选中)
    private int selectedIndex = -1;

    public WorkshopSession(ResourceLocation initialStarChartId) {
        this.currentStarChartId = initialStarChartId;
    }

    /**
     * 设置更新监听器，当输入序列或推演结果发生变化时调用。
     */
    public void setOnUpdateListener(Runnable listener) {
        this.onUpdateListener = listener;
    }

    public void setOnFirstItemAddedListener(Consumer<ItemStack> listener) {
        this.onFirstItemAddedListener = listener;
    }

    /**
     * 设置当前推演使用的星图 ID。
     */
    public void setStarChartId(ResourceLocation id) {
        if (id != null && !id.equals(this.currentStarChartId)) {
            this.currentStarChartId = id;
            requestDeduction();
        }
    }

    public ResourceLocation getStarChartId() {
        return currentStarChartId;
    }

    // --- 输入序列管理 ---
    
    /**
     * 设置当前选中的输入步骤索引。
     */
    public void setSelectedIndex(int index) {
        if (index >= -1 && index < inputs.size()) {
            this.selectedIndex = index;
            // 触发 UI 更新以重绘高亮
            if (onUpdateListener != null) {
                onUpdateListener.run();
            }
        }
    }

    /**
     * 获取当前选中的输入步骤索引。
     */
    public int getSelectedIndex() {
        return selectedIndex;
    }

    /**
     * 获取当前选中的输入对象。
     * @return 选中的 AlchemyInput，如果未选中则返回 null。
     */
    public AlchemyInput getSelectedInput() {
        if (selectedIndex >= 0 && selectedIndex < inputs.size()) {
            return inputs.get(selectedIndex);
        }
        return null;
    }

    /**
     * 获取当前的输入序列副本。
     */
    public List<AlchemyInput> getInputs() {
        return new ArrayList<>(inputs);
    }
    
    /**
     * 获取序列中所有的物品堆栈，用于 UI 渲染。
     */
    public List<ItemStack> getInputItems() {
        List<ItemStack> stacks = new ArrayList<>();
        for (AlchemyInput input : inputs) {
            stacks.add(input.stack());
        }
        return stacks;
    }

    /**
     * 在序列末尾添加一个新物品（默认操作）。
     */
    public void addInput(ItemStack stack) {
        if (inputs.size() < MAX_INPUTS && !stack.isEmpty()) {
            boolean wasEmpty = inputs.isEmpty();
            ItemStack copy = stack.copy();
            copy.setCount(1);
            inputs.add(AlchemyInput.of(copy));
            
            if (wasEmpty && onFirstItemAddedListener != null) {
                onFirstItemAddedListener.accept(copy);
            }

            // 自动选中新添加的项
            this.selectedIndex = inputs.size() - 1;
            notifyUpdate();
        }
    }

    /**
     * 替换序列中指定位置的输入。
     */
    public void setInput(int index, ItemStack stack) {
        if (index >= 0 && index < inputs.size()) {
            if (stack.isEmpty()) {
                inputs.remove(index);
                // 修正选中索引
                if (selectedIndex == index) selectedIndex = -1;
                else if (selectedIndex > index) selectedIndex--;
            } else {
                ItemStack copy = stack.copy();
                copy.setCount(1);
                // 替换物品时保留之前的操作参数（如搅拌旋转）或根据需求重置
                inputs.set(index, AlchemyInput.of(copy));
            }
            notifyUpdate();
        }
    }
    
    /**
     * 在序列中的指定位置插入一个新物品。
     */
    public void insertInput(int index, ItemStack stack) {
        if (index >= 0 && index <= inputs.size() && inputs.size() < MAX_INPUTS && !stack.isEmpty()) {
            boolean wasEmpty = inputs.isEmpty();
            ItemStack copy = stack.copy();
            copy.setCount(1);
            inputs.add(index, AlchemyInput.of(copy));
            
            if (wasEmpty && onFirstItemAddedListener != null) {
                onFirstItemAddedListener.accept(copy);
            }

            // 选中新插入的项
            this.selectedIndex = index;
            notifyUpdate();
        }
    }

    /**
     * 移除序列中指定位置的项。
     */
    public void removeInput(int index) {
        if (index >= 0 && index < inputs.size()) {
            inputs.remove(index);
            // 修正选中索引
            if (selectedIndex == index) selectedIndex = -1;
            else if (selectedIndex > index) selectedIndex--;
            notifyUpdate();
        }
    }

    /**
     * 对序列中的某一步进行“搅拌”操作，改变其旋转强度。
     * @param index 序列索引
     * @param rotation 新的旋转角度/强度
     */
    public void stirInput(int index, float rotation) {
        if (index >= 0 && index < inputs.size()) {
            AlchemyInput old = inputs.get(index);
            inputs.set(index, old.withRotation(rotation));
            notifyUpdate();
        }
    }

    /**
     * 清空所有推演输入。
     */
    public void clear() {
        inputs.clear();
        selectedIndex = -1;
        notifyUpdate();
    }

    // --- 输出结果管理 ---

    /**
     * 更新最新的推演结果（通常由网络数据包处理器调用）。
     */
    public void setDeductionResult(DeductionResult result) {
        this.deductionResult = result;
        if (onUpdateListener != null) {
            onUpdateListener.run();
        }
    }

    public DeductionResult getDeductionResult() {
        return deductionResult;
    }

    // --- 内部同步逻辑 ---

    private void notifyUpdate() {
        // 每当输入发生变化，立即向服务端请求新的推演
        requestDeduction();
        // 触发本地监听器以更新 UI（如序列条显示）
        if (onUpdateListener != null) {
            onUpdateListener.run();
        }
    }

    /**
     * 构建并向服务端发送推演请求数据包。
     */
    private void requestDeduction() {
        if (currentStarChartId != null && !inputs.isEmpty()) {
            PacketDistributor.sendToServer(new RequestDeductionPayload(currentStarChartId, new ArrayList<>(inputs)));
        } else {
            // 如果没有星图或输入为空，则重置结果
            this.deductionResult = null;
        }
    }
}