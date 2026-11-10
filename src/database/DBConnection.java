package database; 

import exception.DatabaseException; 
import java.io.FileInputStream; 
import java.io.IOException; 
import java.sql.Connection; 
import java.sql.DriverManager; 
import java.sql.SQLException; 
import java.util.Properties; 

public class DBConnection { 
    private static final Properties props = new Properties(); 

    static { 
        try (FileInputStream in = new FileInputStream("config.properties")) { 
            props.load(in); 
        } catch (IOException e) { 
            throw new DatabaseException("Cannot read config.properties in the project root", e); 
        } 
    } 

    public static Connection getConnection() throws SQLException { 
        return DriverManager.getConnection( 
            props.getProperty("db.url"), 
            props.getProperty("db.user"), 
            props.getProperty("db.password")); 
    } 
}
