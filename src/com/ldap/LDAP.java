package com.ldap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

/**
 * LDAP Class
 *
 */
public class LDAP {

	/**
	 * @param args
	 *            the command line arguments
	 * @throws IOException
	 */
	public static void main(String[] args) throws NamingException, Exception {

		if (args == null || args.length < 1) {
			throw new Exception(
					"Usage error : java LDAP [Properties file location]");
		}

		System.out.println("Reading properties from location:" + args[0]);

		File file = new File(args[0]);

		if (!file.exists()) {
			throw new FileNotFoundException("File does not exists!");
		}

		InputStream is = new FileInputStream(file.getPath());
		Properties prop = new Properties();
		prop.load(is);

		String ldapAdServer = prop.getProperty("ldap.path");
		System.out.println("ldap.path:: " + ldapAdServer);
		if (ldapAdServer == null || ldapAdServer.isEmpty()) {
			throw new IllegalArgumentException(
					"Missing key 'ldap.path' from properties file!");
		}

		String ldapSearchBase = prop.getProperty("search.base");
		System.out.println("search.base:: " + ldapSearchBase);
		if (ldapSearchBase == null || ldapSearchBase.isEmpty()) {
			throw new IllegalArgumentException(
					"Missing key 'search.base' from properties file!");
		}

		String ldapUsername = prop.getProperty("ldap.user");
		System.out.println("ldap.user:: " + ldapUsername);
		if (ldapUsername == null || ldapUsername.isEmpty()) {
			throw new IllegalArgumentException(
					"Missing key 'ldap.user' from properties file!");
		}

		String ldapPassword = prop.getProperty("ldap.password");
		System.out.println("ldap.password:: " + ldapPassword);

		if (ldapPassword == null || ldapPassword.isEmpty()) {
			throw new IllegalArgumentException(
					"Missing key 'ldap.password' from properties file!");
		}

		System.out.println("Initializing!!");
		Hashtable<String, Object> env = new Hashtable<String, Object>();
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		if (ldapUsername != null) {
			env.put(Context.SECURITY_PRINCIPAL, ldapUsername);
		}
		if (ldapPassword != null) {
			env.put(Context.SECURITY_CREDENTIALS, ldapPassword);
		}
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, ldapAdServer);

		// ensures that objectSID attribute values
		// will be returned as a byte[] instead of a String
		env.put("java.naming.ldap.attributes.binary", "objectSID");

		// the following is helpful in debugging errors
		// env.put("com.sun.jndi.ldap.trace.ber", System.err);

		LdapContext ctx = new InitialLdapContext(env, null);

		LDAP ldap = new LDAP();

		try {
			// 1) lookup the ldap account
			ldap.processUsers(ctx, ldapSearchBase);
		} catch (Exception ex) {
			System.out.println("Unable to process request ." + ex.getMessage());
			throw ex;
		}
		System.out.println("Press any to exit!!");
		System.in.read();
	}

	public SearchResult processUsers(DirContext ctx, String ldapSearchBase)
			throws NamingException, IOException {

		String currentFolder = new java.io.File(".").getCanonicalPath();

		String searchFilter = "(&(|(objectClass=person)(objectClass=user)))";

		SearchControls searchControls = new SearchControls();
		searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

		NamingEnumeration<SearchResult> results = ctx.search(ldapSearchBase,
				searchFilter, searchControls);

		SearchResult searchResult = null;

		String fileName = currentFolder + "\\Users_"
				+ System.currentTimeMillis() + ".csv";
		FileWriter writer = new FileWriter(fileName);

		writer.append("First Name");
		writer.append(',');
		writer.append("Common Name");
		writer.append(',');
		writer.append("Last Name");
		writer.append(',');
		writer.append("Email");
		writer.append('\n');
		int i = 0;
		while (results.hasMoreElements()) {
			searchResult = (SearchResult) results.nextElement();

			System.out.println("Processing user : " + searchResult.getName());
			Attributes attributes = searchResult.getAttributes();
			writer.append(attributes.get("givenName") == null ? "" : attributes
					.get("givenName").get().toString());
			writer.append(',');
			writer.append(attributes.get("cn") == null ? "" : attributes
					.get("cn").get().toString());
			writer.append(',');
			writer.append(attributes.get("sn") == null ? "" : attributes
					.get("sn").get().toString());
			writer.append(',');
			writer.append(attributes.get("mail") == null ? "" : attributes
					.get("mail").get().toString());

			writer.append('\n');
			i++;
		}

		writer.flush();
		writer.close();

		System.out.println("Total Users processed: " + i);

		System.out.println("Created file in the location : " + fileName);

		return searchResult;
	}
}
