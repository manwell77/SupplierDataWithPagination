package dev.ariba;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.net.InetSocketAddress;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import com.mysql.cj.jdbc.MysqlDataSource;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

public class openapi {

	public static void main(String[] args) {

		try {
			
			String proxy_host = "";
			String proxy_port = "";
			String csv_path = "";
			String csv_name = "";
			String vendor = "";
			OkHttpClient client;
			String mysql_host = "";
			Integer mysql_port = 3306;
			String mysql_user = "";
			String mysql_password = "";
			String mysql_table = "";
			String key_secret = "";
			Integer log_level = 0;

			for (int n=0;n<args.length;n++) {
			    if (args[n].equals("-ph")) { proxy_host = args[n+1]; } // proxy host (optional - default direct)
			    if (args[n].equals("-pp")) { proxy_port = args[n+1]; }  // proxy port number (optional - default direct)
			    if (args[n].equals("-cp")) { csv_path = args[n+1]; } // csv path (optional - default user download path)
			    if (args[n].equals("-cn")) { csv_name = args[n+1]; } // csv file name (optional - default suppliers.csv)
			    if (args[n].equals("-v"))  { vendor = args[n+1]; } // specific vendor ariba code (optional - all if not specified)
			    if (args[n].equals("-dh")) { mysql_host = args[n+1]; } // mysql host name (optional - default localhost)
			    if (args[n].equals("-dp")) { mysql_port = Integer.parseInt(args[n+1]); } // mysql port number (optional - default 3306)
			    if (args[n].equals("-du")) { mysql_user = args[n+1]; } // mysql user (optional - default root)
			    if (args[n].equals("-dk")) { mysql_password = args[n+1]; } // mysql passowrd (optional - default techedge)
			    if (args[n].equals("-dt")) { mysql_table = args[n+1]; } // mysql table name (optional - default default.suppliers)
			    if (args[n].equals("-k"))  { key_secret = args[n+1]; } // ariba api secret key base64
			    if (args[n].equals("-l"))  { log_level = Integer.parseInt(args[n+1]); } // log level (optional - default 0, max 2)
			}
			
			// proxy
			if (proxy_host!="")
				{	Proxy proxyTechedge = new Proxy(Proxy.Type.HTTP,new InetSocketAddress(proxy_host,Integer.parseInt(proxy_port))); 
					// build client
				    OkHttpClient.Builder builder = new OkHttpClient.Builder().proxy(proxyTechedge);
				    client = builder.build();	
				    // log
				    if (log_level>=1) { System.out.println("proxy set to: "+proxy_host+":"+proxy_port); } }
			else
				{ client = new OkHttpClient(); 
			      if (log_level>=1) { System.out.println("proxy set to: direct connection"); }
				}
			
			// request token body prepare			
			MediaType tokenBodyMedia = MediaType.parse("application/x-www-form-urlencoded");
			RequestBody tokenBody = RequestBody.create(tokenBodyMedia, "grant_type=openapi_2lo");
			
			// request token
			Request tokenRequest = new Request.Builder().url("https://api-eu.ariba.com/v2/oauth/token")
					.post(tokenBody)
					.addHeader("Content-Type","application/x-www-form-urlencoded")
					.addHeader("Authorization","Basic "+key_secret)
					.addHeader("cache-control", "no-cache")
					.build();
			
			// call
			Response tokenResponse = client.newCall(tokenRequest).execute();
			
			// get json
			JsonObject tokenJson = Jsoner.deserialize(tokenResponse.body().string(),(JsonObject) null);
		    
			// get token
			String access_token = tokenJson.getString(Jsoner.mintJsonKey("access_token",null));
			String refresh_token = tokenJson.getString(Jsoner.mintJsonKey("refresh_token",null));
			
			// print
			if (log_level>=1) {
				System.out.println("access token received: " + access_token);
				System.out.println("refresh token received: " + refresh_token);
			}
			
			// prepare supplier body
			RequestBody supplierBody;
			MediaType supplierBodyMedia = MediaType.parse("application/json");
			if (vendor!="")
			 { supplierBody = RequestBody.create(supplierBodyMedia, "{\"smVendorIds\":[\""+vendor+"\"],\"outputFormat\": \"CSV\",\"withQuestionnaire\": true}"); }
			else
			 { supplierBody = RequestBody.create(supplierBodyMedia, "{\"outputFormat\": \"CSV\",\"withQuestionnaire\": true}"); }
		    
			// prepare supplier request
			Request supplierRequest = new Request.Builder()
					 .url("https://eu.openapi.ariba.com/api/supplierdatapagination/v2/prod/smv1/vendors/")
					 .post(supplierBody)
					 .addHeader("apiKey", "lZ94gPnGpzpPHilvcmC2SivEVP3fu4tz")
					 .addHeader("Authorization", "Bearer" + " " + access_token)
					 .addHeader("Content-Type", "application/json")
					 .addHeader("Accept", "application/json")
					 .addHeader("cache-control", "no-cache")
					 .build();
			
			// call
		    Response supplierResponse = client.newCall(supplierRequest).execute();
    
		    // CSV transformation & parsing
		    String supplierCSV = supplierResponse.body().string().replaceAll(",",";");
		    
		    // csv path/name
		    if (csv_path=="") { csv_path = System.getProperty("user.home")+"\\Downloads\\"; }
		    if (csv_name=="") { csv_name = "suppliers.csv"; }
		    
		    // log
		    if (log_level>=1) { System.out.println("csv will be saved to: "+csv_path+csv_name); }
		    
		    // Write to file system
		    PrintWriter fileWriter = new PrintWriter(csv_path+csv_name, "UTF-8");
		    fileWriter.println(supplierCSV);
		    fileWriter.close();		    
		    
		    // file created
		    if (log_level>=1) { System.out.println("supplier.csv saved to path: "+csv_path+csv_name); }
		    
		    // parse file
		    String [] nextLine;
		    CSVParser csvParser = new CSVParserBuilder().withSeparator(';').build();
		    Path csvPath = Paths.get(csv_path+csv_name);
		    BufferedReader csvBufferedReader = Files.newBufferedReader(csvPath,StandardCharsets.UTF_8);
		    CSVReader csvReader = new CSVReaderBuilder(csvBufferedReader).withCSVParser(csvParser).build(); 

		    // datetime prepare
		    java.util.Date date = new java.util.Date();
		    java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.ITALY);
		    String dateTime = dateFormat.format(date);
		    
		    // mysql
		    if (mysql_host=="") { mysql_host="localhost"; }
		    if (mysql_user=="") { mysql_user="root"; }
		    if (mysql_password=="") { mysql_password="techedge"; }
		    if (mysql_table=="") { mysql_table="default.suppliers"; }

		    // log
		    if (log_level>=1) { System.out.println("MySQL db set to: "+mysql_user+":"+mysql_password+"@"+mysql_host+":"+mysql_port); }
		    
		    // db connection
		    MysqlDataSource dataSource = new MysqlDataSource();
		    dataSource.setUser(mysql_user);
		    dataSource.setPassword(mysql_password);
		    dataSource.setServerName(mysql_host);
		    dataSource.setPort(mysql_port);
		    dataSource.setServerTimezone("Europe/Rome");
		    Connection dbConnection = dataSource.getConnection();
		    
		    // init counters
		    int i = 0; int j = 0; int r = 0;
		    
		    // parse csv and build insertion statements
		    while ((nextLine = csvReader.readNext()) != null) {
		    	// check
		    	if ((i==0)||(nextLine.length!=19)) { i++; continue; }	    	
		    	// build statement
		    	Statement insertStatement = dbConnection.createStatement();
		    	// build instruction
		    	String instruction = "insert into " + mysql_table + " values ('" + dateTime     + "','"  /* timestamp */
																				                + nextLine[2]  + "','"  /* sm vendor id */
																				                + i            + "','"  /* counter */
																				                + nextLine[1]  + "','"  /* erp vendor id */
																				                + nextLine[3]  + "','"  /* an id */
																				                + nextLine[0]  + "','"  /* supplier name */
																				                + nextLine[4]  + "','"  /* registration status */
																				                + nextLine[5]  + "','"  /* integrated to erp */
																				                + nextLine[6]  + "','"  /* address line 1 */
																				                + nextLine[7]  + "','"  /* address line 2 */
																				                + nextLine[8]  + "','"  /* address line 3 */
																				                + nextLine[9]  + "','"  /* city */
																				                + nextLine[10] + "','"  /* country */
																				                + nextLine[11] + "','"  /* region */
																				                + nextLine[12] + "','"  /* po box */
																				                + nextLine[13] + "','"  /* postal code */
																				                + nextLine[14] + "','"  /* qualification status */
																				                + nextLine[15] + "','"  /* preferred status */
																				                + nextLine[16] + "','"  /* category */
																				                + nextLine[17] + "','"  /* region */
																				                + nextLine[18] + "')";  /* business unit */ 
		    	// execute
		        j = insertStatement.executeUpdate(instruction);                               
		        // log
		        if (log_level>=2) { if (j>0) { System.out.println("statement succesfull: "+instruction); } else { System.out.println("statement failure: "+instruction); } }
		        // close statement
		        insertStatement.close();
		        // count
		        r = r + j; i++;
		     }
		    
		    // close db connection
		    dbConnection.close();

		    // records inserted
		    if (log_level>=1) { System.out.println("inserted " + r + " records with timestamp " + dateTime); }
		    
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
}