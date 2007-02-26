/*
 * $Header$
 * $Revision$
 * $Date$
 * ====================================================================
 *
 *  Copyright 2002-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * [Additional notices, if required by prior licensing conditions]
 *
 */
package org.apache.commons.httpclient;

import org.apache.commons.httpclient.util.URIUtil;
import junit.framework.*;


/**
 * Tests the util.URIUtil class.
 *
 * @author Ortwin Gl�ck
 *
 * @since 2.0
 */
public class TestURIUtil2 extends TestCase {

    public TestURIUtil2(String s) {
        super(s);
    }

    public static Test suite() {
        return new TestSuite(TestURIUtil.class);
    }

    public void testEncodeWithinQuery() {
        String unescaped1=  "abc123+ %_?=&#.�";
        try {
            String stringRet = URIUtil.encodeWithinQuery(unescaped1);
            assertEquals("abc123%2B%20%25_%3F%3D%26%23.%C3%A4", stringRet);
            stringRet = URIUtil.decode(stringRet);
            assertEquals(unescaped1, stringRet);
        } catch(Exception e) {
            System.err.println("Exception thrown:  "+e);
        }
    }
}
