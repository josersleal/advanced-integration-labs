package org.jboss.fuse.security.role;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestPropertyDefinition;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.eclipse.jetty.jaas.JAASLoginService;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.util.security.Constraint;
import org.jboss.fuse.security.common.BaseJettyTest;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BasicAuthRESTCamelDSLJettyJaasRoleContraintTest extends BaseJettyTest {

    private static String HOST = "localhost";
    private static int PORT = getPort1();

    @Override protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry jndi = super.createRegistry();
        jndi.bind("myAuthHandler", getSecurityHandler());
        return jndi;
    }

    @Before public void init() throws IOException {
        URL jaasURL = BasicAuthRESTCamelDSLJettyJaasRoleContraintTest.class.getResource("myrealm-jaas.cfg");
        System.setProperty("java.security.auth.login.config", jaasURL.toExternalForm());
    }




    private HttpResult callRestEndpoint(String host, String url, String user, String password, String realm) {

        HttpResult response = new HttpResult();

        // Define the Get Method with the String of the url to access the HTTP Resource
        GetMethod get = new GetMethod(url);

        // Set Credentials
        Credentials creds = new UsernamePasswordCredentials(user, password);
        // Auth Scope
        AuthScope authScope = new AuthScope(host, PORT, realm);

        // Execute request
        try {
            // Get HTTP client
            HttpClient httpclient = new HttpClient();
            // Use preemptive to select BASIC Auth
            httpclient.getParams().setAuthenticationPreemptive(true);
            httpclient.getState().setCredentials(authScope, creds);
            response.setCode(httpclient.executeMethod(get));

            InputStream is = get.getResponseBodyAsStream();
            response.setMessage(inputStreamToString(is));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Release current connection to the connection pool once you are done
            get.releaseConnection();
        }

        return response;
    }


    private SecurityHandler getSecurityHandler() throws IOException {

        /* A security handler is a jetty handler that secures content behind a
         *  particular portion of a url space. The ConstraintSecurityHandler is a
         *  more specialized handler that allows matching of urls to different
         *  constraints. The server sets this as the first handler in the chain,
         *  effectively applying these constraints to all subsequent handlers in
         *  the chain.
         *  The BasicAuthenticator instance is the object that actually checks the credentials
         */
        ConstraintSecurityHandler sh = new ConstraintSecurityHandler();
        sh.setAuthenticator(new BasicAuthenticator());
        sh.setConstraintMappings(getConstraintMappings());

        /*
         * The DefaultIdentityService service handles only role reference maps passed in an
         * associated org.eclipse.jetty.server.UserIdentity.Scope.  If there are roles
         * refs present, then associate will wrap the UserIdentity with one that uses the role references in the
         * org.eclipse.jetty.server.UserIdentity#isUserInRole(String, org.eclipse.jetty.server.UserIdentity.Scope)}
         * implementation.
         *
        */
        DefaultIdentityService dis = new DefaultIdentityService();

        // Service which create a UserRealm suitable for use with JAAS
        JAASLoginService loginService = new JAASLoginService();
        loginService.setName("myrealm");
        loginService.setLoginModuleName("propsFileModule");
        loginService.setIdentityService(dis);

        sh.setLoginService(loginService);
        sh.setConstraintMappings(getConstraintMappings());

        return sh;
    }

    private List<ConstraintMapping> getConstraintMappings() {

        // Access allowed for role Admin
        Constraint constraint0 = new Constraint(Constraint.__BASIC_AUTH, "user");
        constraint0.setAuthenticate(true);
        constraint0.setName("allowedForAll");
        constraint0.setRoles(new String[] { "user", "admin" });
        ConstraintMapping mapping0 = new ConstraintMapping();
        mapping0.setPathSpec("/say/hello/*");
        mapping0.setMethod("GET");
        mapping0.setConstraint(constraint0);

        Constraint constraint1 = new Constraint();
        constraint1.setAuthenticate(true);
        constraint1.setName("allowedForRoleAdmin");
        constraint1.setRoles(new String[]{ "admin" });
        ConstraintMapping mapping1 = new ConstraintMapping();
        mapping1.setPathSpec("/say/bye/*");
        mapping1.setMethod("GET");
        mapping1.setConstraint(constraint1);

        return Arrays.asList(mapping0, mapping1);
    }

}
