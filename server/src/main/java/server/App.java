package server;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// import javax.swing.JFrame;
// import javax.swing.UIManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import Parser.JsonParser;
import database.DatabaseManager;
import database.PredefinedSQLCode;
import database.QueriesManager;
import database.QueryBuilder;
import database.PredefinedSQLCode.Colonne;
import database.PredefinedSQLCode.Tabelle;
import utility.AsciiArtGenerator;
import utility.AsciiArtGenerator.ASCII_STYLE;
import utility.OS_utility;


/**
 * The main class representing the server application.
 * This class initializes and manages the server, database connection, and user interface.
 * It provides methods for starting and stopping the server, setting up the database connection,
 * loading and saving settings, and performing various operations related to the application.
 *
 * @author Your Name
 * @version 1.0
 * @since 2023-12-28
 */
public class App //extends JFrame
{
    public final static String WORKING_DIRECTORY  = OS_utility.formatPath(System.getProperty("user.dir"));
    public final static String SETTINGS_DIRECTORY = OS_utility.formatPath(WORKING_DIRECTORY + "/serverSettings");
    public final static String FILE_SETTINGS_PATH = OS_utility.formatPath(SETTINGS_DIRECTORY + "/settings.json");
    public final static String FILE_POLICY_PATH = OS_utility.formatPath(SETTINGS_DIRECTORY + "/security.policy");

    private static App instance;

    private static enum JsonDataName {

        SERVER_PORT("Port_server"    , 8090),
        DATABASE_PORT("Port_database", 5432),
        DATABASE_IP("IP_database"    , "127.0.0.1"),
        DATABASE_PW("Pw_database"    , "admin"),
        DATABASE_USER("User_database", "postgres"),
        DATABASE_NAME("Name_database", "EmotionalSongs"),
        AUTO_START("Auto_Start"      , false);

        private String s;
        private Object defoultValue;

        private JsonDataName(String s, Object defoultValue) {
            this.s = s;
            this.defoultValue = defoultValue;
        }

        @Override
        public String toString() {
            return this.s;
        }
    }

    private int port;
    private int DB_port;
    private String DB_IP;
    private String DB_password;
    private String DB_user;
    private String DB_name;
    private boolean autoRun = false;
    
    public DatabaseManager database = null;
    private boolean databaseConnected = false;
    private ComunicationManager server = null;
    private Terminal terminal;

    /**
     * Gets the singleton instance of the App class.
     *
     * @return The App instance.
     */
    public static App getInstance() {
        return instance;
    }

    /**
     * The main method that starts the server application.
     *
     * @param args Command line arguments.
     * @throws Exception If an error occurs during initialization.
     */
    public static void main( String[] args ) throws Exception 
    {
        //UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        if(OS_utility.isWindows()) {
            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor(); 
        }
        else {
            new ProcessBuilder("clear").inheritIO().start().waitFor();
        }

