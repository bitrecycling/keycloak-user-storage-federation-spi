package de.bitrecycling.keycloak.sqlserver.spi;


import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.storage.UserStorageProviderFactory;

import javax.naming.InitialContext;
import java.util.ArrayList;
import java.util.List;

@JBossLog
public class SimpleUserStorageProviderFactory implements UserStorageProviderFactory<SimpleUserStorageProvider> {

  @Override
  public void init(Config.Scope config) {
    log.warn("spi init");
  }
  
  @Override
  public SimpleUserStorageProvider create(KeycloakSession session, ComponentModel model) {
    
    try {
      InitialContext ctx = new InitialContext();
      SimpleUserStorageProvider provider = (SimpleUserStorageProvider)ctx.lookup("java:global/sqlserver-user-storage-provider/" + SimpleUserStorageProvider.class.getSimpleName());
      provider.setModel(model);
      provider.setSession(session);
      return provider;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    
  }

  @Override
  public String getId() {
    return "demp-db";
  }

  @Override
  public List<ProviderConfigProperty> getConfigProperties() {
    log.warn("getConfigProperties");
    
    
//    log.warn(userRepository.findUserByUsername("demo").toString());
    return new ArrayList<>();
  }
}
