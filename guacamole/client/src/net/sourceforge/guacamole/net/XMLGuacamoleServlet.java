
package net.sourceforge.guacamole.net;

/*
 *  Guacamole - Pure JavaScript/HTML VNC Client
 *  Copyright (C) 2010  Michael Jumper
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import net.sourceforge.guacamole.Client;
import net.sourceforge.guacamole.GuacamoleException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class XMLGuacamoleServlet extends GuacamoleServlet {

    @Override
    protected final void handleRequest(GuacamoleSession session, HttpServletRequest request, HttpServletResponse response) throws GuacamoleException {

        response.setContentType("text/xml");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        try {

            // Create document
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();

            // Root element
            Element root = document.createElement("guacamole");
            document.appendChild(root);

            try {
                handleRequest(session, request, root);
            }
            catch (Throwable t) {
                addFatalError(root, t.getMessage());

                // FATAL error ... try to disconnect
                if (session != null) {
                    Client client = session.getClient();
                    try {
                        if (client != null)
                            client.disconnect();
                    }
                    catch (GuacamoleException e) {
                        addFatalError(root, e.getMessage());
                    }
                }
            }

            // Set up transformer
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            //transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            // Write XML using transformer
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            StreamResult result = new StreamResult(bos);
            DOMSource source = new DOMSource(document);
            transformer.transform(source, result);


            bos.flush();

            byte[] xmlData = bos.toByteArray();
            response.setContentLength(xmlData.length);
            OutputStream outputStream = response.getOutputStream();
            outputStream.write(xmlData);

            // Close stream
            outputStream.close();
        }
        catch (ParserConfigurationException e) {
            throw new GuacamoleException(e);
        }
        catch (TransformerConfigurationException e) {
            throw new GuacamoleException(e);
        }
        catch (TransformerException e) {
            throw new GuacamoleException(e);
        }
        catch (IOException e) {
            throw new GuacamoleException(e);
        }


    }

    private void addFatalError(Element root, String message) {
        Element error = root.getOwnerDocument().createElement("error");
        error.setAttribute("type", "fatal");
        error.setTextContent(message);
        root.appendChild(error);
    }

    protected abstract void handleRequest(GuacamoleSession session, ServletRequest request, Element root) throws GuacamoleException;

}
