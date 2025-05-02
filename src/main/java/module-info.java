module BalanceGUI {
    requires javafx.controls;
    requires javafx.graphics;
    requires org.mongodb.driver.sync.client;
    requires org.mongodb.bson;
    requires org.mongodb.driver.core;
//    mongodb驱动要用
    requires java.logging;
    requires java.security.sasl;
    exports cn.leafoct;
}