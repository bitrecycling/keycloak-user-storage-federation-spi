package de.bitrecycling.keycloak.sqlserver.spi;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

public class UserAdapter extends AbstractUserAdapterFederatedStorage {

  private final UserEntity userEntity;
  private final String keycloakId;

  public UserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, UserEntity userEntity) {
    super(session, realm, model);
    this.userEntity = userEntity;
    this.keycloakId = StorageId.keycloakId(model, userEntity.getId());
  }

  @Override
  public String getId() {
    return keycloakId;
  }

  @Override
  public String getUsername() {
    return userEntity.getUsername();
  }

  @Override
  public void setUsername(String username) {
    userEntity.setUsername(username);
  }

  @Override
  public String getEmail() {
    return userEntity.getEmail();
  }
  
  public String getPassword(){
    return userEntity.getPassword();
  }
}
