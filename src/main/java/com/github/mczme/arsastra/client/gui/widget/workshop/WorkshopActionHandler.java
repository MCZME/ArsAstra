package com.github.mczme.arsastra.client.gui.widget.workshop;

public interface WorkshopActionHandler {
    void onFilterChanged();
    void onClearRequest();
    void onInfoToggle();
    void onSettingsToggle();
    void onSaveRequest();
    void onChartTypeChanged(String type);
}
