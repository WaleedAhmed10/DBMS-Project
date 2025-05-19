package com.example.oopfrontproject;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Main extends Application
{
    private static Connection connection;
    private static final String DB_NAME = "space_saver.db";
    private static final String SQL_SETUP_FILE = "setup.sql"; // Optional setup file

    @Override
    public void start(Stage primaryStage)
    {
        try {
            initDatabase();
            loadLoginScene(primaryStage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initDatabase() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME);
        System.out.println("Database '" + DB_NAME + "' connected successfully.");
    }

    private void loadLoginScene(Stage primaryStage) throws IOException
    {
        Parent root = FXMLLoader.load(getClass().getResource("login.fxml"));
        Scene scene = new Scene(root);
        primaryStage.setTitle("Parking System Login");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void executeSqlScript(String filePath) throws IOException, SQLException
    {
        String sql = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("Executed SQL script successfully.");
        }
    }

    public static Connection getConnection()
    {
        return connection;
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
