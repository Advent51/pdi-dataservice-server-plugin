/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2015 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.di.trans.dataservice.ui.controller;

import org.pentaho.ui.xul.XulComponent;
import org.pentaho.ui.xul.XulException;
import org.pentaho.ui.xul.binding.BindingFactory;
import org.pentaho.ui.xul.binding.DefaultBindingFactory;
import org.pentaho.ui.xul.components.XulMessageBox;
import org.pentaho.ui.xul.components.XulPromptBox;
import org.pentaho.ui.xul.impl.AbstractXulEventHandler;

/**
 * @author nhudak
 */
public class AbstractController extends AbstractXulEventHandler {

  @SuppressWarnings( "unchecked" ) public <T extends XulComponent> T getElementById( String id ) {
    return (T) document.getElementById( id );
  }

  public XulPromptBox createPromptBox() throws XulException {
    XulPromptBox promptBox = (XulPromptBox) document.createElement( "promptbox" );
    promptBox.setModalParent( xulDomContainer.getOuterContext() );
    return promptBox;
  }

  public XulMessageBox createMessageBox() throws XulException {
    XulMessageBox messageBox = (XulMessageBox) document.createElement( "messagebox" );
    messageBox.setModalParent( xulDomContainer.getOuterContext() );
    return messageBox;
  }

  protected BindingFactory createBindingFactory() {
    DefaultBindingFactory bindingFactory = new DefaultBindingFactory();
    bindingFactory.setDocument( document );
    return bindingFactory;
  }
}
