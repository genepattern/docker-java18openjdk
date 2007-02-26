/*
 * $Header$
 * $Revision$
 * $Date$
 *
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

package org.apache.commons.httpclient.contrib.methods.multipart;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.PartSource;

/** A simple extension to {@link FilePart} that automatically determines the content type
 * of the file.
 * 
 * @author <a href="mailto:adrian@intencha.com">Adrian Sutton</a>
 * @version $Revision $
 * 
 * DISCLAIMER: HttpClient developers DO NOT actively support this component.
 * The component is provided as a reference material, which may be inappropriate
 * to be used without additional customization.
 */
public class ContentTypeFilePart extends FilePart {

    /**
     * ContentTypeFilePart constructor.
     * 
     * @param name the name of the part
     * @param partSource the source for this part
     * @param charset the charset encoding for this part.
     */
    public ContentTypeFilePart(String name, PartSource partSource, String charset) {
        super(name, partSource, ContentType.get(partSource.getFileName()), charset);
    }

    /**
     * ContentTypeFilePart constructor.
     * 
     * @param name the name of the part
     * @param partSource the source for this part
     */
    public ContentTypeFilePart(String name, PartSource partSource) {
        this(name, partSource, null);
    }

    /**
     * ContentTypeFilePart constructor.
     * 
     * @param name the name of the part
     * @param file the file to post
     * @throws FileNotFoundException if the file does not exist
     */
    public ContentTypeFilePart(String name, File file) throws FileNotFoundException {
        this(name, file, null);
    }

    /**
     * ContentTypeFilePart constructor.
     * 
     * @param name the name of the part
     * @param file the file to post
     * @param charset the charset encoding for the file
     * @throws FileNotFoundException
     */
    public ContentTypeFilePart(String name, File file, String charset)
        throws FileNotFoundException {
        super(name, file, ContentType.get(file), charset);
    }

    /**
     * ContentTypeFilePart constructor.
     * 
     * @param name the name of the part
     * @param fileName the file name
     * @param file the file to post
     * @throws FileNotFoundException if the file does not exist
     */
    public ContentTypeFilePart(String name, String fileName, File file)
        throws FileNotFoundException {
        super(name, fileName, file, ContentType.get(fileName), null);
    }

    /**
     * ContentTypeFilePart constructor.
     * 
     * @param name the name of the part
     * @param fileName the file name
     * @param file the file to post
     * @param charset the charset encoding for the file
     * @throws FileNotFoundException if the file does not exist
     */
    public ContentTypeFilePart(String name, String fileName, File file,
        String charset) throws FileNotFoundException {
        super(name, fileName, file, ContentType.get(file), charset);
    }
}
