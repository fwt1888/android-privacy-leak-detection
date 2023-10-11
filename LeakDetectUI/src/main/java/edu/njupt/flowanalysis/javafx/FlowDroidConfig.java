package edu.njupt.flowanalysis.javafx;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

public class FlowDroidConfig {
    private String setting = null;
    private String value = null;

    public FlowDroidConfig(String setting, String value){
        this.setting = setting;
        this.value = value;
    }

    public FlowDroidConfig(){}

    public String getSetting() {
        return setting;
    }

    public String getValue() {
        return value;
    }

    public void setSetting(String setting) {
        this.setting = setting;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public static void showConfig(VBox vBox){
        TableView table = new TableView();
        TableColumn settingCol = new TableColumn("设置");
        TableColumn valueCol = new TableColumn("值");
        table.getColumns().addAll(settingCol, valueCol);

        settingCol.setCellValueFactory(new PropertyValueFactory<FlowDroidConfig, String>("setting"));
        valueCol.setCellValueFactory(new PropertyValueFactory<FlowDroidConfig, String>("value"));
        ObservableList<FlowDroidConfig> data = FXCollections.observableArrayList(
                new FlowDroidConfig("访问路径长度","5"),
                new FlowDroidConfig("符号访问路径","启用"),
                new FlowDroidConfig("路径不可知结果","启用"),
                new FlowDroidConfig("数据类型传播","启用"),
                new FlowDroidConfig("隐式流跟踪","禁用"),
                new FlowDroidConfig("静态字段跟踪","启用"),
                new FlowDroidConfig("异常流跟踪","启用"),
                new FlowDroidConfig("数组大小污染","禁用"),
                new FlowDroidConfig("别名算法","流敏感"),
                new FlowDroidConfig("流敏感别名分析","启用"),
                new FlowDroidConfig("Callgraph算法","SPARK"),
                new FlowDroidConfig("代码优化","Constant Prop."),
                new FlowDroidConfig("Android回调","启用"),
                new FlowDroidConfig("回调源","启用"),
                new FlowDroidConfig("回调分析器","Iterative"),
                new FlowDroidConfig("布局匹配","Sensitive Only")
        );
        table.setItems(data);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        vBox.getChildren().add(table);
    }
}
