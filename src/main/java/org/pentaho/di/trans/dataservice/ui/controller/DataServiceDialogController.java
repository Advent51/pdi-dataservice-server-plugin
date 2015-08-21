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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.dataservice.DataServiceMeta;
import org.pentaho.di.trans.dataservice.ui.DataServiceDelegate;
import org.pentaho.di.trans.dataservice.ui.DataServiceDialog;
import org.pentaho.di.trans.dataservice.ui.model.DataServiceModel;
import org.pentaho.ui.xul.XulException;
import org.pentaho.ui.xul.binding.Binding;
import org.pentaho.ui.xul.binding.BindingFactory;
import org.pentaho.ui.xul.components.XulMenuList;
import org.pentaho.ui.xul.components.XulTextbox;
import org.pentaho.ui.xul.containers.XulDialog;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class DataServiceDialogController extends AbstractController {

  public static final String XUL_DIALOG_ID = "dataservice-dialog";
  private final DataServiceDelegate delegate;
  private final DataServiceModel model;
  private DataServiceMeta dataService;

  private static final Class<?> PKG = DataServiceDialog.class;
  private static final String NAME = "dataServiceDialogController";
  {
    setName( NAME );
  }

  public DataServiceDialogController( DataServiceModel model, DataServiceDelegate delegate ) {
    this.delegate = delegate;
    this.model = model;
  }

  public void init() throws InvocationTargetException, XulException, KettleException {
    BindingFactory bindingFactory = createBindingFactory();

    XulTextbox serviceName = (XulTextbox) document.getElementById( "service-name" );
    XulMenuList steps = (XulMenuList) document.getElementById( "trans-steps" );
    steps.setElements( ImmutableList.copyOf( model.getTransMeta().getStepNames() ) );

    bindingFactory.setBindingType( Binding.Type.BI_DIRECTIONAL );

    bindingFactory.createBinding( model, "serviceStep", steps, "selectedItem" ).fireSourceChanged();

    bindingFactory.createBinding( model, "serviceName", serviceName, "value" ).fireSourceChanged();
  }

  public void showTestDialog() {
    delegate.testDataService( model.getDataService() );
  }

  public void saveAndClose() throws KettleException {
    if ( !validate() ) {
      return;
    }

    delegate.save( model.getDataService() );
    close();
  }

  public Boolean validate() throws KettleException {
    List<String> errors = Lists.newArrayList();

    if ( Const.isEmpty( model.getServiceName() ) ) {
      errors.add( BaseMessages.getString( PKG, "DataServiceDialog.NameRequired.Error" ) );
    } else if( !delegate.saveAllowed( model.getServiceName(), dataService ) ) {
      errors.add( BaseMessages.getString( PKG, "DataServiceDialog.AlreadyExists.Error", model.getServiceName() ) );
    }

    if ( Const.isEmpty( model.getServiceStep() ) ) {
      errors.add( BaseMessages.getString( PKG, "DataServiceDialog.StepRequired.Error" ) );
    }

    if ( errors.size() > 0 ) {
      delegate.showErrors(
        BaseMessages.getString( PKG, "DataServiceDialog.Errors.Title" ),
        Joiner.on( '\n' ).join( errors )
      );
      return false;
    }

    return true;
  }

  public void open(){
    getDialog().show();
  }

  public void close() {
    getDialog().hide();
  }

  public XulDialog getDialog() {
    return (XulDialog) getXulDomContainer().getDocumentRoot().getElementById( XUL_DIALOG_ID );
  }

  public void setDataService( DataServiceMeta dataService ) {
    this.dataService = dataService;
  }

}
