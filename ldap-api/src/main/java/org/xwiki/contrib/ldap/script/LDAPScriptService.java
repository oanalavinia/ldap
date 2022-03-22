/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.ldap.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.contrib.ldap.XWikiLDAPConfig;
import org.xwiki.contrib.ldap.XWikiLDAPConnection;
import org.xwiki.contrib.ldap.XWikiLDAPException;
import org.xwiki.contrib.ldap.internal.LDAPGroupsCache;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;

/**
 * Script service to expose LDAP utilities to script and templating languages.
 * 
 * @version $Id$
 */
@Component
@Named("ldap")
@Singleton
public class LDAPScriptService implements ScriptService
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    private static final String ERROR_KEY = "scriptservice.ldap.error";

    /**
     * Execution, needed to retrieve the legacy XWiki context.
     */
    @Inject
    private Execution execution;

    @Inject
    private LDAPGroupsCache caches;

    /**
     * @return the XWiki context associated with this execution.
     */
    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }

    // API

    /**
     * Check the LDAP connection.
     * 
     * @param ldapHost the host of the server to connect to.
     * @param ldapPort the port of the server to connect to.
     * @param loginDN the user DN to connect to LDAP server.
     * @param password the password to connect to LDAP server.
     * @param pathToKeys the path to SSL keystore to use.
     * @param ssl if true connect using SSL.
     * @return true if the connection succeed, false otherwise.
     * @since 9.1.1
     */
    @Unstable
    public boolean checkConnection(String ldapHost, int ldapPort, String loginDN, String password, String pathToKeys,
        boolean ssl)
    {
        setError(null);
        XWikiLDAPConnection connection = new XWikiLDAPConnection(new XWikiLDAPConfig(null));
        try {
            connection.open(ldapHost, ldapPort, loginDN, password, pathToKeys, ssl, getXWikiContext());
            return true;
        } catch (XWikiLDAPException e) {
            setError(e);
            return false;
        } finally {
            connection.close();
        }
    }

    /**
     * @return {@code true} if the currently configured authentication class extends or is an instance of
     *         {@link org.xwiki.contrib.ldap.XWikiLDAPAuthServiceImpl}. Returns {@code false} otherwise.
     */
    public boolean isXWikiLDAPAuthenticator()
    {
        return org.xwiki.contrib.ldap.XWikiLDAPAuthServiceImpl.class
            .isAssignableFrom(getXWikiContext().getWiki().getAuthService().getClass());
    }

    /**
     * Force to empty the cache containing LDAP groups.
     * 
     * @since 4.1M1
     */
    public void resetGroupCache()
    {
        this.caches.reset();
    }

    /**
     * Get the error generated while performing the previously called action.
     *
     * @return an eventual exception or {@code null} if no exception was thrown
     * @since 9.1.3
     */
    @Unstable
    public Exception getError()
    {
        return (Exception) this.execution.getContext().getProperty(ERROR_KEY);
    }

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getError()}.
     *
     * @param e the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getError()
     * @since 9.1.3
     */
    @Unstable
    private void setError(Exception e)
    {
        this.execution.getContext().setProperty(ERROR_KEY, e);
    }
}
