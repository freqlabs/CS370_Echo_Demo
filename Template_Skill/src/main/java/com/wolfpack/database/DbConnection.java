/**
 * Before attempting to use this class, follow the directions on the wiki at
 * https://github.com/370-Alexa-Project/CS370_Echo_Demo/wiki/Using-the-dbConnection-class
 * to make sure you have everything set up properly. Examples of how to use this class
 * are also provided.
 * 
 * This class can be used to simplify the process of connecting to a PostgreSQL database.
 * In order to connect to the database, you should call getCredentials() before calling
 * getRemoteConnection(). Then you can run your queries with runQuery().
 */

package com.wolfpack.database;

import java.util.*;

import java.sql.*;

import java.io.File;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class DbConnection {

	private static String dbName;
	private static String schemaName;
	private static String username;
	private static String password;
	private static String hostName; // The url to the database
	private static String port;
	private static String localPathToSSL;
	private static Connection dbConnection;
	private static boolean connected;

	/**
	 * When the main constructor is used, the object will attempt to get the
	 * user's credentials and then use that to secure a connection to the
	 * database.
	 */
	public DbConnection() {
		connected = false;
	}

	/**
	 * The second constructor allows you to pass the path to the credentials for
	 * the database and then load in those credentials. This replaces calling
	 * the main constructor and then needing to call getCredentials().
	 */
	public DbConnection(String pathToCredentials) {
		this();
		getCredentials(pathToCredentials);
	}

	/**
	 * This method will attempt to get the user's credentials (username,
	 * password, etc) so that they can log into the database. Since this file
	 * will be posted on GitHub (and apparently even when it is running), people
	 * are able to see the source code, but we do not want them to see that kind
	 * of information. Therefore, you must create a file in the resources
	 * folder called DbConnection.xml with the appropriate information to be
	 * able to log in. (See the wiki for information about how to set that up).
	 * 
	 * @param pathToCredentials
	 *            - The path to the .xml file that contains the credentials for
	 *            accessing the database. The path should be relative to
	 *            src/main/resources.
	 * 
	 * @post If there were no errors, dbName, username, password, hostname, and
	 *       port private data members will be initialized.
	 * 
	 * @return true if the function successfully reads the credentials, and
	 *         false if there was any type of error (unable to find the file,
	 *         couldn't find the correct information in the file, etc).
	 */
	public boolean getCredentials(String pathToCredentials) {
		try {
			ClassLoader classLoader = getClass().getClassLoader();
			String credsResourcePath = classLoader.getResource(pathToCredentials).getFile();
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(new File(credsResourcePath));

			// Normalize text representation
			doc.getDocumentElement().normalize();

			// Get the appropriate data from the file.
			dbName = doc.getElementsByTagName("dbName").item(0).getTextContent();
			schemaName = doc.getElementsByTagName("schemaName").item(0).getTextContent();
			username = doc.getElementsByTagName("username").item(0).getTextContent();
			password = doc.getElementsByTagName("password").item(0).getTextContent();
			hostName = doc.getElementsByTagName("hostName").item(0).getTextContent();
			port = doc.getElementsByTagName("port").item(0).getTextContent();
			localPathToSSL = doc.getElementsByTagName("localPathToSSL").item(0).getTextContent();

			// Error catching
		} catch (java.io.FileNotFoundException fnfe) {
			System.out.println("Unable to find or open database credentials file. Try checking the path provided.");
			return false;
		} catch (SAXParseException saxpe) {
			System.out.println("** Parsing error" + ", line " + saxpe.getLineNumber() + ", uri " + saxpe.getSystemId());
			System.out.println(" " + saxpe.getMessage());
			return false;
		} catch (SAXException e) {
			Exception x = e.getException();
			((x == null) ? e : x).printStackTrace();
			return false;

		} catch (Throwable t) {
			t.printStackTrace();
			return false;
		}

		return true;

	}

	/**
	 * This method is what establishes a connection to the PostgreSQL database,
	 * based on the credentials that the class has been given. If there are any
	 * errors while trying to connect, the program will give an error message.
	 * The method should be called before attempting to make any queries to the
	 * database.
	 * 
	 * @pre The getCredentials() method has been called.
	 * 
	 * @post The dbConnection private data member will be initialized with the
	 *       connection.
	 * 
	 * @return true if the database was successfully connected to. Otherwise,
	 *         false if there were any errors.
	 * 
	 */
	public boolean getRemoteConnection() {

		// Create a URL to be able to connect to the database.
		String jdbcUrl = "jdbc:postgresql://" + hostName + ":" + port + "/" + dbName + "?user=" + username
				+ "&password=" + password + "&sslmode=verify-full&sslrootcert=" + localPathToSSL;

		try {
			Class.forName("org.postgresql.Driver");
			dbConnection = DriverManager.getConnection(jdbcUrl);
			dbConnection.setSchema(schemaName);

		// Error handling
		} catch (ClassNotFoundException e1) {
			System.out.println("Driver needed for connecting to the PostgreSQL database.");
			return false;
		} catch (SQLException e2) {
			System.out.println("Unable to establish a connection to the database.");
			return false;
		}

		return true;

	}

	/**
	 * The runQuery() method will attempt to run the SQL query passed to it and
	 * then return a Map object that uses the column names as they keys and
	 * vectors of what was in the columns as the values for the map. All of the
	 * values get stored as Objects in the vectors.
	 * 
	 * As an example, if your query returned this table: movieId | name
	 * ---------------------- 123 | Tron 456 | Harry Potter
	 * 
	 * The resulting map would look like this: { "movieId" : [ 123, 456 ],
	 * "name" : ["Tron", "Harry Potter"] }
	 * 
	 * In the vectors, the items from a row will all share the same index. So in
	 * the example, the first row's information could be grabbed by:
	 * vectorName.get("movieId")[0]; vectorName.get("name")[0];
	 * 
	 * @return Map<String, Vector<Object>> object representing the query's
	 *         results if everything went successfully. Otherwise, null.
	 */
	public Map<String, Vector<Object>> runQuery(String query) {
		Map<String, Vector<Object>> resultMap = null;
		try {
			PreparedStatement st = dbConnection.prepareStatement(
				query,
				ResultSet.TYPE_SCROLL_INSENSITIVE,
				ResultSet.CONCUR_READ_ONLY
			);
			ResultSet rawResults = st.executeQuery();
			ResultSetMetaData rsmd = rawResults.getMetaData();

			// Get the column count and row count to initialize the
			// collections with the needed capacity.

			int columnCount = rsmd.getColumnCount();
			resultMap = new HashMap<String, Vector<Object>>(columnCount);

			if (!rawResults.last())
				return null;
			int rowCount = rawResults.getRow();
			rawResults.beforeFirst();

			// Set up all of the keys (column names) with empty vectors as the values.
			for (int i = 1; i <= columnCount; i++) {
				String name = rsmd.getColumnName(i);
				resultMap.put(name, new Vector<Object>(rowCount));
			}

			// Push the values into the vectors.
			while (rawResults.next()) {
				for (int i = 1; i <= columnCount; i++) {
					resultMap.get(rsmd.getColumnName(i)).addElement(rawResults.getObject(i));
				}
			}
		}
		// Error checking
		catch (SQLException e) {
			System.out.println("Problem with received query. (Perhaps it is an invalid query?)");
			System.out.println(e);
			return null;
		}

		return resultMap;
	}

	/**
	 * This is for debugging purposes to be able to see what a Map<String,
	 * Vector<Object>> object looks like. It will be printed in the format of:
	 * key1: vectorElement[0] [1] vectorElement[1]... key2: vectorElement[0] [2]
	 * vectorElement[2]...
	 * 
	 * Unless all of the strings being printed out are the same length, the
	 * output produced will not line up nicely.
	 * 
	 * If there are more than 50 entries in a column, the method will only print
	 * out the first 50. This is because sometimes if nothing gets printed out
	 * at all if you try to print out too many things at once.
	 */
	public static void printResultMap(Map<String, Vector<Object>> resultMap) {
		if (resultMap != null) {
			System.out.println("Printing the Results as a map:");

			for (Map.Entry<String, Vector<Object>> entry : resultMap.entrySet()) {
				String aKey = entry.getKey();
				System.out.printf("%s:  ", aKey);

				// Print out the first 50 values from the vector
				Vector<Object> values = entry.getValue();
				for (int i = 0; i < Math.min(values.size(), 50); i++) {
					System.out.printf("|[%d]  %s  ", i, values.get(i));
				}
				System.out.print("\n");

			}
		} else {
			System.out.println("Cannot print a null map.");
		}

	}

}