        System.getProperties().setProperty("java.security.policy", FILE_POLICY_PATH);
        new App(args);
    }

    /**
     * The constructor for the App class.
     *
     * @param args Command line arguments.
     * @throws InterruptedException If the thread is interrupted.
     * @throws IOException If an I/O error occurs.
     * @throws ClassNotFoundException If the class is not found during initialization.
     * @throws SQLException If a SQL error occurs.
     */
    private App(String[] args) throws InterruptedException, IOException, ClassNotFoundException, SQLException 
    {
        super();
        App.instance = this;
        this.terminal = Terminal.getInstance();
        Class.forName("org.postgresql.Driver");

        loadSettings();
        setDatabaseConnection();

        
        terminal.printSeparator();
        this.terminal.start();
    }

    /**
     * Exits the program gracefully by stopping the server and saving settings.
     */
    public void exit() {
        StopServer();
        saveSettings();
        System.exit(0);
    }

    public boolean getServerAutoStart() {
        return autoRun;
    }

    /**
     * Sets up the database connection based on the configured parameters.
     */
    protected void setDatabaseConnection() 
    {
        database = DatabaseManager.getInstance();
        database.setConnectionParametre(this.DB_name, this.DB_IP, this.DB_port, this.DB_user, DB_password);

        
        terminal.printInfoln("Database connection attempt on URL: " + Terminal.Color.CYAN_BOLD_BRIGHT + database.getURL() + Terminal.Color.RESET);

        try {
            database.connect();
            if(database.testConnection() && database.connect()) {
                terminal.printSuccesln("Database found and connection established");

                if(this.DB_IP.toLowerCase().equals("localhost") || this.DB_IP.startsWith("127.")) {
                    terminal.printInfoln("Database IP: " + ComunicationManager.getMachineIP());
                }

                databaseConnected = database.isConnected();
            }
            else {
                terminal.printErrorln(Terminal.Color.RED_BOLD_BRIGHT + "Database not responding" + Terminal.Color.RESET);
                databaseConnected = database.isConnected();
            } 
        } 
        catch (Exception e) {
            terminal.printErrorln("Connection failed. Error: " + Terminal.Color.RED_BOLD_BRIGHT + e.getMessage() + Terminal.Color.RESET);   
            databaseConnected = false;
        }
    }

     /**
     * Checks if the database is currently connected.
     *
     * @return true if connected, false otherwise.
     */
    public boolean isDatabaseConnected() {
        return database.isConnected();
    }

    /**
     * Loads settings from the settings file into the program.
     *
     * @throws IOException If an I/O error occurs.
     */
    private boolean checkSettings() 
    {
        Terminal terminal = Terminal.getInstance();
        Path folder = Paths.get(App.SETTINGS_DIRECTORY);
        Path file = Paths.get(App.FILE_SETTINGS_PATH);

        try {
            if(!Files.exists(folder)) {
                Files.createDirectory(folder);
                terminal.printErrorln("settings file not found");
                initializeSettings();
                return false;
            } 
            if(!Files.exists(file) || Files.size(file) <= 12) {
                terminal.printErrorln("settings file not found");
                initializeSettings();
                return false;
            }  
        } 
        catch (Exception e) {
            e.printStackTrace();
            terminal.printErrorln(e.toString());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            System.exit(0);
        }

        return true;
    }

   /**
     * Loads settings from the settings file into the program.
     *
     * @throws IOException If an I/O error occurs.
     */
    protected void loadSettings() throws IOException {

        Terminal terminal = Terminal.getInstance();
        terminal.printInfoln("Loading settings");

        if(!checkSettings()) {
            terminal.printSuccesln("Loading completed");
            return;
        }

        terminal.printInfoln("File " + App.FILE_SETTINGS_PATH + " found");
            

        JsonNode node = JsonParser.readJsonFile(FILE_SETTINGS_PATH);

        this.port = node.get(JsonDataName.SERVER_PORT.toString()).asInt();
        this.DB_port = node.get(JsonDataName.DATABASE_PORT.toString()).asInt();
        this.DB_IP = node.get(JsonDataName.DATABASE_IP.toString()).asText();
        this.DB_password = node.get(JsonDataName.DATABASE_PW.toString()).asText();
        this.DB_user = node.get(JsonDataName.DATABASE_USER.toString()).asText();
        this.DB_name = node.get(JsonDataName.DATABASE_NAME.toString()).asText();
        this.autoRun = node.get(JsonDataName.AUTO_START.toString()).asBoolean();

        terminal.printSuccesln("Loading completed");

    }

    /**
     * Initializes the setting files with default parameters.
     *
     * @throws IOException If an I/O error occurs.
     */
    private void initializeSettings() throws IOException 
    {
        Terminal terminal = Terminal.getInstance();
        terminal.printInfoln("setup default settings");

        this.DB_IP = (String) JsonDataName.DATABASE_IP.defoultValue;
        this.DB_port = (Integer) JsonDataName.DATABASE_PORT.defoultValue;
        this.DB_password = (String) JsonDataName.DATABASE_PW.defoultValue;
        this.DB_user = (String) JsonDataName.DATABASE_USER.defoultValue;
        this.DB_name = (String) JsonDataName.DATABASE_NAME.defoultValue;
        this.port = (Integer) JsonDataName.SERVER_PORT.defoultValue;
        this.autoRun = (Boolean) JsonDataName.AUTO_START.defoultValue;

        //JsonParser.writeJsonFile(FILE_SETTINGS_PATH, data);
        //loadSettings();
        saveSettings();
        terminal.printInfoln("settings saved");
    }

    /**
     * Saves the program parameters in the settings file.
     */
    protected void saveSettings() {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode data = objectMapper.createObjectNode();

        ((ObjectNode) data).put(JsonDataName.SERVER_PORT.toString()  , this.port);
        ((ObjectNode) data).put(JsonDataName.DATABASE_IP.toString()  , this.DB_IP);
        ((ObjectNode) data).put(JsonDataName.DATABASE_PORT.toString(), this.DB_port);
        ((ObjectNode) data).put(JsonDataName.DATABASE_PW.toString()  , this.DB_password);
        ((ObjectNode) data).put(JsonDataName.DATABASE_USER.toString(), this.DB_user);
        ((ObjectNode) data).put(JsonDataName.DATABASE_NAME.toString(), this.DB_name);
        ((ObjectNode) data).put(JsonDataName.AUTO_START.toString()   , this.autoRun);

        JsonParser.writeJsonFile(FILE_SETTINGS_PATH, data);
    }

     /**
     * Starts the server.
     *
     * @throws RemoteException If a remote error occurs.
     */
    public void runServer() throws RemoteException {

        if(server == null) {
           server = new ComunicationManager(this.port);
           server.start();
        }
    }

    /**
     * Terminates the server.
     */
    public void StopServer() {
        
        if(server != null && server.isAlive()) {
            server.terminate();
            
            while(server.isAlive());
            server = null;
        }
        else if(server != null) {
            server = null;
        } 
    } 
    
    /**
     * Edits the size of a database column.
     *
     * @param colum The column to be edited.
     * @throws SQLException If a SQL error occurs.
     */
    public void editColumSize(Colonne colum) throws SQLException  {
        DatabaseManager db = DatabaseManager.getInstance();
        Terminal terminal = Terminal.getInstance();
        terminal.println("");

        for (Tabelle table : PredefinedSQLCode.Tabelle.values())
            for (Colonne coll : PredefinedSQLCode.tablesAttributes.get(table)) 
                
                if(coll == colum)
                {
                    String query = QueryBuilder.editColumSize(table, coll);
                    terminal.printQueryln(query);
                    database.submitQuery(query);
                }
    }


    /**
     * Performs integrity testing on the database.
     *
     * @throws IOException If an I/O error occurs.
     * @throws SQLException If a SQL error occurs.
     */
    public void DatabaseIntegrityTest() throws IOException, SQLException 
    {
        DatabaseManager db = DatabaseManager.getInstance();
        HashMap<String, Boolean> existingColumns = new HashMap<>();
        HashMap<Tabelle, ArrayList<Colonne>> missingColumns = new HashMap<>();
        ResultSet resultSet = null;


        int maxNameLenght = 0;
        for (Tabelle table : PredefinedSQLCode.Tabelle.values())
            for (Colonne coll : PredefinedSQLCode.tablesAttributes.get(table)) 
                if(coll.getName().length() > maxNameLenght) maxNameLenght = coll.getName().length();

        
        for (Tabelle table : PredefinedSQLCode.Tabelle.values()) 
        {
            terminal.printInfoln("---------------------------------------------------------------------------------------------------");
            terminal.printInfoln("Testing table \"" + Terminal.Color.CYAN_BOLD_BRIGHT + table + Terminal.Color.RESET + "\":");
            Colonne[] colls = PredefinedSQLCode.tablesAttributes.get(table);
            
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            try {
                resultSet = db.getConnection().getMetaData().getColumns(null, null, table.toString().toLowerCase(), null);
                
                //ottengo le colonne che ho nel database
                existingColumns.clear();

                while (resultSet.next()) {
                    String columnName = resultSet.getString("COLUMN_NAME");
                    existingColumns.put(columnName.toLowerCase(), true);
                }


                //verifico che sono le stesse
                for (Colonne coll : colls) 
                {
                    terminal.printInfo("check for column \"" + Terminal.Color.CYAN_BOLD_BRIGHT + coll.getName() + Terminal.Color.RESET + "\"");
                    
                    for(int i = coll.getName().length(); i < maxNameLenght; i++) terminal.print(" ");
                        terminal.print(" ");
                    terminal.print(": ");
                   
                   
                    if(existingColumns.get(coll.getName().toLowerCase()) == null) {
                        terminal.println(Terminal.Color.RED_BOLD_BRIGHT + "Column \"" + coll.getName() + "\" Not Found"  + Terminal.Color.RESET);
                        //terminal.print_ln(Terminal.Color.RED_BOLD_BRIGHT + "not found"  + Terminal.Color.RESET);
                        if(missingColumns.get(table) == null)
                            missingColumns.put(table,  new ArrayList<Colonne>());
                        
                        missingColumns.get(table).add(coll);
                    }
                    else {
                        terminal.println(Terminal.Color.GREEN_BOLD_BRIGHT + "Column \"" + coll.getName() + "\" Found"  + Terminal.Color.RESET);
                        //terminal.print_ln(Terminal.Color.GREEN_BOLD_BRIGHT + "found" + Terminal.Color.RESET);

                        
                    } 
                }

            } catch (SQLException e) {
                e.printStackTrace();
                return;
            }
            finally {
                try {
                    if (resultSet != null) resultSet.close();
                    //if (connection != null) connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }


        if(missingColumns.size() > 0 && terminal.askYesNo("\nadd missing colums ?")) 
        {
            Loader loader = Loader.getInstance();

            for(Tabelle tabella : missingColumns.keySet()) {
                for(Colonne colonna : missingColumns.get(tabella)) {
                    loader.addColum(tabella, colonna);
                } 
            }
        }

        //editColumSize(PredefinedSQLCode.Colonne.ID);
    }
    
}
