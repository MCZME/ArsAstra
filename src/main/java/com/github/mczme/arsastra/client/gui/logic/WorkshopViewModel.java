package com.github.mczme.arsastra.client.gui.logic;

import com.github.mczme.arsastra.network.payload.RequestDeductionPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WorkshopViewModel {
    private final List<ItemStack> sequence = new ArrayList<>();
    private final int MAX_SEQUENCE_SIZE = 16;
    private Runnable onUpdate;
    
    // Prediction State
    private List<Vector2f> predictionPath = Collections.emptyList();
    private float stability = 1.0f;

    public WorkshopViewModel() {
    }

    public void setOnUpdateListener(Runnable onUpdate) {
        this.onUpdate = onUpdate;
    }

    public List<ItemStack> getSequence() {
        return new ArrayList<>(sequence); // Return copy
    }

    public void addToSequence(ItemStack stack) {
        if (sequence.size() < MAX_SEQUENCE_SIZE && !stack.isEmpty()) {
            ItemStack copy = stack.copy();
            copy.setCount(1);
            sequence.add(copy);
            notifyUpdate();
        }
    }

    public void setSequence(int index, ItemStack stack) {
        if (index >= 0 && index < sequence.size()) {
            if (stack.isEmpty()) {
                sequence.remove(index);
            } else {
                ItemStack copy = stack.copy();
                copy.setCount(1);
                sequence.set(index, copy);
            }
            notifyUpdate();
        }
    }

    public void removeFromSequence(int index) {
        if (index >= 0 && index < sequence.size()) {
            sequence.remove(index);
            notifyUpdate();
        }
    }
    
    public void insertToSequence(int index, ItemStack stack) {
        if (index >= 0 && index <= sequence.size() && sequence.size() < MAX_SEQUENCE_SIZE && !stack.isEmpty()) {
            ItemStack copy = stack.copy();
            copy.setCount(1);
            sequence.add(index, copy);
            notifyUpdate();
        }
    }
    
    public void clear() {
        sequence.clear();
        notifyUpdate();
    }
    
    public void updatePrediction(List<Vector2f> path, float stability) {
        this.predictionPath = path;
        this.stability = stability;
        if (onUpdate != null) {
            onUpdate.run();
        }
    }
    
    public List<Vector2f> getPredictionPath() {
        return predictionPath;
    }
    
    public float getStability() {
        return stability;
    }

    private void notifyUpdate() {
        requestPrediction();
        // Listener is called when result comes back or immediately? 
        // Usually immediately for UI update, but prediction is async.
        // We trigger UI update here for sequence change.
        if (onUpdate != null) {
            onUpdate.run();
        }
    }

    private void requestPrediction() {
        // PacketDistributor.sendToServer(new RequestDeductionPayload(new ArrayList<>(sequence)));
        
        // 临时模拟数据，用于 UI 测试，无需服务端介入
        List<Vector2f> dummyPath = new ArrayList<>();
        dummyPath.add(new Vector2f(0, 0)); // 起点
        
        // 简单的随机游走模拟
        float x = 0;
        float y = 0;
        for (int i = 0; i < sequence.size(); i++) {
            x += (float) (Math.random() * 20 - 10);
            y += (float) (Math.random() * 20 - 10);
            dummyPath.add(new Vector2f(x, y));
        }
        
        // 模拟稳定性
        float dummyStability = Math.max(0.0f, 1.0f - (sequence.size() * 0.1f));
        
        updatePrediction(dummyPath, dummyStability);
    }
}
