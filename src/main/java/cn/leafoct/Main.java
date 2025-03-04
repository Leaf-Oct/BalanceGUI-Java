package cn.leafoct;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.bson.Document;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;


public class Main extends Application {

    public Scene main_scene;
    public ChoiceBox<String> year_choice_box = new ChoiceBox<>(), month_choice_box = new ChoiceBox<>(), day_choice_box = new ChoiceBox<>();
    public TextField description_field = new TextField();
    public CheckBox is_expense_checkbox = new CheckBox();
    public TextField amount_field = new TextField();
    public Button submit_button = new Button("提交");
    public TextArea log_area = new TextArea();


    //    我赌这个破程序用不到2030年
    private String[] years = new String[]{"2020", "2021", "2022", "2023", "2024", "2025", "2026", "2027", "2028", "2029", "2030"};
    private String[] days = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31"};

    public double balance, temp_balance;
    public static final String CONFIG_FILE = "config";
    private String config_file;
    private Mongo mongo = new Mongo();

    private String result;
    @Override
    public void init() throws Exception {
        super.init();
        initScene();
//        设置三个下拉列表框的值为今天
        var today = LocalDate.now();
        year_choice_box.setValue(String.valueOf(today.getYear()));
        month_choice_box.setValue(String.valueOf(today.getMonthValue()));
        day_choice_box.setValue(String.valueOf(today.getDayOfMonth()));
        initConfig();
        new Thread(getMongoConnectionTask()).start();

    }

    private Task<Void> getMongoConnectionTask() {
        var connect_mongodb_task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                var result = mongo.connect();
                if (!result) {
                    throw new Exception();
                }
                return null;
            }
        };
        connect_mongodb_task.setOnSucceeded(e -> {
            log_area.appendText("Mongodb连接成功\n");
            log_area.appendText("当前余额"+balance+"\n");
        });
        connect_mongodb_task.setOnFailed(e -> {
            log_area.appendText("Mongodb连接失败\n");
        });
        return connect_mongodb_task;
    }

    private Task<Void> getMongoCommitTask(String collection_name, Document transaction) {
        var commit_task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                result=mongo.insert(collection_name, transaction);
                return null;
            }
        };
        commit_task.setOnSucceeded(e -> {
            log_area.appendText(result);
            log_area.appendText("\n");
            submit_button.setDisable(false);
            balance=temp_balance;
            description_field.clear();
            amount_field.clear();
        });
        commit_task.setOnFailed(e -> {
            log_area.appendText(result);
            log_area.appendText("\n");
            submit_button.setDisable(false);
        });
        return commit_task;
    }

    @Override
    public void start(Stage primaryStage) {
        submit_button.setOnAction(event -> {
//            log_area.clear();
            var y = year_choice_box.getValue();
            var m = month_choice_box.getValue();
            var d = day_choice_box.getValue();
            var description = description_field.getText();
            var is_expense = is_expense_checkbox.isSelected();
            double amount;
            try {
                amount = Double.parseDouble(amount_field.getText());
            } catch (NumberFormatException e) {
                e.printStackTrace();
                log_area.appendText("输入的金额不合法！\n");
                return;
            }
            temp_balance = Math.round((is_expense ? balance - amount : balance + amount)*100.0)/100.0;
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("确认提交？");          // 设置标题
            alert.setHeaderText(null);      // 隐藏副标题（可选）
            alert.setContentText("日期 " + y + "-" + m + "-" + d + "\n" + description + "\n金额 " + amount + "\n余额 " + temp_balance);
            var result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                submit_button.setDisable(true);
                var document = new Document("date", y + "-" + m + "-" + d).append("description", description).append("isexpense", is_expense).append("number", amount).append("balance", temp_balance);
                new Thread(getMongoCommitTask("a"+y+"_"+m, document)).start();
            }

        });

        primaryStage.setTitle("BalanceGUI");
        primaryStage.setScene(main_scene);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        mongo.close();
        updateConfigBalance();
    }

    private void updateConfigBalance(){
        List<String> lines = null;
        try {
            lines = Files.readAllLines(Paths.get(config_file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (lines.size() < 5) {
            return;
        }
        lines.set(4, String.valueOf(balance));
        try {
            Files.write(Paths.get(config_file), lines);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void initScene() {
        VBox root = new VBox();
        root.setSpacing(10);
        root.setPadding(new Insets(10, 10, 10, 10));
        // 第一行：三个下拉列表
        HBox yearMonthDayBox = new HBox();
        yearMonthDayBox.setSpacing(5);
//        设置选项
        year_choice_box.getItems().addAll(years);
        month_choice_box.getItems().addAll(Arrays.copyOf(days, 12));
        day_choice_box.getItems().addAll(days);
//        设置宽度
        year_choice_box.setMaxWidth(Double.MAX_VALUE);
        month_choice_box.setMaxWidth(Double.MAX_VALUE);
        day_choice_box.setMaxWidth(Double.MAX_VALUE);
        yearMonthDayBox.getChildren().addAll(year_choice_box, month_choice_box, day_choice_box);
//        设置宽度三分天下
        HBox.setHgrow(year_choice_box, Priority.ALWAYS);
        HBox.setHgrow(month_choice_box, Priority.ALWAYS);
        HBox.setHgrow(day_choice_box, Priority.ALWAYS);
        // 第二行：单行文本栏
        description_field.setPromptText("描述");
        // 第三行：复选框和说明
        HBox checkBoxBox = new HBox();
        checkBoxBox.setSpacing(5);
        is_expense_checkbox.setSelected(true);
        Label expenseLabel = new Label("支出");
        checkBoxBox.getChildren().addAll(is_expense_checkbox, expenseLabel);
        // 第四行：一半文本框，一半字符串
        HBox amountBox = new HBox();
        amountBox.setSpacing(5);
        amount_field.setPromptText("仅限数字且为正数");
        Label amountLabel = new Label("金额");
        amountBox.getChildren().addAll(amount_field, amountLabel);
        // 第五行：提交按钮
        submit_button.setMaxWidth(Double.MAX_VALUE); // 让按钮占据整个宽度
        // 剩余空间：多行文本框，只读用于输出日志
        log_area.setEditable(false);
        log_area.setPrefHeight(200); // 设定一个初始高度
        // 添加所有组件到root容器中
        root.getChildren().addAll(yearMonthDayBox, description_field, checkBoxBox, amountBox, submit_button, log_area);
        main_scene = new Scene(root, 400, 400);
    }

    private void initConfig() {
        List<String> unnamedParams = getParameters().getUnnamed();
        // 判断是否有参数
        if (!unnamedParams.isEmpty()) {
            config_file = unnamedParams.get(0);
            try {
                if (!Files.exists(Paths.get(config_file))) {
                    System.err.println("配置文件不存在" + config_file);
                    Platform.runLater(() -> {
                        log_area.appendText("配置文件不存在\n");
                    });
                    config_file = CONFIG_FILE;
                }
            } catch (InvalidPathException e) {
                e.printStackTrace();
                System.err.println("参数文件名不合法 " + config_file);
                Platform.runLater(() -> {
                    log_area.appendText("参数文件名不合法\n");
                });
                config_file = CONFIG_FILE;
            }
        } else {
            System.out.println("无参数，使用默认 config 文件");
            config_file = CONFIG_FILE;
        }
        // 读取文件内容
        try {
            var config = Files.readString(Paths.get(config_file));
            var lines = config.split("\\R");
            mongo.address = lines[0];
            mongo.user = lines[1];
            mongo.password = lines[2];
            mongo.login_db = lines[3];
            balance = Double.parseDouble(lines[4]);
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                log_area.appendText("读取配置文件错误\n");
            });
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}